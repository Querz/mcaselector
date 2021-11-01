package net.querz.mcaselector.version.anvil117;

import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.range.Range;
import net.querz.mcaselector.version.ChunkMerger;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.ListTag;

import java.util.List;

public class Anvil117EntityMerger implements ChunkMerger {

	@Override
	public void mergeChunks(CompoundTag source, CompoundTag destination, List<Range> ranges, int yOffset) {
		mergeCompoundTagLists(source, destination, ranges, yOffset, "Entities", c -> c.getListTag("Pos").asDoubleTagList().get(1).asInt() >> 4);
	}

	@Override
	public CompoundTag newEmptyChunk(Point2i absoluteLocation, int dataVersion) {
		CompoundTag root = new CompoundTag();
		int[] position = new int[]{
			absoluteLocation.getX(),
			absoluteLocation.getZ()
		};
		root.putIntArray("Position", position);
		root.putInt("DataVersion", dataVersion);
		root.put("Entities", new ListTag<>(CompoundTag.class));
		return root;
	}
}
