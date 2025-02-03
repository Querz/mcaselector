package net.querz.mcaselector.version.java_1_13;

import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.version.Helper;
import net.querz.mcaselector.version.MCVersionImplementation;
import net.querz.mcaselector.version.mapping.generator.HeightmapConfig;

public class ChunkFilter_1_13_PRE3 {

	@MCVersionImplementation(1503)
	public static class Heightmap extends ChunkFilter_18w19a.Heightmap {

		@Override
		public void worldSurface(ChunkData data) {
			setHeightMap(Helper.getRegion(data), HeightmapConfig.LIGHT_BLOCKING, getHeightMap(Helper.getRegion(data), b -> {
				String name = Helper.stringFromCompound(b, "Name");
				return name != null && cfg.lightBlocking.contains(name);
			}));

			setHeightMap(Helper.getRegion(data), HeightmapConfig.WORLD_SURFACE, getHeightMap(Helper.getRegion(data), b -> {
				String name = Helper.stringFromCompound(b, "Name");
				return name != null && cfg.worldSurface.contains(name);
			}));
		}
	}
}
