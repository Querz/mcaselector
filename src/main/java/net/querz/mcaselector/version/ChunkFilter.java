package net.querz.mcaselector.version;

import net.querz.nbt.io.SNBTUtil;
import net.querz.nbt.tag.CompoundTag;
import java.io.IOException;
import java.util.Map;

public interface ChunkFilter {

	boolean matchBlockNames(CompoundTag data, String... names);

	boolean matchBiomeIDs(CompoundTag data, int... ids);

	void changeBiome(CompoundTag data, int id);

	void forceBiome(CompoundTag data, int id);

	void replaceBlocks(CompoundTag data, Map<String, BlockReplaceData> replace);

	class BlockReplaceData {

		private Integer id;
		private Integer data;
		private String name;
		private CompoundTag state;
		private CompoundTag tile;
		private final BlockReplaceType type;

		public BlockReplaceData(int id, int data) {
			type = BlockReplaceType.ID_DATA;
			this.id = id;
			this.data = data;
		}

		public BlockReplaceData(int id, int data, CompoundTag tile) {
			type = BlockReplaceType.ID_DATA_TILE;
			this.id = id;
			this.data = data;
			this.tile = tile;
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

		public void setId(int id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public void setData(int data) {
			this.data = data;
		}

		public Integer getData() {
			return data;
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
			try {
				return "name:" + name + ", state:" + SNBTUtil.toSNBT(state) + (tile != null ? ", tile:" + SNBTUtil.toSNBT(tile) : "");
			} catch (IOException e) {
				return null;
			}
		}
	}

	enum BlockReplaceType {
		ID_DATA, NAME, STATE, STATE_TILE, ID_DATA_TILE, NAME_TILE
	}
}
