package net.querz.mcaselector.version;

import net.querz.nbt.CompoundTag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public interface HeightmapCalculator {

	void worldSurface(CompoundTag root);

	void oceanFloor(CompoundTag root);

	void motionBlocking(CompoundTag root);

	void motionBlockingNoLeaves(CompoundTag root);

	default boolean isNonMotionBlocking(String blockName) {
		return Data.nonMotionBlocking.contains(blockName);
	}

	default boolean isFoliage(String blockName) {
		return Data.foliage.contains(blockName);
	}

	default boolean isAir(String blockName) {
		return Data.air.contains(blockName);
	}

	default boolean isLiquid(String blockName) {
		return Data.liquid.contains(blockName);
	}

	final class Data {
		private static final Set<String> nonMotionBlocking = new HashSet<>();
		private static final Set<String> foliage = new HashSet<>();
		private static final Set<String> air = new HashSet<>();
		private static final Set<String> liquid = new HashSet<>();

		private Data() {}

		private static final Logger LOGGER = LogManager.getLogger(Data.class);

		static {
			try (BufferedReader bis = new BufferedReader(new InputStreamReader(Objects.requireNonNull(Data.class.getClassLoader().getResourceAsStream("mapping/all_heightmap_data.txt"))))) {
				String line;
				while ((line = bis.readLine()) != null) {
					String[] elements = line.split(";");
					if (elements.length != 2) {
						LOGGER.error("invalid line in heightmap data file: \"{}\"", line);
						continue;
					}
					String type = elements[0];
					String blockName = elements[1];
					switch (type) {
						case "m" -> nonMotionBlocking.add("minecraft:" + blockName);
						case "f" -> foliage.add("minecraft:" + blockName);
						case "w" -> liquid.add("minecraft:" + blockName);
						case "a" -> air.add("minecraft:" + blockName);
						default -> LOGGER.error("invalid heightmap data type \"{}\"", type);
					}
				}
			} catch (IOException ex) {
				throw new RuntimeException("failed to read mapping/all_heightmap_data.txt");
			}
		}

	}
}
