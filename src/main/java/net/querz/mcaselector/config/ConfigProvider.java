package net.querz.mcaselector.config;

import net.querz.mcaselector.io.WorldDirectories;
import java.io.File;
import java.util.List;

public final class ConfigProvider {

	public static GlobalConfig GLOBAL = new GlobalConfig();
	public static WorldConfig WORLD = new WorldConfig();
	public static OverlayConfig OVERLAY = new OverlayConfig();

	private ConfigProvider() {}

	public static void loadGlobalConfig() {
		GLOBAL = GlobalConfig.load();
	}

	public static void loadWorldConfig(WorldDirectories worldDirectories, List<File> dimensionDirectories) {
		WORLD = WorldConfig.load(worldDirectories, dimensionDirectories);
	}

	public static void loadOverlayConfig() {
		OVERLAY = OverlayConfig.load();
	}

	public static void saveAll() {
		if (GLOBAL != null) {
			GLOBAL.save();
		}
		if (OVERLAY != null) {
			OVERLAY.save();
		}
		if (WORLD != null && WORLD.getWorldUUID() != null) {
			WORLD.save();
		}
	}
}
