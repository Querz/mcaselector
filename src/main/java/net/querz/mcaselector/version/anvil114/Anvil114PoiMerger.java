package net.querz.mcaselector.version.anvil114;

import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.range.Range;
import net.querz.mcaselector.version.ChunkMerger;
import net.querz.mcaselector.version.NbtHelper;
import net.querz.nbt.CompoundTag;
import java.util.List;

public class Anvil114PoiMerger implements ChunkMerger {

	@Override
	public void mergeChunks(CompoundTag source, CompoundTag destination, List<Range> ranges, int yOffset) {
		NbtHelper.mergeCompoundTags(source, destination, ranges, yOffset, "Sections");
	}

	@Override
	public CompoundTag newEmptyChunk(Point2i absoluteLocation, int dataVersion) {
		CompoundTag root = new CompoundTag();
		root.put("Sections", new CompoundTag());
		root.putInt("DataVersion", dataVersion);
		return root;
	}
}
