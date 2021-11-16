package net.querz.mcaselector.filter;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.VersionController;
import net.querz.nbt.io.NBTUtil;
import net.querz.nbt.io.NamedTag;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.DoubleTag;
import net.querz.nbt.tag.IntTag;
import net.querz.nbt.tag.ListTag;
import net.querz.nbt.tag.StringTag;
import net.querz.nbt.tag.Tag;

import java.io.File;

public class PlayerDataFilter extends TextFilter<PlayerDataFilter.PlayerDataFilterDefinition> {

	private LongOpenHashSet playerChunks = new LongOpenHashSet();
	private final Object lock = new Object();
	private boolean loaded = false;

	private static final Comparator[] comparators = {
			Comparator.CONTAINS,
			Comparator.CONTAINS_NOT,
	};

	public PlayerDataFilter() {
		this(Operator.AND, Comparator.CONTAINS, null);
	}

	private PlayerDataFilter(Operator operator, Comparator comparator, PlayerDataFilterDefinition value) {
		super(FilterType.PLAYER_DATA, operator, comparator, value);
		setRawValue(value == null ? "" : value.toString());
	}

	@Override
	public Comparator[] getComparators() {
		return comparators;
	}

	@Override
	public void setFilterValue(String raw) {
		String[] separated = raw.split(File.pathSeparator);
		if (separated.length != 2) {
			setValid(false);
			setValue(null);
			return;
		}
		File file = new File(separated[0]);
		if (!file.exists() || !file.isDirectory()) {
			setValid(false);
			setValue(null);
			return;
		}
		Object dimension;
		if (separated[1].matches("^-?[0-9]+$")) {
			try {
				dimension = Integer.parseInt(separated[1]);
			} catch (NumberFormatException ex) {
				setValid(false);
				setValue(null);
				return;
			}
		} else {
			dimension = separated[1];
		}

		setValid(true);
		setValue(new PlayerDataFilterDefinition(file, dimension));
	}

	@Override
	public PlayerDataFilter clone() {
		return new PlayerDataFilter(getOperator(), getComparator(), getFilterValue() == null ? null : getFilterValue().clone());
	}

	@Override
	public String getFormatText() {
		return "<directory>" + File.pathSeparator + "<dimension>";
	}

	@Override
	public boolean contains(PlayerDataFilterDefinition value, ChunkData data) {
		if (data.getRegion() == null || data.getRegion().getData() == null) {
			return false;
		}

		if (!loaded) {
			synchronized (lock) {
				if (playerChunks.isEmpty()) {
					loadPlayerData(value);
				}
			}
		}

		ChunkFilter chunkFilter = VersionController.getChunkFilter(data.getRegion().getData().getInt("DataVersion"));
		IntTag xPos = chunkFilter.getXPos(data.getRegion().getData());
		IntTag zPos = chunkFilter.getZPos(data.getRegion().getData());
		if (xPos == null || zPos == null) {
			return false;
		}
		return playerChunks.contains(new Point2i(xPos.asInt(), zPos.asInt()).asLong());
	}

	@Override
	public boolean containsNot(PlayerDataFilterDefinition value, ChunkData data) {
		return !contains(value, data);
	}

	@Override
	public boolean intersects(PlayerDataFilterDefinition value, ChunkData data) {
		throw new UnsupportedOperationException("\"intersects\" not allowed in PlayerDataFilter");
	}

	public static class PlayerDataFilterDefinition {

		File directory;
		Object dimension; // can be Integer or String

		public PlayerDataFilterDefinition(File directory, Object dimension) {
			this.directory = directory;
			this.dimension = dimension;
		}

		@Override
		public String toString() {
			return directory.toString() + File.pathSeparator + dimension;
		}

		@Override
		public PlayerDataFilterDefinition clone() {
			return new PlayerDataFilterDefinition(directory, dimension);
		}
	}

	private void loadPlayerData(PlayerDataFilterDefinition value) {
		playerChunks = new LongOpenHashSet();

		File[] playerFiles = value.directory.listFiles((d, f) -> f.matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.dat$"));
		if (playerFiles == null || playerFiles.length == 0) {
			return;
		}

		for (File playerFile : playerFiles) {
			try {
				NamedTag data = NBTUtil.read(playerFile);
				CompoundTag root = (CompoundTag) data.getTag();
				ListTag<DoubleTag> pos = root.getListTag("Pos").asDoubleTagList();
				if (pos.size() != 3) {
					continue;
				}
				Tag<?> dimTag = root.get("Dimension");

				Object dim = null;
				if (dimTag instanceof IntTag) {
					dim = root.getInt("Dimension");
				} else if (dimTag instanceof StringTag) {
					dim = root.getString("Dimension");
				}

				if (!value.dimension.equals(dim)) {
					continue;
				}

				playerChunks.add(new Point2i(pos.get(0).asInt(), pos.get(2).asInt()).blockToChunk().asLong());
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		loaded = true;
	}
}
