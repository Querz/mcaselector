package net.querz.mcaselector.version.java_1_13;

import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.version.Helper;
import net.querz.mcaselector.version.MCVersionImplementation;
import net.querz.nbt.StringTag;

public class ChunkFilter_18w19a {

	@MCVersionImplementation(1484)
	public static class Heightmap extends ChunkFilter_18w06a.Heightmap {

		@Override
		public void worldSurface(ChunkData data) {
			setHeightMap(Helper.getRegion(data), "LIGHT_BLOCKING", getHeightMap(Helper.getRegion(data), blockState -> {
				StringTag name = blockState.getStringTag("Name");
				if (name == null) {
					return false;
				}
				return !nonLightBlockingBlocks.contains(name.getValue());
			}));
		}

		@Override
		public void oceanFloor(ChunkData data) {
			setHeightMap(Helper.getRegion(data), "OCEAN_FLOOR", getHeightMap(Helper.getRegion(data), blockState -> {
				StringTag name = blockState.getStringTag("Name");
				if (name == null) {
					return false;
				}
				return !isAir(name.getValue()) && !isLiquid(name.getValue()) && !isNonMotionBlocking(name.getValue());
			}));
		}

		@Override
		public void motionBlocking(ChunkData data) {
			setHeightMap(Helper.getRegion(data), "MOTION_BLOCKING", getHeightMap(Helper.getRegion(data), blockState -> {
				StringTag name = blockState.getStringTag("Name");
				if (name == null) {
					return false;
				}
				return !isAir(name.getValue()) && !isNonMotionBlocking(name.getValue());
			}));
		}

		@Override
		public void motionBlockingNoLeaves(ChunkData data) {
			setHeightMap(Helper.getRegion(data), "MOTION_BLOCKING_NO_LEAVES", getHeightMap(Helper.getRegion(data), blockState -> {
				StringTag name = blockState.getStringTag("Name");
				if (name == null) {
					return false;
				}
				return !isAir(name.getValue()) && !isNonMotionBlocking(name.getValue()) && !isFoliage(name.getValue());
			}));
		}
	}
}
