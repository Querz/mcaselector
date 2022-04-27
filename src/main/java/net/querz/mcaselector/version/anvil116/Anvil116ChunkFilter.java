package net.querz.mcaselector.version.anvil116;

import net.querz.mcaselector.math.Bits;
import net.querz.mcaselector.version.anvil115.Anvil115ChunkFilter;
import net.querz.nbt.ListTag;
import java.util.Map;

public class Anvil116ChunkFilter extends Anvil115ChunkFilter {

	@Override
	protected int getPaletteIndex(int blockIndex, long[] blockStates) {
		int bits = blockStates.length >> 6;
		int indicesPerLong = (int) (64D / bits);
		int blockStatesIndex = blockIndex / indicesPerLong;
		int startBit = (blockIndex % indicesPerLong) * bits;
		return (int) Bits.bitRange(blockStates[blockStatesIndex], startBit, startBit + bits);
	}

	@Override
	protected void setPaletteIndex(int blockIndex, int paletteIndex, long[] blockStates) {
		int bits = blockStates.length >> 6;
		int indicesPerLong = (int) (64D / bits);
		int blockStatesIndex = blockIndex / indicesPerLong;
		int startBit = (blockIndex % indicesPerLong) * bits;
		blockStates[blockStatesIndex] = Bits.setBits(paletteIndex, blockStates[blockStatesIndex], startBit, startBit + bits);
	}

	@Override
	protected long[] adjustBlockStateBits(ListTag palette, long[] blockStates, Map<Integer, Integer> oldToNewMapping) {
		int newBits = 32 - Integer.numberOfLeadingZeros(palette.size() - 1);
		newBits = Math.max(newBits, 4);

		long[] newBlockStates;
		if (newBits == blockStates.length / 64) {
			newBlockStates = blockStates;
		} else {
			int newLength = (int) Math.ceil(4096D / (Math.floor(64D / newBits)));
			newBlockStates = new long[newLength];
		}

		if (oldToNewMapping != null) {
			for (int i = 0; i < 4096; i++) {
				setPaletteIndex(i, oldToNewMapping.get(getPaletteIndex(i, blockStates)), newBlockStates);
			}
		} else {
			for (int i = 0; i < 4096; i++) {
				setPaletteIndex(i, getPaletteIndex(i, blockStates), newBlockStates);
			}
		}

		return newBlockStates;
	}
}
