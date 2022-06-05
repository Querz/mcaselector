package net.querz.mcaselector.io.job;

import net.querz.mcaselector.Config;
import net.querz.mcaselector.filter.GroupFilter;
import net.querz.mcaselector.io.JobHandler;
import net.querz.mcaselector.io.RegionDirectories;
import net.querz.mcaselector.io.WorldDirectories;
import net.querz.mcaselector.io.mca.Region;
import net.querz.mcaselector.selection.ChunkSet;
import net.querz.mcaselector.selection.Selection;
import net.querz.mcaselector.tiles.Tile;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.progress.Progress;
import net.querz.mcaselector.progress.Timer;
import net.querz.mcaselector.text.Translation;
import java.util.function.Consumer;

public final class ChunkFilterSelector {

	private ChunkFilterSelector() {}

	public static void selectFilter(GroupFilter filter, Selection selection, int radius, Consumer<Selection> callback, Progress progressChannel, boolean headless) {
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
		private final Selection selection;
		private final Consumer<Selection> callback;
		private final int radius;

		private MCASelectFilterProcessJob(RegionDirectories dirs, GroupFilter filter, Selection selection, Consumer<Selection> callback, int radius,  Progress progressChannel) {
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

				ChunkSet chunks = region.getFilteredChunks(filter, this.selection);
				if (chunks.size() > 0) {
					if (chunks.size() == Tile.CHUNKS) {
						chunks = null;
					}
					Selection selection = new Selection();
					selection.addAll(location, chunks);

					selection.addRadius(radius, this.selection);

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
}
