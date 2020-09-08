package net.querz.mcaselector.io;

import net.querz.mcaselector.Config;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.progress.Progress;
import net.querz.mcaselector.progress.Timer;
import net.querz.mcaselector.property.DataProperty;
import net.querz.mcaselector.range.Range;
import net.querz.mcaselector.text.Translation;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ChunkImporter {

	private ChunkImporter() {}

	public static void importChunks(File sourceDirectory, Progress progressChannel, boolean headless, boolean overwrite, SelectionData sourceSelection, SelectionData targetSelection, List<Range> ranges, Point2i offset, DataProperty<Map<Point2i, File>> tempFiles) {
		try {
			File[] sourceFiles = sourceDirectory.listFiles();
			if (sourceFiles == null || sourceFiles.length == 0) {
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

			// if source world and target world is the same, we need to create temp files of all source files
			Map<Point2i, File> tempFilesMap = null;
			if (sourceDirectory.equals(Config.getWorldDir())) {
				tempFilesMap = new HashMap<>();
			}
			tempFiles.set(tempFilesMap);

			Map<Point2i, Set<Point2i>> targetMapping = createTargetSourceMapping(sourceDirectory, sourceSelection, targetSelection, offset);

			progressChannel.setMax(targetMapping.size());
			progressChannel.updateProgress(sourceFiles[0].getName(), 0);

			// create local source and local target selections
			for (Map.Entry<Point2i, Set<Point2i>> entry : targetMapping.entrySet()) {
				Point2i targetRegion = entry.getKey();
				Set<Point2i> sourceRegions = entry.getValue();
				File targetFile = FileHelper.createMCAFilePath(targetRegion);

				Map<Point2i, Set<Point2i>> localSourceSelection = new HashMap<>();
				Set<Point2i> localTargetSelection;

				// creating local source selection
				if (sourceSelection == null) {
					localSourceSelection = Collections.emptyMap();
				} else {
					for (Point2i sourceRegion : sourceRegions) {
						Set<Point2i> localSourceChunks;
						localSourceChunks = sourceSelection.getSelection().getOrDefault(sourceRegion, Collections.emptySet());
						localSourceSelection.put(sourceRegion, localSourceChunks);
					}
				}

				// creating local target selection
				if (targetSelection == null) {
					localTargetSelection = Collections.emptySet();
				} else {
					localTargetSelection = targetSelection.getSelection().getOrDefault(targetRegion, Collections.emptySet());
				}

				boolean sourceInverted = sourceSelection != null && sourceSelection.isInverted();
				boolean targetInverted = targetSelection != null && targetSelection.isInverted();

				MCAFilePipe.addJob(new MCAChunkImporterLoadJob(targetFile, sourceDirectory, targetRegion, sourceRegions, offset, progressChannel, overwrite, localSourceSelection, sourceInverted, localTargetSelection, targetInverted, ranges, tempFilesMap));
			}
		} catch (Exception ex) {
			Debug.dumpException("failed creating jobs to import chunks", ex);
		}
	}

	// returns a map where the key is a target region and the value is a set of all source regions, if they exist
	public static Map<Point2i, Set<Point2i>> createTargetSourceMapping(File sourceDirectory, SelectionData sourceSelection, SelectionData targetSelection, Point2i offset) {
		Map<Point2i, Set<Point2i>> sourceTargetMapping = new HashMap<>();

		// get all possible source files
		Set<Point2i> sourceRegions = FileHelper.parseAllMCAFileNames(sourceDirectory);
		if (sourceSelection != null) {
			sourceRegions.removeIf(s -> !sourceSelection.isRegionSelected(s));
		}

		// get target regions with offset based on source regions, target selection and inversion
		for (Point2i sourceRegion : sourceRegions) {
			Set<Point2i> targetRegions = getTargetRegions(sourceRegion, offset);
			if (targetSelection != null) {
				targetRegions.removeIf(t -> !targetSelection.isRegionSelected(t));
			}
			if (targetRegions.isEmpty()) {
				continue;
			}
			sourceTargetMapping.put(sourceRegion, targetRegions);
		}

		// now we invert the mapping to create a target -> sources mapping
		Map<Point2i, Set<Point2i>> targetSourceMapping = new HashMap<>();
		int initSize = Math.min((offset.getX() % 32 != 0 ? 2 : 0) + (offset.getZ() % 32 != 0 ? 2 : 0), 1); // init with 1, 2, 4
		for (Map.Entry<Point2i, Set<Point2i>> entry : sourceTargetMapping.entrySet()) {
			for (Point2i target : entry.getValue()) {
				if (targetSourceMapping.containsKey(target)) {
					targetSourceMapping.get(target).add(entry.getKey());
				} else {
					targetSourceMapping.compute(target, (k, o) -> {
						Set<Point2i> sources = new HashSet<>(initSize);
						sources.add(entry.getKey());
						return sources;
					});
				}
			}
		}

		return targetSourceMapping;
	}

	private static class MCAChunkImporterLoadJob extends LoadDataJob {

		private final Point2i target;
		private final Set<Point2i> sources;
		private final File sourceDir;
		private final Point2i offset;
		private final Progress progressChannel;
		private final boolean overwrite;
		private final Map<Point2i, Set<Point2i>> sourceChunks;
		private final boolean sourceChunksInverted;
		private final Set<Point2i> selection;
		private final boolean targetChunksInverted;
		private final List<Range> ranges;
		private final Map<Point2i, File> tempFilesMap;

		private MCAChunkImporterLoadJob(File targetFile, File sourceDir, Point2i target, Set<Point2i> sources, Point2i offset, Progress progressChannel, boolean overwrite, Map<Point2i, Set<Point2i>> sourceChunks, boolean sourceChunksInverted, Set<Point2i> selection, boolean targetChunksInverted, List<Range> ranges, Map<Point2i, File> tempFilesMap) {
			super(targetFile);
			this.target = target;
			this.sources = sources;
			this.sourceDir = sourceDir;
			this.offset = offset;
			this.progressChannel = progressChannel;
			this.overwrite = overwrite;
			this.sourceChunks = sourceChunks;
			this.sourceChunksInverted = sourceChunksInverted;
			this.selection = selection;
			this.targetChunksInverted = targetChunksInverted;
			this.ranges = ranges;
			this.tempFilesMap = tempFilesMap;
		}

		@Override
		public void execute() {

			// special case for non existing destination file and no offset
			if (offset.getX() == 0 && offset.getZ() == 0 && !getFile().exists() && (selection == null || selection.size() == 0)) {
				//if the entire mca file doesn't exist, just copy it over
				File source = new File(sourceDir, getFile().getName());
				try {
					Files.copy(source.toPath(), getFile().toPath());
				} catch (IOException ex) {
					Debug.dumpException(String.format("failed to copy file %s to %s", source, getFile()), ex);
				}
				progressChannel.incrementProgress(getFile().getName(), sources.size());
				return;
			}

			Map<Point2i, byte[]> sourceDataMapping = new HashMap<>();

			for (Point2i sourceRegion : sources) {
				File source;
				if (tempFilesMap != null && tempFilesMap.containsKey(sourceRegion)) {
					source = tempFilesMap.get(sourceRegion);
				} else {
					source = new File(sourceDir, FileHelper.createMCAFileName(sourceRegion));
				}

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

			if (getFile().exists() && getFile().length() > 0) {
				destData = load();
				if (destData == null) {
					Debug.errorf("error loading destination mca file %s", getFile().getName());
					progressChannel.incrementProgress(getFile().getName(), sourceDataMapping.size());
					return;
				}
			} else {
				destData = null;
			}

			MCAFilePipe.executeProcessData(new MCAChunkImporterProcessJob(getFile(), sourceDir, target, sourceDataMapping, destData, offset, progressChannel, overwrite, sourceChunks, sourceChunksInverted, selection, targetChunksInverted, ranges));
		}
	}

	private static class MCAChunkImporterProcessJob extends ProcessDataJob {

		private final File sourceDir;
		private final Point2i target;
		private final Map<Point2i, byte[]> sourceDataMapping;
		private final Point2i offset;
		private final Progress progressChannel;
		private final boolean overwrite;
		private final Map<Point2i, Set<Point2i>> sourceChunks;
		private final boolean sourceChunksInverted;
		private final Set<Point2i> selection;
		private final boolean targetChunksInverted;
		private final List<Range> ranges;

		private MCAChunkImporterProcessJob(File targetFile, File sourceDir, Point2i target, Map<Point2i, byte[]> sourceDataMapping, byte[] destData, Point2i offset, Progress progressChannel, boolean overwrite, Map<Point2i, Set<Point2i>> sourceChunks, boolean sourceChunksInverted, Set<Point2i> selection, boolean targetChunksInverted, List<Range> ranges) {
			super(targetFile, destData);
			this.sourceDir = sourceDir;
			this.target = target;
			this.sourceDataMapping = sourceDataMapping;
			this.offset = offset;
			this.progressChannel = progressChannel;
			this.overwrite = overwrite;
			this.sourceChunks = sourceChunks;
			this.sourceChunksInverted = sourceChunksInverted;
			this.selection = selection;
			this.targetChunksInverted = targetChunksInverted;
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

				Set<Point2i> selection = this.selection;
				// invert target selection if necessary
				if (targetChunksInverted) {
					selection = SelectionData.createInvertedRegionSet(target, selection);
				}

				Map<Point2i, Set<Point2i>> sourceChunks = this.sourceChunks;
				// invert source selection if necessary
				if (sourceChunksInverted) {
					sourceChunks.replaceAll(SelectionData::createInvertedRegionSet);
				}

				for (Map.Entry<Point2i, byte[]> sourceData : sourceDataMapping.entrySet()) {
					MCAFile source = MCAFile.readAll(new File(sourceDir, FileHelper.createMCAFileName(sourceData.getKey())), new ByteArrayPointer(sourceData.getValue()));

					Debug.dumpf("merging chunks from region %s into %s", sourceData.getKey(), target);

					source.mergeChunksInto(destination, offset, overwrite, sourceChunks == null ? null : sourceChunks.get(sourceData.getKey()), selection == null ? null : selection.size() == 0 ? null : selection, ranges);
				}

				MCAFilePipe.executeSaveData(new MCAChunkImporterSaveJob(getFile(), destination, progressChannel));

			} catch (Exception ex) {
				Debug.dumpException("failed to process chunk import for " + getFile().getName(), ex);
				progressChannel.incrementProgress(getFile().getName());
			}

			Debug.dumpf("took %s to merge chunks into %s with offset %s", t, getFile(), offset);
		}
	}

	private static class MCAChunkImporterSaveJob extends SaveDataJob<MCAFile> {

		private final Progress progressChannel;

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
				Debug.dumpException("failed to save imported chunks to " + getFile(), ex);
			}
			progressChannel.incrementProgress(getFile().getName());
			Debug.dumpf("took %s to save data to %s", t, getFile().getName());
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
