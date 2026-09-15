package net.querz.mcaselector.version.java_26;

import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.util.point.Point2i;
import net.querz.mcaselector.util.point.Point3i;
import net.querz.mcaselector.util.range.Range;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.Helper;
import net.querz.mcaselector.version.MCVersionImplementation;
import net.querz.mcaselector.version.java_1_18.ChunkFilter_21w37a;
import net.querz.mcaselector.version.mapping.generator.HeightmapConfig;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.ListTag;
import net.querz.nbt.StringTag;
import net.querz.nbt.Tag;

import java.util.*;
import java.util.function.Predicate;

public class ChunkFilter_26_3_snapshot_7 {

	@MCVersionImplementation(5009)
	public static class Blocks extends ChunkFilter_21w37a.Blocks {

		@Override
		public boolean matchBlockNames(ChunkData data, Collection<String> names) {
			ListTag sections = Helper.tagFromCompound(Helper.getRegion(data), "sections");
			if (sections == null) {
				return false;
			}

			int c = 0;
			nameLoop:
			for (String name : names) {
				for (CompoundTag t : sections.iterateType(CompoundTag.class)) {
					ListTag palette = Helper.tagFromCompound(Helper.tagFromCompound(t, "block_states"), "palette");
					if (palette == null) {
						continue;
					}
					for (CompoundTag p : palette.iterateType(CompoundTag.class)) {
						if (name.equals(Helper.getBlockID(p, null))) {
							c++;
							continue nameLoop;
						}
					}
				}
			}
			return names.size() == c;
		}

		@Override
		public boolean matchAnyBlockName(ChunkData data, Collection<String> names) {
			ListTag sections = Helper.tagFromCompound(Helper.getRegion(data), "sections");
			if (sections == null) {
				return false;
			}

			for (String name : names) {
				for (CompoundTag t : sections.iterateType(CompoundTag.class)) {
					ListTag palette = Helper.tagFromCompound(Helper.tagFromCompound(t, "block_states"), "palette");
					if (palette == null) {
						continue;
					}
					for (CompoundTag p : palette.iterateType(CompoundTag.class)) {
						if (name.equals(Helper.getBlockID(p, null))) {
							return true;
						}
					}
				}
			}
			return false;
		}

		@Override
		public void replaceBlocks(ChunkData data, Map<String, ChunkFilter.BlockReplaceData> replace) {
			ListTag sections = Helper.tagFromCompound(Helper.getRegion(data), "sections");
			if (sections == null) {
				return;
			}

			Point2i pos = Helper.point2iFromCompound(Helper.getRegion(data), "xPos", "zPos");
			if (pos == null) {
				return;
			}
			pos = pos.chunkToBlock();

			int dataVersion = Helper.getDataVersion(Helper.getRegion(data));

			Range sectionRange = Helper.findSectionRange(Helper.getRegion(data), sections);

			// handle the special case when someone wants to replace air with something else
			if (replace.containsKey("minecraft:air")) {
				Map<Integer, CompoundTag> sectionMap = new HashMap<>();
				List<Integer> heights = new ArrayList<>(sectionRange.num());
				for (CompoundTag section : sections.iterateType(CompoundTag.class)) {
					sectionMap.put(section.getInt("Y"), section);
					heights.add(section.getInt("Y"));
				}

				for (int y = sectionRange.getFrom(); y <= sectionRange.getTo(); y++) {
					if (!sectionMap.containsKey(y)) {
						sectionMap.put(y, completeSection(new CompoundTag(), y));
						heights.add(y);
					} else {
						CompoundTag section = sectionMap.get(y);
						if (!section.containsKey("block_states")) {
							completeSection(sectionMap.get(y), y);
						}
					}
				}

				heights.sort(Integer::compareTo);
				sections.clear();

				for (int height : heights) {
					sections.add(sectionMap.get(height));
				}
			}

			ListTag tileEntities = Helper.tagFromCompound(Helper.getRegion(data), "block_entities");
			if (tileEntities == null) {
				tileEntities = new ListTag();
			}

			for (CompoundTag section : sections.iterateType(CompoundTag.class)) {
				CompoundTag blockStatesTag = section.getCompoundTag("block_states");
				if(blockStatesTag == null) continue;

				ListTag palette = Helper.tagFromCompound(blockStatesTag, "palette");
				long[] blockStates = Helper.longArrayFromCompound(blockStatesTag, "data");
				if (palette == null) {
					continue;
				}

				decompressPalette(palette);

				if (palette.size() == 1 && blockStates == null) {
					blockStates = new long[256];
				}

				int y = Helper.numberFromCompound(section, "Y", sectionRange.getFrom() - 1).intValue();
				if (!sectionRange.contains(y)) {
					continue;
				}

				for (int i = 0; i < 4096; i++) {
					Tag blockState = getBlockTagAt(i, blockStates, palette);

					for (Map.Entry<String, ChunkFilter.BlockReplaceData> entry : replace.entrySet()) {
						if (!Helper.getBlockID(blockState, "").matches(entry.getKey())) {
							continue;
						}
						ChunkFilter.BlockReplaceData replacement = entry.getValue();

						try {
							blockStates = setBlockAt(i, replacement.getState(dataVersion), blockStates, palette);
						} catch (Exception ex) {
							throw new RuntimeException("failed to set block in section " + y, ex);
						}

						Point3i location = indexToLocation(i).add(pos.getX(), y * 16, pos.getZ());

						if (replacement.getTile() != null) {
							CompoundTag tile = replacement.getTile().copy();
							tile.putInt("x", location.getX());
							tile.putInt("y", location.getY());
							tile.putInt("z", location.getZ());
							tileEntities.add(tile);
						} else if (!tileEntities.isEmpty()) {
							for (int t = 0; t < tileEntities.size(); t++) {
								CompoundTag tile = tileEntities.getCompound(t);
								if (tile.getInt("x") == location.getX()
									&& tile.getInt("y") == location.getY()
									&& tile.getInt("z") == location.getZ()) {
									tileEntities.remove(t);
									break;
								}
							}
						}
					}
				}

				try {
					blockStates = cleanupPalette(blockStates, palette);
					compressPalette(palette);
				} catch (Exception ex) {
					throw new RuntimeException("failed to cleanup section " + y, ex);
				}

				if (blockStates == null) {
					blockStatesTag.remove("data");
				} else {
					blockStatesTag.putLongArray("data", blockStates);
				}
			}

			Helper.getRegion(data).put("block_entities", tileEntities);
		}

		@Override
		public int getBlockAmount(ChunkData data, String[] blocks) {
			ListTag sections = Helper.tagFromCompound(Helper.getRegion(data), "sections");
			if (sections == null) {
				return 0;
			}

			int result = 0;

			for (CompoundTag section : sections.iterateType(CompoundTag.class)) {
				ListTag palette = Helper.tagFromCompound(Helper.tagFromCompound(section, "block_states"), "palette");
				long[] blockStates = Helper.longArrayFromCompound(Helper.tagFromCompound(section, "block_states"), "data");
				if (palette == null || blockStates == null) {
					continue;
				}

				for (int i = 0; i < palette.size(); i++) {
					CompoundTag blockState = palette.getCompound(i);
					String name = Helper.getBlockID(blockState, null);
					if (name == null) {
						continue;
					}

					for (String block : blocks) {
						if (name.equals(block)) {
							// count blocks of this type
							for (int k = 0; k < 4096; k++) {
								if (blockState == getBlockAt(k, blockStates, palette)) {
									result++;
								}
							}
							break;
						}
					}
				}
			}
			return result;
		}

		@Override
		public int getAverageHeight(ChunkData data) {
			ListTag sections = Helper.tagFromCompound(Helper.getRegion(data), "sections");
			if (sections == null) {
				return 0;
			}

			sections.sort(this::filterSections);

			int totalHeight = 0;

			for (int cx = 0; cx < 16; cx++) {
				zLoop:
				for (int cz = 0; cz < 16; cz++) {
					for (CompoundTag section : sections.iterateType(CompoundTag.class)) {
						ListTag palette = Helper.tagFromCompound(Helper.tagFromCompound(section, "block_states"), "palette");
						long[] blockStates = Helper.longArrayFromCompound(Helper.tagFromCompound(section, "block_states"), "data");
						if (palette == null || blockStates == null) {
							continue;
						}

						Number height = Helper.numberFromCompound(section, "Y", null);
						if (height == null) {
							continue;
						}

						for (int cy = 15; cy >= 0; cy--) {
							int index = cy * 256 + cz * 16 + cx;
							Tag block = getBlockTagAt(index, blockStates, palette);
							if (!isBlockTagEmpty(block)) {
								totalHeight += height.intValue() * 16 + cy;
								continue zLoop;
							}
						}
					}
				}
			}
			return totalHeight / 256;
		}

		protected Tag getBlockTagAt(int index, long[] blockStates, ListTag palette) {
			return palette.get(getPaletteIndex(index, blockStates));
		}


		protected boolean isBlockTagEmpty(Tag blockData) {
			return switch (Helper.getBlockID(blockData, "")) {
				case "minecraft:air", "minecraft:cave_air", "minecraft:barrier", "minecraft:structure_void" ->
					blockData instanceof StringTag || blockData instanceof CompoundTag c && c.size() == 1;
				default -> false;
			};
		}

		@Override
		protected CompoundTag completeSection(CompoundTag section, int y) {
			section.putByte("Y", (byte) y);
			if (!section.containsKey("block_states")) {
				CompoundTag newBlockStates = new CompoundTag();
				section.put("block_states", newBlockStates);
			}
			CompoundTag blockStates = section.getCompound("block_states");

			if (!blockStates.containsKey("data")) {
				blockStates.putLongArray("data", new long[256]);
			}
			if (!blockStates.containsKey("palette")) {
				ListTag newPalette = new ListTag();
				CompoundTag newBlockState = new CompoundTag();
				newBlockState.putString("", "minecraft:air");
				newPalette.add(newBlockState);
				blockStates.put("palette", newPalette);
			}

			if (!section.containsKey("biomes")) {
				CompoundTag newBiomes = new CompoundTag();
				section.put("biomes", newBiomes);
			}
			CompoundTag biomes = section.getCompound("biomes");

			if (!biomes.containsKey("palette")) {
				ListTag biomePalette = new ListTag();
				biomePalette.addString("minecraft:plains");
				biomes.put("palette", biomePalette);
			}
			if (!biomes.containsKey("data")) {
				biomes.putLongArray("data", new long[1]);
			}
			return section;
		}

		@Override
		protected long[] cleanupPalette(long[] blockStates, ListTag palette) {
			// create mapping of old --> new indices
			Map<Integer, Integer> allIndices = new HashMap<>(palette.size());
			for (int i = 0; i < 4096; i++) {
				int paletteIndex = getPaletteIndex(i, blockStates);
				allIndices.put(paletteIndex, paletteIndex);
			}

			// remove unused indices from palette
			int oldIndex = 0;
			for (int i = 0; i < palette.size(); i++) {
				if (!allIndices.containsKey(oldIndex)) {
					palette.remove(i);
					i--;
				} else {
					allIndices.put(oldIndex, i);
				}
				oldIndex++;
			}

			// add air to the palette if it doesn't contain air
			if (!paletteContainsAir(palette)) {
				CompoundTag air = new CompoundTag();
				air.putString("", "minecraft:air");
				palette.add(air);
			}

			return adjustBlockStateBits(palette, blockStates, allIndices);
		}

		@Override
		protected boolean paletteContainsAir(ListTag palette) {
			return palette.stream().anyMatch(c -> Helper.getBlockID(c, "").equals("minecraft:air"));
		}

		protected void decompressPalette(ListTag palette) {
			if (palette.getElementType() == Tag.Type.STRING) {
				ListTag decompressed = new ListTag(Tag.Type.COMPOUND);
				palette.forEach(e -> {
					CompoundTag t = new CompoundTag();
					t.put("", e);
					decompressed.add(t);
				});
				palette.clear();
				palette.addAll(decompressed);
			}
		}

		protected void compressPalette(ListTag palette) {
			// if none of the entries has properties, we convert it into a string list
			boolean hasProperties = palette.stream().anyMatch(e -> ((CompoundTag) e).containsKey("properties"));
			if (!hasProperties) {
				ListTag simplePalette = new ListTag(Tag.Type.STRING);
				palette.forEach(e -> simplePalette.addString(Helper.getBlockID(e, "")));
				palette.clear(); // removes the ListTag type
				palette.addAll(simplePalette); // sets the ListTag type to STRING
				return;
			}

			palette.forEach(e -> {
				CompoundTag t = (CompoundTag) e;
				String name = Helper.getBlockID(t, "");
				t.remove("");
				t.remove("id");
				if (t.containsKey("properties")) {
					t.putString("id", name);
				} else {
					t.putString("", name);
				}
			});
		}
	}

	@MCVersionImplementation(5009)
	public static class Heightmap extends ChunkFilter_26_3_snapshot_4.Heightmap {

		@Override
		public void worldSurface(ChunkData data) {
			setHeightMap(Helper.getRegion(data), HeightmapConfig.WORLD_SURFACE, getHeightMapByBlockID(Helper.getRegion(data), b -> {
				String name = Helper.getBlockID(b, null);
				return name != null && cfg.worldSurface.contains(name);
			}));
		}

		@Override
		public void oceanFloor(ChunkData data) {
			setHeightMap(Helper.getRegion(data), HeightmapConfig.OCEAN_FLOOR, getHeightMapByBlockID(Helper.getRegion(data), b -> {
				String name = Helper.getBlockID(b, null);
				return name != null && cfg.oceanFloor.contains(name);
			}));
		}

		@Override
		public void motionBlocking(ChunkData data) {
			setHeightMap(Helper.getRegion(data), HeightmapConfig.MOTION_BLOCKING, getHeightMapByBlockID(Helper.getRegion(data), b -> {
				String name = Helper.getBlockID(b, null);
				return name != null && cfg.motionBlocking.contains(name);
			}));
		}

		@Override
		public void motionBlockingNoLeaves(ChunkData data) {
			setHeightMap(Helper.getRegion(data), HeightmapConfig.MOTION_BLOCKING_NO_LEAVES, getHeightMapByBlockID(Helper.getRegion(data), b -> {
				String name = Helper.getBlockID(b, null);
				return name != null && cfg.motionBlocking.contains(name) && !cfg.leaves.contains(name);
			}));
		}

		protected long[] getHeightMapByBlockID(CompoundTag root, Predicate<Tag> matcher) {
			ListTag sections = Helper.getSectionsFromCompound(root, "sections");
			if (sections == null) {
				return new long[37];
			}

			Range sectionRange = Helper.findSectionRange(root, sections);

			ListTag[] palettes = new ListTag[sectionRange.num()];
			long[][] blockStatesArray = new long[sectionRange.num()][];
			sections.forEach(s -> {
				ListTag p = Helper.tagFromCompound(Helper.tagFromCompound(s, "block_states"), "palette");
				long[] b = Helper.longArrayFromCompound(Helper.tagFromCompound(s, "block_states"), "data");
				int y = Helper.numberFromCompound(s, "Y", sectionRange.getFrom() - 1).intValue();
				if (sectionRange.contains(y) && p != null && b != null) {
					palettes[y - sectionRange.getFrom()] = p;
					blockStatesArray[y - sectionRange.getFrom()] = b;
				}
			});

			short[] heightmap = new short[256];

			// loop over x/z
			for (int cx = 0; cx < 16; cx++) {
				loop:
				for (int cz = 0; cz < 16; cz++) {
					for (int i = sectionRange.num() - 1; i >= 0; i--) {
						ListTag palette = palettes[i];
						if (palette == null) {
							continue;
						}
						long[] blockStates = blockStatesArray[i];
						for (int cy = 15; cy >= 0; cy--) {
							int blockIndex = cy * 256 + cz * 16 + cx;
							if (matcher.test(getBlockTagAt(blockIndex, blockStates, palette))) {
								heightmap[cz * 16 + cx] = (short) (i * 16 + cy + 1);
								continue loop;
							}
						}
					}
				}
			}

			int bits = 32 - Integer.numberOfLeadingZeros(sectionRange.num() * 16);
			return applyHeightMap(heightmap, bits);
		}

		protected Tag getBlockTagAt(int index, long[] blockStates, ListTag palette) {
			return palette.get(getPaletteIndex(index, blockStates));
		}
	}

	@MCVersionImplementation(5009)
	public static class Palette implements ChunkFilter.Palette {

		@Override
		public boolean paletteEquals(ChunkData data, Collection<String> names) {
			ListTag sections = Helper.tagFromCompound(Helper.getRegion(data), "sections");
			if (sections == null) {
				return false;
			}

			Set<String> blocks = new HashSet<>();
			for (CompoundTag t : sections.iterateType(CompoundTag.class)) {
				ListTag palette = Helper.tagFromCompound(Helper.tagFromCompound(t, "block_states"), "palette");
				if (palette == null) {
					continue;
				}
				for (CompoundTag p : palette.iterateType(CompoundTag.class)) {
					String n;
					if ((n = Helper.getBlockID(p, null)) != null) {
						if (!names.contains(n)) {
							return false;
						}
						blocks.add(n);
					}
				}
			}
			if (blocks.size() != names.size()) {
				return false;
			}
			for (String name : names) {
				if (!blocks.contains(name)) {
					return false;
				}
			}
			return true;
		}
	}
}
