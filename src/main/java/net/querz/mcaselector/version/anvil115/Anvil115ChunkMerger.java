package net.querz.mcaselector.version.anvil115;

import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.range.Range;
import net.querz.mcaselector.version.ChunkMerger;
import net.querz.mcaselector.version.NbtHelper;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.IntArrayTag;
import net.querz.nbt.ListTag;
import net.querz.nbt.Tag;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Anvil115ChunkMerger implements ChunkMerger {

	@Override
	public void mergeChunks(CompoundTag source, CompoundTag destination, List<Range> ranges, int yOffset) {
		NbtHelper.mergeCompoundTagListsFromLevel(source, destination, ranges, yOffset, "Sections", c -> ((CompoundTag) c).getInt("Y"));
		NbtHelper.mergeCompoundTagListsFromLevel(source, destination, ranges, yOffset, "Entities", c -> ((CompoundTag) c).getList("Pos").getInt(1) >> 4);
		NbtHelper.mergeCompoundTagListsFromLevel(source, destination, ranges, yOffset, "TileEntities", c -> ((CompoundTag) c).getInt("y") >> 4);
		NbtHelper.mergeCompoundTagListsFromLevel(source, destination, ranges, yOffset, "TileTicks", c -> ((CompoundTag) c).getInt("y") >> 4);
		NbtHelper.mergeCompoundTagListsFromLevel(source, destination, ranges, yOffset, "LiquidTicks", c -> ((CompoundTag) c).getInt("y") >> 4);
		NbtHelper.mergeListTagLists(source, destination, ranges, yOffset, "Lights");
		NbtHelper.mergeListTagLists(source, destination, ranges, yOffset, "LiquidsToBeTicked");
		NbtHelper.mergeListTagLists(source, destination, ranges, yOffset, "ToBeTicked");
		NbtHelper.mergeListTagLists(source, destination, ranges, yOffset, "PostProcessing");
		mergeStructures(source, destination, ranges, yOffset);

		// we need to fix entity UUIDs, because Minecraft doesn't like duplicates
		NbtHelper.fixEntityUUIDsMerger(NbtHelper.levelFromRoot(destination));

		mergeBiomes(source, destination, ranges, yOffset);
	}

	protected void mergeBiomes(CompoundTag source, CompoundTag destination, List<Range> ranges, int yOffset) {
		IntArrayTag sourceBiomes = NbtHelper.tagFromLevelFromRoot(source, "Biomes");
		IntArrayTag destinationBiomes = NbtHelper.tagFromLevelFromRoot(destination, "Biomes");

		if (destinationBiomes == null) {
			// if there is no destination, we will let minecraft set the biome
			destinationBiomes = new IntArrayTag(new int[1024]);
			Arrays.fill(destinationBiomes.getValue(), -1);
		}

		if (sourceBiomes == null) {
			// if there is no source biome, we set the biome to -1
			// merge biomes
			for (Range range : ranges) {
				int m = Math.min(range.getTo() + yOffset, 15);
				for (int i = Math.max(range.getFrom() + yOffset, 0); i <= m; i++) {
					setSectionBiomes(-1, destinationBiomes.getValue(), i);
				}
			}
		} else {
			for (Range range : ranges) {
				int m = Math.min(range.getTo() - yOffset, 15);
				for (int i = Math.max(range.getFrom() - yOffset, 0); i <= m; i++) {
					copySectionBiomes(sourceBiomes.getValue(), destinationBiomes.getValue(), i);
				}
			}
		}
	}

	private void copySectionBiomes(int[] sourceBiomes, int[] destinationBiomes, int sectionY) {
		for (int y = 0; y < 4; y++) {
			int biomeY = sectionY * 4 + y;
			for (int x = 0; x < 4; x++) {
				for (int z = 0; z < 4; z++) {
					setBiomeAt(destinationBiomes, x, biomeY, z, getBiomeAt(sourceBiomes, x, biomeY, z));
				}
			}
		}
	}

	private void setSectionBiomes(int biome, int[] destinationBiomes, int sectionY) {
		for (int y = 0; y < 4; y++) {
			int biomeY = sectionY * 4 + y;
			for (int x = 0; x < 4; x++) {
				for (int z = 0; z < 4; z++) {
					setBiomeAt(destinationBiomes, x, biomeY, z, biome);
				}
			}
		}
	}

	private int getBiomeAt(int[] biomes, int biomeX, int biomeY, int biomeZ) {
		if (biomes == null || biomes.length != 1024) {
			return -1;
		}
		return biomes[getBiomeIndex(biomeX, biomeY, biomeZ)];
	}

	private void setBiomeAt(int[] biomes, int biomeX, int biomeY, int biomeZ, int biomeID) {
		if (biomes == null || biomes.length != 1024) {
			biomes = new int[1024];
			Arrays.fill(biomes, -1);
		}
		biomes[getBiomeIndex(biomeX, biomeY, biomeZ)] = biomeID;
	}

	private int getBiomeIndex(int x, int y, int z) {
		return y * 16 + z * 4 + x;
	}

	private void mergeStructures(CompoundTag source, CompoundTag destination, List<Range> ranges, int yOffset) {
		CompoundTag sourceStarts = NbtHelper.tagFromCompound(NbtHelper.tagFromLevelFromRoot(source, "Structures", new CompoundTag()), "Starts", new CompoundTag());
		CompoundTag destinationStarts = NbtHelper.tagFromCompound(NbtHelper.tagFromLevelFromRoot(destination, "Structures", new CompoundTag()), "Starts", new CompoundTag());

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
					int[] bb = NbtHelper.intArrayFromCompound(start.getValue(), "BB");
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
		CompoundTag level = new CompoundTag();
		level.putInt("xPos", absoluteLocation.getX());
		level.putInt("zPos", absoluteLocation.getZ());
		level.putString("Status", "full");
		root.put("Level", level);
		root.putInt("DataVersion", dataVersion);
		return root;
	}
}
