package net.querz.mcaselector.io;

import net.querz.mcaselector.Config;
import net.querz.mcaselector.filter.GroupFilter;
import net.querz.mcaselector.tiles.Tile;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.progress.Progress;
import net.querz.mcaselector.progress.Timer;
import net.querz.mcaselector.text.Translation;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;

public class ChunkFilterSelector {

	private ChunkFilterSelector() {}

	public static void selectFilter(GroupFilter filter, Consumer<Map<Point2i, Set<Point2i>>> callback, Progress progressChannel, boolean headless) {
		File[] files = Config.getWorldDir().listFiles((d, n) -> n.matches(FileHelper.MCA_FILE_PATTERN));
		if (files == null || files.length == 0) {
			if (headless) {
				progressChannel.done("no files");
			} else {
				progressChannel.done(Translation.DIALOG_PROGRESS_NO_FILES.toString());
			}
			return;
		}

		MCAFilePipe.clearQueues();

		progressChannel.setMax(files.length);
		progressChannel.updateProgress(files[0].getName(), 0);

		for (File file : files) {
			MCAFilePipe.addJob(new MCASelectFilterLoadJob(file, filter, callback, progressChannel));
		}
	}

	private static class MCASelectFilterLoadJob extends LoadDataJob {

		private GroupFilter filter;
		private Progress progressChannel;
		private Consumer<Map<Point2i, Set<Point2i>>> callback;

		private MCASelectFilterLoadJob(File file, GroupFilter filter, Consumer<Map<Point2i, Set<Point2i>>> callback, Progress progressChannel) {
			super(file);
			this.filter = filter;
			this.callback = callback;
			this.progressChannel = progressChannel;
		}

		@Override
		public void execute() {
			Matcher m = FileHelper.REGION_GROUP_PATTERN.matcher(getFile().getName());
			if (m.find()) {
				int regionX = Integer.parseInt(m.group("regionX"));
				int regionZ = Integer.parseInt(m.group("regionZ"));

				if (!filter.appliesToRegion(new Point2i(regionX, regionZ))) {
					Debug.dump("filter does not apply to file " + getFile().getName());
					progressChannel.incrementProgress(getFile().getName());
					return;
				}

				byte[] data = load();
				if (data != null) {
					MCAFilePipe.executeProcessData(new MCASelectFilterProcessJob(getFile(), data, filter, callback, new Point2i(regionX, regionZ), progressChannel));
				} else {
					Debug.errorf("error loading mca file %s", getFile().getName());
					progressChannel.incrementProgress(getFile().getName());
				}
			} else {
				Debug.dump("wtf, how did we get here??");
				progressChannel.incrementProgress(getFile().getName());
			}
		}
	}

	private static class MCASelectFilterProcessJob extends ProcessDataJob {

		private Progress progressChannel;
		private GroupFilter filter;
		private Consumer<Map<Point2i, Set<Point2i>>> callback;
		private Point2i location;

		private MCASelectFilterProcessJob(File file, byte[] data, GroupFilter filter, Consumer<Map<Point2i, Set<Point2i>>> callback, Point2i location, Progress progressChannel) {
			super(file, data);
			this.filter = filter;
			this.callback = callback;
			this.location = location;
			this.progressChannel = progressChannel;
		}

		@Override
		public void execute() {
			//load MCAFile
			Timer t = new Timer();
			try {
				MCAFile mca = MCAFile.readAll(getFile(), new ByteArrayPointer(getData()));
				if (mca != null) {
					Set<Point2i> chunks = mca.getFilteredChunks(filter);
					if (chunks.size() == Tile.CHUNKS) {
						chunks = null;
					}
					Map<Point2i, Set<Point2i>> region = new HashMap<>();
					region.put(location, chunks);

					callback.accept(region);

					Debug.dumpf("took %s to delete chunk indices in %s", t, getFile().getName());
				}
			} catch (Exception ex) {
				Debug.errorf("error selecting chunks in %s", getFile().getName());
			}
			progressChannel.incrementProgress(getFile().getName());
		}
	}
}
