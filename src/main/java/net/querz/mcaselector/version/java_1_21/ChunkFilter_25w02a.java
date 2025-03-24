package net.querz.mcaselector.version.java_1_21;

import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.Helper;
import net.querz.mcaselector.version.MCVersionImplementation;
import net.querz.nbt.CompoundTag;

public class ChunkFilter_25w02a {

	@MCVersionImplementation(4317)
	public static class Blending implements ChunkFilter.Blending {

		@Override
		public void forceBlending(ChunkData data) {
			// force blending chunks of the same version no longer works in 25w09a, but we can reset the DataVersion
			// to 1.21.4. New features such as leaf litter will stay in the chunk.
			CompoundTag root = Helper.getRegion(data);
			Helper.setDataVersion(root, 4189);
		}
	}
}
