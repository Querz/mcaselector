package net.querz.mcaselector.version.java_26;

import net.querz.mcaselector.version.Helper;
import net.querz.mcaselector.version.MCVersionImplementation;
import net.querz.mcaselector.version.java_1_17.ChunkFilter_20w45a;
import net.querz.nbt.IntArrayTag;

public class ChunkFilter_26_3_snapshot_3 {

	@MCVersionImplementation(5001)
	public static class RelocateEntities extends ChunkFilter_20w45a.RelocateEntities {

		public RelocateEntities() {
			super();
			functions.put("minecraft:cushion", (e, o) -> {
				IntArrayTag blockPos = e.getIntArrayTag("block_pos");
				Helper.applyOffsetToIntArrayPos(blockPos, o);
			});
		}
	}
}
