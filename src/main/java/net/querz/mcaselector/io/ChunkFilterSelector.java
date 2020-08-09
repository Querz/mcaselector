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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;

public class ChunkFilterSelector {

	private ChunkFilterSelector() {}

	public static void selectFilter(GroupFilter filter, int radius, Consumer<Map<Point2i, Set<Point2i>>> callback, Progress progressChannel, boolean headless) {
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
			MCAFilePipe.addJob(new MCASelectFilterLoadJob(file, filter, radius, callback, progressChannel));
		}
	}

	private static class MCASelectFilterLoadJob extends LoadDataJob {

		private final GroupFilter filter;
		private final Progress progressChannel;
		private final Consumer<Map<Point2i, Set<Point2i>>> callback;
		private final int radius;

		private MCASelectFilterLoadJob(File file, GroupFilter filter, int radius, Consumer<Map<Point2i, Set<Point2i>>> callback, Progress progressChannel) {
			super(file);
			this.filter = filter;
			this.radius = radius;
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
					MCAFilePipe.executeProcessData(new MCASelectFilterProcessJob(getFile(), data, filter, callback, new Point2i(regionX, regionZ), radius, progressChannel));
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

		private final Progress progressChannel;
		private final GroupFilter filter;
		private final Consumer<Map<Point2i, Set<Point2i>>> callback;
		private final Point2i location;
		private final int radius;

		private MCASelectFilterProcessJob(File file, byte[] data, GroupFilter filter, Consumer<Map<Point2i, Set<Point2i>>> callback, Point2i location, int radius,  Progress progressChannel) {
			super(file, data);
			this.filter = filter;
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
				MCAFile mca = MCAFile.readAll(getFile(), new ByteArrayPointer(getData()));
				if (mca != null) {
					Set<Point2i> chunks = mca.getFilteredChunks(filter);
					if (chunks.size() == Tile.CHUNKS) {
						chunks = null;
					}
					Map<Point2i, Set<Point2i>> region = new HashMap<>();
					region.put(location, chunks);

					region = applyRadius(region);

					callback.accept(region);

					Debug.dumpf("took %s to delete chunk indices in %s", t, getFile().getName());
				}
			} catch (Exception ex) {
				Debug.dumpException("error selecting chunks in " + getFile().getName(), ex);
			}
			progressChannel.incrementProgress(getFile().getName());
		}

		private Map<Point2i, Set<Point2i>> applyRadius(Map<Point2i, Set<Point2i>> region) {
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
