package net.querz.mcaselector.version.anvil114;

import net.querz.mcaselector.range.Range;
import net.querz.mcaselector.version.ChunkMerger;
import net.querz.nbt.tag.CompoundTag;
import java.util.List;

public class Anvil114PoiMerger implements ChunkMerger {

	@Override
	public void mergeChunks(CompoundTag source, CompoundTag destination, List<Range> ranges, int yOffset) {
		mergeCompoundTags(source, destination, ranges, yOffset, "Sections");
	}
}
