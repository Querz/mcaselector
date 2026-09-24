package net.querz.mcaselector.io.job;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.querz.mcaselector.config.ConfigProvider;
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.io.JobHandler;
import net.querz.mcaselector.io.RegionDirectories;
import net.querz.mcaselector.io.mca.Region;
import net.querz.mcaselector.util.point.Point2i;
import net.querz.mcaselector.util.progress.Progress;
import net.querz.mcaselector.selection.ChunkSet;
import net.querz.mcaselector.selection.Selection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.function.Consumer;

public final class DeleteSelectionJob extends ProcessDataJob {

	private static final Logger LOGGER = LogManager.getLogger(DeleteSelectionJob.class);

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
			DeleteSelectionJob job = new DeleteSelectionJob(FileHelper.createRegionDirectories(new Point2i(entry.getLongKey())), entry.getValue(), progressChannel);
			job.errorHandler = errorHandler;
			JobHandler.addJob(job);
		}
	}

	private final Progress progressChannel;
	private final ChunkSet selection;

	private DeleteSelectionJob(RegionDirectories dirs, ChunkSet selection, Progress progressChannel) {
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
				LOGGER.debug("deleted region file {}", getRegionDirectories().getRegion());
			} else {
				LOGGER.warn("failed to delete region file {}", getRegionDirectories().getRegion());
			}

			// delete poi
			if (getRegionDirectories().getPoi().delete()) {
				LOGGER.debug("deleted poi file {}", getRegionDirectories().getPoi());
			} else {
				LOGGER.warn("failed to delete poi file {}", getRegionDirectories().getPoi());
			}

			// delete entities
			if (getRegionDirectories().getEntities().delete()) {
				LOGGER.debug("deleted entities file {}", getRegionDirectories().getEntities());
			} else {
				LOGGER.warn("failed to delete entities file {}", getRegionDirectories().getEntities());
			}

			progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
			return true;
		}

		// load MCAFile
		try {
			// only load headers, we don't care for chunk contents
			Region region = Region.loadRegionHeaders(getRegionDirectories());

			region.deleteChunks(selection);

			region.deFragment();
		} catch (Exception ex) {
			LOGGER.warn("error deleting chunks from selection in {}", getRegionDirectories().getLocationAsFileName());
		}
		progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
		return true;
	}
}
