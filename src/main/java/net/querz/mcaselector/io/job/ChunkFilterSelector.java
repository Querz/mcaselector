package net.querz.mcaselector.io.job;

import net.querz.mcaselector.config.ConfigProvider;
import net.querz.mcaselector.filter.filters.GroupFilter;
import net.querz.mcaselector.io.JobHandler;
import net.querz.mcaselector.io.RegionDirectories;
import net.querz.mcaselector.io.WorldDirectories;
import net.querz.mcaselector.io.mca.Region;
import net.querz.mcaselector.selection.ChunkSet;
import net.querz.mcaselector.selection.Selection;
import net.querz.mcaselector.tile.Tile;
import net.querz.mcaselector.util.point.Point2i;
import net.querz.mcaselector.util.progress.Progress;
import net.querz.mcaselector.util.progress.Timer;
import net.querz.mcaselector.text.Translation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.function.Consumer;

public final class ChunkFilterSelector {

	private static final Logger LOGGER = LogManager.getLogger(ChunkFilterSelector.class);

	private ChunkFilterSelector() {}

	public static void selectFilter(GroupFilter filter, Selection selection, int radius, Consumer<Selection> callback, Progress progressChannel, boolean cli) {
		WorldDirectories wd = ConfigProvider.WORLD.getWorldDirs();
		RegionDirectories[] rd = wd.listRegions(selection);
		if (rd == null || rd.length == 0) {
			if (cli) {
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
			MCASelectFilterProcessJob job = new MCASelectFilterProcessJob(r, filter, selection, callback, radius, progressChannel);
			job.errorHandler = errorHandler;
			JobHandler.addJob(job);
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
				LOGGER.debug("filter does not apply to region {}", getRegionDirectories().getLocation());
				progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
				return true;
			}

			// load MCAFile
			Timer t = new Timer();
			try {
				Region region = Region.loadRegion(getRegionDirectories());

				ChunkSet chunks = region.getFilteredChunks(filter, this.selection);
				if (!chunks.isEmpty()) {
					if (chunks.size() == Tile.CHUNKS) {
						chunks = null;
					}
					Selection selection = new Selection();
					selection.addAll(location, chunks);

					selection.addRadius(radius, this.selection);

					callback.accept(selection);
				}
				LOGGER.debug("took {} to select chunks in {}", t, getRegionDirectories().getLocationAsFileName());
			} catch (Exception ex) {
				LOGGER.warn("error selecting chunks in {}", getRegionDirectories().getLocationAsFileName(), ex);
			}
			progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
			return true;
		}
	}
}
