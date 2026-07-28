package net.querz.mcaselector.version;

import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.util.point.Point2i;
import net.querz.mcaselector.util.point.Point3i;
import net.querz.mcaselector.util.range.Range;
import net.querz.mcaselector.version.mapping.registry.BiomeRegistry;
import net.querz.mcaselector.version.mapping.registry.StatusRegistry;
import net.querz.nbt.*;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;

public interface ChunkFilter {

	interface Biomes {
		boolean matchBiomes(ChunkData data, Collection<BiomeRegistry.BiomeIdentifier> biomes);
		boolean matchAnyBiome(ChunkData data, Collection<BiomeRegistry.BiomeIdentifier> biomes);
		void changeBiome(ChunkData data, BiomeRegistry.BiomeIdentifier biome);
		void forceBiome(ChunkData data, BiomeRegistry.BiomeIdentifier biome);
	}

	interface Blocks {
		boolean matchBlockNames(ChunkData data, Collection<String> names);
		boolean matchAnyBlockName(ChunkData data, Collection<String> names);
		boolean replaceBlocks(ChunkData data, Map<BlockReplaceSource, BlockReplaceData> replace);
		default BlockReplacePreviewData previewReplaceBlocks(ChunkData data, Map<BlockReplaceSource, BlockReplaceData> replace) {
			return BlockReplacePreviewData.unsupported();
		}
		int getBlockAmount(ChunkData data, String[] blocks);
		int getAverageHeight(ChunkData data);
	}

	interface Palette {
		boolean paletteEquals(ChunkData data, Collection<String> names);
	}

	interface TileEntities {
		ListTag getTileEntities(ChunkData data);
	}

	interface Sections {
		ListTag getSections(ChunkData data);
		void deleteSections(ChunkData data, List<Range> ranges);
	}

	interface InhabitedTime {
		LongTag getInhabitedTime(ChunkData data);
		void setInhabitedTime(ChunkData data, long inhabitedTime);
	}

	interface Status {
		StringTag getStatus(ChunkData data);
		void setStatus(ChunkData data, StatusRegistry.StatusIdentifier status);
		boolean matchStatus(ChunkData data, StatusRegistry.StatusIdentifier status);
	}

	interface LastUpdate {
		LongTag getLastUpdate(ChunkData data);
		void setLastUpdate(ChunkData data, long lastUpdate);
	}

	interface Pos {
		IntTag getXPos(ChunkData data);
		IntTag getYPos(ChunkData data);
		IntTag getZPos(ChunkData data);
	}

	interface Structures {
		CompoundTag getStructureStarts(ChunkData data);
		CompoundTag getStructureReferences(ChunkData data);
		String[] parseStructureStarts(CompoundTag data);
	}

	interface LightPopulated {
		ByteTag getLightPopulated(ChunkData data);
		void setLightPopulated(ChunkData data, byte lightPopulated);
	}

	interface Blending {
		void forceBlending(ChunkData data);
	}

	interface Relocate {
		boolean relocate(CompoundTag root, Point3i offset);

		default boolean applyOffsetToSection(CompoundTag section, Point3i offset, Range sectionRange) {
			NumberTag value;
			if ((value = Helper.tagFromCompound(section, "Y")) != null) {
				if (!sectionRange.contains(value.asInt())) {
					return false;
				}

				int y = value.asInt() + offset.getY();
				if (!sectionRange.contains(y)) {
					return false;
				}
				section.putByte("Y", (byte) y);
			}
			return true;
		}
	}

	interface RelocateEntities extends Relocate {}

	interface RelocatePOI extends Relocate {}

	interface Merge {
		void mergeChunks(CompoundTag source, CompoundTag destination, List<Range> ranges, int yOffset);

		CompoundTag newEmptyChunk(Point2i absoluteLocation, int dataVersion);

		default ListTag mergeLists(ListTag source, ListTag destination, List<Range> ranges, Function<Tag, Integer> ySupplier, int yOffset) {
			ListTag result = new ListTag();
			for (Tag dest : destination) {
				int y = ySupplier.apply(dest);
				for (Range range : ranges) {
					if (!range.contains(y + yOffset)) {
						result.add(dest);
					}
				}
			}

			for (Tag sourceElement : source) {
				int y = ySupplier.apply(sourceElement);
				for (Range range : ranges) {
					if (range.contains(y - yOffset)) {
						result.add(sourceElement);
						break;
					}
				}
			}

			return result;
		}

		default void mergeListTagLists(CompoundTag source, CompoundTag destination, List<Range> ranges, int yOffset, String name) {
			ListTag sourceList = Helper.tagFromLevelFromRoot(source, name);
			ListTag destinationList = Helper.tagFromLevelFromRoot(destination, name, sourceList);

			if (sourceList == null || destinationList == null || sourceList.size() != destinationList.size()) {
				return;
			}

			for (Range range : ranges) {
				int m = Math.min(range.getTo() + yOffset, sourceList.size() - 1);
				for (int i = Math.max(range.getFrom() + yOffset, 0); i <= m; i++) {
					destinationList.set(i, sourceList.get(i));
				}
			}

			initLevel(destination).put(name, destinationList);
		}

		default void mergeCompoundTagListsFromLevel(CompoundTag source, CompoundTag destination, List<Range> ranges, int yOffset, String name, Function<Tag, Integer> ySupplier) {
			ListTag sourceElements = Helper.tagFromLevelFromRoot(source, name, new ListTag());
			ListTag destinationElements = Helper.tagFromLevelFromRoot(destination, name, new ListTag());

			initLevel(destination).put(name, mergeLists(sourceElements, destinationElements, ranges, ySupplier, yOffset));
		}

		default void mergeCompoundTagLists(CompoundTag source, CompoundTag destination, List<Range> ranges, int yOffset, String name, Function<Tag, Integer> ySupplier) {
			ListTag sourceElements = Helper.tagFromCompound(source, name, new ListTag());
			ListTag destinationElements = Helper.tagFromCompound(destination, name, new ListTag());

			destination.put(name, mergeLists(sourceElements, destinationElements, ranges, ySupplier, yOffset));
		}

		// merge based on compound tag keys, assuming compound tag keys are ints
		default void mergeCompoundTags(CompoundTag source, CompoundTag destination, List<Range> ranges, int yOffset, String name) {
			CompoundTag sourceElements = Helper.tagFromCompound(source, name, new CompoundTag());
			CompoundTag destinationElements = Helper.tagFromCompound(destination, name, new CompoundTag());

			for (Map.Entry<String, Tag> sourceElement : sourceElements) {
				if (sourceElement.getKey().matches("^-?[0-9]{1,2}$")) {
					int y = Integer.parseInt(sourceElement.getKey());
					for (Range range : ranges) {
						if (range.contains(y - yOffset)) {
							destinationElements.put(sourceElement.getKey(), sourceElement.getValue());
							break;
						}
					}
				}
			}
		}

		default CompoundTag initLevel(CompoundTag c) {
			CompoundTag level = Helper.levelFromRoot(c);
			if (level == null) {
				c.put("Level", level = new CompoundTag());
			}
			return level;
		}

		default void fixEntityUUIDs(CompoundTag root) {
			ListTag entities = Helper.tagFromCompound(root, "Entities", null);
			if (entities != null) {
				entities.forEach(e -> fixEntityUUID((CompoundTag) e));
			}
		}

		private static void fixEntityUUID(CompoundTag entity) {
			Helper.fixEntityUUID(entity);
			if (entity.containsKey("Passengers")) {
				ListTag passengers = Helper.tagFromCompound(entity, "Passengers", null);
				if (passengers != null) {
					passengers.forEach(e -> fixEntityUUID((CompoundTag) e));
				}
			}
		}
	}

	interface MergeEntities extends Merge {}

	interface MergePOI extends Merge {}

	class BlockReplaceSource {

		private final BlockReplaceSourceType type;
		private final BlockReplaceTileEntityMode tileEntityMode;
		private final String name;
		private final CompoundTag state;
		private final Integer minY;
		private final Integer maxY;
		private final Set<String> biomes;
		private final Pattern pattern;

		public BlockReplaceSource(String name) {
			this.type = BlockReplaceSourceType.LEGACY_REGEX_NAME;
			this.tileEntityMode = BlockReplaceTileEntityMode.ANY;
			this.name = name;
			state = null;
			minY = null;
			maxY = null;
			biomes = Collections.emptySet();
			pattern = Pattern.compile(name);
		}

		public BlockReplaceSource(CompoundTag state) {
			this.type = BlockReplaceSourceType.EXACT_STATE;
			this.tileEntityMode = BlockReplaceTileEntityMode.ANY;
			this.state = (CompoundTag) state.copy();
			name = this.state.getString("Name");
			minY = null;
			maxY = null;
			biomes = Collections.emptySet();
			pattern = null;
		}

		private BlockReplaceSource(BlockReplaceSourceType type, BlockReplaceTileEntityMode tileEntityMode, String name, CompoundTag state, Integer minY, Integer maxY, Set<String> biomes) {
			this(type, tileEntityMode, name, state, minY, maxY, biomes, null);
		}

		private BlockReplaceSource(BlockReplaceSourceType type, BlockReplaceTileEntityMode tileEntityMode, String name,
				CompoundTag state, Integer minY, Integer maxY, Set<String> biomes, Pattern pattern) {
			this.type = type;
			this.tileEntityMode = tileEntityMode;
			this.name = name;
			this.state = state == null ? null : (CompoundTag) state.copy();
			this.minY = minY;
			this.maxY = maxY;
			this.biomes = biomes == null || biomes.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(new LinkedHashSet<>(biomes));
			this.pattern = pattern != null ? pattern : switch (type) {
				case LEGACY_REGEX_NAME, REGEX_NAME -> Pattern.compile(name);
				default -> null;
			};
		}

		public static BlockReplaceSource regexName(String pattern) {
			return new BlockReplaceSource(BlockReplaceSourceType.REGEX_NAME, BlockReplaceTileEntityMode.ANY, pattern, null, null, null, Collections.emptySet());
		}

		public static BlockReplaceSource literalName(String name) {
			return new BlockReplaceSource(BlockReplaceSourceType.LITERAL_NAME, BlockReplaceTileEntityMode.ANY, name, null, null, null, Collections.emptySet());
		}

		public static BlockReplaceSource selectedProperties(CompoundTag state) {
			return new BlockReplaceSource(BlockReplaceSourceType.SELECTED_PROPERTIES, BlockReplaceTileEntityMode.ANY, state.getString("Name"), state, null, null, Collections.emptySet());
		}

		public BlockReplaceSource withTileEntityMode(BlockReplaceTileEntityMode tileEntityMode) {
			return new BlockReplaceSource(type, tileEntityMode, name, state, minY, maxY, biomes, pattern);
		}

		public BlockReplaceSource withYRange(Integer minY, Integer maxY) {
			return new BlockReplaceSource(type, tileEntityMode, name, state, minY, maxY, biomes, pattern);
		}

		public BlockReplaceSource withBiomes(Collection<String> biomes) {
			return new BlockReplaceSource(type, tileEntityMode, name, state, minY, maxY,
					biomes == null ? Collections.emptySet() : new LinkedHashSet<>(biomes), pattern);
		}

		public boolean matches(CompoundTag blockState) {
			return !requiresLocationContext() && matchesBlockState(blockState);
		}

		public boolean matches(CompoundTag blockState, boolean hasTileEntity) {
			return !hasYRange() && !hasBiomeRestriction()
					&& matchesBlockState(blockState) && matchesTileEntityMode(hasTileEntity);
		}

		public boolean matches(CompoundTag blockState, boolean hasTileEntity, int y) {
			return !hasBiomeRestriction() && matchesY(y)
					&& matchesBlockState(blockState) && matchesTileEntityMode(hasTileEntity);
		}

		public boolean matches(CompoundTag blockState, boolean hasTileEntity, int y, String biome) {
			return matchesY(y) && matchesBiome(biome)
					&& matchesBlockState(blockState) && matchesTileEntityMode(hasTileEntity);
		}

		private boolean matchesBlockState(CompoundTag blockState) {
			String blockName = blockState.getString("Name");
			switch (type) {
				case EXACT_STATE:
					return state.equals(blockState);
				case SELECTED_PROPERTIES:
					return Objects.equals(name, blockName) && matchesSelectedProperties(blockState);
				case LITERAL_NAME:
					return Objects.equals(name, blockName);
				case LEGACY_REGEX_NAME:
				case REGEX_NAME:
					return blockName != null && pattern.matcher(blockName).matches();
				default:
					return false;
			}
		}

		private boolean matchesTileEntityMode(boolean hasTileEntity) {
			switch (tileEntityMode) {
				case REQUIRE_TILE_ENTITY:
					return hasTileEntity;
				case EXCLUDE_TILE_ENTITY:
					return !hasTileEntity;
				default:
					return true;
			}
		}

		private boolean matchesSelectedProperties(CompoundTag blockState) {
			CompoundTag selectedProperties = state.getCompoundTag("Properties");
			if (selectedProperties == null || selectedProperties.isEmpty()) {
				return false;
			}
			CompoundTag blockProperties = blockState.getCompoundTag("Properties");
			if (blockProperties == null) {
				return false;
			}
			for (Map.Entry<String, Tag> property : selectedProperties) {
				Tag blockValue = blockProperties.get(property.getKey());
				if (!property.getValue().equals(blockValue)) {
					return false;
				}
			}
			return true;
		}

		public boolean matchesAir() {
			CompoundTag air = new CompoundTag();
			air.putString("Name", "minecraft:air");
			return tileEntityMode != BlockReplaceTileEntityMode.REQUIRE_TILE_ENTITY && matchesBlockState(air);
		}

		public boolean matchesAirInSection(int sectionY) {
			return matchesAir() && intersectsSection(sectionY);
		}

		public boolean matchesY(int y) {
			return (minY == null || y >= minY) && (maxY == null || y <= maxY);
		}

		public boolean intersectsSection(int sectionY) {
			int sectionMinY = sectionY * 16;
			int sectionMaxY = sectionMinY + 15;
			return (minY == null || sectionMaxY >= minY) && (maxY == null || sectionMinY <= maxY);
		}

		public boolean matchesBiome(String biome) {
			return biomes.isEmpty() || biome != null && biomes.contains(biome);
		}

		public BlockReplaceSourceType getType() {
			return type;
		}

		public BlockReplaceTileEntityMode getTileEntityMode() {
			return tileEntityMode;
		}

		public String getName() {
			return name;
		}

		public CompoundTag getState() {
			return state == null ? null : (CompoundTag) state.copy();
		}

		public boolean hasYRange() {
			return minY != null || maxY != null;
		}

		public boolean hasBiomeRestriction() {
			return !biomes.isEmpty();
		}

		public boolean requiresLocationContext() {
			return tileEntityMode != BlockReplaceTileEntityMode.ANY || hasYRange() || hasBiomeRestriction();
		}

		public Integer getMinY() {
			return minY;
		}

		public Integer getMaxY() {
			return maxY;
		}

		public Set<String> getBiomes() {
			return biomes;
		}

		@Override
		public String toString() {
			String value = baseToString();
			switch (tileEntityMode) {
				case REQUIRE_TILE_ENTITY:
					value = "tile(" + value + ")";
					break;
				case EXCLUDE_TILE_ENTITY:
					value = "no_tile(" + value + ")";
					break;
			}
			if (hasYRange()) {
				value = "y(" + formatYRange() + ", " + value + ")";
			}
			if (hasBiomeRestriction()) {
				value = "biome(" + formatBiomes() + ", " + value + ")";
			}
			return value;
		}

		private String baseToString() {
			switch (type) {
				case EXACT_STATE:
					return NBTUtil.toSNBT(state);
				case SELECTED_PROPERTIES:
					return "props(" + NBTUtil.toSNBT(state) + ")";
				case REGEX_NAME:
					return "regex(" + formatWrapperArgument(name) + ")";
				case LITERAL_NAME:
					return "literal(" + formatWrapperArgument(name) + ")";
				case LEGACY_REGEX_NAME:
					if (name.startsWith("minecraft:")) {
						return name;
					}
					return "'" + name + "'";
				default:
					return null;
			}
		}

		private static String formatWrapperArgument(String value) {
			if (!value.equals(value.trim()) || value.contains(")")) {
				return "'" + value + "'";
			}
			return value;
		}

		private String formatYRange() {
			return (minY == null ? "" : minY) + ".." + (maxY == null ? "" : maxY);
		}

		private String formatBiomes() {
			StringJoiner joiner = new StringJoiner(";");
			for (String biome : biomes) {
				joiner.add(biome);
			}
			return joiner.toString();
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (!(o instanceof BlockReplaceSource that)) {
				return false;
			}
			return type == that.type
					&& tileEntityMode == that.tileEntityMode
					&& Objects.equals(name, that.name)
					&& Objects.equals(state, that.state)
					&& Objects.equals(minY, that.minY)
					&& Objects.equals(maxY, that.maxY)
					&& Objects.equals(biomes, that.biomes);
		}

		@Override
		public int hashCode() {
			return Objects.hash(type, tileEntityMode, name, state, minY, maxY, biomes);
		}
	}

	enum BlockReplaceSourceType {
		LEGACY_REGEX_NAME, REGEX_NAME, LITERAL_NAME, EXACT_STATE, SELECTED_PROPERTIES
	}

	enum BlockReplaceTileEntityMode {
		ANY, REQUIRE_TILE_ENTITY, EXCLUDE_TILE_ENTITY
	}

	class BlockReplaceData {

		private String name;
		private CompoundTag state;
		private CompoundTag tile;
		private final BlockReplaceType type;

		public BlockReplaceData(String name) {
			type = BlockReplaceType.NAME;
			this.name = name;
			state = new CompoundTag();
			state.putString("Name", name);
		}

		public BlockReplaceData(String name, CompoundTag tile) {
			type = BlockReplaceType.NAME_TILE;
			this.name = name;
			this.tile = tile;
			state = new CompoundTag();
			state.putString("Name", name);
		}

		public BlockReplaceData(CompoundTag state) {
			type = BlockReplaceType.STATE;
			this.state = state;
			name = state.getString("Name");
		}

		public BlockReplaceData(CompoundTag state, CompoundTag tile) {
			type = BlockReplaceType.STATE_TILE;
			this.state = state;
			this.tile = tile;
			name = state.getString("Name");
		}

		public BlockReplaceType getType() {
			return type;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setState(CompoundTag state) {
			this.state = state;
		}

		public CompoundTag getState() {
			return state;
		}

		public void setTile(CompoundTag tile) {
			this.tile = tile;
		}

		public CompoundTag getTile() {
			return tile;
		}

		@Override
		public String toString() {
			switch (type) {
				case NAME:
					if (name.startsWith("minecraft:")) {
						return name;
					} else {
						return "'" + name + "'";
					}
				case STATE:
					return NBTUtil.toSNBT(state);
				case STATE_TILE:
					return NBTUtil.toSNBT(state) + ";" + NBTUtil.toSNBT(tile);
				case NAME_TILE:
					if (name.startsWith("minecraft:")) {
						return name + ";" + NBTUtil.toSNBT(tile);
					} else {
						return "'" + name + "';" + NBTUtil.toSNBT(tile);
					}
				default:
					return null;
			}
		}
	}

	class BlockReplacePreviewData {

		private final boolean supported;
		private final List<BlockReplaceRulePreviewData> rules = new ArrayList<>();
		private long blocks;
		private int sections;
		private int lightSections;
		private int completedAirSections;
		private long tileEntityAdditions;
		private long tileEntityRemovals;
		private long tileEntityUpdates;
		private long overlappingBlocks;

		private BlockReplacePreviewData(boolean supported) {
			this.supported = supported;
		}

		public static BlockReplacePreviewData supported() {
			return new BlockReplacePreviewData(true);
		}

		public static BlockReplacePreviewData supported(Map<BlockReplaceSource, BlockReplaceData> replace) {
			BlockReplacePreviewData result = supported();
			result.setRules(replace);
			return result;
		}

		public static BlockReplacePreviewData unsupported() {
			return new BlockReplacePreviewData(false);
		}

		public boolean isSupported() {
			return supported;
		}

		public void addSection(long blocks) {
			if (blocks > 0) {
				this.blocks += blocks;
				sections++;
			}
		}

		private void setRules(Map<BlockReplaceSource, BlockReplaceData> replace) {
			int index = 1;
			for (Map.Entry<BlockReplaceSource, BlockReplaceData> rule : replace.entrySet()) {
				rules.add(new BlockReplaceRulePreviewData(index++, rule.getKey(), rule.getValue()));
			}
		}

		public void incrementRuleBlocks(int index) {
			rules.get(index).incrementBlocks();
		}

		public void incrementOverlappingBlocks() {
			overlappingBlocks++;
		}

		public void incrementLightSections() {
			lightSections++;
		}

		public void incrementCompletedAirSections() {
			completedAirSections++;
		}

		public void incrementTileEntityAdditions() {
			tileEntityAdditions++;
		}

		public void incrementTileEntityRemovals() {
			tileEntityRemovals++;
		}

		public void incrementTileEntityUpdates() {
			tileEntityUpdates++;
		}

		public long getBlocks() {
			return blocks;
		}

		public int getSections() {
			return sections;
		}

		public int getLightSections() {
			return lightSections;
		}

		public int getCompletedAirSections() {
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

		public List<BlockReplaceRulePreviewData> getRules() {
			return Collections.unmodifiableList(rules);
		}
	}

	class BlockReplaceRulePreviewData {

		private final int index;
		private final BlockReplaceSource source;
		private final BlockReplaceData target;
		private long blocks;

		private BlockReplaceRulePreviewData(int index, BlockReplaceSource source, BlockReplaceData target) {
			this.index = index;
			this.source = source;
			this.target = target;
		}

		private void incrementBlocks() {
			blocks++;
		}

		public int getIndex() {
			return index;
		}

		public BlockReplaceSourceType getSourceType() {
			return source.getType();
		}

		public String getSourceText() {
			return source.toString();
		}

		public String getTargetText() {
			return target.toString();
		}

		public long getBlocks() {
			return blocks;
		}
	}

	enum BlockReplaceType {
		NAME, STATE, STATE_TILE, NAME_TILE
	}

	interface Entities {
		void deleteEntities(ChunkData data, List<Range> ranges);
		ListTag getEntities(ChunkData data);
	}

	interface Heightmap {
		void worldSurface(ChunkData data);
		void oceanFloor(ChunkData data);
		void motionBlocking(ChunkData data);
		void motionBlockingNoLeaves(ChunkData data);
	}
}
