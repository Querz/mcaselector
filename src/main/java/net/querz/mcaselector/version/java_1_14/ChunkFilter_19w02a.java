package net.querz.mcaselector.version.java_1_14;

import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.util.point.Point2i;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.Helper;
import net.querz.mcaselector.version.MCVersionImplementation;
import net.querz.mcaselector.version.java_1_13.ChunkFilter_18w16a;
import net.querz.nbt.ByteTag;
import net.querz.nbt.CompoundTag;

public class ChunkFilter_19w02a {

	@MCVersionImplementation(1921)
	public static class LightPopulated implements ChunkFilter.LightPopulated {

		@Override
		public ByteTag getLightPopulated(ChunkData data) {
			return Helper.tagFromLevelFromRoot(Helper.getRegion(data), "isLightOn");
		}

		@Override
		public void setLightPopulated(ChunkData data, byte lightPopulated) {
			CompoundTag level = Helper.levelFromRoot(Helper.getRegion(data));
			if (level != null) {
				level.putLong("isLightOn", lightPopulated);
			}
		}
	}

	@MCVersionImplementation(1466)
	public static class Merge extends ChunkFilter_18w16a.Merge {

		@Override
		public CompoundTag newEmptyChunk(Point2i absoluteLocation, int dataVersion) {
			CompoundTag root = new CompoundTag();
			CompoundTag level = new CompoundTag();
			level.putInt("xPos", absoluteLocation.getX());
			level.putInt("zPos", absoluteLocation.getZ());
			level.putString("Status", "full");
			root.put("Level", level);
			root.putInt("DataVersion", dataVersion);
			return root;
		}
	}
}
