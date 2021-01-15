package net.querz.mcaselector.io;

import net.querz.mcaselector.Config;
import net.querz.mcaselector.filter.GroupFilter;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.io.mca.Region;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.progress.Progress;
import net.querz.mcaselector.progress.Timer;
import net.querz.mcaselector.text.Translation;
import java.util.Map;
import java.util.Set;

public class ChunkFilterDeleter {

	private ChunkFilterDeleter() {}

	public static void deleteFilter(GroupFilter filter, SelectionData selection, Progress progressChannel, boolean headless) {
		WorldDirectories wd = Config.getWorldDirs();
		RegionDirectories[] rd = wd.listRegions();
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
			MCAFilePipe.addJob(new MCADeleteFilterLoadJob(r, filter, sel, progressChannel));
		}
	}

	private static class MCADeleteFilterLoadJob extends LoadDataJob {

		private final GroupFilter filter;
		private final Map<Point2i, Set<Point2i>> selection;
		private final Progress progressChannel;

		private MCADeleteFilterLoadJob(RegionDirectories rd, GroupFilter filter, Map<Point2i, Set<Point2i>> selection, Progress progressChannel) {
			super(rd);
			this.filter = filter;
			this.selection = selection;
			this.progressChannel = progressChannel;
		}

		@Override
		public void execute() {
			// load all files
			Point2i location = getRegionDirectories().getLocation();

			if (!filter.appliesToRegion(location) || selection != null && !selection.containsKey(location)) {
				Debug.dump("filter does not apply to region " + getRegionDirectories().getLocation());
				progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
				return;
			}

			byte[] regionData = loadRegion();
			byte[] poiData = loadPOI();
			byte[] entitiesData = loadEntities();

			if (regionData == null && poiData == null && entitiesData == null) {
				Debug.errorf("failed to load any data from %s", getRegionDirectories().getLocationAsFileName());
				progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
			} else {
				MCAFilePipe.executeProcessData(new MCADeleteFilterProcessJob(getRegionDirectories(), regionData, poiData, entitiesData, filter, selection != null ? selection.get(location) : null, progressChannel));
			}
		}
	}

	private static class MCADeleteFilterProcessJob extends ProcessDataJob {

		private final Progress progressChannel;
		private final GroupFilter filter;
		private final Set<Point2i> selection;

		private MCADeleteFilterProcessJob(RegionDirectories dirs, byte[] regionData, byte[] poiData, byte[] entitiesData, GroupFilter filter, Set<Point2i> selection, Progress progressChannel) {
			super(dirs, regionData, poiData, entitiesData);
			this.filter = filter;
			this.selection = selection;
			this.progressChannel = progressChannel;
		}

		@Override
		public void execute() {
			//load MCAFile
			try {
				// parse raw data
				Region region = Region.loadRegion(getRegionDirectories(), getRegionData(), getPoiData(), getEntitiesData());

				region.deleteChunks(filter, selection);

				MCAFilePipe.executeSaveData(new MCADeleteFilterSaveJob(getRegionDirectories(), region, progressChannel));

			} catch (Exception ex) {
				progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
				Debug.errorf("error deleting chunk indices in %s", getRegionDirectories().getLocationAsFileName());
			}
		}
	}

	private static class MCADeleteFilterSaveJob extends SaveDataJob<Region> {

		private final Progress progressChannel;

		private MCADeleteFilterSaveJob(RegionDirectories dirs, Region region, Progress progressChannel) {
			super(dirs, region);
			this.progressChannel = progressChannel;
		}

		@Override
		public void execute() {
			Timer t = new Timer();
			try {
				getData().saveWithTempFiles();
			} catch (Exception ex) {
				Debug.dumpException("failed to delete filtered chunks from " + getRegionDirectories().getLocationAsFileName(), ex);
			}
			progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
			Debug.dumpf("took %s to save data for %s", t, getRegionDirectories().getLocationAsFileName());
		}
	}
}
