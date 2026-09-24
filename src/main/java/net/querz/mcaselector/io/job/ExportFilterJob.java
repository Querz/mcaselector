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
import java.io.File;
import java.util.function.Consumer;

public final class ExportFilterJob extends ProcessDataJob {

	private static final Logger LOGGER = LogManager.getLogger(ExportFilterJob.class);

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
			ExportFilterJob job = new ExportFilterJob(r, filter, selection, destination, progressChannel);
			job.errorHandler = errorHandler;
			JobHandler.addJob(job);
		}
	}

	private final Progress progressChannel;
	private final GroupFilter filter;
	private final Selection selection;
	private final WorldDirectories destination;

	private ExportFilterJob(RegionDirectories dirs, GroupFilter filter, Selection selection, WorldDirectories destination, Progress progressChannel) {
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

		// load MCAFile
		try {
			Region region = Region.loadRegion(getRegionDirectories());

			region.keepChunks(filter, selection);

			region.deFragment(to);
		} catch (Exception ex) {
			LOGGER.warn("error exporting chunks from {}", getRegionDirectories().getLocationAsFileName(), ex);
		}
		progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
		return true;
	}
}
