package net.querz.mcaselector.io.job;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.querz.mcaselector.Config;
import net.querz.mcaselector.filter.GroupFilter;
import net.querz.mcaselector.io.JobHandler;
import net.querz.mcaselector.io.RegionDirectories;
import net.querz.mcaselector.io.SelectionData;
import net.querz.mcaselector.io.WorldDirectories;
import net.querz.mcaselector.io.mca.Region;
import net.querz.mcaselector.tiles.Tile;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.progress.Progress;
import net.querz.mcaselector.progress.Timer;
import net.querz.mcaselector.text.Translation;
import java.util.function.Consumer;

public final class ChunkFilterSelector {

	private ChunkFilterSelector() {}

	public static void selectFilter(GroupFilter filter, SelectionData selection, int radius, Consumer<Long2ObjectOpenHashMap<LongOpenHashSet>> callback, Progress progressChannel, boolean headless) {
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
			JobHandler.addJob(new MCASelectFilterProcessJob(r, filter, selection, callback, radius, progressChannel));
		}
	}

	private static class MCASelectFilterProcessJob extends ProcessDataJob {

		private final Progress progressChannel;
		private final GroupFilter filter;
		private final SelectionData selection;
		private final Consumer<Long2ObjectOpenHashMap<LongOpenHashSet>> callback;
		private final int radius;

		private MCASelectFilterProcessJob(RegionDirectories dirs, GroupFilter filter, SelectionData selection, Consumer<Long2ObjectOpenHashMap<LongOpenHashSet>> callback, int radius,  Progress progressChannel) {
			super(dirs, PRIORITY_LOW);
			this.filter = filter;
			this.selection = selection;
			this.callback = callback;
			this.progressChannel = progressChannel;
			this.radius = radius;
		}

		@Override
		public boolean execute() {
			// load all files
			Point2i location = getRegionDirectories().getLocation();

			if (!filter.appliesToRegion(location)) {
				Debug.dumpf("filter does not apply to region %s", getRegionDirectories().getLocation());
				progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
				return true;
			}

			byte[] regionData = loadRegion();
			byte[] poiData = loadPoi();
			byte[] entitiesData = loadEntities();

			if (regionData == null && poiData == null && entitiesData == null) {
				Debug.errorf("failed to load any data from %s", getRegionDirectories().getLocationAsFileName());
				progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
				return true;
			}

			// load MCAFile
			Timer t = new Timer();
			try {
				Region region = Region.loadRegion(getRegionDirectories(), regionData, poiData, entitiesData);

				if (region.getRegion() == null) {
					progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
					return true;
				}

				LongOpenHashSet chunks = region.getFilteredChunks(filter, this.selection);
				if (chunks.size() > 0) {
					if (chunks.size() == Tile.CHUNKS) {
						chunks = null;
					}
					Long2ObjectOpenHashMap<LongOpenHashSet> selection = new Long2ObjectOpenHashMap<>();
					selection.put(location.asLong(), chunks);

					selection = applyRadius(selection, this.selection, this.radius);

					callback.accept(selection);
				}
				Debug.dumpf("took %s to select chunks in %s", t, getRegionDirectories().getLocationAsFileName());
			} catch (Exception ex) {
				Debug.dumpException("error selecting chunks in " + getRegionDirectories().getLocationAsFileName(), ex);
			}
			progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
			return true;
		}
	}

	/**
	 * Adds a radius to a region selection. This may result in a selection in more regions than the initial selection.
	 * @param region The initially selected chunks in a region.
	 * @param selection The complete selection, in case we want to stay within the boundaries of a target selection.
	 * @return A new selection with the radius applied.
	 */
	static Long2ObjectOpenHashMap<LongOpenHashSet> applyRadius(Long2ObjectOpenHashMap<LongOpenHashSet> region, SelectionData selection, int radius) {
		if (radius <= 0) {
			return region;
		}

		Long2ObjectOpenHashMap<LongOpenHashSet> output = new Long2ObjectOpenHashMap<>();

		for (Long2ObjectMap.Entry<LongOpenHashSet> reg : region.long2ObjectEntrySet()) {
			if (reg.getValue() == null) {
				output.put(reg.getLongKey(), null);
				// full region
				Point2i startChunk = new Point2i(reg.getLongKey()).regionToChunk();
				Point2i endChunk = startChunk.add(Tile.SIZE_IN_CHUNKS - 1);

				for (int x = startChunk.getX() - radius; x <= endChunk.getX() + radius; x++) {
					for (int z = startChunk.getZ() - radius; z <= endChunk.getZ() + radius; z++) {
						Point2i currentChunk = new Point2i(x, z);
						if (selection != null && !selection.isChunkSelected(currentChunk)) {
							continue;
						}
						long currentRegion = currentChunk.chunkToRegion().asLong();

						if (currentRegion == reg.getLongKey()) {
							z += Tile.SIZE_IN_CHUNKS - 1;
							continue;
						}

						if (!output.containsKey(currentRegion)) {
							output.put(currentRegion, new LongOpenHashSet());
						}

						output.get(currentRegion).add(currentChunk.asLong());
					}
				}
			} else {
				output.put(reg.getLongKey(), new LongOpenHashSet(reg.getValue()));
				for (long chunk : reg.getValue()) {
					Point2i c = new Point2i(chunk);
					for (int x = c.getX() - radius; x <= c.getX() + radius; x++) {
						for (int z = c.getZ() - radius; z <= c.getZ() + radius; z++) {
							Point2i currentChunk = new Point2i(x, z);
							if (selection != null && !selection.isChunkSelected(currentChunk)) {
								continue;
							}
							long currentRegion = currentChunk.chunkToRegion().asLong();
							if (!output.containsKey(currentRegion)) {
								output.put(currentRegion, new LongOpenHashSet());
							}
							output.get(currentRegion).add(currentChunk.asLong());
						}
					}
				}
			}
		}
		return output;
	}
}
