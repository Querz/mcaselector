package net.querz.mcaselector;

import javafx.scene.paint.Color;
import java.io.File;
import java.util.UUID;

public final class Config {

	private static File worldDir = null;
	private static File baseCacheDir = new File(System.getProperty("user.dir") + "/cache");
	private static File cacheDir = null;

	private static Color regionSelectionColor = new Color(1, 0, 0, 0.8);
	private static Color chunkSelectionColor = new Color(1, 0.45, 0, 0.8);
	private static int loadThreads = 1;
	private static int processThreads = Runtime.getRuntime().availableProcessors();
	private static int writeThreads = 4;
	private static int maxLoadedFiles = processThreads + (processThreads / 2);

	private static boolean debug = false;

	private Config() {}

	public static void importArgs(String[] args) {
		for (String arg : args) {
			switch (arg.toLowerCase()) {
			case "-debug":
				setDebug(true);
			}
		}
	}

	public static File getWorldDir() {
		return worldDir;
	}

	public static void setWorldDir(File worldDir) {
		Config.worldDir = worldDir;
		UUID uuid = UUID.nameUUIDFromBytes(worldDir.getAbsolutePath().getBytes());
		cacheDir = new File(baseCacheDir, uuid.toString().replace("-", ""));
	}

	public static File getCacheDir() {
		return cacheDir;
	}

	public static void setDebug(boolean debug) {
		Config.debug = debug;
	}

	public static boolean debug() {
		return Config.debug;
	}

	public static Color getRegionSelectionColor() {
		return regionSelectionColor;
	}

	public static void setRegionSelectionColor(Color regionSelectionColor) {
		Config.regionSelectionColor = regionSelectionColor;
	}

	public static Color getChunkSelectionColor() {
		return chunkSelectionColor;
	}

	public static void setChunkSelectionColor(Color chunkSelectionColor) {
		Config.chunkSelectionColor = chunkSelectionColor;
	}

	public static int getLoadThreads() {
		return loadThreads;
	}

	public static void setLoadThreads(int loadThreads) {
		Config.loadThreads = loadThreads;
	}

	public static int getProcessThreads() {
		return processThreads;
	}

	public static void setProcessThreads(int processThreads) {
		Config.processThreads = processThreads;
	}

	public static int getWriteThreads() {
		return writeThreads;
	}

	public static void setWriteThreads(int writeThreads) {
		Config.writeThreads = writeThreads;
	}

	public static int getMaxLoadedFiles() {
		return maxLoadedFiles;
	}

	public static void setMaxLoadedFiles(int maxLoadedFiles) {
		Config.maxLoadedFiles = maxLoadedFiles;
	}
}
