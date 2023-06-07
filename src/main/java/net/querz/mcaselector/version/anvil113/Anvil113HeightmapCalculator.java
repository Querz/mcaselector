package net.querz.mcaselector.version.anvil113;

import net.querz.mcaselector.math.Bits;
import net.querz.mcaselector.tile.Tile;
import net.querz.mcaselector.version.HeightmapCalculator;
import net.querz.mcaselector.version.Helper;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.ListTag;
import net.querz.nbt.StringTag;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

public class Anvil113HeightmapCalculator implements HeightmapCalculator {

	private static final Set<String> nonLightBlockingBlocks = new HashSet<>();

	static {
		try (BufferedReader bis = new BufferedReader(new InputStreamReader(Objects.requireNonNull(Data.class.getClassLoader().getResourceAsStream("mapping/113/heightmap_data.txt"))))) {
			String line;
			while ((line = bis.readLine()) != null) {
				nonLightBlockingBlocks.add("minecraft:" + line);
			}
		} catch (IOException ex) {
			throw new RuntimeException("failed to read mapping/113/heightmap_data.txt");
		}
	}

	@Override
	public void worldSurface(CompoundTag root) {
		setHeightMap(root, "WORLD_SURFACE", getHeightMap(root, blockState -> {
			StringTag name = blockState.getStringTag("Name");
			if (name == null) {
				return false;
			}
			return !isAir(name.getValue());
		}));

		setHeightMap(root, "LIGHT_BLOCKING", getHeightMap(root, blockState -> {
			StringTag name = blockState.getStringTag("Name");
			if (name == null) {
				return false;
			}
			return !nonLightBlockingBlocks.contains(name.getValue());
		}));
	}

	@Override
	public void oceanFloor(CompoundTag root) {
		setHeightMap(root, "OCEAN_FLOOR", getHeightMap(root, blockState -> {
			StringTag name = blockState.getStringTag("Name");
			if (name == null) {
				return false;
			}
			return !isAir(name.getValue()) && !isLiquid(name.getValue()) && !isNonMotionBlocking(name.getValue());
		}));
	}

	@Override
	public void motionBlocking(CompoundTag root) {
		setHeightMap(root, "MOTION_BLOCKING", getHeightMap(root, blockState -> {
			StringTag name = blockState.getStringTag("Name");
			if (name == null) {
				return false;
			}
			return !isAir(name.getValue()) && !isNonMotionBlocking(name.getValue());
		}));
	}

	@Override
	public void motionBlockingNoLeaves(CompoundTag root) {
		setHeightMap(root, "MOTION_BLOCKING_NO_LEAVES", getHeightMap(root, blockState -> {
			StringTag name = blockState.getStringTag("Name");
			if (name == null) {
				return false;
			}
			return !isAir(name.getValue()) && !isNonMotionBlocking(name.getValue()) && !isFoliage(name.getValue());
		}));
	}

	protected void setHeightMap(CompoundTag root, String name, long[] heightmap) {
		CompoundTag level = root.getCompoundTag("Level");
		if (level == null) {
			return;
		}
		CompoundTag heightmaps = level.getCompoundOrDefault("Heightmaps", new CompoundTag());
		heightmaps.putLongArray(name, heightmap);
		level.put("Heightmaps", heightmaps);
	}

	protected long[] getHeightMap(CompoundTag root, Predicate<CompoundTag> matcher) {
		ListTag sections = Helper.getSectionsFromLevelFromRoot(root, "Sections");
		if (sections == null) {
			return new long[36];
		}

		ListTag[] palettes = new ListTag[16];
		long[][] blockStatesArray = new long[16][];
		sections.forEach(s -> {
			ListTag p = Helper.tagFromCompound(s, "Palette");
			long[] b = Helper.longArrayFromCompound(s, "BlockStates");
			int y = Helper.numberFromCompound(s, "Y", -1).intValue();
			if (y >= 0 && y <= 15 && p != null && b != null) {
				palettes[y] = p;
				blockStatesArray[y] = b;
			}
		});

		short[] heightmap = new short[256];

		// loop over x/z
		for (int cx = 0; cx < Tile.CHUNK_SIZE; cx++) {
			loop:
			for (int cz = 0; cz < Tile.CHUNK_SIZE; cz++) {
				for (int i = 15; i >= 0; i--) {
					ListTag palette = palettes[i];
					if (palette == null) {
						continue;
					}
					long[] blockStates = blockStatesArray[i];
					for (int cy = 15; cy >= 0; cy--) {
						int blockIndex = cy * Tile.CHUNK_SIZE * Tile.CHUNK_SIZE + cz * Tile.CHUNK_SIZE + cx;
						if (matcher.test(getBlockAt(blockIndex, blockStates, palette))) {
							heightmap[cz * Tile.CHUNK_SIZE + cx] = (short) (i * Tile.CHUNK_SIZE + cy + 1);
							continue loop;
						}
					}
				}
			}
		}
		return applyHeightMap(heightmap);
	}

	protected long[] applyHeightMap(short[] rawHeightmap) {
		long[] data = new long[36];
		int offset = 0;
		int index = 0;
		for (int i = 0; i < 36; i++) {
			long l = 0L;
			for (int j = 0; j < 8 && index < 256; j++, index++) {
				int shift = 9 * j - offset;
				if (shift < 0) {
					l += ((long) rawHeightmap[index] >> -shift);
				} else {
					l += ((long) rawHeightmap[index] << shift);
				}
			}
			offset++;
			if (offset == 9) {
				offset = 0;
			} else {
				index--;
			}
			data[i] = l;
		}
		return data;
	}

	protected CompoundTag getBlockAt(int index, long[] blockStates, ListTag palette) {
		return palette.getCompound(getPaletteIndex(index, blockStates));
	}

	protected int getPaletteIndex(int blockIndex, long[] blockStates) {
		int bits = blockStates.length >> 6;
		double blockStatesIndex = blockIndex / (4096D / blockStates.length);
		int longIndex = (int) blockStatesIndex;
		int startBit = (int) ((blockStatesIndex - Math.floor(blockStatesIndex)) * 64D);
		if (startBit + bits > 64) {
			long prev = Bits.bitRange(blockStates[longIndex], startBit, 64);
			long next = Bits.bitRange(blockStates[longIndex + 1], 0, startBit + bits - 64);
			return (int) ((next << 64 - startBit) + prev);
		} else {
			return (int) Bits.bitRange(blockStates[longIndex], startBit, startBit + bits);
		}
	}

}
