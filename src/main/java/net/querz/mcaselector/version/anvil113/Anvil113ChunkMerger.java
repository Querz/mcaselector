package net.querz.mcaselector.version.anvil113;

import net.querz.mcaselector.range.Range;
import net.querz.mcaselector.version.ChunkMerger;
import net.querz.mcaselector.version.Helper;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.ListTag;
import net.querz.nbt.tag.Tag;
import java.util.List;
import java.util.Map;

public class Anvil113ChunkMerger implements ChunkMerger {

	@Override
	public void mergeChunks(CompoundTag source, CompoundTag destination, List<Range> ranges, int yOffset) {
		mergeCompoundTagLists(source, destination, ranges, yOffset, "Sections", c -> (int) c.getByte("Y"));
		mergeCompoundTagLists(source, destination, ranges, yOffset, "Entities", c -> c.getListTag("Pos").asDoubleTagList().get(1).asInt() >> 4);
		mergeCompoundTagLists(source, destination, ranges, yOffset, "TileEntities", c -> c.getInt("y") >> 4);
		mergeCompoundTagLists(source, destination, ranges, yOffset, "TileTicks", c -> c.getInt("y") >> 4);
		mergeCompoundTagLists(source, destination, ranges, yOffset, "LiquidTicks", c -> c.getInt("y") >> 4);
		mergeListTagLists(source, destination, ranges, yOffset, "Lights");
		mergeListTagLists(source, destination, ranges, yOffset, "LiquidsToBeTicked");
		mergeListTagLists(source, destination, ranges, yOffset, "ToBeTicked");
		mergeListTagLists(source, destination, ranges, yOffset, "PostProcessing");
		mergeStructures(source, destination, ranges, yOffset);

		// we need to fix entity UUIDs, because Minecraft doesn't like duplicates
		fixEntityUUIDs(destination);
	}

	private void mergeStructures(CompoundTag source, CompoundTag destination, List<Range> ranges, int yOffset) {
		CompoundTag sourceStarts = Helper.tagFromCompound(Helper.tagFromLevelFromRoot(source, "Structures", new CompoundTag()), "Starts", new CompoundTag());
		CompoundTag destinationStarts = Helper.tagFromCompound(Helper.tagFromLevelFromRoot(destination, "Structures", new CompoundTag()), "Starts", new CompoundTag());

		if (destinationStarts.size() != 0) {
			// remove BBs from destination
			for (Map.Entry<String, Tag<?>> start : destinationStarts) {
				ListTag<CompoundTag> children = Helper.tagFromCompound(start.getValue(), "Children", null);
				if (children != null) {
					child: for (int i = 0; i < children.size(); i++) {
						CompoundTag child = children.get(i);
						int[] bb = Helper.intArrayFromCompoundTag(child, "BB");
						if (bb != null && bb.length == 6) {
							for (Range range : ranges) {
								if (range.contains(bb[1] >> 4) && range.contains(bb[4] >> 4)) {
									children.remove(i);
									i--;
									continue child;
								}
							}
						}
					}
				}

				// if we removed all children, we check the start BB
				if (children == null || children.size() == 0) {
					int[] bb = Helper.intArrayFromCompoundTag(start.getValue(), "BB");
					if (bb != null && bb.length == 6) {
						for (Range range : ranges) {
							if (range.contains(bb[1] >> 4) && range.contains(bb[4] >> 4)) {
								CompoundTag emptyStart = new CompoundTag();
								emptyStart.putString("id", "INVALID");
								destinationStarts.put(start.getKey(), emptyStart);
								break;
							}
						}
					}
				}
			}
		}

		// add BBs from source to destination
		// if child BB doesn't exist in destination, we copy start over to destination
		for (Map.Entry<String, Tag<?>> start : sourceStarts) {
			ListTag<CompoundTag> children = Helper.tagFromCompound(start.getValue(), "Children", null);
			if (children != null) {
				child:
				for (int i = 0; i < children.size(); i++) {
					CompoundTag child = children.get(i);
					int[] bb = Helper.intArrayFromCompoundTag(child, "BB");
					if (bb == null) {
						continue;
					}
					for (Range range : ranges) {
						if (range.contains(bb[1] >> 4 - yOffset) || range.contains(bb[4] >> 4 - yOffset)) {
							CompoundTag destinationStart = Helper.tagFromCompound(destinationStarts, start.getKey(), null);
							if (destinationStart == null || "INVALID".equals(destinationStart.getString("id"))) {
								destinationStart = ((CompoundTag) start.getValue()).clone();

								// we need to remove the children, we don't want all of them
								ListTag<CompoundTag> clonedDestinationChildren = Helper.tagFromCompound(destinationStart, "Children", null);
								if (clonedDestinationChildren != null) {
									clonedDestinationChildren.clear();
								}
								destinationStarts.put(start.getKey(), destinationStart);
							}

							ListTag<CompoundTag> destinationChildren = Helper.tagFromCompound(destinationStarts.get(start.getKey()), "Children", null);
							if (destinationChildren == null) {
								destinationChildren = new ListTag<>(CompoundTag.class);
								destinationStart.put("Children", destinationChildren);
							}

							destinationChildren.add(children.get(i));
							continue child;
						}
					}
				}
			}
		}
	}
}
