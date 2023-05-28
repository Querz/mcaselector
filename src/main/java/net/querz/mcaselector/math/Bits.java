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

	// returns only the msb of i
	public static int getMsb(int i) {
		i |= i >> 1;
		i |= i >> 2;
		i |= i >> 4;
		i |= i >> 8;
		i |= i >> 16;
		i = (i >> 1) + 1;
		return i;
	}

	private static final byte[] multiplyDeBruijnBitPosition = new byte[]{0, 1, 28, 2, 29, 14, 24, 3, 30, 22, 20, 15, 25, 17, 4, 8, 31, 27, 13, 23, 21, 19, 16, 7, 26, 12, 18, 6, 11, 5, 10, 9};

	// returns the position of the least significant 1 bit; 0 when i is 0.
	public static int lsbPosition(int i) {
		return multiplyDeBruijnBitPosition[((i & -i) * 0x077CB531) >> 27];
	}
}
