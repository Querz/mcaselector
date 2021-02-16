package net.querz.mcaselector.math;

import net.querz.mcaselector.text.TextHelper;

public final class Bits {

	// sets bits "from" to "to" in "dest" to the first "to-from" bits from "src"
	public static long setBits(long src, long dest, int from, int to) {
		return (to > 63 ? 0 : dest >>> to << to) + (src << from) + (dest & ((1L << from) - 1L));
	}

	public static long setBits2(long src, long dest, int from, int to) {
		System.out.println("dest:        " + TextHelper.longToBinaryString(dest, 4));

		long destCleared = to > 63 ? 0 : dest >>> to << to;
		System.out.println("destCleared: " + TextHelper.longToBinaryString(destCleared, 4));

		long srcShifted = src << from;
		System.out.println("srcShifted:  " + TextHelper.longToBinaryString(srcShifted, 4));

		long destWithSrc = destCleared + srcShifted;
		System.out.println("destWithSrc: " + TextHelper.longToBinaryString(destWithSrc, 4));

		long destLSB = dest & ((1L << from) - 1L);
		System.out.println("destLSB:     " + TextHelper.longToBinaryString(destLSB, 4));

		long result = destWithSrc + destLSB;
		System.out.println("result:      " + TextHelper.longToBinaryString(result, 4));

		return (to > 63 ? 0 : dest >>> to << to) + (src << from) + (dest & ((1L << from) - 1L));
	}

	public static void main(String[] args) {
		int blockIndex = 4095;
		int blockStatesLength = 256;

		int bits = blockStatesLength >> 6;
		double indicesPerLong = 64D / bits;
		int blockStatesIndex = (int) (blockIndex / indicesPerLong);
		int startBit = (blockIndex % (int) indicesPerLong) * bits;

		System.out.println("len: " +blockStatesLength + ", index: " + blockStatesIndex + ", blockIndex: " + blockIndex + ", bits: " + bits + ", indicesPerLong: " + indicesPerLong + ", startBit: " + startBit);
	}

	public static long bitRange(long value, int from, int to) {
		int waste = 64 - to;
		return (value << waste) >>> (waste + from);
	}
}
