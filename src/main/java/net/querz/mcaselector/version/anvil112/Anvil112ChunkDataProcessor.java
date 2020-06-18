package net.querz.mcaselector.version.anvil112;

import net.querz.mcaselector.range.Range;
import net.querz.mcaselector.version.ChunkDataProcessor;
import net.querz.mcaselector.version.ColorMapping;
import net.querz.mcaselector.tiles.Tile;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.ListTag;
import net.querz.nbt.tag.Tag;
import java.util.*;
import java.util.function.Function;
import static net.querz.mcaselector.validation.ValidationHelper.*;

public class Anvil112ChunkDataProcessor implements ChunkDataProcessor {

	@Override
	public void drawChunk(CompoundTag root, ColorMapping colorMapping, int x, int z, int[] pixelBuffer, int[] waterPixels, byte[] terrainHeights, byte[] waterHeights, boolean water) {
		ListTag<CompoundTag> sections = withDefault(() -> root.getCompoundTag("Level").getListTag("Sections").asCompoundTagList(), null);
		if (sections == null) {
			return;
		}
		sections.sort(this::filterSections);

		//loop over x / z
		for (int cx = 0; cx < Tile.CHUNK_SIZE; cx++) {
			zLoop:
			for (int cz = 0; cz < Tile.CHUNK_SIZE; cz++) {

				byte[] biomes = withDefault(() -> root.getCompoundTag("Level").getByteArray("Biomes"), null);
				int biome = -1;
				if (biomes != null && biomes.length != 0) {
					biome = biomes[getBlockIndex(cx, 0, cz)];
				}

				boolean waterDepth = false;
				//loop over sections
				for (int i = 0; i < sections.size(); i++) {
					final int si = i;
					byte[] blocks = withDefault(() -> sections.get(si).getByteArray("Blocks"), null);
					if (blocks == null) {
						continue;
					}
					byte[] data = withDefault(() -> sections.get(si).getByteArray("Data"), null);
					if (data == null) {
						continue;
					}

					Byte height = withDefault(() -> sections.get(si).getByte("Y"), null);
					if (height == null) {
						continue;
					}
					int sectionHeight = height * 16;

					//loop over y value in section from top to bottom
					for (int cy = Tile.CHUNK_SIZE - 1; cy >= 0; cy--) {
						int index = getBlockIndex(cx, cy, cz);
						short block = (short) (blocks[index] & 0xFF);

						//ignore bedrock and netherrack until 75
						if (isIgnoredInNether(biome, block, sectionHeight + cy)) {
							continue;
						}

						byte blockData = (byte) (index % 2 == 0 ? data[index / 2] & 0x0F : (data[index / 2] >> 4) & 0x0F);

						if (!isEmpty(block)) {
							int regionIndex = (z + cz) * Tile.SIZE + (x + cx);
							if (water) {
								if (!waterDepth) {
									pixelBuffer[regionIndex] = colorMapping.getRGB(((block << 4) + blockData)) | 0xFF000000;
									waterHeights[regionIndex] = (byte) (sectionHeight + cy);
								}
								if (isWater(block)) {
									waterDepth = true;
									continue;
								} else {
									waterPixels[regionIndex] = colorMapping.getRGB(((block << 4) + blockData)) | 0xFF000000;
								}
							} else {
								pixelBuffer[regionIndex] = colorMapping.getRGB(((block << 4) + blockData)) | 0xFF000000;
							}
							terrainHeights[regionIndex] = (byte) (sectionHeight + cy);
							continue zLoop;
						}
					}
				}
			}
		}
	}

	private boolean isWater(short block) {
		switch (block) {
		case 8:
		case 9:
			return true;
		}
		return false;
	}

	private boolean isIgnoredInNether(int biome, short block, int height) {
		if (biome == 8) {
			switch (block) {
			case 7:   //bedrock
			case 10:  //flowing_lava
			case 11:  //lava
			case 87:  //netherrack
			case 153: //quartz_ore
				return height > 75;
			}
		}
		return false;
	}

	private boolean isEmpty(int blockID) {
		return blockID == 0 || blockID == 166 || blockID == 217;
	}

	private int getBlockIndex(int x, int y, int z) {
		return y * Tile.CHUNK_SIZE * Tile.CHUNK_SIZE + z * Tile.CHUNK_SIZE + x;
	}

	private int filterSections(CompoundTag sectionA, CompoundTag sectionB) {
		return withDefault(() -> sectionB.getByte("Y"), (byte) -1) - withDefault(() -> sectionA.getByte("Y"), (byte) -1);
	}

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
		// do not merge biomes here, we will overwrite this function in a future version

		// we need to fix entity UUIDs, because Minecraft doesn't like duplicates
		fixEntityUUIDs(destination);
	}

	protected void fixEntityUUIDs(CompoundTag root) {
		ListTag<CompoundTag> entities = withDefault(() -> root.getCompoundTag("Level").getListTag("Entities").asCompoundTagList(), null);
		if (entities == null) {
			return;
		}
		entities.forEach(this::fixEntityUUID);
	}

	protected void fixEntityUUID(CompoundTag entity) {
		if (entity.containsKey("UUIDMost")) {
			entity.putLong("UUIDMost", random.nextLong());
		}
		if (entity.containsKey("UUIDLeast")) {
			entity.putLong("UUIDLeast", random.nextLong());
		}
		if (entity.containsKey("Passengers")) {
			ListTag<CompoundTag> passengers = withDefault(() -> entity.getListTag("Passengers").asCompoundTagList(), null);
			if (passengers != null) {
				passengers.forEach(this::fixEntityUUID);
			}
		}
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
						int[] bb = children.get(i).getIntArray("BB");
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
					int[] bb = ((CompoundTag) start.getValue()).getIntArray("BB");
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
						int[] bb = children.get(i).getIntArray("BB");
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
		ListTag<ListTag<?>> sourceList = withDefault(() -> source.getCompoundTag("Level").getListTag(name).asListTagList(), new ListTag<>(ListTag.class));
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

	private <T extends Tag<?>> ListTag<T> mergeLists(ListTag<T> source, ListTag<T> destination, List<Range> ranges, Function<T, Integer> ySupplier) {
		@SuppressWarnings("unchecked")
		ListTag<T> resultList = (ListTag<T>) ListTag.createUnchecked(null);

		Set<T> resultSet = new HashSet<>();
		for (T dest : destination) {
			resultSet.add(dest);
		}

		elem: for (T destinationElement : destination) {
			for (Range range : ranges) {
				if (range.contains(ySupplier.apply(destinationElement))) {
					resultSet.remove(destinationElement);
					continue elem;
				}
			}
		}

		elem: for (T sourceElement : source) {
			for (Range range : ranges) {
				if (range.contains(ySupplier.apply(sourceElement))) {
					resultSet.add(sourceElement);
					continue elem;
				}
			}
		}

		resultList.addAll(resultSet);
		return resultList;
	}

	private CompoundTag initLevel(CompoundTag c) {
		CompoundTag level = c.getCompoundTag("Level");
		if (level == null) {
			c.put("Level", level = new CompoundTag());
		}
		return level;
	}
}
