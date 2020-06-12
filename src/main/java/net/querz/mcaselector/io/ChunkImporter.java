package net.querz.mcaselector.io;

import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.progress.Progress;
import net.querz.mcaselector.progress.Timer;
import net.querz.mcaselector.range.Range;
import net.querz.mcaselector.text.Translation;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ChunkImporter {

	private ChunkImporter() {}

	public static void importChunks(File importDir, Progress progressChannel, boolean headless, boolean overwrite, Map<Point2i, Set<Point2i>> selection, List<Range> ranges, Point2i offset) {
		try {
			File[] importFiles = importDir.listFiles((dir, name) -> name.matches(FileHelper.MCA_FILE_PATTERN));
			if (importFiles == null || importFiles.length == 0) {
				if (headless) {
					progressChannel.done("no files");
				} else {
					progressChannel.done(Translation.DIALOG_PROGRESS_NO_FILES.toString());
				}
				return;
			}

			MCAFilePipe.clearQueues();

			if (headless) {
				progressChannel.setMessage("collecting data...");
			} else {
				progressChannel.setMessage(Translation.DIALOG_PROGRESS_COLLECTING_DATA.toString());
			}

			Map<Point2i, Set<Point2i>> targetMapping = new HashMap<>();

			// find target files
			for (File file : importFiles) {
				Point2i source = FileHelper.parseMCAFileName(file);
				if (source == null) {
					Debug.dumpf("could not parse region from mca file name: %s", file.getName());
					continue;
				}

				try {
					Set<Point2i> targets = getTargetRegions(source, offset);
					mapSourceRegionsByTargetRegion(source, targets, targetMapping);
				} catch (Exception ex) {
					Debug.error(ex);
				}
			}

			progressChannel.setMax(targetMapping.size());

			progressChannel.updateProgress(importFiles[0].getName(), 0);

			for (Map.Entry<Point2i, Set<Point2i>> entry : targetMapping.entrySet()) {
				if (selection == null || selection.containsKey(entry.getKey())) {
					File targetFile = FileHelper.createMCAFilePath(entry.getKey());
					Set<Point2i> targetSelection = selection == null ? null : selection.get(entry.getKey());
					if (targetSelection == null) {
						// null --> no selection, 0 --> all chunks in this region are selected
						targetSelection = new HashSet<>(0);
					}
					MCAFilePipe.addJob(new MCAChunkImporterLoadJob(targetFile, importDir, entry.getKey(), entry.getValue(), offset, progressChannel, overwrite, targetSelection, ranges));
				} else {
					progressChannel.incrementProgress(FileHelper.createMCAFileName(entry.getKey()), entry.getValue().size());
				}
			}
		} catch (Exception ex) {
			Debug.error(ex);
		}
	}

	private static class MCAChunkImporterLoadJob extends LoadDataJob {

		private Point2i target;
		private Set<Point2i> sources;
		private File sourceDir;
		private Point2i offset;
		private Progress progressChannel;
		private boolean overwrite;
		private Set<Point2i> selection;
		private List<Range> ranges;

		private MCAChunkImporterLoadJob(File targetFile, File sourceDir, Point2i target, Set<Point2i> sources, Point2i offset, Progress progressChannel, boolean overwrite, Set<Point2i> selection, List<Range> ranges) {
			super(targetFile);
			this.target = target;
			this.sources = sources;
			this.sourceDir = sourceDir;
			this.offset = offset;
			this.progressChannel = progressChannel;
			this.overwrite = overwrite;
			this.selection = selection;
			this.ranges = ranges;
		}

		@Override
		public void execute() {

			// special case for non existing destination file and no offset
			if (offset.getX() == 0 && offset.getY() == 0 && !getFile().exists() && (selection == null || selection.size() == 0)) {
				//if the entire mca file doesn't exist, just copy it over
				File source = new File(sourceDir, getFile().getName());
				try {

					Files.copy(source.toPath(), getFile().toPath());
				} catch (IOException ex) {
					Debug.errorf("failed to copy file %s to %s: %s", source, getFile(), ex.getMessage());
				}
				progressChannel.incrementProgress(getFile().getName(), sources.size());
				return;
			}

			// regular case

			Map<Point2i, byte[]> sourceDataMapping = new HashMap<>();

			for (Point2i sourceRegion : sources) {
				File source = new File(sourceDir, FileHelper.createMCAFileName(sourceRegion));

				byte[] sourceData = load(source);

				if (sourceData == null) {
					Debug.errorf("error loading source mca file %s", source.getName());
					continue;
				}

				sourceDataMapping.put(sourceRegion, sourceData);
			}

			if (sourceDataMapping.isEmpty()) {
				Debug.errorf("could not load any source mca files to merge into %s with offset %s", getFile().getName(), offset);
				progressChannel.incrementProgress(getFile().getName());
				return;
			}

			byte[] destData;

			if (getFile().exists()) {
				destData = load();
				if (destData == null) {
					Debug.errorf("error loading destination mca file %s", getFile().getName());
					progressChannel.incrementProgress(getFile().getName(), sourceDataMapping.size());
					return;
				}
			} else {
				destData = null;
			}

			MCAFilePipe.executeProcessData(new MCAChunkImporterProcessJob(getFile(), sourceDir, target, sourceDataMapping, destData, offset, progressChannel, overwrite, selection, ranges));
		}
	}

	private static class MCAChunkImporterProcessJob extends ProcessDataJob {

		private File sourceDir;
		private Point2i target;
		private Map<Point2i, byte[]> sourceDataMapping;
		private Point2i offset;
		private Progress progressChannel;
		private boolean overwrite;
		private Set<Point2i> selection;
		private List<Range> ranges;

		private MCAChunkImporterProcessJob(File targetFile, File sourceDir, Point2i target, Map<Point2i, byte[]> sourceDataMapping, byte[] destData, Point2i offset, Progress progressChannel, boolean overwrite, Set<Point2i> selection, List<Range> ranges) {
			super(targetFile, destData);
			this.sourceDir = sourceDir;
			this.target = target;
			this.sourceDataMapping = sourceDataMapping;
			this.offset = offset;
			this.progressChannel = progressChannel;
			this.overwrite = overwrite;
			this.selection = selection;
			this.ranges = ranges;
		}

		@Override
		public void execute() {
			Timer t = new Timer();
			try {
				MCAFile destination;
				// no destination file: create new MCAFile
				if (getData() == null) {
					 destination = new MCAFile(getFile());
				} else {
					destination = MCAFile.readAll(getFile(), new ByteArrayPointer(getData()));
				}

				if (destination == null) {
					progressChannel.incrementProgress(getFile().getName(), sourceDataMapping.size());
					Debug.errorf("failed to load target MCAFile %s", getFile().getName());
					return;
				}

				for (Map.Entry<Point2i, byte[]> sourceData : sourceDataMapping.entrySet()) {
					MCAFile source = MCAFile.readAll(new File(sourceDir, FileHelper.createMCAFileName(sourceData.getKey())), new ByteArrayPointer(sourceData.getValue()));

					Debug.dumpf("merging chunk from  region %s into %s", sourceData.getKey(), target);

					source.mergeChunksInto(destination, offset, overwrite, selection == null ? null : selection.size() == 0 ? null : selection, ranges);
				}

				MCAFilePipe.executeSaveData(new MCAChunkImporterSaveJob(getFile(), destination, progressChannel));

			} catch (Exception ex) {
				Debug.error(ex);
				progressChannel.incrementProgress(getFile().getName());
			}

			Debug.dumpf("took %s to merge chunks into %s with offset %s", t, getFile(), offset);
		}
	}

	private static class MCAChunkImporterSaveJob extends SaveDataJob<MCAFile> {

		private Progress progressChannel;

		private MCAChunkImporterSaveJob(File file, MCAFile data, Progress progressChannel) {
			super(file, data);
			this.progressChannel = progressChannel;
		}

		@Override
		public void execute() {
			Timer t = new Timer();
			try {
				File tmpFile = File.createTempFile(getFile().getName(), null, null);
				boolean wroteChunks;
				try (RandomAccessFile raf = new RandomAccessFile(tmpFile, "rw")) {
					 wroteChunks = getData().saveAll(raf);
				}
				if (wroteChunks) {
					Files.move(tmpFile.toPath(), getFile().toPath(), StandardCopyOption.REPLACE_EXISTING);
				} else {
					Files.deleteIfExists(tmpFile.toPath());
				}
			} catch (Exception ex) {
				Debug.error(ex);
			}
			progressChannel.incrementProgress(getFile().getName());
			Debug.dumpf("took %s to save data to %s", t, getFile().getName());
		}
	}

	private static void mapSourceRegionsByTargetRegion(Point2i source, Set<Point2i> targets, Map<Point2i, Set<Point2i>> map) {
		for (Point2i target : targets) {
			map.computeIfAbsent(target, key -> new HashSet<>(4));
			map.get(target).add(source);
		}
	}


	// source is a region coordinate, offset is a chunk coordinate
	private static Set<Point2i> getTargetRegions(Point2i source, Point2i offset) {
		Set<Point2i> result = new HashSet<>(4);
		Point2i sourceChunk = source.regionToChunk().add(offset);
		result.add(sourceChunk.chunkToRegion());
		result.add(sourceChunk.add(31, 0).chunkToRegion());
		result.add(sourceChunk.add(0, 31).chunkToRegion());
		result.add(sourceChunk.add(31, 31).chunkToRegion());
		return result;
	}
}
