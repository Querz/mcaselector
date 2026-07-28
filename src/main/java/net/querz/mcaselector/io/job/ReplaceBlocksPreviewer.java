package net.querz.mcaselector.io.job;

import net.querz.mcaselector.changer.fields.ReplaceBlocksField;
import net.querz.mcaselector.config.ConfigProvider;
import net.querz.mcaselector.io.RegionDirectories;
import net.querz.mcaselector.io.WorldDirectories;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.io.mca.Region;
import net.querz.mcaselector.io.mca.RegionChunk;
import net.querz.mcaselector.selection.Selection;
import net.querz.mcaselector.util.point.Point2i;
import net.querz.mcaselector.util.progress.Progress;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.VersionHandler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ReplaceBlocksPreviewer {

	private ReplaceBlocksPreviewer() {}

	public static Result preview(ReplaceBlocksField field, Selection selection, Progress progressChannel) {
		Result result = new Result();
		if (field == null || !field.needsChange()) {
			progressChannel.done("no ReplaceBlocks field");
			return result;
		}

		result.initializeRules(field.getNewValue());
		result.replaceAir = field.getNewValue().keySet().stream().anyMatch(ChunkFilter.BlockReplaceSource::matchesAir);
		field.getNewValue().values().forEach(v -> {
			if (v.getTile() != null) {
				result.replaceWithTileEntity = true;
			}
		});

		WorldDirectories wd = ConfigProvider.WORLD.getWorldDirs();
		RegionDirectories[] regionDirectories = wd.listRegions(selection);
		if (regionDirectories == null || regionDirectories.length == 0) {
			progressChannel.done("no files");
			return result;
		}

		progressChannel.setMax(regionDirectories.length);
		for (RegionDirectories dirs : regionDirectories) {
			if (progressChannel.taskCancelled()) {
				break;
			}
			result.scannedRegions++;
			try {
				Region region = Region.loadRegion(dirs);
				scanRegion(region, dirs.getLocation(), field, selection, result, progressChannel);
			} catch (Exception ex) {
				result.errorRegions++;
				result.addError(dirs.getLocationAsFileName() + ": " + ex.getMessage());
			}
			progressChannel.incrementProgress(dirs.getLocationAsFileName());
		}
		result.finish(selection);
		progressChannel.done("done");
		return result;
	}

	private static void scanRegion(Region region, Point2i regionLocation, ReplaceBlocksField field, Selection selection,
			Result result, Progress progressChannel) {
		Point2i firstChunk = regionLocation.regionToChunk();
		for (int x = 0; x < 32; x++) {
			for (int z = 0; z < 32; z++) {
				if (progressChannel.taskCancelled()) {
					return;
				}
				Point2i chunkLocation = firstChunk.add(x, z);
				if (selection != null && !selection.isChunkSelected(chunkLocation)) {
					continue;
				}
				ChunkData chunkData = region.getChunkDataAt(chunkLocation, selection == null || selection.isChunkSelected(chunkLocation));
				RegionChunk regionChunk = chunkData.region();
				if (regionChunk == null || regionChunk.isEmpty() || regionChunk.getData() == null) {
					continue;
				}
				result.scannedChunks++;
				try {
					ChunkFilter.BlockReplacePreviewData preview = VersionHandler
							.getImpl(chunkData, ChunkFilter.Blocks.class)
							.previewReplaceBlocks(chunkData, field.getNewValue());
					if (!preview.isSupported()) {
						result.unsupportedChunks++;
						continue;
					}
					result.addChunk(chunkLocation, preview);
				} catch (Exception ex) {
					result.errorChunks++;
					result.addError(chunkLocation + ": " + ex.getMessage());
				}
			}
		}
	}

	public static class Result {

		private long scannedRegions;
		private long scannedChunks;
		private long affectedChunks;
		private long affectedSections;
		private long matchedBlocks;
		private long lightSections;
		private long completedAirSections;
		private long tileEntityAdditions;
		private long tileEntityRemovals;
		private long tileEntityUpdates;
		private long overlappingBlocks;
		private long unsupportedChunks;
		private long errorChunks;
		private long errorRegions;
		private int potentialAdjacentRelightChunks;
		private boolean replaceAir;
		private boolean replaceWithTileEntity;
		private final List<String> errors = new ArrayList<>(5);
		private final List<RulePreview> rules = new ArrayList<>();
		private final Set<Point2i> affectedChunkLocations = new HashSet<>();

		private void initializeRules(Map<ChunkFilter.BlockReplaceSource, ChunkFilter.BlockReplaceData> replace) {
			int index = 1;
			for (Map.Entry<ChunkFilter.BlockReplaceSource, ChunkFilter.BlockReplaceData> entry : replace.entrySet()) {
				rules.add(new RulePreview(index++, entry.getKey(), entry.getValue()));
			}
		}

		void addChunk(Point2i chunkLocation, ChunkFilter.BlockReplacePreviewData preview) {
			if (preview.getBlocks() > 0) {
				affectedChunks++;
				affectedChunkLocations.add(chunkLocation);
				affectedSections += preview.getSections();
				matchedBlocks += preview.getBlocks();
			}
			for (ChunkFilter.BlockReplaceRulePreviewData rule : preview.getRules()) {
				int index = rule.getIndex() - 1;
				if (index >= 0 && index < rules.size()) {
					rules.get(index).addBlocks(rule.getBlocks());
				}
			}
			overlappingBlocks += preview.getOverlappingBlocks();
			lightSections += preview.getLightSections();
			completedAirSections += preview.getCompletedAirSections();
			tileEntityAdditions += preview.getTileEntityAdditions();
			tileEntityRemovals += preview.getTileEntityRemovals();
			tileEntityUpdates += preview.getTileEntityUpdates();
		}

		void finish(Selection selection) {
			if (selection == null || affectedChunkLocations.isEmpty()) {
				return;
			}
			Set<Point2i> adjacent = FieldChanger.getAdjacentRelightChunks(affectedChunkLocations);
			adjacent.removeIf(selection::isChunkSelected);
			potentialAdjacentRelightChunks = adjacent.size();
		}

		private void addError(String message) {
			if (errors.size() < 5) {
				errors.add(message);
			}
		}

		public long getScannedRegions() {
			return scannedRegions;
		}

		public long getScannedChunks() {
			return scannedChunks;
		}

		public long getAffectedChunks() {
			return affectedChunks;
		}

		public long getAffectedSections() {
			return affectedSections;
		}

		public long getMatchedBlocks() {
			return matchedBlocks;
		}

		public long getLightSections() {
			return lightSections;
		}

		public long getCompletedAirSections() {
			return completedAirSections;
		}

		public long getTileEntityAdditions() {
			return tileEntityAdditions;
		}

		public long getTileEntityRemovals() {
			return tileEntityRemovals;
		}

		public long getTileEntityUpdates() {
			return tileEntityUpdates;
		}

		public long getOverlappingBlocks() {
			return overlappingBlocks;
		}

		public long getUnsupportedChunks() {
			return unsupportedChunks;
		}

		public long getErrorChunks() {
			return errorChunks;
		}

		public long getErrorRegions() {
			return errorRegions;
		}

		public int getPotentialAdjacentRelightChunks() {
			return potentialAdjacentRelightChunks;
		}

		public boolean replacesAir() {
			return replaceAir;
		}

		public boolean replacesWithTileEntity() {
			return replaceWithTileEntity;
		}

		public List<String> getErrors() {
			return errors;
		}

		public List<RulePreview> getRules() {
			return Collections.unmodifiableList(rules);
		}
	}

	public static class RulePreview {

		private final int index;
		private final String sourceMode;
		private final String sourceText;
		private final String targetText;
		private long blocks;

		private RulePreview(int index, ChunkFilter.BlockReplaceSource source, ChunkFilter.BlockReplaceData target) {
			this.index = index;
			String mode = source.getType().name().toLowerCase(Locale.ROOT).replace('_', '-');
			if (source.getTileEntityMode() != ChunkFilter.BlockReplaceTileEntityMode.ANY) {
				mode += "/" + source.getTileEntityMode().name().toLowerCase(Locale.ROOT).replace('_', '-');
			}
			if (source.hasBiomeRestriction()) {
				mode += "/biome";
			}
			sourceMode = mode;
			sourceText = source.toString();
			targetText = target.toString();
		}

		private void addBlocks(long blocks) {
			this.blocks += blocks;
		}

		public int getIndex() {
			return index;
		}

		public String getSourceMode() {
			return sourceMode;
		}

		public String getSourceText() {
			return sourceText;
		}

		public String getTargetText() {
			return targetText;
		}

		public long getBlocks() {
			return blocks;
		}
	}
}
