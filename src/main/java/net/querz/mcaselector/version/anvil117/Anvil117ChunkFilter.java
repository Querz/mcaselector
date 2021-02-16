package net.querz.mcaselector.version.anvil117;

import net.querz.mcaselector.math.Bits;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.point.Point3i;
import net.querz.mcaselector.progress.Timer;
import net.querz.mcaselector.text.TextHelper;
import net.querz.mcaselector.validation.ValidationHelper;
import net.querz.mcaselector.version.anvil113.Anvil113ChunkFilter;
import net.querz.nbt.io.SNBTUtil;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.ListTag;
import net.querz.nbt.tag.LongArrayTag;
import net.querz.nbt.tag.Tag;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static net.querz.mcaselector.validation.ValidationHelper.catchClassCastException;
import static net.querz.mcaselector.validation.ValidationHelper.withDefault;

public class Anvil117ChunkFilter extends Anvil113ChunkFilter {

	@Override
	public void forceBiome(CompoundTag data, int id) {
		if (data.containsKey("Level")) {
			int[] biomes = ValidationHelper.withDefault(() -> data.getCompoundTag("Level").getIntArray("Biomes"), null);
			if (biomes != null && (biomes.length == 1024 || biomes.length == 1536)) {
				biomes = new int[biomes.length];
			} else {
				biomes = new int[1536];
			}
			Arrays.fill(biomes, id);
			data.getCompoundTag("Level").putIntArray("Biomes", biomes);
		}
	}

	@Override
	public void replaceBlocks(CompoundTag data, Map<String, BlockReplaceData> replace) {
		CompoundTag level = withDefault(() -> data.getCompoundTag("Level"), null);
		if (level == null) {
			return;
		}
		Tag<?> rawSections = level.get("Sections");
		if (rawSections == null || rawSections.getID() == LongArrayTag.ID) {
			return;
		}
		ListTag<CompoundTag> sections = catchClassCastException(((ListTag<?>) rawSections)::asCompoundTagList);
		if (sections == null) {
			return;
		}

		Point2i pos = withDefault(() -> new Point2i(level.getInt("xPos"), level.getInt("zPos")).chunkToBlock(), null);
		if (pos == null) {
			return;
		}


		Timer replaceTimer = new Timer();

		// handle the special case when someone wants to replace air with something else
		if (replace.containsKey("minecraft:air")) {
			Map<Integer, CompoundTag> sectionMap = new HashMap<>();
			List<Integer> heights = new ArrayList<>(26);
			for (CompoundTag section : sections) {
				sectionMap.put((int) section.getByte("Y"), section);
				heights.add((int) section.getByte("Y"));
			}

			for (int y = -4; y < 20; y++) {
				if (!sectionMap.containsKey(y)) {
					sectionMap.put(y, createEmptySection(y));
					heights.add(y);
				} else {
					CompoundTag section = sectionMap.get(y);
					if (!section.containsKey("BlockStates") || !section.containsKey("Palette")) {
						sectionMap.put(y, createEmptySection(y));
					}
				}
			}

			heights.sort(Integer::compareTo);
			sections.clear();

			for (int height : heights) {
				sections.add(sectionMap.get(height));
			}
		}

		ListTag<CompoundTag> tileEntities = catchClassCastException(() -> level.getListTag("TileEntities").asCompoundTagList());
		if (tileEntities == null) {
			tileEntities = new ListTag<>(CompoundTag.class);
		}

		for (CompoundTag section : sections) {
			Timer p = new Timer();
			Tag<?> rawPalette = section.getListTag("Palette");
			if (rawPalette == null || rawPalette.getID() != ListTag.ID) {
				continue;
			}

			ListTag<CompoundTag> palette = catchClassCastException(((ListTag<?>) rawPalette)::asCompoundTagList);
			if (palette == null) {
				continue;
			}

			long[] blockStates = catchClassCastException(() -> section.getLongArray("BlockStates"));
			if (blockStates == null) {
				continue;
			}

			int y = section.getByte("Y");

//			if (y != 3) {
//				continue;
//			}

			long getBlockTime = 0;
			long setBlockTime = 0;
			long tileTime = 0;
			long cleanupTime = 0;

			for (int i = 0; i < 4096; i++) {
				Timer getBlockTimer = new Timer();
				CompoundTag blockState = getBlockAt(i, blockStates, palette);
				getBlockTime += getBlockTimer.getNano();

				BlockReplaceData replacement = replace.get(blockState.getString("Name"));
				if (replacement != null) {
					Timer setBlockTimer = new Timer();
					try {
						blockStates = setBlockAt(i, replacement.getState(), blockStates, palette);
					} catch (Exception ex) {
						System.out.println("failed at y = " + y);
						throw ex;
					}
					setBlockTime += setBlockTimer.getNano();

					Point3i location = indexToLocation(i).add(pos.getX(), y * 16, pos.getZ());

					if (replacement.getTile() != null) {
						CompoundTag tile = replacement.getTile().clone();
						tile.putInt("x", location.getX());
						tile.putInt("y", location.getY());
						tile.putInt("z", location.getZ());
						tileEntities.add(tile);
					} else if (tileEntities.size() != 0) {
						Timer tileTimer = new Timer();
						for (int t = 0; t < tileEntities.size(); t++) {
							CompoundTag tile = tileEntities.get(t);
							if (tile.getInt("x") == location.getX()
									&& tile.getInt("y") == location.getY()
									&& tile.getInt("z") == location.getZ()) {
								tileEntities.remove(t);
								break;
							}
						}
						tileTime += tileTimer.getNano();
					}
				}
			}

			Timer cleanupTimer = new Timer();
			blockStates = cleanupPalette(blockStates, palette);
			cleanupTime += cleanupTimer.getNano();

			section.putLongArray("BlockStates", blockStates);

//			System.out.printf("took %s to replace blocks in section %d\n", p, y);
//			System.out.printf("took %s to get blocks\n", Timer.formatNano(getBlockTime));
//			System.out.printf("took %s to set blocks\n", Timer.formatNano(setBlockTime));
//			System.out.printf("took %s to set tiles\n", Timer.formatNano(tileTime));
//			System.out.printf("took %s to cleanup\n", Timer.formatNano(cleanupTime));

		}

		level.put("TileEntities", tileEntities);

//		System.out.printf("took %s to replace blocks in %s\n", replaceTimer, pos);
	}

	private Point3i indexToLocation(int i) {
		int x = i % 16;
		int z = (i - x) / 16 % 16;
		int y = (i - z * 16 - x) / 256;
		return new Point3i(x, y, z);
	}

	// returns the block state at the given index
	private CompoundTag getBlockAt(int index, long[] blockStates, ListTag<CompoundTag> palette) {
		return palette.get(getPaletteIndex(index, blockStates));
	}

	// sets a new block state at the given index.
	// if the length of blockStates changes, a new blockStates array is returned, otherwise blockStates is returned.
	private long[] setBlockAt(int index, CompoundTag blockState, long[] blockStates, ListTag<CompoundTag> palette) {
		// search palette for block and add it if necessary
		int paletteIndex = -1;
		for (int i = 0; i < palette.size(); i++) {
			if (palette.get(i).equals(blockState)) {
				paletteIndex = i;
				break;
			}
		}

		if (paletteIndex == -1) {
			palette.add(blockState);
			paletteIndex = palette.size() - 1;

			// test if we wil have to increase the blockStates array

			if ((paletteIndex & (paletteIndex - 1)) == 0) {
				blockStates = adjustBlockStateBits(palette, blockStates, null);
			}
		}

		setPaletteIndex(index, paletteIndex, blockStates);
		return blockStates;
	}

	public static void main(String[] args) {
		Anvil117ChunkFilter a = new Anvil117ChunkFilter();
//		long[] blockStates = new long[342];
//		blockStates[45] = 0b0000_00000_00000_00000_00000_00000_00000_00000_00000_11111_10000_00010_00011L;
//
//		System.out.println(a.getPaletteIndex(543, blockStates));
//
//		a.setPaletteIndex(543, 30, blockStates);
//		System.out.println(TextHelper.longToBinaryString(blockStates[45], 5));

		// -------------------------------------------------------------

//		long[] blockStates = new long[256];
//		blockStates[45] = 0b0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_0000_1111_0000_0000_0000L;
//
//		System.out.println(a.getPaletteIndex(723, blockStates));
//
//		a.setPaletteIndex(723, 14, blockStates);
//		System.out.println(TextHelper.longToBinaryString(blockStates[45], 4));

		// -------------------------------------------------------------

		long[] blockStates = new long[384];
		blockStates[45] = 0b0000_000000_000000_000000_000000_000000_000000_111111_000000_000000_000000L;

		System.out.println(a.getPaletteIndex(453, blockStates));

		a.setPaletteIndex(453, 62, blockStates);
		System.out.println(TextHelper.longToBinaryString(blockStates[45], 6));

	}

	private int getPaletteIndex(int blockIndex, long[] blockStates) {
		int bits = blockStates.length >> 6;
//		System.out.println("bits:             " + bits);

		int indicesPerLong = (int) (64D / bits);
//		System.out.println("indicesPerLong:   " + indicesPerLong);

		int blockStatesIndex = blockIndex / indicesPerLong;
//		System.out.println("blockStatesIndex: " + blockStatesIndex);

		int startBit = (blockIndex % indicesPerLong) * bits;
//		System.out.println("startBit:         " + startBit);

		return (int) Bits.bitRange(blockStates[blockStatesIndex], startBit, startBit + bits);
	}

	private void setPaletteIndex(int blockIndex, int paletteIndex, long[] blockStates) {
		int bits = blockStates.length >> 6;
//		System.out.println("bits:             " + bits);

		int indicesPerLong = (int) (64D / bits);
//		System.out.println("indicesPerLong:   " + indicesPerLong);

		int blockStatesIndex = blockIndex / indicesPerLong;
//		System.out.println("blockStatesIndex: " + blockStatesIndex);

		int startBit = (blockIndex % indicesPerLong) * bits;
//		System.out.println("startBit:         " + startBit);

		blockStates[blockStatesIndex] = Bits.setBits(paletteIndex, blockStates[blockStatesIndex], startBit, startBit + bits);
	}

	private long[] adjustBlockStateBits(ListTag<CompoundTag> palette, long[] blockStates, Map<Integer, Integer> oldToNewMapping) {

		Timer adjustTimer = new Timer();
		int newBits = 32 - Integer.numberOfLeadingZeros(palette.size() - 1);
		newBits = Math.max(newBits, 4);

		long[] newBlockStates;
		if (newBits == blockStates.length / 64) {
			System.out.println("using old blockStates array with length " + blockStates.length);
			newBlockStates = blockStates;
		} else {
			int newLength = (int) Math.ceil(4096D / (Math.floor(64D / newBits)));
			System.out.println("creating new blockStates array with length " + newLength);
			newBlockStates = new long[newLength];
		}

		if (oldToNewMapping != null) {
			for (int i = 0; i < 4096; i++) {
//				try {
					setPaletteIndex(i, oldToNewMapping.get(getPaletteIndex(i, blockStates)), newBlockStates);
//				} catch (Exception ex) {
//					System.out.println("---------------------------------------");
//					System.out.printf("i: %d, paletteIndex: %d\n", i, getPaletteIndex(i, blockStates));
//					System.out.println(oldToNewMapping);
//					try {
//						System.out.println(SNBTUtil.toSNBT(palette));
//					} catch (IOException e) {
//						e.printStackTrace();
//					}
//					throw ex;
//				}
			}
		} else {
			for (int i = 0; i < 4096; i++) {
				setPaletteIndex(i, getPaletteIndex(i, blockStates), newBlockStates);
			}
		}

//		System.out.printf("took %s to adjust block state bits\n", adjustTimer);
		return newBlockStates;
	}

	private long[] cleanupPalette(long[] blockStates, ListTag<CompoundTag> palette) {
		// create mapping of old --> new indices

//		printBlockStates(blockStates);

		Timer getIndicesTimer = new Timer();
		Map<Integer, Integer> allIndices = new HashMap<>(palette.size());
		for (int i = 0; i < 4096; i++) {
			int paletteIndex = getPaletteIndex(i, blockStates);
			allIndices.put(paletteIndex, paletteIndex);
		}
//		System.out.println("allIndices: " + allIndices);
//		System.out.printf("took %s to get all indices\n", getIndicesTimer);

		// remove unused indices from palette
		Timer removeIndicesTimer = new Timer();
		int oldIndex = 0;
		for (int i = 0; i < palette.size(); i++) {
			if (!allIndices.containsKey(oldIndex)) {
				palette.remove(i);
				i--;
			} else {
				allIndices.put(oldIndex, i);
			}
			oldIndex++;
		}
//		System.out.println("allIndices2: " + allIndices);
//		System.out.printf("took %s to remove all unused indices\n", removeIndicesTimer);

		// add air to the palette if it doesn't contain air
		Timer airTimer = new Timer();
		if (!paletteContainsAir(palette)) {
			CompoundTag air = new CompoundTag();
			air.putString("Name", "minecraft:air");
			palette.add(air);
		}
//		System.out.printf("took %s to add air\n", airTimer);

//		try {
			return adjustBlockStateBits(palette, blockStates, allIndices);
//		} catch (Exception ex) {
//			printBlockStates(blockStates);
//			throw ex;
//		}
	}

	private boolean paletteContainsAir(ListTag<CompoundTag> palette) {
		for (int i = 0; i < palette.size(); i++) {
			if (palette.get(i).getString("Name").equals("minecraft:air")) {
				return true;
			}
		}
		return false;
	}

	private void printBlockStates(long[] blockStates) {
		System.out.print("[");
		for (int i = 0; i < 4096; i++) {
			System.out.print((i == 0 ? "" : ", ") + getPaletteIndex(i, blockStates));
		}
		System.out.println("]");
	}
}
