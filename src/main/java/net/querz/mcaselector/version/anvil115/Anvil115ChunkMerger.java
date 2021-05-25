package net.querz.mcaselector.version.anvil115;

import net.querz.mcaselector.range.Range;
import net.querz.mcaselector.version.ChunkMerger;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.ListTag;
import net.querz.nbt.tag.Tag;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import static net.querz.mcaselector.validation.ValidationHelper.catchClassCastException;
import static net.querz.mcaselector.validation.ValidationHelper.withDefault;

public class Anvil115ChunkMerger implements ChunkMerger {

	@Override
	public void mergeChunks(CompoundTag source, CompoundTag destination, List<Range> ranges) {
		mergeCompoundTagLists(source, destination, ranges, "Sections", c -> (int) c.getByte("Y"));
		mergeCompoundTagLists(source, destination, ranges, "Entities", c -> c.getListTag("Pos").asDoubleTagList().get(1).asInt() >> 4);
		mergeCompoundTagLists(source, destination, ranges, "TileEntities", c -> c.getInt("y") >> 4);
		mergeCompoundTagLists(source, destination, ranges, "TileTicks", c -> c.getInt("y") >> 4);
		mergeCompoundTagLists(source, destination, ranges, "LiquidTicks", c -> c.getInt("y") >> 4);
		mergeListTagLists(source, destination, ranges, "Lights");
		mergeListTagLists(source, destination, ranges, "LiquidsToBeTicked");
		mergeListTagLists(source, destination, ranges, "ToBeTicked");
		mergeListTagLists(source, destination, ranges, "PostProcessing");
		mergeStructures(source, destination, ranges);

		// we need to fix entity UUIDs, because Minecraft doesn't like duplicates
		fixEntityUUIDs(destination);

		mergeBiomes(source, destination, ranges);
	}

	private void fixEntityUUIDs(CompoundTag root) {
		ListTag<CompoundTag> entities = withDefault(() -> root.getCompoundTag("Level").getListTag("Entities").asCompoundTagList(), null);
		if (entities == null) {
			return;
		}
		entities.forEach(this::fixEntityUUID);
	}

	private void fixEntityUUID(CompoundTag entity) {
		if (entity.containsKey("UUID")) {
			int[] uuid = entity.getIntArray("UUID");
			if (uuid.length == 4) {
				for (int i = 0; i < 4; i++) {
					uuid[i] = random.nextInt();
				}
			}
		}
		if (entity.containsKey("Passengers")) {
			ListTag<CompoundTag> passengers = withDefault(() -> entity.getListTag("Passengers").asCompoundTagList(), null);
			if (passengers != null) {
				passengers.forEach(this::fixEntityUUID);
			}
		}
	}

	protected void mergeBiomes(CompoundTag source, CompoundTag destination, List<Range> ranges) {
		int[] sourceBiomes = withDefault(() -> source.getCompoundTag("Level").getIntArray("Biomes"), null);
		int[] destinationBiomes = withDefault(() -> destination.getCompoundTag("Level").getIntArray("Biomes"), null);

		if (destinationBiomes == null) {
			// if there is no destination, we will let minecraft set the biome
			destinationBiomes = new int[1024];
			Arrays.fill(destinationBiomes, -1);
		}

		if (sourceBiomes == null) {
			// if there is no source biome, we set the biome to -1
			// merge biomes
			for (Range range : ranges) {
				int m = Math.min(range.getTo(), 15);
				for (int i = Math.max(range.getFrom(), 0); i <= m; i++) {
					setSectionBiomes(-1, destinationBiomes, i);
				}
			}
		} else {
			for (Range range : ranges) {
				int m = Math.min(range.getTo(), 15);
				for (int i = Math.max(range.getFrom(), 0); i <= m; i++) {
					copySectionBiomes(sourceBiomes, destinationBiomes, i);
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

	private void mergeStructures(CompoundTag source, CompoundTag destination, List<Range> ranges) {
		CompoundTag sourceStarts = withDefault(() -> source.getCompoundTag("Level").getCompoundTag("Structures").getCompoundTag("Starts"), new CompoundTag());
		CompoundTag destinationStarts = withDefault(() -> destination.getCompoundTag("Level").getCompoundTag("Structures").getCompoundTag("Starts"), new CompoundTag());

		if (destinationStarts != null && destinationStarts.size() != 0) {
			// remove BBs from destination
			for (Map.Entry<String, Tag<?>> start : destinationStarts) {
				ListTag<CompoundTag> children = withDefault(() -> ((CompoundTag) start.getValue()).getListTag("Children").asCompoundTagList(), null);
				if (children != null) {
					child: for (int i = 0; i < children.size(); i++) {
						CompoundTag child = children.get(i);
						int[] bb = catchClassCastException(() -> child.getIntArray("BB"));
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
					int[] bb = catchClassCastException(() -> ((CompoundTag) start.getValue()).getIntArray("BB"));
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

		if (sourceStarts != null) {
			// add BBs from source to destination
			// if child BB doesn't exist in destination, we copy start over to destination
			for (Map.Entry<String, Tag<?>> start : sourceStarts) {
				ListTag<CompoundTag> children = withDefault(() -> ((CompoundTag) start.getValue()).getListTag("Children").asCompoundTagList(), null);
				if (children != null) {
					child:
					for (int i = 0; i < children.size(); i++) {
						CompoundTag child = children.get(i);
						int[] bb = catchClassCastException(() -> child.getIntArray("BB"));
						if (bb == null) {
							continue;
						}
						for (Range range : ranges) {
							if (range.contains(bb[1] >> 4) || range.contains(bb[4] >> 4)) {
								CompoundTag destinationStart = (CompoundTag) destinationStarts.get(start.getKey());
								if (destinationStart == null || "INVALID".equals(destinationStart.getString("id"))) {
									destinationStart = ((CompoundTag) start.getValue()).clone();
									// we need to remove the children, we don't want all of them
									final CompoundTag finalDestinationStart = destinationStart;
									ListTag<CompoundTag> clonedDestinationChildren = withDefault(() -> finalDestinationStart.getListTag("Children").asCompoundTagList(), null);
									if (clonedDestinationChildren != null) {
										clonedDestinationChildren.clear();
									}
									destinationStarts.put(start.getKey(), destinationStart);
								}

								ListTag<CompoundTag> destinationChildren = withDefault(() -> ((CompoundTag) destinationStarts.get(start.getKey())).getListTag("Children").asCompoundTagList(), null);
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

	private void mergeListTagLists(CompoundTag source, CompoundTag destination, List<Range> ranges, String name) {
		ListTag<ListTag<?>> def = new ListTag<>(ListTag.class);
		ListTag<ListTag<?>> sourceList = withDefault(() -> source.getCompoundTag("Level").getListTag(name).asListTagList(), def);
		ListTag<ListTag<?>> destinationList = withDefault(() -> destination.getCompoundTag("Level").getListTag(name).asListTagList(), sourceList);

		if (sourceList.size() != destinationList.size()) {
			return;
		}

		for (Range range : ranges) {
			int m = Math.min(range.getTo(), sourceList.size() - 1);
			for (int i = Math.max(range.getFrom(), 0); i <= m; i++) {
				destinationList.set(i, sourceList.get(i));
			}
		}

		initLevel(destination).put(name, destinationList);
	}

	private void mergeCompoundTagLists(CompoundTag source, CompoundTag destination, List<Range> ranges, String name, Function<CompoundTag, Integer> ySupplier) {
		ListTag<CompoundTag> sourceElements = withDefault(() -> source.getCompoundTag("Level").getListTag(name).asCompoundTagList(), new ListTag<>(CompoundTag.class));
		ListTag<CompoundTag> destinationElements = withDefault(() -> destination.getCompoundTag("Level").getListTag(name).asCompoundTagList(), new ListTag<>(CompoundTag.class));

		initLevel(destination).put(name, mergeLists(sourceElements, destinationElements, ranges, ySupplier));
	}

	private CompoundTag initLevel(CompoundTag c) {
		CompoundTag level = c.getCompoundTag("Level");
		if (level == null) {
			c.put("Level", level = new CompoundTag());
		}
		return level;
	}
}
