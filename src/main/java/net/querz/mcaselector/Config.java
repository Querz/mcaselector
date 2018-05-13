package net.querz.mcaselector;

import java.io.File;

public class Config {

	private static File worldDir = new File(System.getProperty("user.dir"));
	private static File baseCacheDir = new File(System.getProperty("user.dir") + "/cache");
	private static File cacheDir = new File(baseCacheDir, "unknown");

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
}
