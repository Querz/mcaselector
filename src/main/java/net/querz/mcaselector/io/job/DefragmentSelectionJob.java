package net.querz.mcaselector.io.job;

import net.querz.mcaselector.config.ConfigProvider;
import net.querz.mcaselector.io.JobHandler;
import net.querz.mcaselector.io.RegionDirectories;
import net.querz.mcaselector.io.WorldDirectories;
import net.querz.mcaselector.io.mca.Region;
import net.querz.mcaselector.selection.Selection;
import net.querz.mcaselector.text.Translation;
import net.querz.mcaselector.util.progress.Progress;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.function.Consumer;

public final class DefragmentSelectionJob extends ProcessDataJob {

	private static final Logger LOGGER = LogManager.getLogger(DefragmentSelectionJob.class);

	public static void defragmentSelection(Selection selection, Progress progressChannel, boolean headless) {
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
			DefragmentSelectionJob job = new DefragmentSelectionJob(r, progressChannel);
			job.errorHandler = errorHandler;
			JobHandler.addJob(job);
		}
	}

	private final Progress progressChannel;

	private DefragmentSelectionJob(RegionDirectories dirs, Progress progressChannel) {
		super(dirs, PRIORITY_LOW);
		this.progressChannel = progressChannel;
	}

	@Override
	public boolean execute() {
		try {
			Region region = Region.loadRegionHeaders(getRegionDirectories());
			region.defragment();
		} catch (Exception ex) {
			LOGGER.warn("error defragmenting region file {}", getRegionDirectories().getLocationAsFileName(), ex);
		}
		progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
		return true;
	}
}
