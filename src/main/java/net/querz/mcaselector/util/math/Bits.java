package net.querz.mcaselector.util.math;

public final class Bits {

	// sets bits "from" to "to" in "dest" to the first "to-from" bits from "src"
	public static long setBits(long src, long dest, int from, int to) {
		return (to > 63 ? 0 : dest >>> to << to) + (src << from) + (dest & ((1L << from) - 1L));
	}

	public static long bitRange(long value, int from, int to) {
		int waste = 64 - to;
		return (value << waste) >>> (waste + from);
	}

	// returns the position of the least significant 1 bit; 0 when 'i' is 0.
	public static int lsbPosition(int i) {
		if (i == 0) {
			return 0;
		}
		return Integer.numberOfTrailingZeros(i);
	}

	public static int msbPosition(int i) {
		return 31 - Integer.numberOfLeadingZeros(i);
	}
}
