package net.querz.mcaselector.math;

public final class Bits {

	// sets bits "from" to "to" in "dest" to the first "to-from" bits from "src"
	public static long setBits(long src, long dest, int from, int to) {
		return (to > 63 ? 0 : dest >>> to << to) + (src << from) + (dest & ((1L << from) - 1L));
	}

	public static long bitRange(long value, int from, int to) {
		int waste = 64 - to;
		return (value << waste) >>> (waste + from);
	}

	// a shortened way to find the number of leading zeroes of an int that uses max 8 of its LSBs
	public static int fastNumberOfLeadingZeroes(int i) {
		int n = 25;
		i <<= 24;
		if (i >>> 28 == 0) {
			n += 4;
			i <<= 4;
		}
		if (i >>> 30 == 0) {
			n += 2;
			i <<= 2;
		}
		n -= i >>> 31;
		return n;
	}
}
