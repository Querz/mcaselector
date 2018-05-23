package net.querz.mcaselector;

import java.io.File;
import java.util.UUID;

public class Config {

	private static File worldDir = new File(System.getProperty("user.dir"));
	private static File baseCacheDir = new File(System.getProperty("user.dir") + "/cache");
	private static File cacheDir = new File(baseCacheDir, "unknown");

	private static boolean debug = false;

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
}
