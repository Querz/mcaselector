package net.querz.mcaselector.version;

import net.querz.mcaselector.changer.fields.ReplaceBlocksField;
import net.querz.mcaselector.exception.ParseException;
import net.querz.mcaselector.io.StringPointer;
import net.querz.mcaselector.io.registry.BiomeRegistry;
import net.querz.mcaselector.range.Range;
import net.querz.nbt.NBTUtil;
import net.querz.nbt.*;
import net.querz.nbt.io.snbt.SNBTParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public interface ChunkFilter {

	// returns true if ALL block names are present
	boolean matchBlockNames(CompoundTag data, Collection<String> names);

	// returns true if ALL biomes are present
	boolean matchBiomes(CompoundTag data, Collection<BiomeRegistry.BiomeIdentifier> biomes);

	// returns true if AT LEAST ONE block name is present
	boolean matchAnyBlockName(CompoundTag data, Collection<String> names);

	// returns true if the palette ONLY contains the block names, ignoring air
	boolean paletteEquals(CompoundTag data, Collection<String> names);

	// returns true if AT LEAST ONE biome is present
	boolean matchAnyBiome(CompoundTag data, Collection<BiomeRegistry.BiomeIdentifier> biomes);

	void changeBiome(CompoundTag data, BiomeRegistry.BiomeIdentifier biome);

	void forceBiome(CompoundTag data, BiomeRegistry.BiomeIdentifier biome);

	void replaceBlocks(CompoundTag data, Map<BlockReplaceData, BlockReplaceData> replace);

	int getAverageHeight(CompoundTag data);

	int getBlockAmount(CompoundTag data, String[] blocks);

	ListTag getTileEntities(CompoundTag data);

	CompoundTag getStructureStarts(CompoundTag data);

	CompoundTag getStructureReferences(CompoundTag data);

	ListTag getSections(CompoundTag data);

	void deleteSections(CompoundTag data, List<Range> ranges);

	LongTag getInhabitedTime(CompoundTag data);

	void setInhabitedTime(CompoundTag data, long inhabitedTime);

	StringTag getStatus(CompoundTag data);

	void setStatus(CompoundTag data, String status);

	LongTag getLastUpdate(CompoundTag data);

	void setLastUpdate(CompoundTag data, long lastUpdate);

	IntTag getXPos(CompoundTag data);

	IntTag getYPos(CompoundTag data);

	IntTag getZPos(CompoundTag data);

	ByteTag getLightPopulated(CompoundTag data);

	void setLightPopulated(CompoundTag data, byte lightPopulated);

	void forceBlending(CompoundTag data);


	public static void main(String[] args) {
		String s = "minecraft:stone;{tile:test}";

//		BlockReplaceData.parse(s);
	}

	class BlockReplaceData {

		private static final Logger LOGGER = LogManager.getLogger(ReplaceBlocksField.class);

		private static final Set<String> validNames = new HashSet<>();

		static {
			try (BufferedReader bis = new BufferedReader(
					new InputStreamReader(Objects.requireNonNull(ReplaceBlocksField.class.getClassLoader().getResourceAsStream("mapping/all_block_names.txt"))))) {
				String line;
				while ((line = bis.readLine()) != null) {
					validNames.add("minecraft:" + line);
				}
			} catch (IOException ex) {
				LOGGER.error("error reading mapping/all_block_names.txt", ex);
			}
		}

		public static final BlockReplaceData AIR = new BlockReplaceData("minecraft:air");

		private String name;
		private CompoundTag state;
		private CompoundTag tile;
		private final BlockReplaceType type;

		public static BlockReplaceData parse(String s) throws ParseException, net.querz.nbt.io.snbt.ParseException {
			// minecraft:<block-name>
			// <block-name>
			// '<custom-block-name-with-namespace>'
			// <snbt-string-block-state>
			// <from>;<snbt-string-tile-entity>

			StringPointer sp = new StringPointer(s);
			sp.skipWhitespace();
			CompoundTag state = null;
			String name = null;
			switch (sp.currentChar()) {
				case '{': // start of snbt
					String remaining = s.substring(sp.index());
					SNBTParser parser = new SNBTParser(remaining);
					state = (CompoundTag) parser.parse(true);
					int read = parser.getReadChars() - 1;
					sp.skip(read);
				case '\'': // start of custom name
					name = sp.parseQuotedString('\'');
				default: // start of registered minecraft name

			}
			sp.skipWhitespace();

			// parse tile
			CompoundTag tile = null;
			if (sp.hasNext() && sp.currentChar() == ';') {
				sp.skip(1);
				String remaining = s.substring(sp.index());
				tile = (CompoundTag) NBTUtil.fromSNBT(remaining, true);
			}
			return null;
		}

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

		public boolean matches(CompoundTag state, CompoundTag tile) {
			return switch (type) {
				case NAME -> state.getStringOrDefault("Name", "").matches(name);
				case NAME_TILE -> state.getStringOrDefault("Name", "").matches(name) && (this.tile == null || this.tile.partOf(tile));
				case STATE -> this.state.partOf(state);
				case STATE_TILE -> this.state.partOf(state) && (this.tile == null || this.tile.partOf(tile));
			};
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

		@Override
		public int hashCode() {
			return Objects.hash(name, state, tile, type);
		}

		@Override
		public boolean equals(Object other) {
			return other instanceof BlockReplaceData o
					&& name.equals(o.name)
					&& state.equals(o.state)
					&& (tile == null || tile.equals(o.tile))
					&& type == o.type;
		}
	}

	enum BlockReplaceType {
		NAME, STATE, STATE_TILE, NAME_TILE
	}
}
