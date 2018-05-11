package net.querz.mcaselector.util;

public class Helper {
	public static int blockToChunk(int i) {
		return i >> 4;
	}

	public static int blockToRegion(int i) {
		return i >> 9;
	}

	public static int chunkToRegion(int i) {
		return i >> 5;
	}

	public static int chunkToBlock(int i) {
		return i << 4;
	}

	public static int regionToBlock(int i) {
		return i << 9;
	}

	public static int regionToChunk(int i) {
		return i << 5;
	}

	public static Point2i blockToRegion(Point2i i) {
		return i.shiftRight(9);
	}

	public static Point2i regionToBlock(Point2i i) {
		return i.shiftLeft(9);
	}

	public static Point2i blockToChunk(Point2i i) {
		return i.shiftRight(4);
	}

	public static Point2i chunkToBlock(Point2i i) {
		return i.shiftLeft(4);
	}

	public static Point2i chunkToRegion(Point2i i) {
		return i.shiftRight(5);
	}

	public static Point2i regionToChunk(Point2i i) {
		return i.shiftLeft(5);
	}

	public static void runAsync(Runnable r) {
		new Thread(r).start();
	}
}
