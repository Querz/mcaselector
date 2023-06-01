package net.querz.mcaselector.io.job;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.querz.mcaselector.config.ConfigProvider;
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.io.JobHandler;
import net.querz.mcaselector.io.RegionDirectories;
import net.querz.mcaselector.io.mca.Region;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.progress.Progress;
import net.querz.mcaselector.progress.Timer;
import net.querz.mcaselector.selection.ChunkSet;
import net.querz.mcaselector.selection.Selection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.function.Consumer;

public final class SelectionDeleter {

	private static final Logger LOGGER = LogManager.getLogger(SelectionDeleter.class);

	private SelectionDeleter() {}

	public static void deleteSelection(Selection selection, Progress progressChannel) {
		if (selection.isEmpty()) {
			progressChannel.done("no selection");
			return;
		}

		JobHandler.clearQueues();

		progressChannel.setMessage("preparing");

		Selection trueSelection = selection.getTrueSelection(ConfigProvider.WORLD.getWorldDirs());

		progressChannel.setMax(trueSelection.size());

		Point2i first = trueSelection.one();

		progressChannel.updateProgress(FileHelper.createMCAFileName(first), 0);

		Consumer<Throwable> errorHandler = t -> progressChannel.incrementProgress("error");

		for (Long2ObjectMap.Entry<ChunkSet> entry : trueSelection) {
			MCADeleteSelectionProcessJob job = new MCADeleteSelectionProcessJob(FileHelper.createRegionDirectories(new Point2i(entry.getLongKey())), entry.getValue(), progressChannel);
			job.errorHandler = errorHandler;
			JobHandler.addJob(job);
		}
	}

	private static class MCADeleteSelectionProcessJob extends ProcessDataJob {

		private final Progress progressChannel;
		private final ChunkSet selection;

		private MCADeleteSelectionProcessJob(RegionDirectories dirs, ChunkSet selection, Progress progressChannel) {
			super(dirs, PRIORITY_LOW);
			this.selection = selection;
			this.progressChannel = progressChannel;
		}

		@Override
		public boolean execute() {
			// delete whole files if everything is selected
			if (selection == null) {
				// delete region
				if (getRegionDirectories().getRegion().delete()) {
					LOGGER.debug("deleted file {}", getRegionDirectories().getRegion());
				} else {
					LOGGER.warn("failed to delete file {}", getRegionDirectories().getRegion());
				}

				// delete poi
				if (getRegionDirectories().getPoi().delete()) {
					LOGGER.debug("deleted file {}", getRegionDirectories().getPoi());
				} else {
					LOGGER.warn("failed to delete file {}", getRegionDirectories().getPoi());
				}

				// delete entities
				if (getRegionDirectories().getEntities().delete()) {
					LOGGER.debug("deleted file {}", getRegionDirectories().getEntities());
				} else {
					LOGGER.warn("failed to delete file {}", getRegionDirectories().getEntities());
				}

				progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
				return true;
			}

			byte[] regionData = loadRegionHeader();
			byte[] poiData = loadPoiHeader();
			byte[] entitiesData = loadEntitiesHeader();

			if (regionData == null && poiData == null && entitiesData == null) {
				LOGGER.warn("failed to load any data from {}", getRegionDirectories().getLocationAsFileName());
				progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
				return true;
			}

			// load MCAFile
			try {
				// only load headers, we don't care for chunk contents
				Region region = Region.loadRegionHeaders(getRegionDirectories(), regionData, poiData, entitiesData);

				region.deleteChunks(selection);

				MCADeleteSelectionSaveJob job = new MCADeleteSelectionSaveJob(getRegionDirectories(), region, progressChannel);
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

	private static class MCADeleteSelectionSaveJob extends SaveDataJob<Region> {

		private final Progress progressChannel;

		private MCADeleteSelectionSaveJob(RegionDirectories dirs, Region region, Progress progressChannel) {
			super(dirs, region);
			this.progressChannel = progressChannel;
		}

		@Override
		public void execute() {
			Timer t = new Timer();
			try {
				getData().deFragment();
			} catch (Exception ex) {
				LOGGER.warn("failed to delete selected chunks from {}", getRegionDirectories().getLocationAsFileName(), ex);
			}
			progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
			LOGGER.debug("took {} to save data for {}", t, getRegionDirectories().getLocationAsFileName());
		}
	}
}
