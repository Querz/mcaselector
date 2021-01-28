package net.querz.mcaselector.io;

import net.querz.mcaselector.Config;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.io.mca.MCAFile;
import net.querz.mcaselector.io.mca.Region;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.progress.Progress;
import net.querz.mcaselector.progress.Timer;
import net.querz.mcaselector.property.DataProperty;
import net.querz.mcaselector.range.Range;
import net.querz.mcaselector.text.Translation;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ChunkImporter {

	private ChunkImporter() {}

	public static void importChunks(WorldDirectories source, Progress progressChannel, boolean headless, boolean overwrite, SelectionData sourceSelection, SelectionData targetSelection, List<Range> ranges, Point2i offset, DataProperty<Map<Point2i, RegionDirectories>> tempFiles) {
		try {
			WorldDirectories wd = Config.getWorldDirs();
			RegionDirectories[] rd = wd.listRegions();
			if (rd == null || rd.length == 0) {
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
			Map<Point2i, RegionDirectories> tempFilesMap = null;
			if (source.sharesDirectories(Config.getWorldDirs())) {
				tempFilesMap = new HashMap<>();
			}
			tempFiles.set(tempFilesMap);

			// only pass regions here
			Map<Point2i, Set<Point2i>> targetMapping = createTargetSourceMapping(source.getRegion(), sourceSelection, targetSelection, offset);

			progressChannel.setMax(targetMapping.size());
			progressChannel.updateProgress(rd[0].getLocationAsFileName(), 0);

			// create local source and local target selections
			for (Map.Entry<Point2i, Set<Point2i>> entry : targetMapping.entrySet()) {
				Point2i targetRegion = entry.getKey();
				Set<Point2i> sourceRegions = entry.getValue();

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

				RegionDirectories targetDirs = FileHelper.createRegionDirectories(targetRegion);

				MCAFilePipe.addJob(new MCAChunkImporterLoadJob(targetDirs, source, targetRegion, sourceRegions, offset, progressChannel, overwrite, localSourceSelection, sourceInverted, localTargetSelection, targetInverted, ranges, tempFilesMap));
			}
		} catch (Exception ex) {
			Debug.dumpException("failed creating jobs to import chunks", ex);
		}
	}

	// returns a map where the key is a target region and the value is a set of all source regions, if they exist
	private static Map<Point2i, Set<Point2i>> createTargetSourceMapping(File sourceDirectory, SelectionData sourceSelection, SelectionData targetSelection, Point2i offset) {
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

	private static class MCAChunkImporterLoadJob extends LoadDataJob {

		private final Point2i target;
		private final Set<Point2i> sources;
		private final WorldDirectories sourceDirs;
		private final Point2i offset;
		private final Progress progressChannel;
		private final boolean overwrite;
		private final Map<Point2i, Set<Point2i>> sourceChunks;
		private final boolean sourceChunksInverted;
		private final Set<Point2i> selection;
		private final boolean targetChunksInverted;
		private final List<Range> ranges;
		private final Map<Point2i, RegionDirectories> tempFilesMap;

		private MCAChunkImporterLoadJob(RegionDirectories targetDirs, WorldDirectories sourceDirs, Point2i target, Set<Point2i> sources, Point2i offset, Progress progressChannel, boolean overwrite, Map<Point2i, Set<Point2i>> sourceChunks, boolean sourceChunksInverted, Set<Point2i> selection, boolean targetChunksInverted, List<Range> ranges, Map<Point2i, RegionDirectories> tempFilesMap) {
			super(targetDirs);
			this.target = target;
			this.sources = sources;
			this.sourceDirs = sourceDirs;
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

			boolean allCopied = true;

			// try to copy files directly if there is no offset, no selection and the target file does not exist
			if (offset.getX() == 0 && offset.getZ() == 0 && (selection == null || selection.size() == 0)) {
				if (!getRegionDirectories().getRegion().exists()) {
					//if the entire mca file doesn't exist, just copy it over
					File source = new File(sourceDirs.getRegion(), getRegionDirectories().getLocationAsFileName());
					try {
						Files.copy(source.toPath(), getRegionDirectories().getRegion().toPath());
					} catch (IOException ex) {
						Debug.dumpException(String.format("failed to copy file %s to %s", source, getRegionDirectories().getRegion()), ex);
					}
				} else {
					allCopied = false;
				}

				if (!getRegionDirectories().getPoi().exists()) {
					File source = new File(sourceDirs.getPoi(), getRegionDirectories().getLocationAsFileName());
					try {
						Files.copy(source.toPath(), getRegionDirectories().getPoi().toPath());
					} catch (IOException ex) {
						Debug.dumpException(String.format("failed to copy file %s to %s", source, getRegionDirectories().getPoi()), ex);
					}
				} else {
					allCopied = false;
				}

				if (!getRegionDirectories().getEntities().exists()) {
					File source = new File(sourceDirs.getEntities(), getRegionDirectories().getLocationAsFileName());
					try {
						Files.copy(source.toPath(), getRegionDirectories().getEntities().toPath());
					} catch (IOException ex) {
						Debug.dumpException(String.format("failed to copy file %s to %s", source, getRegionDirectories().getEntities()), ex);
					}
				} else {
					allCopied = false;
				}
			}

			if (!allCopied) {
				progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
				return;
			}


			// ---------------------------------------------------------------------------------------------------------

			// LOAD SOURCE DATA
			Map<Point2i, byte[]> sourceDataMappingRegion = new HashMap<>();
			Map<Point2i, byte[]> sourceDataMappingPoi = new HashMap<>();
			Map<Point2i, byte[]> sourceDataMappingEntities = new HashMap<>();

			for (Point2i source : sources) {
				RegionDirectories sourceDirs;
				if (tempFilesMap != null && tempFilesMap.containsKey(source)) {
					sourceDirs = tempFilesMap.get(source);
				} else {
					sourceDirs = new RegionDirectories();
				}

				File sourceFile;
				byte[] sourceData;

				// region
				if (sourceDirs.getRegion() != null) {
					sourceFile = sourceDirs.getRegion();
				} else {
					sourceFile = new File(this.sourceDirs.getRegion(), FileHelper.createMCAFileName(source));
				}
				if (sourceFile.exists()) {
					sourceData = load(sourceFile);
					if (sourceData == null) {
						Debug.errorf("failed to load source mca file %s", sourceFile);
					} else {
						sourceDataMappingRegion.put(source, sourceData);
					}
				}

				// poi
				if (sourceDirs.getPoi() != null) {
					sourceFile = sourceDirs.getPoi();
				} else {
					sourceFile = new File(this.sourceDirs.getPoi(), FileHelper.createMCAFileName(source));
				}
				if (sourceFile.exists()) {
					sourceData = load(sourceFile);
					if (sourceData == null) {
						Debug.errorf("failed to load source mca file %s", sourceFile);
					} else {
						sourceDataMappingPoi.put(source, sourceData);
					}
				}

				// entities
				if (sourceDirs.getEntities() != null) {
					sourceFile = sourceDirs.getEntities();
				} else {
					sourceFile = new File(this.sourceDirs.getEntities(), FileHelper.createMCAFileName(source));
				}
				if (sourceFile.exists()) {
					sourceData = load(sourceFile);
					if (sourceData == null) {
						Debug.errorf("failed to load source mca file %s", sourceFile);
					} else {
						sourceDataMappingEntities.put(source, sourceData);
					}
				}
			}

			// ---------------------------------------------------------------------------------------------------------

			// check if we need to do anything
			if (sourceDataMappingRegion.isEmpty() && sourceDataMappingPoi.isEmpty() && sourceDataMappingEntities.isEmpty()) {
				Debug.errorf("did not load any source mca files to merge into %s", getRegionDirectories().getLocationAsFileName());
				progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
				return;
			}

			// ---------------------------------------------------------------------------------------------------------

			// LOAD DESTINATION DATA
			byte[] destDataRegion = null;
			if (getRegionDirectories().getRegion().exists() && getRegionDirectories().getRegion().length() > 0) {
				destDataRegion = load(getRegionDirectories().getRegion());
				if (destDataRegion == null) {
					Debug.errorf("failed to load destination mca file %s", getRegionDirectories().getRegion());
					progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
					return;
				}
			}

			byte[] destDataPoi = null;
			if (getRegionDirectories().getPoi().exists() && getRegionDirectories().getPoi().length() > 0) {
				destDataPoi = load(getRegionDirectories().getPoi());
				if (destDataPoi == null) {
					Debug.errorf("failed to load destination mca file %s", getRegionDirectories().getPoi());
					progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
					return;
				}
			}

			byte[] destDataEntities = null;
			if (getRegionDirectories().getEntities().exists() && getRegionDirectories().getEntities().length() > 0) {
				destDataEntities = load(getRegionDirectories().getEntities());
				if (destDataEntities == null) {
					Debug.errorf("failed to load destination mca file %s", getRegionDirectories().getEntities());
					progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
					return;
				}
			}

			MCAFilePipe.executeProcessData(new MCAChunkImporterProcessJob(getRegionDirectories(), sourceDirs, target, sourceDataMappingRegion, sourceDataMappingPoi, sourceDataMappingEntities, destDataRegion, destDataPoi, destDataEntities, offset, progressChannel, overwrite, sourceChunks, sourceChunksInverted, selection, targetChunksInverted, ranges));
		}
	}

	private static class MCAChunkImporterProcessJob extends ProcessDataJob {

		private final WorldDirectories sourceDirs;
		private final Point2i target;
		private final Map<Point2i, byte[]> sourceDataMappingRegion, sourceDataMappingPoi, sourceDataMappingEntities;
		private final Point2i offset;
		private final Progress progressChannel;
		private final boolean overwrite;
		private final Map<Point2i, Set<Point2i>> sourceChunks;
		private final boolean sourceChunksInverted;
		private final Set<Point2i> selection;
		private final boolean targetChunksInverted;
		private final List<Range> ranges;

		private MCAChunkImporterProcessJob(RegionDirectories targetDirs, WorldDirectories sourceDirs, Point2i target, Map<Point2i, byte[]> sourceDataMappingRegion, Map<Point2i, byte[]> sourceDataMappingPoi, Map<Point2i, byte[]> sourceDataMappingEntities, byte[] destDataRegion, byte[] destDataPoi, byte[] destDataEntities, Point2i offset, Progress progressChannel, boolean overwrite, Map<Point2i, Set<Point2i>> sourceChunks, boolean sourceChunksInverted, Set<Point2i> selection, boolean targetChunksInverted, List<Range> ranges) {
			super(targetDirs, destDataRegion, destDataPoi, destDataEntities);
			this.sourceDirs = sourceDirs;
			this.target = target;
			this.sourceDataMappingRegion = sourceDataMappingRegion;
			this.sourceDataMappingPoi = sourceDataMappingPoi;
			this.sourceDataMappingEntities = sourceDataMappingEntities;
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
				// load target region
				Region targetRegion = Region.loadRegion(getRegionDirectories(), getRegionData(), getPoiData(),getEntitiesData());

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

				for (Map.Entry<Point2i, byte[]> sourceData : sourceDataMappingRegion.entrySet()) {
					MCAFile source = new MCAFile(new File(sourceDirs.getRegion(), FileHelper.createMCAFileName(sourceData.getKey())));
					source.load(new ByteArrayPointer(sourceData.getValue()));

					Debug.dumpf("merging region chunks from %s into %s", sourceData.getKey(), target);

					if (targetRegion.getRegion() == null) {
						targetRegion.setRegion(new MCAFile(getRegionDirectories().getRegion()));
					}

					source.mergeChunksInto(targetRegion.getRegion(), offset, overwrite, sourceChunks == null ? null : sourceChunks.get(sourceData.getKey()), selection == null ? null : selection.size() == 0 ? null : selection, ranges);
				}

				for (Map.Entry<Point2i, byte[]> sourceData : sourceDataMappingPoi.entrySet()) {
					MCAFile source = new MCAFile(new File(sourceDirs.getPoi(), FileHelper.createMCAFileName(sourceData.getKey())));
					source.load(new ByteArrayPointer(sourceData.getValue()));

					Debug.dumpf("merging poi chunks from %s into %s", sourceData.getKey(), target);

					if (targetRegion.getPoi() == null) {
						targetRegion.setPoi(new MCAFile(getRegionDirectories().getPoi()));
					}

					source.mergeChunksInto(targetRegion.getPoi(), offset, overwrite, sourceChunks == null ? null : sourceChunks.get(sourceData.getKey()), selection == null ? null : selection.size() == 0 ? null : selection, ranges);
				}

				for (Map.Entry<Point2i, byte[]> sourceData : sourceDataMappingEntities.entrySet()) {
					MCAFile source = new MCAFile(new File(sourceDirs.getEntities(), FileHelper.createMCAFileName(sourceData.getKey())));
					source.load(new ByteArrayPointer(sourceData.getValue()));

					Debug.dumpf("merging entities chunks from %s into %s", sourceData.getKey(), target);

					if (targetRegion.getEntities() == null) {
						targetRegion.setEntities(new MCAFile(getRegionDirectories().getEntities()));
					}

					source.mergeChunksInto(targetRegion.getEntities(), offset, overwrite, sourceChunks == null ? null : sourceChunks.get(sourceData.getKey()), selection == null ? null : selection.size() == 0 ? null : selection, ranges);
				}

				// -----------------------------------------------------------------------------------------------------

				MCAFilePipe.executeSaveData(new MCAChunkImporterSaveJob(getRegionDirectories(), targetRegion, progressChannel));

			} catch (Exception ex) {
				Debug.dumpException("failed to process chunk import for " + getRegionDirectories().getLocationAsFileName(), ex);
				progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
			}

			Debug.dumpf("took %s to merge chunks into %s with offset %s", t, getRegionDirectories().getLocation(), offset);
		}
	}

	private static class MCAChunkImporterSaveJob extends SaveDataJob<Region> {

		private final Progress progressChannel;

		private MCAChunkImporterSaveJob(RegionDirectories targetDirs, Region data, Progress progressChannel) {
			super(targetDirs, data);
			this.progressChannel = progressChannel;
		}

		@Override
		public void execute() {
			Timer t = new Timer();
			try {
				getData().saveWithTempFiles();
			} catch (Exception ex) {
				Debug.dumpException("failed to save imported chunks to " + getRegionDirectories().getLocationAsFileName(), ex);
			}
			progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
			Debug.dumpf("took %s to save data for %s", t, getRegionDirectories().getLocationAsFileName());
		}
	}
}
