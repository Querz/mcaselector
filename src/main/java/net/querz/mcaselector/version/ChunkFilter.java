package net.querz.mcaselector.version;

import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.util.point.Point2i;
import net.querz.mcaselector.util.point.Point3i;
import net.querz.mcaselector.util.range.Range;
import net.querz.mcaselector.version.mapping.registry.BiomeRegistry;
import net.querz.mcaselector.version.mapping.registry.StatusRegistry;
import net.querz.nbt.NBTUtil;
import net.querz.nbt.*;
import java.util.*;
import java.util.function.Function;

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
		void replaceBlocks(ChunkData data, Map<String, BlockReplaceData> replace);
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
