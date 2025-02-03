package net.querz.mcaselector.version.java_1_14;

import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.version.Helper;
import net.querz.mcaselector.version.MCVersionImplementation;
import net.querz.mcaselector.version.mapping.generator.HeightmapConfig;

public class ChunkFilter_18w45a {

	@MCVersionImplementation(1908)
	public static class Heightmap extends ChunkFilter_18w43a.Heightmap {

		@Override
		public void worldSurface(ChunkData data) {
			setHeightMap(Helper.getRegion(data), HeightmapConfig.WORLD_SURFACE, getHeightMap(Helper.getRegion(data), b -> {
				String name = Helper.stringFromCompound(b, "Name");
				return name != null && cfg.worldSurface.contains(name);
			}));
		}
	}
}
