package net.querz.mcaselector.io;

import net.querz.mcaselector.Config;
import net.querz.mcaselector.filter.GroupFilter;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.io.mca.Region;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.progress.Progress;
import net.querz.mcaselector.progress.Timer;
import net.querz.mcaselector.text.Translation;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

public class ChunkFilterExporter {

	private ChunkFilterExporter() {}

	public static void exportFilter(GroupFilter filter, SelectionData selection, File destination, Progress progressChannel, boolean headless) {
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

		// make sure that target directories exist
		try {
			createDirectoryOrThrowException(destination, "region");
			createDirectoryOrThrowException(destination, "poi");
			createDirectoryOrThrowException(destination, "entities");
		} catch (IOException ex) {
			Debug.dumpException("failed to create directories", ex);
			return;
		}

		MCAFilePipe.clearQueues();

		Map<Point2i, Set<Point2i>> sel = SelectionHelper.getTrueSelection(selection);

		progressChannel.setMax(rd.length);
		progressChannel.updateProgress(rd[0].getLocationAsFileName(), 0);

		for (RegionDirectories r : rd) {
			MCAFilePipe.addJob(new MCAExportFilterLoadJob(r, filter, sel, destination, progressChannel));
		}
	}

	private static void createDirectoryOrThrowException(File dir, String folder) throws IOException {
		File d = new File(dir, folder);
		if (!d.exists() && !d.mkdirs()) {
			throw new IOException("failed to create directory " + d);
		}
	}

	private static class MCAExportFilterLoadJob extends LoadDataJob {

		private final GroupFilter filter;
		private final Map<Point2i, Set<Point2i>> selection;
		private final Progress progressChannel;
		private final File destination;

		private MCAExportFilterLoadJob(RegionDirectories dirs, GroupFilter filter, Map<Point2i, Set<Point2i>> selection, File destination, Progress progressChannel) {
			super(dirs);
			this.filter = filter;
			this.selection = selection;
			this.destination = destination;
			this.progressChannel = progressChannel;
		}

		@Override
		public void execute() {
			Point2i location = getRegionDirectories().getLocation();

			if (!filter.appliesToRegion(location) || selection != null && !selection.containsKey(location)) {
				Debug.dump("filter does not apply to region " + getRegionDirectories().getLocation());
				progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
				return;
			}

			File toRegion = new File(destination, "region/" + getRegionDirectories().getLocationAsFileName());
			File toPoi = new File(destination, "region/" + getRegionDirectories().getLocationAsFileName());
			File toEntities = new File(destination, "region/" + getRegionDirectories().getLocationAsFileName());
			if (toRegion.exists() || toPoi.exists() || toEntities.exists()) {
				Debug.dumpf("%s exists, not overwriting", getRegionDirectories().getLocationAsFileName());
				progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
				return;
			}

			RegionDirectories to = new RegionDirectories(getRegionDirectories().getLocation(), toRegion, toPoi, toEntities);

			byte[] regionData = loadRegion();
			byte[] poiData = loadPOI();
			byte[] entitiesData = loadEntities();

			if (regionData == null && poiData == null && entitiesData == null) {
				Debug.errorf("failed to load any data from %s", getRegionDirectories().getLocationAsFileName());
				progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
			} else {
				MCAFilePipe.executeProcessData(new MCAExportFilterProcessJob(getRegionDirectories(), regionData, poiData, entitiesData, filter, selection == null ? null : selection.get(location), to, progressChannel));
			}
		}
	}

	private static class MCAExportFilterProcessJob extends ProcessDataJob {

		private final Progress progressChannel;
		private final GroupFilter filter;
		private final Set<Point2i> selection;
		private final RegionDirectories to;

		private MCAExportFilterProcessJob(RegionDirectories dirs, byte[] regionData, byte[] poiData, byte[] entitiesData, GroupFilter filter, Set<Point2i> selection, RegionDirectories to, Progress progressChannel) {
			super(dirs, regionData, poiData, entitiesData);
			this.filter = filter;
			this.selection = selection;
			this.to = to;
			this.progressChannel = progressChannel;
		}

		@Override
		public void execute() {
			//load MCAFile
			try {
				Region region = Region.loadRegion(getRegionDirectories(), getRegionData(), getPoiData(), getEntitiesData());

				region.keepChunks(filter, selection);

				MCAFilePipe.executeSaveData(new MCAExportFilterSaveJob(getRegionDirectories(), region, to, progressChannel));

			} catch (Exception ex) {
				progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
				Debug.errorf("error deleting chunk indices in %s", getRegionDirectories().getLocationAsFileName());
			}
		}
	}

	private static class MCAExportFilterSaveJob extends SaveDataJob<Region> {

		private final RegionDirectories to;
		private final Progress progressChannel;

		private MCAExportFilterSaveJob(RegionDirectories src, Region region, RegionDirectories to, Progress progressChannel) {
			super(src, region);
			this.to = to;
			this.progressChannel = progressChannel;
		}

		@Override
		public void execute() {
			Timer t = new Timer();
			try {
				getData().saveWithTempFiles(to);
			} catch (Exception ex) {
				Debug.dumpException("failed to save exported filtered chunks in " + getRegionDirectories().getLocationAsFileName(), ex);
			}
			progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
			Debug.dumpf("took %s to save data for %s", t, getRegionDirectories().getLocationAsFileName());
		}
	}
}
