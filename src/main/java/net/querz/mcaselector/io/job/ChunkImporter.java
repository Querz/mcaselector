package net.querz.mcaselector.io.job;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.querz.mcaselector.config.ConfigProvider;
import net.querz.mcaselector.io.ByteArrayPointer;
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.io.JobHandler;
import net.querz.mcaselector.io.RegionDirectories;
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
import net.querz.mcaselector.selection.ChunkSet;
import net.querz.mcaselector.selection.Selection;
import net.querz.mcaselector.text.Translation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class ChunkImporter {

	private static final Logger LOGGER = LogManager.getLogger(ChunkImporter.class);

	private ChunkImporter() {}

	public static void importChunks(WorldDirectories source, Progress progressChannel, boolean cli, boolean overwrite, Selection sourceSelection, Selection targetSelection, List<Range> ranges, Point3i offset, DataProperty<Map<Point2i, RegionDirectories>> tempFiles) {
		try {
			WorldDirectories wd = ConfigProvider.WORLD.getWorldDirs();
			RegionDirectories[] rd = wd.listRegions(targetSelection);
			if (rd == null || rd.length == 0) {
				if (cli) {
					progressChannel.done("no files");
				} else {
					progressChannel.done(Translation.DIALOG_PROGRESS_NO_FILES.toString());
				}
				return;
			}

			JobHandler.clearQueues();

			if (cli) {
				progressChannel.setMessage("collecting data...");
			} else {
				progressChannel.setMessage(Translation.DIALOG_PROGRESS_COLLECTING_DATA.toString());
			}

			// if source world and target world is the same, we need to create temp files of all source files
			Map<Point2i, RegionDirectories> tempFilesMap = null;
			if (source.sharesDirectories(ConfigProvider.WORLD.getWorldDirs())) {
				tempFilesMap = new HashMap<>();
			}
			tempFiles.set(tempFilesMap);


			// map all target regions to the source regions they will need to get data from for the import
			// all map values will either have 1, 2 or 4 entries
			Long2ObjectOpenHashMap<LongSet> targetMapping = createTargetSourceMapping(source.getRegion(), sourceSelection, targetSelection, offset.toPoint2i());

			progressChannel.setMax(targetMapping.size());
			progressChannel.updateProgress(rd[0].getLocationAsFileName(), 0);

			Consumer<Throwable> errorHandler = t -> progressChannel.incrementProgress("error");

			for (Long2ObjectMap.Entry<LongSet> entry : targetMapping.long2ObjectEntrySet()) {
				Point2i target = new Point2i(entry.getLongKey());
				RegionDirectories targetDirs = FileHelper.createRegionDirectories(target);
				MCAChunkImporterProcessJob job = new MCAChunkImporterProcessJob(targetDirs, source, target, entry.getValue(), offset, progressChannel, overwrite, sourceSelection, targetSelection, ranges, tempFilesMap);
				job.errorHandler = errorHandler;
				JobHandler.addJob(job);
			}
		} catch (Exception ex) {
			LOGGER.warn("failed creating jobs to import chunks", ex);
		}
	}

	// returns a map where the key is a target region and the value is a set of all source regions, if they exist
	private static Long2ObjectOpenHashMap<LongSet> createTargetSourceMapping(File sourceDirectory, Selection sourceSelection, Selection targetSelection, Point2i offset) {
		Long2ObjectOpenHashMap<LongSet> sourceTargetMapping = new Long2ObjectOpenHashMap<>();

		// get all possible source files
		LongOpenHashSet sourceRegions = FileHelper.parseAllMCAFileNames(sourceDirectory);
		if (sourceSelection != null) {
			sourceRegions.removeIf(s -> !sourceSelection.isAnyChunkInRegionSelected(s));
		}

		// get target regions with offset based on source regions, target selection and inversion
		for (long sourceRegion : sourceRegions) {
			LongSet targetRegions = getTargetRegions(new Point2i(sourceRegion), offset, targetSelection);
			if (targetRegions.isEmpty()) {
				continue;
			}
			sourceTargetMapping.put(sourceRegion, targetRegions);
		}

		// now we invert the mapping to create a target -> sources mapping
		Long2ObjectOpenHashMap<LongSet> targetSourceMapping = new Long2ObjectOpenHashMap<>();
		int initSize = Math.min((offset.getX() % 32 != 0 ? 2 : 0) + (offset.getZ() % 32 != 0 ? 2 : 0), 1); // init with 1, 2, 4
		for (Long2ObjectMap.Entry<LongSet> entry : sourceTargetMapping.long2ObjectEntrySet()) {
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

	private static LongSet getTargetRegions(Point2i source, Point2i offset, Selection selection) {
		LongOpenHashSet result = new LongOpenHashSet(5, 0.9f);
		Point2i sourceChunk = source.regionToChunk().add(offset);
		addIfInSelection(result, sourceChunk.chunkToRegion(), selection);
		addIfInSelection(result, sourceChunk.add(31, 0).chunkToRegion(), selection);
		addIfInSelection(result, sourceChunk.add(0, 31).chunkToRegion(), selection);
		addIfInSelection(result, sourceChunk.add(31, 31).chunkToRegion(), selection);
		return result;
	}

	private static void addIfInSelection(LongOpenHashSet set, Point2i p, Selection selection) {
		if (selection == null || selection.isAnyChunkInRegionSelected(p)) {
			set.add(p.asLong());
		}
	}

	private static class MCAChunkImporterProcessJob extends ProcessDataJob {

		private final WorldDirectories sourceDirs;
		private final LongSet sourceRegions;
		private final Point2i target;
		private final Point3i offset;
		private final Progress progressChannel;
		private final boolean overwrite;
		private final Selection sourceSelection;
		private final Selection targetSelection;
		private final List<Range> ranges;
		private final Map<Point2i, RegionDirectories> tempFilesMap;

		private MCAChunkImporterProcessJob(RegionDirectories targetDirs, WorldDirectories sourceDirs, Point2i target, LongSet sourceRegions, Point3i offset, Progress progressChannel, boolean overwrite, Selection sourceSelection, Selection targetSelection, List<Range> ranges, Map<Point2i, RegionDirectories> tempFilesMap) {
			super(targetDirs, PRIORITY_LOW);
			this.sourceDirs = sourceDirs;
			this.sourceRegions = sourceRegions;
			this.target = target;
			this.offset = offset;
			this.progressChannel = progressChannel;
			this.overwrite = overwrite;
			this.sourceSelection = sourceSelection;
			this.targetSelection = targetSelection;
			this.ranges = ranges;
			this.tempFilesMap = tempFilesMap;
		}

		@Override
		public boolean execute() {
			// try to copy files directly if there is no offset, no selection and the target file does not exist
			if (offset.getX() == 0 && offset.getY() == 0 && offset.getZ() == 0 && targetSelection == null && sourceSelection == null && !getRegionDirectories().exists()) {
				boolean allCopied = true;

				if (!getRegionDirectories().getRegion().exists()) {
					//if the entire mca file doesn't exist, just copy it over
					File source = new File(sourceDirs.getRegion(), getRegionDirectories().getLocationAsFileName());
					if (source.exists()) {
						try {
							Files.copy(source.toPath(), getRegionDirectories().getRegion().toPath());
						} catch (IOException ex) {
							LOGGER.warn("failed to copy file {} to {}", source, getRegionDirectories().getRegion(), ex);
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
							LOGGER.warn("failed to copy file {} to {}", source, getRegionDirectories().getPoi(), ex);
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
							LOGGER.warn("failed to copy file {} to {}", source, getRegionDirectories().getEntities(), ex);
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

			for (long source : sourceRegions) {
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
						LOGGER.warn("failed to load source mca file {}", sourceFile);
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
						LOGGER.warn("failed to load source mca file {}", sourceFile);
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
						LOGGER.warn("failed to load source mca file {}", sourceFile);
					} else {
						sourceDataMappingEntities.put(s, sourceData);
					}
				}
			}

			// ---------------------------------------------------------------------------------------------------------

			// check if we need to do anything
			if (sourceDataMappingRegion.isEmpty() && sourceDataMappingPoi.isEmpty() && sourceDataMappingEntities.isEmpty()) {
				LOGGER.warn("did not load any source mca files to merge into {}", getRegionDirectories().getLocationAsFileName());
				progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
				return true;
			}

			// ---------------------------------------------------------------------------------------------------------

			// LOAD DESTINATION DATA
			byte[] destDataRegion = null;
			if (getRegionDirectories().getRegion().exists() && getRegionDirectories().getRegion().length() > 0) {
				destDataRegion = load(getRegionDirectories().getRegion());
				if (destDataRegion == null) {
					LOGGER.warn("failed to load destination mca file {}", getRegionDirectories().getRegion());
					progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
					return true;
				}
			}

			byte[] destDataPoi = null;
			if (getRegionDirectories().getPoi().exists() && getRegionDirectories().getPoi().length() > 0) {
				destDataPoi = load(getRegionDirectories().getPoi());
				if (destDataPoi == null) {
					LOGGER.warn("failed to load destination mca file {}", getRegionDirectories().getPoi());
					progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
					return true;
				}
			}

			byte[] destDataEntities = null;
			if (getRegionDirectories().getEntities().exists() && getRegionDirectories().getEntities().length() > 0) {
				destDataEntities = load(getRegionDirectories().getEntities());
				if (destDataEntities == null) {
					LOGGER.warn("failed to load destination mca file {}", getRegionDirectories().getEntities());
					progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
					return true;
				}
			}

			Timer t = new Timer();
			try {
				// load target region
				Region targetRegion = Region.loadRegion(getRegionDirectories(), destDataRegion, destDataPoi, destDataEntities);

				ChunkSet targetChunks = null;
				if (targetSelection != null) {
					targetChunks = targetSelection.getSelectedChunks(target);
				}

				for (Map.Entry<Point2i, byte[]> sourceData : sourceDataMappingRegion.entrySet()) {
					RegionMCAFile source = new RegionMCAFile(new File(sourceDirs.getRegion(), FileHelper.createMCAFileName(sourceData.getKey())));
					source.load(new ByteArrayPointer(sourceData.getValue()));

					LOGGER.debug("merging region chunks from {} into {}", sourceData.getKey(), target);

					if (targetRegion.getRegion() == null) {
						targetRegion.setRegion(new RegionMCAFile(getRegionDirectories().getRegion()));
					}

					ChunkSet sourceChunks = null;
					if (sourceSelection != null) {
						sourceChunks = sourceSelection.getSelectedChunks(sourceData.getKey());
					}

					source.mergeChunksInto(targetRegion.getRegion(), offset, overwrite, sourceChunks, targetChunks, ranges);
				}

				for (Map.Entry<Point2i, byte[]> sourceData : sourceDataMappingPoi.entrySet()) {
					PoiMCAFile source = new PoiMCAFile(new File(sourceDirs.getPoi(), FileHelper.createMCAFileName(sourceData.getKey())));
					source.load(new ByteArrayPointer(sourceData.getValue()));

					LOGGER.debug("merging poi chunks from {} into {}", sourceData.getKey(), target);

					if (targetRegion.getPoi() == null) {
						targetRegion.setPoi(new PoiMCAFile(getRegionDirectories().getPoi()));
					}

					ChunkSet sourceChunks = null;
					if (sourceSelection != null) {
						sourceChunks = sourceSelection.getSelectedChunks(sourceData.getKey());
					}

					source.mergeChunksInto(targetRegion.getPoi(), offset, overwrite, sourceChunks, targetChunks, ranges);
				}

				for (Map.Entry<Point2i, byte[]> sourceData : sourceDataMappingEntities.entrySet()) {
					EntitiesMCAFile source = new EntitiesMCAFile(new File(sourceDirs.getEntities(), FileHelper.createMCAFileName(sourceData.getKey())));
					source.load(new ByteArrayPointer(sourceData.getValue()));

					LOGGER.debug("merging entities chunks from {} into {}", sourceData.getKey(), target);

					if (targetRegion.getEntities() == null) {
						targetRegion.setEntities(new EntitiesMCAFile(getRegionDirectories().getEntities()));
					}

					ChunkSet sourceChunks = null;
					if (sourceSelection != null) {
						sourceChunks = sourceSelection.getSelectedChunks(sourceData.getKey());
					}

					source.mergeChunksInto(targetRegion.getEntities(), offset, overwrite, sourceChunks, targetChunks, ranges);
				}

				// -----------------------------------------------------------------------------------------------------

				MCAChunkImporterSaveJob job = new MCAChunkImporterSaveJob(getRegionDirectories(), targetRegion, progressChannel);
				job.errorHandler = errorHandler;
				JobHandler.executeSaveData(job);
				LOGGER.debug("took {} to merge chunks into {} with offset {}", t, getRegionDirectories().getLocation(), offset);
				return false;

			} catch (Exception ex) {
				LOGGER.warn("failed to process chunk import for {}", getRegionDirectories().getLocationAsFileName(), ex);
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
				LOGGER.warn("failed to save imported chunks to {}", getRegionDirectories().getLocationAsFileName(), ex);
			}
			progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
			LOGGER.debug("took {} to save data for {}", t, getRegionDirectories().getLocationAsFileName());
		}
	}
}
