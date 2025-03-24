package net.querz.mcaselector.filter.filters;

import net.querz.mcaselector.filter.Comparator;
import net.querz.mcaselector.filter.FilterType;
import net.querz.mcaselector.filter.Operator;
import net.querz.mcaselector.util.point.Point2i;
import net.querz.nbt.*;
import java.io.File;
import java.util.regex.Pattern;

public class PlayerSpawnFilter extends PlayerLocationFilter {

	private static final Pattern playerFilePattern = Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.dat$");

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

		File[] playerFiles = value.directory.listFiles((d, f) -> playerFilePattern.matcher(f).matches());
		if (playerFiles == null || playerFiles.length == 0) {
			return;
		}

		for (File playerFile : playerFiles) {
			try {
				CompoundTag root = (CompoundTag) NBTUtil.read(playerFile);
				IntTag spawnX = root.getIntTag("SpawnX");
				IntTag spawnZ = root.getIntTag("SpawnZ");
				if (spawnX == null || spawnZ == null) {
					continue;
				}
				Tag dimTag = root.get("SpawnDimension");

				Object dim = null;
				if (dimTag instanceof IntTag) {
					IntTag tag = root.getIntTag("SpawnDimension");
					if (tag != null) {
						dim = tag.asInt();
					}
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
