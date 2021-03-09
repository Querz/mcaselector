package net.querz.mcaselector.io.job;

import net.querz.mcaselector.Config;
import net.querz.mcaselector.filter.GroupFilter;
import net.querz.mcaselector.io.MCAFilePipe;
import net.querz.mcaselector.io.RegionDirectories;
import net.querz.mcaselector.io.SelectionData;
import net.querz.mcaselector.io.SelectionHelper;
import net.querz.mcaselector.io.WorldDirectories;
import net.querz.mcaselector.io.mca.Region;
import net.querz.mcaselector.tiles.Tile;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.progress.Progress;
import net.querz.mcaselector.progress.Timer;
import net.querz.mcaselector.text.Translation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class ChunkFilterSelector {

	private ChunkFilterSelector() {}

	public static void selectFilter(GroupFilter filter, SelectionData selection, int radius, Consumer<Map<Point2i, Set<Point2i>>> callback, Progress progressChannel, boolean headless) {
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
			MCAFilePipe.addJob(new MCASelectFilterLoadJob(r, filter, sel, radius, callback, progressChannel));
		}
	}

	private static class MCASelectFilterLoadJob extends LoadDataJob {

		private final GroupFilter filter;
		private final Progress progressChannel;
		private final Consumer<Map<Point2i, Set<Point2i>>> callback;
		private final Map<Point2i, Set<Point2i>> selection;
		private final int radius;

		private MCASelectFilterLoadJob(RegionDirectories dirs, GroupFilter filter, Map<Point2i, Set<Point2i>> selection, int radius, Consumer<Map<Point2i, Set<Point2i>>> callback, Progress progressChannel) {
			super(dirs);
			this.filter = filter;
			this.selection = selection;
			this.radius = radius;
			this.callback = callback;
			this.progressChannel = progressChannel;
		}

		@Override
		public void execute() {
			// load all files
			Point2i location = getRegionDirectories().getLocation();

			if (!filter.appliesToRegion(location)) {
				Debug.dumpf("filter does not apply to region %s", getRegionDirectories().getLocation());
				progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
				return;
			}

			byte[] regionData = loadRegion();
			byte[] poiData = loadPoi();
			byte[] entitiesData = loadEntities();

			if (regionData == null && poiData == null && entitiesData == null) {
				Debug.errorf("failed to load any data from %s", getRegionDirectories().getLocationAsFileName());
				progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
			} else {
				MCAFilePipe.executeProcessData(new MCASelectFilterProcessJob(getRegionDirectories(), regionData, poiData, entitiesData, filter, selection, callback, location, radius, progressChannel));
			}
		}
	}

	private static class MCASelectFilterProcessJob extends ProcessDataJob {

		private final Progress progressChannel;
		private final GroupFilter filter;
		private final Map<Point2i, Set<Point2i>> selection;
		private final Consumer<Map<Point2i, Set<Point2i>>> callback;
		private final Point2i location;
		private final int radius;

		private MCASelectFilterProcessJob(RegionDirectories dirs, byte[] regionData, byte[] poiData, byte[] entitiesData, GroupFilter filter, Map<Point2i, Set<Point2i>> selection, Consumer<Map<Point2i, Set<Point2i>>> callback, Point2i location, int radius,  Progress progressChannel) {
			super(dirs, regionData, poiData, entitiesData);
			this.filter = filter;
			this.selection = selection;
			this.callback = callback;
			this.location = location;
			this.progressChannel = progressChannel;
			this.radius = radius;
		}

		@Override
		public void execute() {
			//load MCAFile
			Timer t = new Timer();
			try {
				Region region = Region.loadRegion(getRegionDirectories(), getRegionData(), getPoiData(), getEntitiesData());

				Set<Point2i> chunks = region.getFilteredChunks(filter, selection == null ? null : selection.get(getRegionDirectories().getLocation()));
				if (chunks.size() > 0) {
					if (chunks.size() == Tile.CHUNKS) {
						chunks = null;
					}
					Map<Point2i, Set<Point2i>> selection = new HashMap<>();
					selection.put(location, chunks);

					selection = applyRadius(selection, this.selection);

					callback.accept(selection);
				}
				Debug.dumpf("took %s to select chunks in %s", t, getRegionDirectories().getLocationAsFileName());
			} catch (Exception ex) {
				Debug.dumpException("error selecting chunks in " + getRegionDirectories().getLocationAsFileName(), ex);
			}
			progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
		}

		private boolean selectionContainsChunk(Map<Point2i, Set<Point2i>> selection, Point2i chunk) {
			Point2i region = chunk.chunkToRegion();
			Set<Point2i> chunks;
			return selection.containsKey(region) && ((chunks = selection.get(chunk)) == null || chunks.contains(chunk));
		}

		private Map<Point2i, Set<Point2i>> applyRadius(Map<Point2i, Set<Point2i>> region, Map<Point2i, Set<Point2i>> selection) {
			if (radius <= 0) {
				return region;
			}

			Map<Point2i, Set<Point2i>> output = new HashMap<>();

			for (Map.Entry<Point2i, Set<Point2i>> reg : region.entrySet()) {
				if (reg.getValue() == null) {
					output.put(reg.getKey(), null);
					// full region
					Point2i startChunk = reg.getKey().regionToChunk();
					Point2i endChunk = startChunk.add(Tile.SIZE_IN_CHUNKS - 1);

					for (int x = startChunk.getX() - radius; x <= endChunk.getX() + radius; x++) {
						for (int z = startChunk.getZ() - radius; z <= endChunk.getZ() + radius; z++) {
							Point2i currentChunk = new Point2i(x, z);
							if (!selectionContainsChunk(selection, currentChunk)) {
								continue;
							}
							Point2i currentRegion = currentChunk.chunkToRegion();

							if (currentRegion.equals(reg.getKey())) {
								z += Tile.SIZE_IN_CHUNKS - 1;
								continue;
							}

							if (!output.containsKey(currentRegion)) {
								output.put(currentRegion, new HashSet<>());
							}

							output.get(currentRegion).add(currentChunk);
						}
					}
				} else {
					output.put(reg.getKey(), new HashSet<>(reg.getValue()));
					for (Point2i chunk : reg.getValue()) {
						for (int x = chunk.getX() - radius; x <= chunk.getX() + radius; x++) {
							for (int z = chunk.getZ() - radius; z <= chunk.getZ() + radius; z++) {
								Point2i currentChunk = new Point2i(x, z);
								if (!selectionContainsChunk(selection, currentChunk)) {
									continue;
								}
								Point2i currentRegion = currentChunk.chunkToRegion();
								if (!output.containsKey(currentRegion)) {
									output.put(currentRegion, new HashSet<>());
								}
								output.get(currentRegion).add(currentChunk);
							}
						}
					}
				}
			}
			return output;
		}
	}
}
