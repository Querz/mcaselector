package net.querz.mcaselector.version.anvil112;

import net.querz.mcaselector.tile.Tile;
import net.querz.mcaselector.version.HeightmapCalculator;
import net.querz.mcaselector.version.Helper;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.ListTag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

public class Anvil112HeightmapCalculator implements HeightmapCalculator {

	private static final Set<Short> nonWorldSurfaceBlocks = new HashSet<>();

	private static final Logger LOGGER = LogManager.getLogger(Data.class);

	static {
		try (BufferedReader bis = new BufferedReader(new InputStreamReader(Objects.requireNonNull(Data.class.getClassLoader().getResourceAsStream("mapping/112/heightmap_data.txt"))))) {
			String line;
			while ((line = bis.readLine()) != null) {
				try {
					nonWorldSurfaceBlocks.add(Short.parseShort(line));
				} catch (NumberFormatException ex) {
					LOGGER.error("invalid line in heightmap data file: \"{}\"", line);
				}
			}
		} catch (IOException ex) {
			throw new RuntimeException("failed to read mapping/112/heightmap_data.txt");
		}
	}

	@Override
	public void worldSurface(CompoundTag root) {
		setHeightMap(root, getHeightMap(root, block -> !nonWorldSurfaceBlocks.contains(block)));
	}

	@Override
	public void oceanFloor(CompoundTag root) {
		// nothing to do until 1.13
	}

	@Override
	public void motionBlocking(CompoundTag root) {
		// nothing to do until 1.13
	}

	@Override
	public void motionBlockingNoLeaves(CompoundTag root) {
		// nothing to do until 1.13
	}

	protected void setHeightMap(CompoundTag root, int[] heightmap) {
		CompoundTag level = root.getCompoundTag("Level");
		if (level == null) {
			return;
		}
		level.putIntArray("HeightMap", heightmap);
	}

	protected int[] getHeightMap(CompoundTag root, Predicate<Short> matcher) {
		ListTag sections = Helper.getSectionsFromLevelFromRoot(root, "Sections");
		if (sections == null) {
			return new int[256];
		}

		byte[][] blocksArray = new byte[16][];
		for (CompoundTag s : sections.iterateType(CompoundTag.class)) {
			if (!s.containsKey("Blocks")) {
				continue;
			}
			int y = Helper.numberFromCompound(s, "Y", -1).intValue();
			byte[] b = Helper.byteArrayFromCompound(s, "Blocks");
			if (y >= 0 && y < 16 && b != null) {
				blocksArray[y] = b;
			}
		}

		int[] heightmap = new int[256];

		// loop over x/z
		for (int cx = 0; cx < Tile.CHUNK_SIZE; cx++) {
			loop:
			for (int cz = 0; cz < Tile.CHUNK_SIZE; cz++) {
				for (int i = 15; i >= 0; i--) {
					byte[] blocks = blocksArray[i];
					if (blocks == null) {
						continue;
					}
					for (int cy = 15; cy >= 0; cy--) {
						int index = cy * Tile.CHUNK_SIZE * Tile.CHUNK_SIZE + cz * Tile.CHUNK_SIZE + cx;
						short block = (short) (blocks[index] & 0xFF);
						if (matcher.test(block)) {
							heightmap[cz * Tile.CHUNK_SIZE + cx] = i * Tile.CHUNK_SIZE + cy + 1;
							continue loop;
						}
					}
				}
			}
		}
		return heightmap;
	}
}
