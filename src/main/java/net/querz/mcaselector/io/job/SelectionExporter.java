package net.querz.mcaselector.io.job;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.io.JobHandler;
import net.querz.mcaselector.io.RegionDirectories;
import net.querz.mcaselector.io.WorldDirectories;
import net.querz.mcaselector.io.mca.Region;
import net.querz.mcaselector.selection.ChunkSet;
import net.querz.mcaselector.selection.Selection;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.progress.Progress;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.function.Consumer;

public final class SelectionExporter {

	private static final Logger LOGGER = LogManager.getLogger(SelectionExporter.class);

	private SelectionExporter() {}

	public static void exportSelection(Selection selection, WorldDirectories destination, Progress progressChannel) {
		if (selection.isEmpty()) {
			progressChannel.done("no selection");
			return;
		}

		JobHandler.clearQueues();

		Selection trueSelection = selection.getTrueSelection(destination);

		progressChannel.setMax(trueSelection.size());
		progressChannel.updateProgress(FileHelper.createMCAFileName(trueSelection.one()), 0);

		Consumer<Throwable> errorHandler = t -> progressChannel.incrementProgress("error");

		for (Long2ObjectMap.Entry<ChunkSet> entry : trueSelection) {
			MCADeleteSelectionProcessJob job = new MCADeleteSelectionProcessJob(
					FileHelper.createRegionDirectories(new Point2i(entry.getLongKey())),
					entry.getValue(),
					destination,
					progressChannel);
			job.errorHandler = errorHandler;
			JobHandler.addJob(job);
		}
	}

	private static class MCADeleteSelectionProcessJob extends ProcessDataJob {

		private final Progress progressChannel;
		private final ChunkSet chunksToBeExported;
		private final WorldDirectories destination;

		private MCADeleteSelectionProcessJob(RegionDirectories dirs, ChunkSet chunksToBeExported, WorldDirectories destination, Progress progressChannel) {
			super(dirs, PRIORITY_LOW);
			this.chunksToBeExported = chunksToBeExported;
			this.destination = destination;
			this.progressChannel = progressChannel;
		}

		@Override
		public boolean execute() {
			File toRegion = new File(destination.getRegion(), getRegionDirectories().getLocationAsFileName());
			File toPoi = new File(destination.getPoi(), getRegionDirectories().getLocationAsFileName());
			File toEntities = new File(destination.getEntities(), getRegionDirectories().getLocationAsFileName());
			if (toRegion.exists() || toPoi.exists() || toEntities.exists()) {
				LOGGER.debug("{} exists, not overwriting", getRegionDirectories().getLocationAsFileName());
				progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
				return true;
			}

			RegionDirectories to = new RegionDirectories(getRegionDirectories().getLocation(), toRegion, toPoi, toEntities);

			if (chunksToBeExported == null) {

				// copy region
				try {
					Files.copy(getRegionDirectories().getRegion().toPath(), to.getRegion().toPath(), StandardCopyOption.REPLACE_EXISTING);
					LOGGER.debug("copied file {}", getRegionDirectories().getRegion());
				} catch (Exception ex) {
					LOGGER.warn("failed to copy file {}", getRegionDirectories().getRegion(), ex);
				}

				// copy poi
				try {
					Files.copy(getRegionDirectories().getPoi().toPath(), to.getPoi().toPath(), StandardCopyOption.REPLACE_EXISTING);
					LOGGER.debug("copied file {}", getRegionDirectories().getPoi());
				} catch (Exception ex) {
					LOGGER.warn("failed to copy file {}", getRegionDirectories().getPoi(), ex);
				}

				// copy entities
				try {
					Files.copy(getRegionDirectories().getEntities().toPath(), to.getEntities().toPath(), StandardCopyOption.REPLACE_EXISTING);
					LOGGER.debug("copied file {}", getRegionDirectories().getEntities());
				} catch (Exception ex) {
					LOGGER.warn("failed to copy file {}", getRegionDirectories().getEntities(), ex);
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


			try {
				// only load headers, because we don't care for chunk data
				Region region = Region.loadRegionHeaders(getRegionDirectories(), regionData, poiData, entitiesData);
				region.deleteChunks(chunksToBeExported.flip());
				MCADeleteSelectionSaveJob job = new MCADeleteSelectionSaveJob(getRegionDirectories(), region, to, progressChannel);
				job.errorHandler = errorHandler;
				JobHandler.executeSaveData(job);
				return false;

			} catch (Exception ex) {
				LOGGER.warn("error deleting chunk indices in {}", getRegionDirectories().getLocationAsFileName(), ex);
				progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
			}
			return true;
		}
	}

	private static class MCADeleteSelectionSaveJob extends SaveDataJob<Region> {

		private final Progress progressChannel;
		private final RegionDirectories destinations;

		private MCADeleteSelectionSaveJob(RegionDirectories dirs, Region region, RegionDirectories destinations, Progress progressChannel) {
			super(dirs, region);
			this.destinations = destinations;
			this.progressChannel = progressChannel;
		}

		@Override
		public void execute() {
			try {
				getData().deFragment(destinations);
			} catch (Exception ex) {
				LOGGER.warn("failed to export filtered chunks from {}", getRegionDirectories().getLocationAsFileName(), ex);
			}
			progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
		}
	}
}
