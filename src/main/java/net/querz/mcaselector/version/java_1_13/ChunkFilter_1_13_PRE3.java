package net.querz.mcaselector.version.java_1_13;

import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.version.Helper;
import net.querz.mcaselector.version.MCVersionImplementation;
import net.querz.nbt.StringTag;

public class ChunkFilter_1_13_PRE3 {

	@MCVersionImplementation(1503)
	public static class Heightmap extends ChunkFilter_18w19a.Heightmap {

		@Override
		public void worldSurface(ChunkData data) {
			setHeightMap(Helper.getRegion(data), "WORLD_SURFACE", getHeightMap(Helper.getRegion(data), blockState -> {
				StringTag name = blockState.getStringTag("Name");
				if (name == null) {
					return false;
				}
				return !isAir(name.getValue());
			}));

			setHeightMap(Helper.getRegion(data), "LIGHT_BLOCKING", getHeightMap(Helper.getRegion(data), blockState -> {
				StringTag name = blockState.getStringTag("Name");
				if (name == null) {
					return false;
				}
				return !nonLightBlockingBlocks.contains(name.getValue());
			}));
		}
	}
}
