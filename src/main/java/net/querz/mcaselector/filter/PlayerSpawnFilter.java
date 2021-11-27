package net.querz.mcaselector.filter;

import net.querz.mcaselector.point.Point2i;
import net.querz.nbt.io.NBTUtil;
import net.querz.nbt.io.NamedTag;
import net.querz.nbt.tag.*;
import java.io.File;

public class PlayerSpawnFilter extends PlayerLocationFilter {

	public PlayerSpawnFilter() {
		this(FilterType.PLAYER_SPAWN, Operator.AND, Comparator.CONTAINS, null, new Object());
	}

	private PlayerSpawnFilter(FilterType type, Operator operator, Comparator comparator, PlayerLocationFilterDefinition value, Object lock) {
		super(type, operator, comparator, value, lock);
		setRawValue(value == null ? "" : value.toString());
	}

	@Override
	protected void loadPlayerData(PlayerLocationFilterDefinition value) {
		playerChunks.clear();
		playerRegions.clear();

		File[] playerFiles = value.directory.listFiles((d, f) -> f.matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.dat$"));
		if (playerFiles == null || playerFiles.length == 0) {
			return;
		}

		for (File playerFile : playerFiles) {
			try {
				NamedTag data = NBTUtil.read(playerFile);
				CompoundTag root = (CompoundTag) data.getTag();
				IntTag spawnX = root.getIntTag("SpawnX");
				IntTag spawnZ = root.getIntTag("SpawnZ");
				if (spawnX == null || spawnZ == null) {
					continue;
				}
				Tag<?> dimTag = root.get("SpawnDimension");

				Object dim = null;
				if (dimTag instanceof IntTag) {
					dim = root.getInt("SpawnDimension");
				} else if (dimTag instanceof StringTag) {
					dim = root.getString("SpawnDimension");
				}

				if (!value.dimension.equals(dim)) {
					continue;
				}

				Point2i playerLocation = new Point2i(spawnX.asInt(), spawnZ.asInt());
				playerChunks.add(playerLocation.blockToChunk().asLong());
				playerRegions.add(playerLocation.blockToRegion().asLong());
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		loaded.set(true);
	}

	@Override
	public PlayerSpawnFilter clone() {
		PlayerSpawnFilter clone = new PlayerSpawnFilter(getType(), getOperator(), getComparator(), getFilterValue() == null ? null : getFilterValue().clone(), lock);
		clone.playerChunks = playerChunks;
		clone.playerRegions = playerRegions;
		clone.loaded = loaded;
		return clone;
	}

	@Override
	public String toString() {
		return "PlayerSpawn " + getComparator().getQueryString() + " \"" + getRawValue().replace("\\", "\\\\") + "\"";
	}
}
