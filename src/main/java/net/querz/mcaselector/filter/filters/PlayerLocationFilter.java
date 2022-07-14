package net.querz.mcaselector.filter.filters;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.querz.mcaselector.filter.*;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.property.DataProperty;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.VersionController;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.IntTag;
import net.querz.nbt.ListTag;
import net.querz.nbt.NBTUtil;
import net.querz.nbt.StringTag;
import net.querz.nbt.Tag;
import java.io.File;
import java.io.Serializable;

public class PlayerLocationFilter extends TextFilter<PlayerLocationFilter.PlayerLocationFilterDefinition> implements RegionMatcher {

	protected LongOpenHashSet playerChunks = new LongOpenHashSet();
	protected LongOpenHashSet playerRegions = new LongOpenHashSet();
	protected final Object lock;
	protected DataProperty<Boolean> loaded = new DataProperty<>(false);

	private static final Comparator[] comparators = {
			Comparator.CONTAINS,
			Comparator.CONTAINS_NOT,
	};

	public PlayerLocationFilter() {
		this(Operator.AND, Comparator.CONTAINS, null);
	}

	private PlayerLocationFilter(Operator operator, Comparator comparator, PlayerLocationFilterDefinition value) {
		this(FilterType.PLAYER_DATA, operator, comparator, value, new Object());
	}

	protected PlayerLocationFilter(FilterType type, Operator operator, Comparator comparator, PlayerLocationFilterDefinition value, Object lock) {
		super(type, operator, comparator, value);
		this.lock = lock;
		setRawValue(value == null ? "" : value.toString());
	}

	@Override
	public Comparator[] getComparators() {
		return comparators;
	}

	@Override
	public void setFilterValue(String raw) {
		String[] separated = raw.split(File.pathSeparator, 2);
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
		setValue(new PlayerLocationFilterDefinition(file, dimension));
		setRawValue(raw);
	}

	@Override
	public PlayerLocationFilter clone() {
		PlayerLocationFilter clone = new PlayerLocationFilter(getType(), getOperator(), getComparator(), getFilterValue() == null ? null : getFilterValue().clone(), lock);
		clone.playerChunks = playerChunks;
		clone.playerRegions = playerRegions;
		clone.loaded = loaded;
		return clone;
	}

	@Override
	public String getFormatText() {
		return "<directory>" + File.pathSeparator + "<dimension>";
	}

	@Override
	public boolean contains(PlayerLocationFilterDefinition value, ChunkData data) {
		if (data.region() == null || data.region().getData() == null) {
			return false;
		}

		if (!loaded.get()) {
			synchronized (lock) {
				if (playerChunks.isEmpty()) {
					loadPlayerData(value);
				}
			}
		}

		ChunkFilter chunkFilter = VersionController.getChunkFilter(data.getDataVersion());
		IntTag xPos = chunkFilter.getXPos(data.region().getData());
		IntTag zPos = chunkFilter.getZPos(data.region().getData());
		if (xPos == null || zPos == null) {
			return false;
		}
		return playerChunks.contains(new Point2i(xPos.asInt(), zPos.asInt()).asLong());
	}

	@Override
	public boolean containsNot(PlayerLocationFilterDefinition value, ChunkData data) {
		return !contains(value, data);
	}

	@Override
	public boolean intersects(PlayerLocationFilterDefinition value, ChunkData data) {
		throw new UnsupportedOperationException("\"intersects\" not allowed in player filter");
	}

	@Override
	public boolean matchesRegion(Point2i region) {
		if (!loaded.get()) {
			synchronized (lock) {
				if (playerRegions.isEmpty()) {
					loadPlayerData(value);
				}
			}
		}

		return switch (getComparator()) {
			case CONTAINS -> playerRegions.contains(region.asLong());
			case CONTAINS_NOT -> !playerRegions.contains(region.asLong());
			default -> false;
		};
	}

	public static class PlayerLocationFilterDefinition implements Serializable {

		File directory;
		Object dimension; // can be Integer or String

		public PlayerLocationFilterDefinition(File directory, Object dimension) {
			this.directory = directory;
			this.dimension = dimension;
		}

		@Override
		public String toString() {
			return directory + File.pathSeparator + dimension;
		}

		@Override
		public PlayerLocationFilterDefinition clone() {
			return new PlayerLocationFilterDefinition(directory, dimension);
		}
	}

	protected void loadPlayerData(PlayerLocationFilterDefinition value) {
		playerChunks.clear();
		playerRegions.clear();

		File[] playerFiles = value.directory.listFiles((d, f) -> f.matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.dat$"));
		if (playerFiles == null || playerFiles.length == 0) {
			return;
		}

		for (File playerFile : playerFiles) {
			try {
				CompoundTag root = (CompoundTag) NBTUtil.read(playerFile);
				ListTag pos = root.getListTag("Pos");
				if (pos.size() != 3) {
					continue;
				}
				Tag dimTag = root.get("Dimension");

				Object dim = null;
				if (dimTag instanceof IntTag) {
					dim = root.getInt("Dimension");
				} else if (dimTag instanceof StringTag) {
					dim = root.getString("Dimension");
				}

				if (!value.dimension.equals(dim)) {
					continue;
				}

				Point2i playerLocation = new Point2i(pos.getInt(0), pos.getInt(2));
				playerChunks.add(playerLocation.blockToChunk().asLong());
				playerRegions.add(playerLocation.blockToRegion().asLong());
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		loaded.set(true);
	}

	@Override
	public String toString() {
		return "PlayerLocation " + getComparator().getQueryString() + " \"" + getRawValue().replace("\\", "\\\\") + "\"";
	}

	@Override
	public void resetTempData() {
		synchronized (lock) {
			playerChunks.clear();
			playerRegions.clear();
			loaded.set(false);
		}
	}
}
