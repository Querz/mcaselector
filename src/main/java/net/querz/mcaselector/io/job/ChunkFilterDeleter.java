package net.querz.mcaselector.io.job;

import net.querz.mcaselector.config.ConfigProvider;
import net.querz.mcaselector.filter.filters.GroupFilter;
import net.querz.mcaselector.io.JobHandler;
import net.querz.mcaselector.io.RegionDirectories;
import net.querz.mcaselector.io.WorldDirectories;
import net.querz.mcaselector.io.mca.Region;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.progress.Progress;
import net.querz.mcaselector.selection.Selection;
import net.querz.mcaselector.text.Translation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.function.Consumer;

public final class ChunkFilterDeleter {

	private static final Logger LOGGER = LogManager.getLogger(ChunkFilterDeleter.class);

	private ChunkFilterDeleter() {}

	public static void deleteFilter(GroupFilter filter, Selection selection, Progress progressChannel, boolean headless) {
		WorldDirectories wd = ConfigProvider.WORLD.getWorldDirs();
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

		Consumer<Throwable> errorHandler = t -> progressChannel.incrementProgress("error");

		for (RegionDirectories r : rd) {
			MCADeleteFilterProcessJob job = new MCADeleteFilterProcessJob(r, filter, selection, progressChannel);
			job.errorHandler = errorHandler;
			JobHandler.addJob(job);
		}
	}

	private static class MCADeleteFilterProcessJob extends ProcessDataJob {

		private final Progress progressChannel;
		private final GroupFilter filter;
		private final Selection selection;

		private MCADeleteFilterProcessJob(RegionDirectories dirs, GroupFilter filter, Selection selection, Progress progressChannel) {
			super(dirs, PRIORITY_LOW);
			this.filter = filter;
			this.selection = selection;
			this.progressChannel = progressChannel;
		}

		@Override
		public boolean execute() {
			// load all files
			Point2i location = getRegionDirectories().getLocation();

			if (!filter.appliesToRegion(location) || selection != null && !selection.isAnyChunkInRegionSelected(location)) {
				LOGGER.debug("filter does not apply to region {}", getRegionDirectories().getLocation());
				progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
				return true;
			}

			byte[] regionData = loadRegion();
			byte[] poiData = loadPoi();
			byte[] entitiesData = loadEntities();

			if (regionData == null && poiData == null && entitiesData == null) {
				LOGGER.warn("failed to load any data from {}", getRegionDirectories().getLocationAsFileName());
				progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
				return true;
			}


			try {
				// parse raw data
				Region region = Region.loadRegion(getRegionDirectories(), regionData, poiData, entitiesData);

				if (region.deleteChunks(filter, selection)) {
					// only save file if we actually deleted something
					MCADeleteFilterSaveJob job = new MCADeleteFilterSaveJob(getRegionDirectories(), region, progressChannel);
					job.errorHandler = errorHandler;
					JobHandler.executeSaveData(job);
					return false;
				} else {
					progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
					LOGGER.debug("nothing to delete in {}, not saving", getRegionDirectories().getLocationAsFileName());
				}
			} catch (Exception ex) {
				progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
				LOGGER.warn("error deleting chunk indices in {}", getRegionDirectories().getLocationAsFileName());
			}
			return true;
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
			try {
				getData().deFragment();
			} catch (Exception ex) {
				LOGGER.warn("failed to delete filtered chunks from {}", getRegionDirectories().getLocationAsFileName(), ex);
			}
			progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
		}
	}
}
