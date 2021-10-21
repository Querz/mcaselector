package net.querz.mcaselector.version.anvil112;

import net.querz.mcaselector.range.Range;
import net.querz.mcaselector.version.ChunkMerger;
import net.querz.nbt.tag.CompoundTag;
import java.util.List;

public class Anvil112ChunkMerger implements ChunkMerger {

	@Override
	public void mergeChunks(CompoundTag source, CompoundTag destination, List<Range> ranges, int yOffset) {
		mergeCompoundTagLists(source, destination, ranges, yOffset, "Sections", c -> c.getNumber("Y").intValue());
		mergeCompoundTagLists(source, destination, ranges, yOffset, "Entities", c -> c.getListTag("Pos").asDoubleTagList().get(1).asInt() >> 4);
		mergeCompoundTagLists(source, destination, ranges, yOffset, "TileEntities", c -> c.getInt("y") >> 4);
		mergeCompoundTagLists(source, destination, ranges, yOffset, "TileTicks", c -> c.getInt("y") >> 4);
		mergeCompoundTagLists(source, destination, ranges, yOffset, "LiquidTicks", c -> c.getInt("y") >> 4);
		mergeListTagLists(source, destination, ranges, yOffset, "Lights");
		mergeListTagLists(source, destination, ranges, yOffset, "LiquidsToBeTicked");
		mergeListTagLists(source, destination, ranges, yOffset, "ToBeTicked");
		mergeListTagLists(source, destination, ranges, yOffset, "PostProcessing");

		// we need to fix entity UUIDs, because Minecraft doesn't like duplicates
		fixEntityUUIDs(destination);
	}
}
