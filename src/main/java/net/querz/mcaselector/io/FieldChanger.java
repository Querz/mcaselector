package net.querz.mcaselector.io;

import net.querz.mcaselector.Config;
import net.querz.mcaselector.changer.Field;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.io.mca.Region;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.progress.Progress;
import net.querz.mcaselector.progress.Timer;
import net.querz.mcaselector.text.Translation;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FieldChanger {

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

		MCAFilePipe.clearQueues();

		Map<Point2i, Set<Point2i>> sel = SelectionHelper.getTrueSelection(selection);

		progressChannel.setMax(rd.length);
		progressChannel.updateProgress(rd[0].getLocationAsFileName(), 0);

		for (RegionDirectories r : rd) {
			MCAFilePipe.addJob(new MCAFieldChangeLoadJob(r, fields, force, sel, progressChannel));
		}
	}

	public static class MCAFieldChangeLoadJob extends LoadDataJob {

		private final Progress progressChannel;
		private final List<Field<?>> fields;
		private final boolean force;
		private final Map<Point2i, Set<Point2i>> selection;

		private MCAFieldChangeLoadJob(RegionDirectories dirs, List<Field<?>> fields, boolean force, Map<Point2i, Set<Point2i>> selection, Progress progressChannel) {
			super(dirs);
			this.fields = fields;
			this.force = force;
			this.selection = selection;
			this.progressChannel = progressChannel;
		}

		@Override
		public void execute() {
			Set<Point2i> chunks = null;
			if (selection != null) {
				Point2i location = getRegionDirectories().getLocation();
				if (!selection.containsKey(location)) {
					Debug.dumpf("will not apply nbt changes to %s", getRegionDirectories().getLocationAsFileName());
					progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
					return;
				}
				chunks = selection.get(location);
			}

			byte[] regionData = loadRegion();
			byte[] poiData = loadPOI();
			byte[] entitiesData = loadEntities();

			if (regionData == null && poiData == null && entitiesData == null) {
				Debug.errorf("failed to load any data from %s", getRegionDirectories().getLocationAsFileName());
				progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
			} else {
				MCAFilePipe.executeProcessData(new MCAFieldChangeProcessJob(getRegionDirectories(), regionData, poiData, entitiesData, fields, force, chunks, progressChannel));
			}
		}
	}

	public static class MCAFieldChangeProcessJob extends ProcessDataJob {

		private final Progress progressChannel;
		private final List<Field<?>> fields;
		private final boolean force;
		private final Set<Point2i> selection;

		private MCAFieldChangeProcessJob(RegionDirectories dirs, byte[] regionData, byte[] poiData, byte[] entitiesData, List<Field<?>> fields, boolean force, Set<Point2i> selection, Progress progressChannel) {
			super(dirs, regionData, poiData, entitiesData);
			this.fields = fields;
			this.force = force;
			this.selection = selection;
			this.progressChannel = progressChannel;
		}

		@Override
		public void execute() {
			//load MCAFile
			try {
				Region region = Region.loadRegion(getRegionDirectories(), getRegionData(), getPoiData(), getEntitiesData());

				region.applyFieldChanges(fields, force, selection);

				MCAFilePipe.executeSaveData(new MCAFieldChangeSaveJob(getRegionDirectories(), region, progressChannel));
			} catch (Exception ex) {
				progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
				Debug.errorf("error changing fields in %s", getRegionDirectories().getLocationAsFileName());
				ex.printStackTrace();
			}
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
