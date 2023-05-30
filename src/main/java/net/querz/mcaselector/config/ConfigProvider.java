package net.querz.mcaselector.config;

import net.querz.mcaselector.io.WorldDirectories;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.File;
import java.util.List;

public final class ConfigProvider {

	public static GlobalConfig GLOBAL = new GlobalConfig();
	public static WorldConfig WORLD = new WorldConfig();
	public static OverlayConfig OVERLAY = new OverlayConfig();

	private static final Logger LOGGER = LogManager.getLogger(Config.class);

	private ConfigProvider() {}

	public static void loadGlobalConfig() {
		GLOBAL = GlobalConfig.load();
		LOGGER.debug("loaded global config: {}", GLOBAL);
	}

	public static void loadWorldConfig(WorldDirectories worldDirectories, List<File> dimensionDirectories) {
		WORLD = WorldConfig.load(worldDirectories, dimensionDirectories);
		LOGGER.debug("loaded world config: {}", WORLD);
	}

	public static void loadOverlayConfig() {
		OVERLAY = OverlayConfig.load();
		LOGGER.debug("loaded overlay config: {}", OVERLAY);
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
