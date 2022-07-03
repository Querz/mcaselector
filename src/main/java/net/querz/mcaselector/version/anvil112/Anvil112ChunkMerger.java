package net.querz.mcaselector.version.anvil112;

import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.range.Range;
import net.querz.mcaselector.version.ChunkMerger;
import net.querz.mcaselector.version.NbtHelper;
import net.querz.nbt.CompoundTag;
import java.util.List;

public class Anvil112ChunkMerger implements ChunkMerger {

	@Override
	public void mergeChunks(CompoundTag source, CompoundTag destination, List<Range> ranges, int yOffset) {
		mergeCompoundTagListsFromLevel(source, destination, ranges, yOffset, "Sections", c -> ((CompoundTag) c).getInt("Y"));
		mergeCompoundTagListsFromLevel(source, destination, ranges, yOffset, "Entities", c -> ((CompoundTag) c).getList("Pos").getInt(1) >> 4);
		mergeCompoundTagListsFromLevel(source, destination, ranges, yOffset, "TileEntities", c -> ((CompoundTag) c).getInt("y") >> 4);
		mergeCompoundTagListsFromLevel(source, destination, ranges, yOffset, "TileTicks", c -> ((CompoundTag) c).getInt("y") >> 4);
		mergeCompoundTagListsFromLevel(source, destination, ranges, yOffset, "LiquidTicks", c -> ((CompoundTag) c).getInt("y") >> 4);
		mergeListTagLists(source, destination, ranges, yOffset, "Lights");
		mergeListTagLists(source, destination, ranges, yOffset, "LiquidsToBeTicked");
		mergeListTagLists(source, destination, ranges, yOffset, "ToBeTicked");
		mergeListTagLists(source, destination, ranges, yOffset, "PostProcessing");

		// we need to fix entity UUIDs, because Minecraft doesn't like duplicates
		fixEntityUUIDs(NbtHelper.levelFromRoot(destination));
	}

	@Override
	public CompoundTag newEmptyChunk(Point2i absoluteLocation, int dataVersion) {
		CompoundTag root = new CompoundTag();
		CompoundTag level = new CompoundTag();
		level.putInt("xPos", absoluteLocation.getX());
		level.putInt("zPos", absoluteLocation.getZ());
		level.putString("Status", "postprocessed");
		root.put("Level", level);
		root.putInt("DataVersion", dataVersion);
		return root;
	}
}
