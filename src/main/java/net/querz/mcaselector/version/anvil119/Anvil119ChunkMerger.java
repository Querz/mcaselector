package net.querz.mcaselector.version.anvil119;

import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.range.Range;
import net.querz.mcaselector.version.ChunkMerger;
import net.querz.mcaselector.version.NbtHelper;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.ListTag;
import net.querz.nbt.Tag;
import java.util.List;
import java.util.Map;

public class Anvil119ChunkMerger implements ChunkMerger {

	@Override
	public void mergeChunks(CompoundTag source, CompoundTag destination, List<Range> ranges, int yOffset) {
		NbtHelper.mergeCompoundTagLists(source, destination, ranges, yOffset, "sections", c -> ((CompoundTag) c).getInt("Y"));
		NbtHelper.mergeCompoundTagLists(source, destination, ranges, yOffset, "block_entities", c -> ((CompoundTag) c).getInt("y") >> 4);
		NbtHelper.mergeCompoundTagLists(source, destination, ranges, yOffset, "block_ticks", c -> ((CompoundTag) c).getInt("y") >> 4);
		NbtHelper.mergeCompoundTagLists(source, destination, ranges, yOffset, "fluid_ticks", c -> ((CompoundTag) c).getInt("y") >> 4);
		NbtHelper.mergeListTagLists(source, destination, ranges, yOffset, "PostProcessing");
		mergeStructures(source, destination, ranges, yOffset);
	}

	private void mergeStructures(CompoundTag source, CompoundTag destination, List<Range> ranges, int yOffset) {
		CompoundTag sourceStarts = NbtHelper.tagFromCompound(NbtHelper.tagFromCompound(source, "structures"), "starts", new CompoundTag());
		CompoundTag destinationStarts = NbtHelper.tagFromCompound(NbtHelper.tagFromCompound(destination, "structures"), "starts", new CompoundTag());

		if (destinationStarts.size() != 0) {
			// remove BBs from destination
			for (Map.Entry<String, Tag> start : destinationStarts) {
				ListTag children = NbtHelper.tagFromCompound(start.getValue(), "Children", null);
				if (children != null) {
					child: for (int i = 0; i < children.size(); i++) {
						CompoundTag child = children.getCompound(i);
						int[] bb = NbtHelper.intArrayFromCompound(child, "BB");
						if (bb != null && bb.length == 6) {
							for (Range range : ranges) {
								if (range.contains(bb[1] >> 4 - yOffset) && range.contains(bb[4] >> 4 - yOffset)) {
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
					int[] bb = NbtHelper.intArrayFromCompound(start.getValue(), "BB");
					if (bb != null && bb.length == 6) {
						for (Range range : ranges) {
							if (range.contains(bb[1] >> 4 - yOffset) && range.contains(bb[4] >> 4 - yOffset)) {
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
		for (Map.Entry<String, Tag> start : sourceStarts) {
			ListTag children = NbtHelper.tagFromCompound(start.getValue(), "Children", null);
			if (children != null) {
				child:
				for (int i = 0; i < children.size(); i++) {
					CompoundTag child = children.getCompound(i);
					int[] bb = NbtHelper.intArrayFromCompound(child, "BB");
					if (bb == null) {
						continue;
					}
					for (Range range : ranges) {
						if (range.contains(bb[1] >> 4 - yOffset) || range.contains(bb[4] >> 4 - yOffset)) {
							CompoundTag destinationStart = NbtHelper.tagFromCompound(destinationStarts, start.getKey(), null);
							if (destinationStart == null || "INVALID".equals(destinationStart.getString("id"))) {
								destinationStart = ((CompoundTag) start.getValue()).copy();

								// we need to remove the children, we don't want all of them
								ListTag clonedDestinationChildren = NbtHelper.tagFromCompound(destinationStart, "Children", null);
								if (clonedDestinationChildren != null) {
									clonedDestinationChildren.clear();
								}
								destinationStarts.put(start.getKey(), destinationStart);
							}

							ListTag destinationChildren = NbtHelper.tagFromCompound(destinationStarts.get(start.getKey()), "Children", null);
							if (destinationChildren == null) {
								destinationChildren = new ListTag();
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

	@Override
	public CompoundTag newEmptyChunk(Point2i absoluteLocation, int dataVersion) {
		CompoundTag root = new CompoundTag();
		root.putInt("xPos", absoluteLocation.getX());
		root.putInt("yPos", -4);
		root.putInt("zPos", absoluteLocation.getZ());
		root.putString("Status", "full");
		root.putInt("DataVersion", dataVersion);
		return root;
	}
}
