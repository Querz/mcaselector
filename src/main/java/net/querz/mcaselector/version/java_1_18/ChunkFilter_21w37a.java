package net.querz.mcaselector.version.java_1_18;

import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.util.point.Point2i;
import net.querz.mcaselector.util.point.Point3i;
import net.querz.mcaselector.util.range.Range;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.Helper;
import net.querz.mcaselector.version.MCVersionImplementation;
import net.querz.mcaselector.version.java_1_17.ChunkFilter_20w45a;
import net.querz.mcaselector.version.java_1_17.ChunkFilter_21w06a;
import net.querz.mcaselector.version.java_1_9.ChunkFilter_15w32a;
import net.querz.mcaselector.version.mapping.generator.HeightmapConfig;
import net.querz.mcaselector.version.mapping.registry.BiomeRegistry;
import net.querz.nbt.*;
import java.util.*;
import java.util.function.Predicate;

import static net.querz.mcaselector.util.validation.ValidationHelper.attempt;

public class ChunkFilter_21w37a {

	@MCVersionImplementation(2834)
	public static class Blocks extends ChunkFilter_21w06a.Blocks {

		@Override
		public boolean matchBlockNames(ChunkData data, Collection<String> names) {
			ListTag sections = Helper.tagFromLevelFromRoot(Helper.getRegion(data), "Sections");
			if (sections == null) {
				return false;
			}

			int c = 0;
			nameLoop:
			for (String name : names) {
				for (CompoundTag t : sections.iterateType(CompoundTag.class)) {
					ListTag palette = Helper.tagFromCompound(Helper.tagFromCompound(t, "block_states"), "palette");
					if (palette == null) {
						continue;
					}
					for (CompoundTag p : palette.iterateType(CompoundTag.class)) {
						if (name.equals(Helper.stringFromCompound(p, "Name"))) {
							c++;
							continue nameLoop;
						}
					}
				}
			}
			return names.size() == c;
		}

		@Override
		public boolean matchAnyBlockName(ChunkData data, Collection<String> names) {
			ListTag sections = Helper.tagFromLevelFromRoot(Helper.getRegion(data), "Sections");
			if (sections == null) {
				return false;
			}

			for (String name : names) {
				for (CompoundTag t : sections.iterateType(CompoundTag.class)) {
					ListTag palette = Helper.tagFromCompound(Helper.tagFromCompound(t, "block_states"), "palette");
					if (palette == null) {
						continue;
					}
					for (CompoundTag p : palette.iterateType(CompoundTag.class)) {
						if (name.equals(Helper.stringFromCompound(p, "Name"))) {
							return true;
						}
					}
				}
			}
			return false;
		}

		@Override
		public boolean replaceBlocks(ChunkData data, Map<ChunkFilter.BlockReplaceSource, ChunkFilter.BlockReplaceData> replace) {
			CompoundTag level = Helper.levelFromRoot(Helper.getRegion(data));
			ListTag sections = Helper.tagFromCompound(level, "Sections");
			if (sections == null) {
				return false;
			}

			Point2i pos = Helper.point2iFromCompound(level, "xPos", "zPos");
			if (pos == null) {
				return false;
			}
			pos = pos.chunkToBlock();
			boolean changed = false;

			Range sectionRange = Helper.findSectionRange(level, sections);
			Set<CompoundTag> syntheticSectionsWithUnknownBiomes = Collections.newSetFromMap(new IdentityHashMap<>());

			// handle the special case when someone wants to replace air with something else
			if (replace.keySet().stream().anyMatch(ChunkFilter.BlockReplaceSource::matchesAir)) {
				Map<Integer, CompoundTag> sectionMap = new HashMap<>();
				List<Integer> heights = new ArrayList<>(sectionRange.num());
				boolean completedSections = false;
				for (CompoundTag section : sections.iterateType(CompoundTag.class)) {
					sectionMap.put(section.getInt("Y"), section);
					heights.add(section.getInt("Y"));
				}

				for (int y = sectionRange.getFrom(); y <= sectionRange.getTo(); y++) {
					if (!hasAirReplacementInSection(replace, y)) {
						continue;
					}
					CompoundTag section = sectionMap.get(y);
					if (section == null) {
						if (!hasSyntheticAirReplacementInSection(null, y, replace)) {
							continue;
						}
						CompoundTag completed = completeSection(new CompoundTag(), y);
						sectionMap.put(y, completed);
						syntheticSectionsWithUnknownBiomes.add(completed);
						heights.add(y);
						completedSections = true;
					} else {
						if (!section.containsKey("block_states")) {
							if (!hasSyntheticAirReplacementInSection(section, y, replace)) {
								continue;
							}
							boolean biomeUnknown = getBiomeAt(section, 0) == null;
							completeSection(sectionMap.get(y), y);
							completedSections = true;
							if (biomeUnknown) {
								syntheticSectionsWithUnknownBiomes.add(section);
							}
						}
					}
				}

				if (completedSections) {
					heights.sort(Integer::compareTo);
					sections.clear();

					for (int height : heights) {
						sections.add(sectionMap.get(height));
					}
				}
			}

			ListTag tileEntities = Helper.tagFromCompound(level, "TileEntities");
			if (tileEntities == null) {
				tileEntities = new ListTag();
			}
			boolean needsTileContext = needsTileContext(replace);
			boolean needsYContext = needsYContext(replace);
			boolean needsBiomeContext = needsBiomeContext(replace);
			Set<String> sourceTileEntityLocations = needsTileContext
					? getTileEntityLocations(tileEntities)
					: Collections.emptySet();

			for (CompoundTag section : sections.iterateType(CompoundTag.class)) {
				boolean sectionChanged = false;
				CompoundTag blockStatesTag = section.getCompoundTag("block_states");
				if(blockStatesTag == null) continue;

				ListTag palette = Helper.tagFromCompound(blockStatesTag, "palette");
				long[] blockStates = Helper.longArrayFromCompound(blockStatesTag, "data");
				if (palette == null) {
					continue;
				}

				if (palette.size() == 1 && blockStates == null) {
					blockStates = new long[256];
				}

				int y = Helper.numberFromCompound(section, "Y", sectionRange.getFrom() - 1).intValue();
				if (!sectionRange.contains(y)) {
					continue;
				}
				if (!sourceMayMatchSection(replace, y)) {
					continue;
				}

				for (int i = 0; i < 4096; i++) {
					CompoundTag blockState = getBlockAt(i, blockStates, palette);
					int blockY = y * 16 + (i >>> 8);
					int blockX = pos.getX() + (i & 15);
					int blockZ = pos.getZ() + ((i >>> 4) & 15);
					boolean sourceHasTileEntity = needsTileContext
							&& sourceTileEntityLocations.contains(locationKey(blockX, blockY, blockZ));
					String biome = needsBiomeContext && !syntheticSectionsWithUnknownBiomes.contains(section)
							? getBiomeAt(section, i) : null;

					for (Map.Entry<ChunkFilter.BlockReplaceSource, ChunkFilter.BlockReplaceData> entry : replace.entrySet()) {
						if (!matchesSource(entry.getKey(), blockState, sourceHasTileEntity,
								needsYContext ? blockY : 0, biome)) {
							continue;
						}
						ChunkFilter.BlockReplaceData replacement = entry.getValue();
						if (!sectionChanged) {
							section.remove("BlockLight");
							section.remove("SkyLight");
							sectionChanged = true;
							changed = true;
						}

						try {
							blockStates = setBlockAt(i, replacement.getState(), blockStates, palette);
						} catch (Exception ex) {
							throw new RuntimeException("failed to set block in section " + y, ex);
						}

						if (replacement.getTile() != null) {
							Point3i location = new Point3i(blockX, blockY, blockZ);
							removeTileEntitiesAt(tileEntities, location);
							CompoundTag tile = replacement.getTile().copy();
							tile.putInt("x", location.getX());
							tile.putInt("y", location.getY());
							tile.putInt("z", location.getZ());
							tileEntities.add(tile);
						} else if (!tileEntities.isEmpty()) {
							removeTileEntitiesAt(tileEntities, new Point3i(blockX, blockY, blockZ));
						}

					}
				}

				if (!sectionChanged) {
					continue;
				}
				try {
					blockStates = cleanupPalette(blockStates, palette);
				} catch (Exception ex) {
					throw new RuntimeException("failed to cleanup section " + y, ex);
				}

				if (blockStates == null) {
					blockStatesTag.remove("data");
				} else {
					blockStatesTag.putLongArray("data", blockStates);
				}
			}

			if (changed) {
				level.put("TileEntities", tileEntities);
			}
			return changed;
		}

		@Override
		public ChunkFilter.BlockReplacePreviewData previewReplaceBlocks(ChunkData data, Map<ChunkFilter.BlockReplaceSource, ChunkFilter.BlockReplaceData> replace) {
			CompoundTag level = Helper.levelFromRoot(Helper.getRegion(data));
			ListTag sections = Helper.tagFromCompound(level, "Sections");
			return previewReplaceBlocks(level, sections, "TileEntities", replace);
		}

		protected ChunkFilter.BlockReplacePreviewData previewReplaceBlocks(CompoundTag root, ListTag sections, String tileEntitiesKey, Map<ChunkFilter.BlockReplaceSource, ChunkFilter.BlockReplaceData> replace) {
			ChunkFilter.BlockReplacePreviewData result = ChunkFilter.BlockReplacePreviewData.supported(replace);
			if (root == null || sections == null) {
				return result;
			}

			Range sectionRange = Helper.findSectionRange(root, sections);
			if (sectionRange == null) {
				return result;
			}

			boolean needsTileContext = needsTileContext(replace);
			boolean needsYContext = needsYContext(replace);
			boolean needsBiomeContext = needsBiomeContext(replace);
			ListTag tileEntities = Helper.tagFromCompound(root, tileEntitiesKey);
			Set<String> tileEntityLocations = needsTileContext
					? getTileEntityLocations(tileEntities)
					: Collections.emptySet();

			if (replace.keySet().stream().anyMatch(ChunkFilter.BlockReplaceSource::matchesAir)) {
				Map<Integer, CompoundTag> sectionMap = new HashMap<>();
				for (CompoundTag section : sections.iterateType(CompoundTag.class)) {
					sectionMap.put(section.getInt("Y"), section);
				}
				for (int y = sectionRange.getFrom(); y <= sectionRange.getTo(); y++) {
					if (!hasAirReplacementInSection(replace, y)) {
						continue;
					}
					CompoundTag section = sectionMap.get(y);
					if (section == null || !section.containsKey("block_states")) {
						long matches = countSyntheticAirSection(section, y, replace, result);
						if (matches > 0) {
							result.incrementCompletedAirSections();
							result.incrementLightSections();
							result.addSection(matches);
						}
					}
				}
			}

			Point2i pos = Helper.point2iFromCompound(root, "xPos", "zPos");
			if (pos == null) {
				return result;
			}
			pos = pos.chunkToBlock();

			for (CompoundTag section : sections.iterateType(CompoundTag.class)) {
				CompoundTag blockStatesTag = section.getCompoundTag("block_states");
				if (blockStatesTag == null) {
					continue;
				}

				ListTag palette = Helper.tagFromCompound(blockStatesTag, "palette");
				long[] blockStates = Helper.longArrayFromCompound(blockStatesTag, "data");
				if (palette == null) {
					continue;
				}

				if (palette.size() == 1 && blockStates == null) {
					blockStates = new long[256];
				}

				int y = Helper.numberFromCompound(section, "Y", sectionRange.getFrom() - 1).intValue();
				if (!sectionRange.contains(y)) {
					continue;
				}
				if (!sourceMayMatchSection(replace, y)) {
					continue;
				}

				long sectionMatches = 0;
				for (int i = 0; i < 4096; i++) {
					CompoundTag blockState = getBlockAt(i, blockStates, palette);
					int blockY = y * 16 + (i >>> 8);
					int blockX = pos.getX() + (i & 15);
					int blockZ = pos.getZ() + ((i >>> 4) & 15);
					String biome = needsBiomeContext ? getBiomeAt(section, i) : null;
					boolean hasTileEntity = needsTileContext
							? tileEntityLocations.contains(locationKey(blockX, blockY, blockZ))
							: tileEntities != null && !tileEntities.isEmpty()
							&& hasTileEntityAt(tileEntities, blockX, blockY, blockZ);
					if (countMatchingBlock(blockState, replace, result, hasTileEntity,
							needsYContext ? blockY : 0, biome)) {
						sectionMatches++;
					}
				}
				if (sectionMatches > 0) {
					result.incrementLightSections();
				}
				result.addSection(sectionMatches);
			}

			return result;
		}

		protected boolean hasAirReplacementInSection(Map<ChunkFilter.BlockReplaceSource, ChunkFilter.BlockReplaceData> replace, int sectionY) {
			return replace.keySet().stream().anyMatch(source -> source.matchesAirInSection(sectionY));
		}

		protected boolean sourceMayMatchSection(Map<ChunkFilter.BlockReplaceSource, ChunkFilter.BlockReplaceData> replace, int sectionY) {
			return replace.keySet().stream().anyMatch(source -> source.intersectsSection(sectionY));
		}

		protected boolean needsTileContext(Map<ChunkFilter.BlockReplaceSource, ChunkFilter.BlockReplaceData> replace) {
			return replace.keySet().stream().anyMatch(source ->
					source.getTileEntityMode() != ChunkFilter.BlockReplaceTileEntityMode.ANY);
		}

		protected boolean needsYContext(Map<ChunkFilter.BlockReplaceSource, ChunkFilter.BlockReplaceData> replace) {
			return replace.keySet().stream().anyMatch(source -> source.hasYRange() || source.hasBiomeRestriction());
		}

		protected boolean needsBiomeContext(Map<ChunkFilter.BlockReplaceSource, ChunkFilter.BlockReplaceData> replace) {
			return replace.keySet().stream().anyMatch(ChunkFilter.BlockReplaceSource::hasBiomeRestriction);
		}

		protected boolean matchesSource(ChunkFilter.BlockReplaceSource source, CompoundTag blockState,
				boolean hasTileEntity, int y, String biome) {
			if (source.hasBiomeRestriction()) {
				return source.matches(blockState, hasTileEntity, y, biome);
			}
			if (source.hasYRange()) {
				return source.matches(blockState, hasTileEntity, y);
			}
			if (source.getTileEntityMode() != ChunkFilter.BlockReplaceTileEntityMode.ANY) {
				return source.matches(blockState, hasTileEntity);
			}
			return source.matches(blockState);
		}

		private long countSyntheticAirSection(CompoundTag section, int sectionY, Map<ChunkFilter.BlockReplaceSource, ChunkFilter.BlockReplaceData> replace, ChunkFilter.BlockReplacePreviewData result) {
			CompoundTag air = new CompoundTag();
			air.putString("Name", "minecraft:air");
			long matches = 0;
			for (int i = 0; i < 4096; i++) {
				int y = sectionY * 16 + indexToLocation(i).getY();
				if (countMatchingBlock(air, replace, result, false, y, getBiomeAt(section, i))) {
					matches++;
				}
			}
			return matches;
		}

		private boolean countMatchingBlock(CompoundTag blockState, Map<ChunkFilter.BlockReplaceSource, ChunkFilter.BlockReplaceData> replace, ChunkFilter.BlockReplacePreviewData result, boolean hasTileEntity, int y, String biome) {
			boolean matched = false;
			boolean tileEntityPresent = hasTileEntity;
			int matchedRules = 0;
			int ruleIndex = 0;
			for (Map.Entry<ChunkFilter.BlockReplaceSource, ChunkFilter.BlockReplaceData> entry : replace.entrySet()) {
				if (!matchesSource(entry.getKey(), blockState, hasTileEntity, y, biome)) {
					ruleIndex++;
					continue;
				}
				matched = true;
				matchedRules++;
				result.incrementRuleBlocks(ruleIndex);
				if (entry.getValue().getTile() != null) {
					if (tileEntityPresent) {
						result.incrementTileEntityUpdates();
					} else {
						result.incrementTileEntityAdditions();
					}
					tileEntityPresent = true;
				} else if (tileEntityPresent) {
					result.incrementTileEntityRemovals();
					tileEntityPresent = false;
				}
				ruleIndex++;
			}
			if (matchedRules > 1) {
				result.incrementOverlappingBlocks();
			}
			return matched;
		}

		protected boolean hasSyntheticAirReplacementInSection(CompoundTag section, int sectionY, Map<ChunkFilter.BlockReplaceSource, ChunkFilter.BlockReplaceData> replace) {
			CompoundTag air = new CompoundTag();
			air.putString("Name", "minecraft:air");
			for (int i = 0; i < 4096; i++) {
				int y = sectionY * 16 + indexToLocation(i).getY();
				String biome = getBiomeAt(section, i);
				for (ChunkFilter.BlockReplaceSource source : replace.keySet()) {
					if (source.matches(air, false, y, biome)) {
						return true;
					}
				}
			}
			return false;
		}

		protected Set<String> getTileEntityLocations(CompoundTag root, String tileEntitiesKey) {
			ListTag tileEntities = Helper.tagFromCompound(root, tileEntitiesKey);
			return getTileEntityLocations(tileEntities);
		}

		protected Set<String> getTileEntityLocations(ListTag tileEntities) {
			Set<String> locations = new HashSet<>();
			if (tileEntities == null) {
				return locations;
			}
			for (CompoundTag tile : tileEntities.iterateType(CompoundTag.class)) {
				locations.add(locationKey(tile.getInt("x"), tile.getInt("y"), tile.getInt("z")));
			}
			return locations;
		}

		protected int removeTileEntitiesAt(ListTag tileEntities, Point3i location) {
			int removed = 0;
			for (int t = 0; t < tileEntities.size(); t++) {
				CompoundTag tile = tileEntities.getCompound(t);
				if (tile.getInt("x") == location.getX()
						&& tile.getInt("y") == location.getY()
						&& tile.getInt("z") == location.getZ()) {
					tileEntities.remove(t);
					t--;
					removed++;
				}
			}
			return removed;
		}

		protected boolean hasTileEntityAt(ListTag tileEntities, int x, int y, int z) {
			for (CompoundTag tile : tileEntities.iterateType(CompoundTag.class)) {
				if (tile.getInt("x") == x && tile.getInt("y") == y && tile.getInt("z") == z) {
					return true;
				}
			}
			return false;
		}

		protected String locationKey(Point3i location) {
			return locationKey(location.getX(), location.getY(), location.getZ());
		}

		protected String locationKey(int x, int y, int z) {
			return x + "," + y + "," + z;
		}

		protected String getBiomeAt(CompoundTag section, int blockIndex) {
			CompoundTag biomes = Helper.tagFromCompound(section, "biomes");
			ListTag palette = Helper.tagFromCompound(biomes, "palette");
			if (palette == null || palette.isEmpty()) {
				return null;
			}
			if (palette.size() == 1) {
				return palette.getString(0);
			}
			long[] data = Helper.longArrayFromCompound(biomes, "data");
			if (data == null || data.length == 0) {
				return null;
			}

			Point3i location = indexToLocation(blockIndex);
			int index = (location.getY() >> 2) * 16 + (location.getZ() >> 2) * 4 + (location.getX() >> 2);
			int bits = 32 - Integer.numberOfLeadingZeros(palette.size() - 1);
			int indexesPerLong = 64 / bits;
			if (data.length != Math.ceilDiv(64, indexesPerLong)) {
				return null;
			}
			int biomeDataIndex = index / indexesPerLong;
			if (biomeDataIndex >= data.length) {
				return null;
			}
			long clean = (1L << bits) - 1L;
			int startBit = (index % indexesPerLong) * bits;
			int paletteIndex = (int) (data[biomeDataIndex] >>> startBit & clean);
			if (paletteIndex >= palette.size()) {
				return null;
			}
			return palette.getString(paletteIndex);
		}

		protected CompoundTag completeSection(CompoundTag section, int y) {
			section.putByte("Y", (byte) y);
			if (!section.containsKey("block_states")) {
				CompoundTag newBlockStates = new CompoundTag();
				section.put("block_states", newBlockStates);
			}
			CompoundTag blockStates = section.getCompound("block_states");

			if (!blockStates.containsKey("data")) {
				blockStates.putLongArray("data", new long[256]);
			}
			if (!blockStates.containsKey("palette")) {
				ListTag newPalette = new ListTag();
				CompoundTag newBlockState = new CompoundTag();
				newBlockState.putString("Name", "minecraft:air");
				newPalette.add(newBlockState);
				blockStates.put("palette", newPalette);
			}

			if (!section.containsKey("biomes")) {
				CompoundTag newBiomes = new CompoundTag();
				section.put("biomes", newBiomes);
			}
			CompoundTag biomes = section.getCompound("biomes");

			if (!biomes.containsKey("palette")) {
				ListTag biomePalette = new ListTag();
				biomePalette.addString("minecraft:plains");
				biomes.put("palette", biomePalette);
			}
			if (!biomes.containsKey("data")) {
				biomes.putLongArray("data", new long[1]);
			}
			return section;
		}

		@Override
		public int getBlockAmount(ChunkData data, String[] blocks) {
			ListTag sections = Helper.tagFromLevelFromRoot(Helper.getRegion(data), "Sections");
			if (sections == null) {
				return 0;
			}

			int result = 0;

			for (CompoundTag section : sections.iterateType(CompoundTag.class)) {
				ListTag palette = Helper.tagFromCompound(Helper.tagFromCompound(section, "block_states"), "palette");
				long[] blockStates = Helper.longArrayFromCompound(Helper.tagFromCompound(section, "block_states"), "data");
				if (palette == null || blockStates == null) {
					continue;
				}

				for (int i = 0; i < palette.size(); i++) {
					CompoundTag blockState = palette.getCompound(i);
					String name = Helper.stringFromCompound(blockState, "Name");
					if (name == null) {
						continue;
					}

					for (String block : blocks) {
						if (name.equals(block)) {
							// count blocks of this type
							for (int k = 0; k < 4096; k++) {
								if (blockState == getBlockAt(k, blockStates, palette)) {
									result++;
								}
							}
							break;
						}
					}
				}
			}
			return result;
		}

		@Override
		public int getAverageHeight(ChunkData data) {
			ListTag sections = Helper.tagFromLevelFromRoot(Helper.getRegion(data), "Sections");
			if (sections == null) {
				return 0;
			}

			sections.sort(this::filterSections);

			int totalHeight = 0;

			for (int cx = 0; cx < 16; cx++) {
				zLoop:
				for (int cz = 0; cz < 16; cz++) {
					for (CompoundTag section : sections.iterateType(CompoundTag.class)) {
						ListTag palette = Helper.tagFromCompound(Helper.tagFromCompound(section, "block_states"), "palette");
						long[] blockStates = Helper.longArrayFromCompound(Helper.tagFromCompound(section, "block_states"), "data");
						if (palette == null || blockStates == null) {
							continue;
						}

						Number height = Helper.numberFromCompound(section, "Y", null);
						if (height == null) {
							continue;
						}

						for (int cy = 15; cy >= 0; cy--) {
							int index = cy * 256 + cz * 16 + cx;
							CompoundTag block = getBlockAt(index, blockStates, palette);
							if (!isEmpty(block)) {
								totalHeight += height.intValue() * 16 + cy;
								continue zLoop;
							}
						}
					}
				}
			}
			return totalHeight / 256;
		}
	}

	@MCVersionImplementation(2834)
	public static class Heightmap extends ChunkFilter_21w06a.Heightmap {

		@Override
		protected void loadCfg() {
			cfg = FileHelper.loadFromResource("mapping/java_1_18/heightmaps_21w37a.json", HeightmapConfig::load);
		}

		@Override
		protected long[] getHeightMap(CompoundTag root, Predicate<CompoundTag> matcher) {
			ListTag sections = Helper.getSectionsFromLevelFromRoot(root, "Sections");
			if (sections == null) {
				return new long[37];
			}

			Range sectionRange = Helper.findSectionRange(root, sections);

			ListTag[] palettes = new ListTag[sectionRange.num()];
			long[][] blockStatesArray = new long[sectionRange.num()][];
			sections.forEach(s -> {
				CompoundTag blockStates = Helper.tagFromCompound(s, "block_states");
				ListTag p = Helper.tagFromCompound(blockStates, "palette");
				long[] b = Helper.longArrayFromCompound(blockStates, "data");
				int y = Helper.numberFromCompound(s, "Y", sectionRange.getFrom() - 1).intValue();
				if (sectionRange.contains(y) && p != null && !p.isEmpty() && (b != null || p.size() == 1)) {
					palettes[y - sectionRange.getFrom()] = p;
					blockStatesArray[y - sectionRange.getFrom()] = b;
				}
			});

			short[] heightmap = new short[256];

			// loop over x/z
			for (int cx = 0; cx < 16; cx++) {
				loop:
				for (int cz = 0; cz < 16; cz++) {
					for (int i = palettes.length - 1; i >= 0; i--) {
						ListTag palette = palettes[i];
						if (palette == null) {
							continue;
						}
						long[] blockStates = blockStatesArray[i];
						if (palette.size() == 1) {
							if (matcher.test(palette.getCompound(0))) {
								heightmap[cz * 16 + cx] = (short) ((i + 1) * 16);
								continue loop;
							}
							continue;
						}
						if (blockStates == null) {
							continue;
						}
						for (int cy = 15; cy >= 0; cy--) {
							int blockIndex = cy * 256 + cz * 16 + cx;
							if (matcher.test(getBlockAt(blockIndex, blockStates, palette))) {
								heightmap[cz * 16 + cx] = (short) (i * 16 + cy + 1);
								continue loop;
							}
						}
					}
				}
			}

			int bits = 32 - Integer.numberOfLeadingZeros(sectionRange.num() * 16);
			return applyHeightMap(heightmap, bits);
		}
	}

	@MCVersionImplementation(2834)
	public static class Palette implements ChunkFilter.Palette {

		@Override
		public boolean paletteEquals(ChunkData data, Collection<String> names) {
			ListTag sections = Helper.tagFromLevelFromRoot(Helper.getRegion(data), "Sections");
			if (sections == null) {
				return false;
			}

			Set<String> blocks = new HashSet<>();
			for (CompoundTag t : sections.iterateType(CompoundTag.class)) {
				ListTag palette = Helper.tagFromCompound(Helper.tagFromCompound(t, "block_states"), "palette");
				if (palette == null) {
					continue;
				}
				for (CompoundTag p : palette.iterateType(CompoundTag.class)) {
					String n;
					if ((n = Helper.stringFromCompound(p, "Name")) != null) {
						if (!names.contains(n)) {
							return false;
						}
						blocks.add(n);
					}
				}
			}
			if (blocks.size() != names.size()) {
				return false;
			}
			for (String name : names) {
				if (!blocks.contains(name)) {
					return false;
				}
			}
			return true;
		}
	}

	@MCVersionImplementation(2834)
	public static class Biomes implements ChunkFilter.Biomes {

		@Override
		public boolean matchBiomes(ChunkData data, Collection<BiomeRegistry.BiomeIdentifier> biomes) {
			ListTag sections = Helper.tagFromLevelFromRoot(Helper.getRegion(data), "Sections");
			if (sections == null) {
				return false;
			}

			Set<String> names = new HashSet<>(biomes.size());

			filterLoop:
			for (BiomeRegistry.BiomeIdentifier identifier : biomes) {
				for (CompoundTag section : sections.iterateType(CompoundTag.class)) {
					ListTag biomePalette = Helper.tagFromCompound(Helper.tagFromCompound(section, "biomes"), "palette");
					if (biomePalette == null) {
						continue;
					}
					for (StringTag biomeName : biomePalette.iterateType(StringTag.class)) {
						if (identifier.matches(biomeName.getValue())) {
							names.add(biomeName.getValue());
							if (biomes.size() == names.size()) {
								return true;
							}
							continue filterLoop;
						}
					}
				}
			}
			return biomes.size() == names.size();
		}

		@Override
		public boolean matchAnyBiome(ChunkData data, Collection<BiomeRegistry.BiomeIdentifier> biomes) {
			ListTag sections = Helper.tagFromLevelFromRoot(Helper.getRegion(data), "Sections");
			if (sections == null) {
				return false;
			}

			for (BiomeRegistry.BiomeIdentifier identifier : biomes) {
				for (CompoundTag section : sections.iterateType(CompoundTag.class)) {
					ListTag biomePalette = Helper.tagFromCompound(Helper.tagFromCompound(section, "biomes"), "palette");
					if (biomePalette == null) {
						continue;
					}
					for (StringTag biomeName : biomePalette.iterateType(StringTag.class)) {
						if (identifier.matches(biomeName.getValue())) {
							return true;
						}
					}
				}
			}
			return false;
		}

		@Override
		public void changeBiome(ChunkData data, BiomeRegistry.BiomeIdentifier biome) {
			ListTag sections = Helper.tagFromLevelFromRoot(Helper.getRegion(data), "Sections");
			if (sections == null) {
				return;
			}

			for (CompoundTag section : sections.iterateType(CompoundTag.class)) {
				CompoundTag biomes = Helper.tagFromCompound(section, "biomes");
				if (biomes == null) {
					continue;
				}

				ListTag newBiomePalette = new ListTag();
				newBiomePalette.addString(biome.name());
				biomes.put("palette", newBiomePalette);
				biomes.putLongArray("data", new long[1]);
			}
		}

		@Override
		public void forceBiome(ChunkData data, BiomeRegistry.BiomeIdentifier biome) {
			ListTag sections = Helper.tagFromLevelFromRoot(Helper.getRegion(data), "Sections");
			if (sections == null) {
				return;
			}

			for (CompoundTag section : sections.iterateType(CompoundTag.class)) {
				CompoundTag biomes = new CompoundTag();
				ListTag newBiomePalette = new ListTag();
				newBiomePalette.addString(biome.name());
				biomes.put("palette", newBiomePalette);
				biomes.putLongArray("data", new long[1]);
				section.put("biomes", biomes);
			}
		}
	}

	@MCVersionImplementation(2834)
	public static class Sections extends ChunkFilter_15w32a.Sections {

		@Override
		public void deleteSections(ChunkData data, List<Range> ranges) {
			CompoundTag level = Helper.levelFromRoot(Helper.getRegion(data));
			switch (Helper.stringFromCompound(level, "Status", "")) {
			case "light", "spawn", "heightmaps", "full" -> level.putString("Status", "features");
			default -> {return;}
			}
			ListTag sections = Helper.tagFromCompound(level, "Sections");
			if (sections == null) {
				return;
			}
			for (int i = 0; i < sections.size(); i++) {
				CompoundTag section = sections.getCompound(i);
				for (Range range : ranges) {
					if (range.contains(section.getInt("Y"))) {
						deleteSection(section);
					}
				}
			}
		}

		// only delete blocks, not biomes
		protected void deleteSection(CompoundTag section) {
			CompoundTag blockStates = section.getCompound("block_states");
			blockStates.remove("data");
			ListTag blockPalette = new ListTag();
			CompoundTag air = new CompoundTag();
			air.putString("Name", "minecraft:air");
			blockPalette.add(air);
			blockStates.put("palette", blockPalette);
			section.remove("BlockLight");
		}
	}

	@MCVersionImplementation(2834)
	public static class RelocateEntities extends ChunkFilter_20w45a.RelocateEntities {

		@Override
		protected void applyOffsetToEntity(CompoundTag entity, Point3i offset) {
			if (entity == null) {
				return;
			}

			ListTag entityPos = Helper.tagFromCompound(entity, "Pos");
			if (entityPos != null && entityPos.size() == 3) {
				entityPos.set(0, DoubleTag.valueOf(entityPos.getDouble(0) + offset.getX()));
				entityPos.set(1, DoubleTag.valueOf(entityPos.getDouble(1) + offset.getY()));
				entityPos.set(2, DoubleTag.valueOf(entityPos.getDouble(2) + offset.getZ()));
			}

			// leashed entities
			CompoundTag leash = Helper.tagFromCompound(entity, "Leash");
			Helper.applyIntOffsetIfRootPresent(leash, "X", "Y", "Z", offset);

			// projectiles
			if (attempt(() -> Helper.applyIntOffsetIfRootPresent(entity, "xTile", "yTile", "zTile", offset))) {
				attempt(() -> Helper.applyShortOffsetIfRootPresent(entity, "xTile", "yTile", "zTile", offset));
			}

			// entities that have a sleeping place
			Helper.applyIntOffsetIfRootPresent(entity, "SleepingX", "SleepingY", "SleepingZ", offset);

			// positions for specific entity types
			String id = Helper.stringFromCompound(entity, "id", "");
			switch (id) {
			case "minecraft:dolphin":
				Helper.applyIntOffsetIfRootPresent(entity, "TreasurePosX", "TreasurePosY", "TreasurePosZ", offset);
				break;
			case "minecraft:phantom":
				Helper.applyIntOffsetIfRootPresent(entity, "AX", "AY", "AZ", offset);
				break;
			case "minecraft:shulker":
				Helper.applyIntOffsetIfRootPresent(entity, "APX", "APY", "APZ", offset);
				break;
			case "minecraft:turtle":
				Helper.applyIntOffsetIfRootPresent(entity, "HomePosX", "HomePosY", "HomePosZ", offset);
				Helper.applyIntOffsetIfRootPresent(entity, "TravelPosX", "TravelPosY", "TravelPosZ", offset);
				break;
			case "minecraft:vex":
				Helper.applyIntOffsetIfRootPresent(entity, "BoundX", "BoundY", "BoundZ", offset);
				break;
			case "minecraft:wandering_trader":
				CompoundTag wanderTarget = Helper.tagFromCompound(entity, "WanderTarget");
				Helper.applyIntOffsetIfRootPresent(wanderTarget, "X", "Y", "Z", offset);
				break;
			case "minecraft:shulker_bullet":
				CompoundTag owner = Helper.tagFromCompound(entity, "Owner");
				Helper.applyIntOffsetIfRootPresent(owner, "X", "Y", "Z", offset);
				CompoundTag target = Helper.tagFromCompound(entity, "Target");
				Helper.applyIntOffsetIfRootPresent(target, "X", "Y", "Z", offset);
				break;
			case "minecraft:end_crystal":
				CompoundTag beamTarget = Helper.tagFromCompound(entity, "BeamTarget");
				Helper.applyIntOffsetIfRootPresent(beamTarget, "X", "Y", "Z", offset);
				break;
			case "minecraft:item_frame":
			case "minecraft:painting":
				Helper.applyIntOffsetIfRootPresent(entity, "TileX", "TileY", "TileZ", offset);
				break;
			case "minecraft:villager":
				CompoundTag memories = Helper.tagFromCompound(Helper.tagFromCompound(entity, "Brain"), "memories");
				if (memories != null && !memories.isEmpty()) {
					Relocate.instance.applyOffsetToVillagerMemory(Helper.tagFromCompound(memories, "minecraft:meeting_point"), offset);
					Relocate.instance.applyOffsetToVillagerMemory(Helper.tagFromCompound(memories, "minecraft:home"), offset);
					Relocate.instance.applyOffsetToVillagerMemory(Helper.tagFromCompound(memories, "minecraft:job_site"), offset);
				}
				break;
			case "minecraft:pillager":
			case "minecraft:witch":
			case "minecraft:vindicator":
			case "minecraft:ravager":
			case "minecraft:illusioner":
			case "minecraft:evoker":
				CompoundTag patrolTarget = Helper.tagFromCompound(entity, "PatrolTarget");
				Helper.applyIntOffsetIfRootPresent(patrolTarget, "X", "Y", "Z", offset);
				break;
			case "minecraft:falling_block":
				CompoundTag tileEntityData = Helper.tagFromCompound(entity, "TileEntityData");
				Relocate.instance.applyOffsetToTileEntity(tileEntityData, offset);
				break;
			case "minecraft:sniffer":
				CompoundTag snifferMemories = Helper.tagFromCompound(Helper.tagFromCompound(entity, "Brain"), "memories");
				if (snifferMemories != null && !snifferMemories.isEmpty()) {
					ListTag value = Helper.tagFromCompound(Helper.tagFromCompound(snifferMemories, "minecraft:sniffer_explored_positions"), "value");
					if (value != null && !value.isEmpty() && value.getElementType() == Tag.Type.COMPOUND) {
						for (CompoundTag v : value.iterateType(CompoundTag.class)) {
							IntArrayTag pos = v.getIntArrayTag("pos");
							if (pos != null) {
								Helper.applyOffsetToIntArrayPos(pos, offset);
							}
						}
					}
				}
				break;
			}

			// recursively update passengers
			ListTag passengers = Helper.tagFromCompound(entity, "Passengers");
			if (passengers != null) {
				passengers.forEach(p -> applyOffsetToEntity((CompoundTag) p, offset));
			}

			CompoundTag item = Helper.tagFromCompound(entity, "Item");
			Relocate.instance.applyOffsetToItem(item, offset);

			ListTag items = Helper.tagFromCompound(entity, "Items");
			if (items != null) {
				items.forEach(i -> Relocate.instance.applyOffsetToItem((CompoundTag) i, offset));
			}

			ListTag handItems = Helper.tagFromCompound(entity, "HandItems");
			if (handItems != null) {
				handItems.forEach(i -> Relocate.instance.applyOffsetToItem((CompoundTag) i, offset));
			}

			ListTag armorItems = Helper.tagFromCompound(entity, "ArmorItems");
			if (armorItems != null) {
				armorItems.forEach(i -> Relocate.instance.applyOffsetToItem((CompoundTag) i, offset));
			}

			Helper.fixEntityUUID(entity);
		}
	}

	@MCVersionImplementation(2834)
	protected static class Relocate extends ChunkFilter_21w06a.Relocate {

		static Relocate instance;

		public Relocate() {
			instance = this;
		}

		@Override
		protected void applyOffsetToVillagerMemory(CompoundTag memory, Point3i offset) {
			super.applyOffsetToVillagerMemory(memory, offset);
		}

		@Override
		protected void applyOffsetToTileEntity(CompoundTag tileEntity, Point3i offset) {
			super.applyOffsetToTileEntity(tileEntity, offset);
		}

		@Override
		protected void applyOffsetToItem(CompoundTag item, Point3i offset) {
			super.applyOffsetToItem(item, offset);
		}
	}
}
