package net.querz.mcaselector.io.job;

import net.querz.mcaselector.config.ConfigProvider;
import net.querz.mcaselector.filter.filters.GroupFilter;
import net.querz.mcaselector.io.JobHandler;
import net.querz.mcaselector.io.RegionDirectories;
import net.querz.mcaselector.io.WorldDirectories;
import net.querz.mcaselector.io.mca.Region;
import net.querz.mcaselector.util.point.Point2i;
import net.querz.mcaselector.util.progress.Progress;
import net.querz.mcaselector.selection.Selection;
import net.querz.mcaselector.text.Translation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.function.Consumer;

public final class DeleteFilterJob extends ProcessDataJob {

	private static final Logger LOGGER = LogManager.getLogger(DeleteFilterJob.class);

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
			DeleteFilterJob job = new DeleteFilterJob(r, filter, selection, progressChannel);
			job.errorHandler = errorHandler;
			JobHandler.addJob(job);
		}
	}

	private final Progress progressChannel;
	private final GroupFilter filter;
	private final Selection selection;

	private DeleteFilterJob(RegionDirectories dirs, GroupFilter filter, Selection selection, Progress progressChannel) {
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

		try {
			// parse raw data
			Region region = Region.loadRegion(getRegionDirectories());

			if (region.deleteChunks(filter, selection)) {
				// only save file if we actually deleted something
				region.defragment();
			} else {
				LOGGER.debug("nothing to delete in {}, not saving", getRegionDirectories().getLocationAsFileName());
			}
		} catch (Exception ex) {
			LOGGER.warn("failed to delete filtered chunks from {}", getRegionDirectories().getLocationAsFileName(), ex);
		}
		progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
		return true;
	}
}
