package net.querz.mcaselector.version.java_1_13;

import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.version.Helper;
import net.querz.mcaselector.version.MCVersionImplementation;
import net.querz.mcaselector.version.mapping.generator.HeightmapConfig;

public class ChunkFilter_18w19a {

	@MCVersionImplementation(1484)
	public static class Heightmap extends ChunkFilter_18w06a.Heightmap {

		protected HeightmapConfig cfg;

		@Override
		protected void loadCfg() {
			cfg = FileHelper.loadFromResource("mapping/java_1_13/heightmaps_18w19a.json", HeightmapConfig::load);
		}

		@Override
		public void worldSurface(ChunkData data) {
			setHeightMap(Helper.getRegion(data), HeightmapConfig.LIGHT_BLOCKING, getHeightMap(Helper.getRegion(data), b -> {
				String name = Helper.stringFromCompound(b, "Name");
				return name != null && cfg.lightBlocking.contains(name);
			}));
		}

		@Override
		public void oceanFloor(ChunkData data) {
			setHeightMap(Helper.getRegion(data), HeightmapConfig.OCEAN_FLOOR, getHeightMap(Helper.getRegion(data), b -> {
				String name = Helper.stringFromCompound(b, "Name");
				return name != null && cfg.oceanFloor.contains(name);
			}));
		}

		@Override
		public void motionBlocking(ChunkData data) {
			setHeightMap(Helper.getRegion(data), HeightmapConfig.MOTION_BLOCKING, getHeightMap(Helper.getRegion(data), b -> {
				String name = Helper.stringFromCompound(b, "Name");
				return name != null && cfg.motionBlocking.contains(name);
			}));
		}

		@Override
		public void motionBlockingNoLeaves(ChunkData data) {
			setHeightMap(Helper.getRegion(data), HeightmapConfig.MOTION_BLOCKING_NO_LEAVES, getHeightMap(Helper.getRegion(data), b -> {
				String name = Helper.stringFromCompound(b, "Name");
				return name != null && cfg.motionBlocking.contains(name) && !cfg.leaves.contains(name);
			}));
		}
	}
}
