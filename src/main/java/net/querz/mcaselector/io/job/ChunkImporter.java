package net.querz.mcaselector.io.job;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.querz.mcaselector.Config;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.io.ByteArrayPointer;
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.io.JobHandler;
import net.querz.mcaselector.io.RegionDirectories;
import net.querz.mcaselector.io.SelectionData;
import net.querz.mcaselector.io.WorldDirectories;
import net.querz.mcaselector.io.mca.EntitiesMCAFile;
import net.querz.mcaselector.io.mca.PoiMCAFile;
import net.querz.mcaselector.io.mca.Region;
import net.querz.mcaselector.io.mca.RegionMCAFile;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.point.Point3i;
import net.querz.mcaselector.progress.Progress;
import net.querz.mcaselector.progress.Timer;
import net.querz.mcaselector.property.DataProperty;
import net.querz.mcaselector.range.Range;
import net.querz.mcaselector.text.Translation;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ChunkImporter {

	private ChunkImporter() {}

	public static void importChunks(WorldDirectories source, Progress progressChannel, boolean headless, boolean overwrite, SelectionData sourceSelection, SelectionData targetSelection, List<Range> ranges, Point3i offset, DataProperty<Map<Point2i, RegionDirectories>> tempFiles) {
		try {
			WorldDirectories wd = Config.getWorldDirs();
			RegionDirectories[] rd = wd.listRegions(targetSelection);
			if (rd == null || rd.length == 0) {
				if (headless) {
					progressChannel.done("no files");
				} else {
					progressChannel.done(Translation.DIALOG_PROGRESS_NO_FILES.toString());
				}
				return;
			}

			JobHandler.clearQueues();

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
			Long2ObjectOpenHashMap<LongOpenHashSet> targetMapping = createTargetSourceMapping(source.getRegion(), sourceSelection, targetSelection, offset.toPoint2i());

			progressChannel.setMax(targetMapping.size());
			progressChannel.updateProgress(rd[0].getLocationAsFileName(), 0);

			// create local source and local target selections
			for (Long2ObjectMap.Entry<LongOpenHashSet> entry : targetMapping.long2ObjectEntrySet()) {
				long targetRegion = entry.getLongKey();
				LongOpenHashSet sourceRegions = entry.getValue();

				Long2ObjectOpenHashMap<LongOpenHashSet> localSourceSelection = new Long2ObjectOpenHashMap<>();
				LongOpenHashSet localTargetSelection;

				// creating local source selection
				if (sourceSelection == null) {
					localSourceSelection = new Long2ObjectOpenHashMap<>(0);
				} else {
					for (long sourceRegion : sourceRegions) {
						LongOpenHashSet localSourceChunks;
						localSourceChunks = sourceSelection.selection().getOrDefault(sourceRegion, new LongOpenHashSet(0));
						localSourceSelection.put(sourceRegion, localSourceChunks);
					}
				}

				// creating local target selection
				if (targetSelection == null) {
					localTargetSelection = new LongOpenHashSet(0);
				} else {
					localTargetSelection = targetSelection.selection().getOrDefault(targetRegion, new LongOpenHashSet(0));
				}

				boolean sourceInverted = sourceSelection != null && sourceSelection.inverted();
				boolean targetInverted = targetSelection != null && targetSelection.inverted();

				Point2i target = new Point2i(targetRegion);
				RegionDirectories targetDirs = FileHelper.createRegionDirectories(target);

				JobHandler.addJob(new MCAChunkImporterProcessJob(targetDirs, source, target, sourceRegions, offset, progressChannel, overwrite, localSourceSelection, sourceInverted, localTargetSelection, targetInverted, ranges, tempFilesMap));
			}
		} catch (Exception ex) {
			Debug.dumpException("failed creating jobs to import chunks", ex);
		}
	}

	// returns a map where the key is a target region and the value is a set of all source regions, if they exist
	private static Long2ObjectOpenHashMap<LongOpenHashSet> createTargetSourceMapping(File sourceDirectory, SelectionData sourceSelection, SelectionData targetSelection, Point2i offset) {
		Long2ObjectOpenHashMap<LongOpenHashSet> sourceTargetMapping = new Long2ObjectOpenHashMap<>();

		// get all possible source files
		LongOpenHashSet sourceRegions = FileHelper.parseAllMCAFileNames(sourceDirectory);
		if (sourceSelection != null) {
			sourceRegions.removeIf(s -> !sourceSelection.isRegionSelected(s));
		}

		// get target regions with offset based on source regions, target selection and inversion
		for (long sourceRegion : sourceRegions) {
			LongOpenHashSet targetRegions = getTargetRegions(new Point2i(sourceRegion), offset);
			if (targetSelection != null) {
				targetRegions.removeIf(t -> !targetSelection.isRegionSelected(t));
			}
			if (targetRegions.isEmpty()) {
				continue;
			}
			sourceTargetMapping.put(sourceRegion, targetRegions);
		}

		// now we invert the mapping to create a target -> sources mapping
		Long2ObjectOpenHashMap<LongOpenHashSet> targetSourceMapping = new Long2ObjectOpenHashMap<>();
		int initSize = Math.min((offset.getX() % 32 != 0 ? 2 : 0) + (offset.getZ() % 32 != 0 ? 2 : 0), 1); // init with 1, 2, 4
		for (Long2ObjectMap.Entry<LongOpenHashSet> entry : sourceTargetMapping.long2ObjectEntrySet()) {
			for (long target : entry.getValue()) {
				if (targetSourceMapping.containsKey(target)) {
					targetSourceMapping.get(target).add(entry.getLongKey());
				} else {
					targetSourceMapping.compute(target, (k, o) -> {
						LongOpenHashSet sources = new LongOpenHashSet(initSize);
						sources.add(entry.getLongKey());
						return sources;
					});
				}
			}
		}

		return targetSourceMapping;
	}

	// source is a region coordinate, offset is a chunk coordinate
	private static LongOpenHashSet getTargetRegions(Point2i source, Point2i offset) {
		LongOpenHashSet result = new LongOpenHashSet(4);
		Point2i sourceChunk = source.regionToChunk().add(offset);
		result.add(sourceChunk.chunkToRegion().asLong());
		result.add(sourceChunk.add(31, 0).chunkToRegion().asLong());
		result.add(sourceChunk.add(0, 31).chunkToRegion().asLong());
		result.add(sourceChunk.add(31, 31).chunkToRegion().asLong());
		return result;
	}

	private static class MCAChunkImporterProcessJob extends ProcessDataJob {

		private final WorldDirectories sourceDirs;
		private final LongOpenHashSet sources;
		private final Point2i target;
		private final Point3i offset;
		private final Progress progressChannel;
		private final boolean overwrite;
		private final Long2ObjectOpenHashMap<LongOpenHashSet> sourceChunks;
		private final boolean sourceChunksInverted;
		private final LongOpenHashSet selection;
		private final boolean targetChunksInverted;
		private final List<Range> ranges;
		private final Map<Point2i, RegionDirectories> tempFilesMap;

		private MCAChunkImporterProcessJob(RegionDirectories targetDirs, WorldDirectories sourceDirs, Point2i target, LongOpenHashSet sources, Point3i offset, Progress progressChannel, boolean overwrite, Long2ObjectOpenHashMap<LongOpenHashSet> sourceChunks, boolean sourceChunksInverted, LongOpenHashSet selection, boolean targetChunksInverted, List<Range> ranges, Map<Point2i, RegionDirectories> tempFilesMap) {
			super(targetDirs, PRIORITY_LOW);
			this.sourceDirs = sourceDirs;
			this.sources = sources;
			this.target = target;
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
		public boolean execute() {
			// try to copy files directly if there is no offset, no selection and the target file does not exist
			if (offset.getX() == 0 && offset.getY() == 0 && offset.getZ() == 0 && (selection == null || selection.size() == 0) && sourceChunks == null && !sourceChunksInverted) {
				boolean allCopied = true;

				if (!getRegionDirectories().getRegion().exists()) {
					//if the entire mca file doesn't exist, just copy it over
					File source = new File(sourceDirs.getRegion(), getRegionDirectories().getLocationAsFileName());
					if (source.exists()) {
						try {
							Files.copy(source.toPath(), getRegionDirectories().getRegion().toPath());
						} catch (IOException ex) {
							Debug.dumpException(String.format("failed to copy file %s to %s", source, getRegionDirectories().getRegion()), ex);
						}
					}
				} else {
					allCopied = false;
				}

				if (!getRegionDirectories().getPoi().exists() && sourceDirs.getPoi() != null) {
					File source = new File(sourceDirs.getPoi(), getRegionDirectories().getLocationAsFileName());
					if (source.exists()) {
						try {
							Files.copy(source.toPath(), getRegionDirectories().getPoi().toPath());
						} catch (IOException ex) {
							Debug.dumpException(String.format("failed to copy file %s to %s", source, getRegionDirectories().getPoi()), ex);
						}
					}
				} else {
					allCopied = false;
				}

				if (!getRegionDirectories().getEntities().exists() && sourceDirs.getEntities() != null) {
					File source = new File(sourceDirs.getEntities(), getRegionDirectories().getLocationAsFileName());
					if (source.exists()) {
						try {
							Files.copy(source.toPath(), getRegionDirectories().getEntities().toPath());
						} catch (IOException ex) {
							Debug.dumpException(String.format("failed to copy file %s to %s", source, getRegionDirectories().getEntities()), ex);
						}
					}
				} else {
					allCopied = false;
				}

				if (allCopied) {
					progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
					return true;
				}
			}

			// ---------------------------------------------------------------------------------------------------------

			// LOAD SOURCE DATA
			Map<Point2i, byte[]> sourceDataMappingRegion = new HashMap<>();
			Map<Point2i, byte[]> sourceDataMappingPoi = new HashMap<>();
			Map<Point2i, byte[]> sourceDataMappingEntities = new HashMap<>();

			for (long source : sources) {
				Point2i s = new Point2i(source);
				RegionDirectories sourceDirs;
				if (tempFilesMap != null && tempFilesMap.containsKey(s)) {
					sourceDirs = tempFilesMap.get(s);
				} else {
					sourceDirs = new RegionDirectories();
				}

				File sourceFile;
				byte[] sourceData;

				// region
				if (sourceDirs.getRegion() != null) {
					sourceFile = sourceDirs.getRegion();
				} else {
					sourceFile = new File(this.sourceDirs.getRegion(), FileHelper.createMCAFileName(s));
				}
				if (sourceFile.exists()) {
					sourceData = load(sourceFile);
					if (sourceData == null) {
						Debug.errorf("failed to load source mca file %s", sourceFile);
					} else {
						sourceDataMappingRegion.put(s, sourceData);
					}
				}

				// poi
				if (sourceDirs.getPoi() != null) {
					sourceFile = sourceDirs.getPoi();
				} else {
					sourceFile = new File(this.sourceDirs.getPoi(), FileHelper.createMCAFileName(s));
				}
				if (sourceFile.exists()) {
					sourceData = load(sourceFile);
					if (sourceData == null) {
						Debug.errorf("failed to load source mca file %s", sourceFile);
					} else {
						sourceDataMappingPoi.put(s, sourceData);
					}
				}

				// entities
				if (sourceDirs.getEntities() != null) {
					sourceFile = sourceDirs.getEntities();
				} else {
					sourceFile = new File(this.sourceDirs.getEntities(), FileHelper.createMCAFileName(s));
				}
				if (sourceFile.exists()) {
					sourceData = load(sourceFile);
					if (sourceData == null) {
						Debug.errorf("failed to load source mca file %s", sourceFile);
					} else {
						sourceDataMappingEntities.put(s, sourceData);
					}
				}
			}

			// ---------------------------------------------------------------------------------------------------------

			// check if we need to do anything
			if (sourceDataMappingRegion.isEmpty() && sourceDataMappingPoi.isEmpty() && sourceDataMappingEntities.isEmpty()) {
				Debug.errorf("did not load any source mca files to merge into %s", getRegionDirectories().getLocationAsFileName());
				progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
				return true;
			}

			// ---------------------------------------------------------------------------------------------------------

			// LOAD DESTINATION DATA
			byte[] destDataRegion = null;
			if (getRegionDirectories().getRegion().exists() && getRegionDirectories().getRegion().length() > 0) {
				destDataRegion = load(getRegionDirectories().getRegion());
				if (destDataRegion == null) {
					Debug.errorf("failed to load destination mca file %s", getRegionDirectories().getRegion());
					progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
					return true;
				}
			}

			byte[] destDataPoi = null;
			if (getRegionDirectories().getPoi().exists() && getRegionDirectories().getPoi().length() > 0) {
				destDataPoi = load(getRegionDirectories().getPoi());
				if (destDataPoi == null) {
					Debug.errorf("failed to load destination mca file %s", getRegionDirectories().getPoi());
					progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
					return true;
				}
			}

			byte[] destDataEntities = null;
			if (getRegionDirectories().getEntities().exists() && getRegionDirectories().getEntities().length() > 0) {
				destDataEntities = load(getRegionDirectories().getEntities());
				if (destDataEntities == null) {
					Debug.errorf("failed to load destination mca file %s", getRegionDirectories().getEntities());
					progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
					return true;
				}
			}

			Timer t = new Timer();
			try {
				// load target region
				Region targetRegion = Region.loadRegion(getRegionDirectories(), destDataRegion, destDataPoi, destDataEntities);

				LongOpenHashSet selection = this.selection;
				// invert target selection if necessary
				if (targetChunksInverted) {
					selection = SelectionData.createInvertedRegionSet(target, selection);
				}

				Long2ObjectOpenHashMap<LongOpenHashSet> sourceChunks = this.sourceChunks;
				// invert source selection if necessary
				if (sourceChunksInverted) {
					sourceChunks.replaceAll(SelectionData::createInvertedRegionSet);
				}

				for (Map.Entry<Point2i, byte[]> sourceData : sourceDataMappingRegion.entrySet()) {
					RegionMCAFile source = new RegionMCAFile(new File(sourceDirs.getRegion(), FileHelper.createMCAFileName(sourceData.getKey())));
					source.load(new ByteArrayPointer(sourceData.getValue()));

					Debug.dumpf("merging region chunks from %s into %s", sourceData.getKey(), target);

					if (targetRegion.getRegion() == null) {
						targetRegion.setRegion(new RegionMCAFile(getRegionDirectories().getRegion()));
					}

					source.mergeChunksInto(targetRegion.getRegion(), offset, overwrite, sourceChunks == null ? null : sourceChunks.get(sourceData.getKey().asLong()), selection == null ? null : selection.size() == 0 ? null : selection, ranges);
				}

				for (Map.Entry<Point2i, byte[]> sourceData : sourceDataMappingPoi.entrySet()) {
					PoiMCAFile source = new PoiMCAFile(new File(sourceDirs.getPoi(), FileHelper.createMCAFileName(sourceData.getKey())));
					source.load(new ByteArrayPointer(sourceData.getValue()));

					Debug.dumpf("merging poi chunks from %s into %s", sourceData.getKey(), target);

					if (targetRegion.getPoi() == null) {
						targetRegion.setPoi(new PoiMCAFile(getRegionDirectories().getPoi()));
					}

					source.mergeChunksInto(targetRegion.getPoi(), offset, overwrite, sourceChunks == null ? null : sourceChunks.get(sourceData.getKey().asLong()), selection == null ? null : selection.size() == 0 ? null : selection, ranges);
				}

				for (Map.Entry<Point2i, byte[]> sourceData : sourceDataMappingEntities.entrySet()) {
					EntitiesMCAFile source = new EntitiesMCAFile(new File(sourceDirs.getEntities(), FileHelper.createMCAFileName(sourceData.getKey())));
					source.load(new ByteArrayPointer(sourceData.getValue()));

					Debug.dumpf("merging entities chunks from %s into %s", sourceData.getKey(), target);

					if (targetRegion.getEntities() == null) {
						targetRegion.setEntities(new EntitiesMCAFile(getRegionDirectories().getEntities()));
					}

					source.mergeChunksInto(targetRegion.getEntities(), offset, overwrite, sourceChunks == null ? null : sourceChunks.get(sourceData.getKey().asLong()), selection == null ? null : selection.size() == 0 ? null : selection, ranges);
				}

				// -----------------------------------------------------------------------------------------------------

				JobHandler.executeSaveData(new MCAChunkImporterSaveJob(getRegionDirectories(), targetRegion, progressChannel));
				Debug.dumpf("took %s to merge chunks into %s with offset %s", t, getRegionDirectories().getLocation(), offset);
				return false;

			} catch (Exception ex) {
				Debug.dumpException("failed to process chunk import for " + getRegionDirectories().getLocationAsFileName(), ex);
				progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
			}

			return true;
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
