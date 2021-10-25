package net.querz.mcaselector.version.anvil117;

import net.querz.mcaselector.range.Range;
import net.querz.mcaselector.version.ChunkMerger;
import net.querz.nbt.tag.CompoundTag;
import java.util.List;

public class Anvil117EntityMerger implements ChunkMerger {

	@Override
	public void mergeChunks(CompoundTag source, CompoundTag destination, List<Range> ranges, int yOffset) {
		mergeCompoundTagLists(source, destination, ranges, yOffset, "Entities", c -> c.getListTag("Pos").asDoubleTagList().get(1).asInt() >> 4);
	}
}
