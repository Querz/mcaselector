package net.querz.mcaselector.io.job;

import net.querz.mcaselector.Config;
import net.querz.mcaselector.changer.Field;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.io.JobHandler;
import net.querz.mcaselector.io.RegionDirectories;
import net.querz.mcaselector.io.SelectionData;
import net.querz.mcaselector.io.WorldDirectories;
import net.querz.mcaselector.io.mca.Region;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.progress.Progress;
import net.querz.mcaselector.progress.Timer;
import net.querz.mcaselector.text.Translation;
import java.util.List;

public final class FieldChanger {

	private FieldChanger() {}

	public static void changeNBTFields(List<Field<?>> fields, boolean force, SelectionData selection, Progress progressChannel, boolean headless) {
		WorldDirectories wd = Config.getWorldDirs();
		RegionDirectories[] rd = wd.listRegions(selection);
		if (rd == null || rd.length == 0) {
			if (headless) {
				progressChannel.done("no files");
			} else {
				progressChannel.done(Translation.DIALOG_PROGRESS_NO_FILES.toString());
			}
			return;
		}

		JobHandler.clearQueues();

		progressChannel.setMax(rd.length);
		progressChannel.updateProgress(rd[0].getLocationAsFileName(), 0);

		for (RegionDirectories r : rd) {
			JobHandler.addJob(new MCAFieldChangeProcessJob(r, fields, force, selection, progressChannel));
		}
	}

	public static class MCAFieldChangeProcessJob extends ProcessDataJob {

		private final Progress progressChannel;
		private final List<Field<?>> fields;
		private final boolean force;
		private final SelectionData selection;

		private MCAFieldChangeProcessJob(RegionDirectories dirs, List<Field<?>> fields, boolean force, SelectionData selection, Progress progressChannel) {
			super(dirs, PRIORITY_LOW);
			this.fields = fields;
			this.force = force;
			this.selection = selection;
			this.progressChannel = progressChannel;
		}

		@Override
		public boolean execute() {
			if (selection != null) {
				Point2i location = getRegionDirectories().getLocation();
				if (!selection.isRegionSelected(location)) {
					Debug.dumpf("will not apply nbt changes to %s", getRegionDirectories().getLocationAsFileName());
					progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
					return true;
				}
			}

			byte[] regionData = loadRegion();
			byte[] poiData = loadPoi();
			byte[] entitiesData = loadEntities();

			if (regionData == null && poiData == null && entitiesData == null) {
				Debug.errorf("failed to load any data from %s", getRegionDirectories().getLocationAsFileName());
				progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
				return true;
			}

			//load MCAFile
			try {
				Region region = Region.loadRegion(getRegionDirectories(), regionData, poiData, entitiesData);

				region.applyFieldChanges(fields, force, selection);

				JobHandler.executeSaveData(new MCAFieldChangeSaveJob(getRegionDirectories(), region, progressChannel));
				return false;
			} catch (Exception ex) {
				progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
				Debug.dumpException("error changing fields in " + getRegionDirectories().getLocationAsFileName(), ex);
			}
			return true;
		}
	}

	public static class MCAFieldChangeSaveJob extends SaveDataJob<Region> {

		private final Progress progressChannel;

		private MCAFieldChangeSaveJob(RegionDirectories file, Region region, Progress progressChannel) {
			super(file, region);
			this.progressChannel = progressChannel;
		}

		@Override
		public void execute() {
			Timer t = new Timer();
			try {
				getData().saveWithTempFiles();
			} catch (Exception ex) {
				Debug.dumpException("failed to save changed fields for " + getRegionDirectories().getLocationAsFileName(), ex);
			}
			progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
			Debug.dumpf("took %s to save data for %s", t, getRegionDirectories().getLocationAsFileName());
		}
	}
}
