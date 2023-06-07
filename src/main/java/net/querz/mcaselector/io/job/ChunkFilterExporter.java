package net.querz.mcaselector.io.job;

import net.querz.mcaselector.config.ConfigProvider;
import net.querz.mcaselector.filter.filters.GroupFilter;
import net.querz.mcaselector.io.JobHandler;
import net.querz.mcaselector.io.RegionDirectories;
import net.querz.mcaselector.io.WorldDirectories;
import net.querz.mcaselector.io.mca.Region;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.progress.Progress;
import net.querz.mcaselector.progress.Timer;
import net.querz.mcaselector.selection.Selection;
import net.querz.mcaselector.text.Translation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.File;
import java.util.function.Consumer;

public final class ChunkFilterExporter {

	private static final Logger LOGGER = LogManager.getLogger(ChunkFilterExporter.class);

	private ChunkFilterExporter() {}

	public static void exportFilter(GroupFilter filter, Selection selection, WorldDirectories destination, Progress progressChannel, boolean headless) {
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
			MCAExportFilterProcessJob job = new MCAExportFilterProcessJob(r, filter, selection, destination, progressChannel);
			job.errorHandler = errorHandler;
			JobHandler.addJob(job);
		}
	}

	private static class MCAExportFilterProcessJob extends ProcessDataJob {

		private final Progress progressChannel;
		private final GroupFilter filter;
		private final Selection selection;
		private final WorldDirectories destination;

		private MCAExportFilterProcessJob(RegionDirectories dirs, GroupFilter filter, Selection selection, WorldDirectories destination, Progress progressChannel) {
			super(dirs, PRIORITY_LOW);
			this.filter = filter;
			this.selection = selection;
			this.destination = destination;
			this.progressChannel = progressChannel;
		}

		@Override
		public boolean execute() {
			Point2i location = getRegionDirectories().getLocation();

			if (!filter.appliesToRegion(location) || selection != null && !selection.isAnyChunkInRegionSelected(location)) {
				LOGGER.debug("filter does not apply to region {}", getRegionDirectories().getLocation());
				progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
				return true;
			}

			File toRegion = new File(destination.getRegion(), getRegionDirectories().getLocationAsFileName());
			File toPoi = new File(destination.getPoi(), getRegionDirectories().getLocationAsFileName());
			File toEntities = new File(destination.getEntities(), getRegionDirectories().getLocationAsFileName());
			if (toRegion.exists() || toPoi.exists() || toEntities.exists()) {
				LOGGER.debug("{} exists, not overwriting", getRegionDirectories().getLocationAsFileName());
				progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
				return true;
			}

			RegionDirectories to = new RegionDirectories(getRegionDirectories().getLocation(), toRegion, toPoi, toEntities);

			byte[] regionData = loadRegion();
			byte[] poiData = loadPoi();
			byte[] entitiesData = loadEntities();

			if (regionData == null && poiData == null && entitiesData == null) {
				LOGGER.warn("failed to load any data from {}", getRegionDirectories().getLocationAsFileName());
				progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
				return true;
			}


			// load MCAFile
			try {
				Region region = Region.loadRegion(getRegionDirectories(), regionData, poiData, entitiesData);

				region.keepChunks(filter, selection);

				MCAExportFilterSaveJob job = new MCAExportFilterSaveJob(getRegionDirectories(), region, to, progressChannel);
				job.errorHandler = errorHandler;
				JobHandler.executeSaveData(job);
				return false;
			} catch (Exception ex) {
				progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
				LOGGER.warn("error deleting chunk indices in {}", getRegionDirectories().getLocationAsFileName());
			}
			return true;
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
				getData().deFragment(to);
			} catch (Exception ex) {
				LOGGER.warn("failed to save exported filtered chunks in {}", getRegionDirectories().getLocationAsFileName(), ex);
			}
			progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
			LOGGER.debug("took {} to save data for {}", t, getRegionDirectories().getLocationAsFileName());
		}
	}
}
