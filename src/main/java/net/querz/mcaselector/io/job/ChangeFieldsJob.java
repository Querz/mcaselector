package net.querz.mcaselector.io.job;

import net.querz.mcaselector.changer.Field;
import net.querz.mcaselector.changer.fields.ScriptField;
import net.querz.mcaselector.config.ConfigProvider;
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
import java.util.List;
import java.util.function.Consumer;

public final class ChangeFieldsJob extends ProcessDataJob {

	private static final Logger LOGGER = LogManager.getLogger(ChangeFieldsJob.class);

	public static void changeNBTFields(List<Field<?>> fields, boolean force, Selection selection, Progress progressChannel, boolean headless) {
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
			ChangeFieldsJob job = new ChangeFieldsJob(r, fields, force, selection, progressChannel);
			job.errorHandler = errorHandler;
			JobHandler.addJob(job);
		}
	}

	private final Progress progressChannel;
	private final List<Field<?>> fields;
	private final boolean force;
	private final Selection selection;

	private ChangeFieldsJob(RegionDirectories dirs, List<Field<?>> fields, boolean force, Selection selection, Progress progressChannel) {
		super(dirs, PRIORITY_LOW);
		this.fields = fields;
		this.force = force;
		this.selection = selection;
		this.progressChannel = progressChannel;
	}

	@Override
	public boolean execute() {
		if (fields.size() == 1 && fields.getFirst() instanceof ScriptField sf) {
			if (!sf.matchesRegion(getRegionDirectories().getLocation())) {
				progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
				return true;
			}
		}

		if (selection != null) {
			Point2i location = getRegionDirectories().getLocation();
			if (!selection.isAnyChunkInRegionSelected(location)) {
				LOGGER.debug("will not apply nbt changes to {}", getRegionDirectories().getLocationAsFileName());
				progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
				return true;
			}
		}

		//load MCAFile
		try {
			Region region = Region.loadRegion(getRegionDirectories());

			region.applyFieldChanges(fields, force, selection);

			region.saveWithTempFiles();
		} catch (Exception ex) {
			LOGGER.warn("error changing fields in {}", getRegionDirectories().getLocationAsFileName(), ex);
		}
		progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
		return true;
	}
}
