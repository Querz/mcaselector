package net.querz.mcaselector.io.job;

import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.io.JobHandler;
import net.querz.mcaselector.io.RegionDirectories;
import net.querz.mcaselector.io.SelectionData;
import net.querz.mcaselector.io.SelectionHelper;
import net.querz.mcaselector.io.WorldDirectories;
import net.querz.mcaselector.io.mca.Region;
import net.querz.mcaselector.tiles.Tile;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.progress.Progress;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class SelectionExporter {

	private SelectionExporter() {}

	public static void exportSelection(SelectionData selection, WorldDirectories destination, Progress progressChannel) {
		if (selection.selection().isEmpty() && !selection.inverted()) {
			progressChannel.done("no selection");
			return;
		}

		JobHandler.clearQueues();

		Map<Point2i, Set<Point2i>> sel = SelectionHelper.getTrueSelection(selection);

		progressChannel.setMax(sel.size());
		Point2i first = sel.entrySet().iterator().next().getKey();
		progressChannel.updateProgress(FileHelper.createMCAFileName(first), 0);

		for (Map.Entry<Point2i, Set<Point2i>> entry : sel.entrySet()) {
			JobHandler.addJob(new MCADeleteSelectionProcessJob(
					FileHelper.createRegionDirectories(entry.getKey()),
					entry.getValue(),
					destination,
					progressChannel));
		}
	}

	private static class MCADeleteSelectionProcessJob extends ProcessDataJob {

		private final Progress progressChannel;
		private final Set<Point2i> chunksToBeExported;
		private final WorldDirectories destination;

		private MCADeleteSelectionProcessJob(RegionDirectories dirs, Set<Point2i> chunksToBeExported, WorldDirectories destination, Progress progressChannel) {
			super(dirs);
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
				Debug.dumpf("%s exists, not overwriting", getRegionDirectories().getLocationAsFileName());
				progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
				return true;
			}

			RegionDirectories to = new RegionDirectories(getRegionDirectories().getLocation(), toRegion, toPoi, toEntities);

			if (chunksToBeExported == null) {

				// copy region
				try {
					Files.copy(getRegionDirectories().getRegion().toPath(), to.getRegion().toPath(), StandardCopyOption.REPLACE_EXISTING);
					Debug.dumpf("copied file %s", getRegionDirectories().getRegion());
				} catch (Exception ex) {
					Debug.dumpException("failed to copy file " + getRegionDirectories().getRegion(), ex);
				}

				// copy poi
				try {
					Files.copy(getRegionDirectories().getPoi().toPath(), to.getPoi().toPath(), StandardCopyOption.REPLACE_EXISTING);
					Debug.dumpf("copied file %s", getRegionDirectories().getPoi());
				} catch (Exception ex) {
					Debug.dumpException("failed to copy file " + getRegionDirectories().getPoi(), ex);
				}

				// copy entities
				try {
					Files.copy(getRegionDirectories().getEntities().toPath(), to.getEntities().toPath(), StandardCopyOption.REPLACE_EXISTING);
					Debug.dumpf("copied file %s", getRegionDirectories().getEntities());
				} catch (Exception ex) {
					Debug.dumpException("failed to copy file " + getRegionDirectories().getEntities(), ex);
				}

				progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
				return true;
			}

			byte[] regionData = loadRegionHeader();
			byte[] poiData = loadPoiHeader();
			byte[] entitiesData = loadEntitiesHeader();

			if (regionData == null && poiData == null && entitiesData == null) {
				Debug.errorf("failed to load any data from %s", getRegionDirectories().getLocationAsFileName());
				progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
				return true;
			}


			try {
				// only load headers, because we don't care for chunk data
				Region region = Region.loadRegionHeaders(getRegionDirectories(), regionData, poiData, entitiesData);

				Set<Point2i> inverted = new HashSet<>(Tile.CHUNKS - chunksToBeExported.size());
				Point2i origin = chunksToBeExported.iterator().next().chunkToRegion().regionToChunk();
				for (int x = origin.getX(); x < origin.getX() + Tile.SIZE_IN_CHUNKS; x++) {
					for (int z = origin.getZ(); z < origin.getZ() + Tile.SIZE_IN_CHUNKS; z++) {
						Point2i cp = new Point2i(x, z);
						if (!chunksToBeExported.contains(cp)) {
							inverted.add(cp);
						}
					}
				}

				region.deleteChunks(inverted);
				JobHandler.executeSaveData(new MCADeleteSelectionSaveJob(getRegionDirectories(), region, to, progressChannel));
				return false;

			} catch (Exception ex) {
				Debug.dumpException("error deleting chunk indices in " + getRegionDirectories().getLocationAsFileName(), ex);
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
				Debug.dumpException("failed to export filtered chunks from " + getRegionDirectories().getLocationAsFileName(), ex);
			}
			progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
		}
	}
}
