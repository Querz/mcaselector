package net.querz.mcaselector;

import java.io.File;

public class Config {

	private static File worldDir = new File(System.getProperty("user.dir"));
	private static File baseCacheDir = new File(System.getProperty("user.dir") + "/cache");
	private static File cacheDir = new File(baseCacheDir, "unknown");

	private static boolean debug = false;

	public static void importArgs(String[] args) {
		for (int i = 0; i < args.length; i++) {
			switch (args[i].toLowerCase()) {
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
		String worldName = worldDir.getParentFile().getName();
		cacheDir = new File(baseCacheDir, worldName);
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
}
