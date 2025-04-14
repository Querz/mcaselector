package net.querz.mcaselector.version.java_1_18;

import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.util.point.Point2i;
import net.querz.mcaselector.util.point.Point3i;
import net.querz.mcaselector.util.range.Range;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.Helper;
import net.querz.mcaselector.version.MCVersionImplementation;
import net.querz.mcaselector.version.java_1_17.ChunkFilter_20w45a;
import net.querz.mcaselector.version.java_1_17.ChunkFilter_21w06a;
import net.querz.mcaselector.version.java_1_9.ChunkFilter_15w32a;
import net.querz.mcaselector.version.mapping.registry.BiomeRegistry;
import net.querz.mcaselector.version.mapping.registry.StatusRegistry;
import net.querz.nbt.*;
import java.util.*;
import java.util.function.Predicate;
import static net.querz.mcaselector.util.validation.ValidationHelper.*;

public class ChunkFilter_21w43a {

	@MCVersionImplementation(2844)
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
						if (name.equals(Helper.stringFromCompound(p, "Name"))) {
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
						if (name.equals(Helper.stringFromCompound(p, "Name"))) {
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

				if (palette.size() == 1 && blockStates == null) {
					blockStates = new long[256];
				}

				int y = Helper.numberFromCompound(section, "Y", sectionRange.getFrom() - 1).intValue();
				if (!sectionRange.contains(y)) {
					continue;
				}

				section.remove("BlockLight");
				section.remove("SkyLight");

				for (int i = 0; i < 4096; i++) {
					CompoundTag blockState = getBlockAt(i, blockStates, palette);

					for (Map.Entry<String, ChunkFilter.BlockReplaceData> entry : replace.entrySet()) {
						if (!blockState.getString("Name").matches(entry.getKey())) {
							continue;
						}
						ChunkFilter.BlockReplaceData replacement = entry.getValue();

						try {
							blockStates = setBlockAt(i, replacement.getState(), blockStates, palette);
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
					String name = Helper.stringFromCompound(blockState, "Name");
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
							CompoundTag block = getBlockAt(index, blockStates, palette);
							if (!isEmpty(block)) {
								totalHeight += height.intValue() * 16 + cy;
								continue zLoop;
							}
						}
					}
				}
			}
			return totalHeight / 256;
		}
	}

	@MCVersionImplementation(2844)
	public static class Heightmap extends ChunkFilter_21w37a.Heightmap {

		@Override
		protected long[] getHeightMap(CompoundTag root, Predicate<CompoundTag> matcher) {
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
							if (matcher.test(getBlockAt(blockIndex, blockStates, palette))) {
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
	}

	@MCVersionImplementation(2844)
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
					if ((n = Helper.stringFromCompound(p, "Name")) != null) {
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

	@MCVersionImplementation(2844)
	public static class Biomes implements ChunkFilter.Biomes {

		@Override
		public boolean matchBiomes(ChunkData data, Collection<BiomeRegistry.BiomeIdentifier> biomes) {
			ListTag sections = Helper.tagFromCompound(Helper.getRegion(data), "sections");
			if (sections == null) {
				return false;
			}

			Set<String> names = new HashSet<>(biomes.size());

			filterLoop:
			for (BiomeRegistry.BiomeIdentifier identifier : biomes) {
				for (CompoundTag section : sections.iterateType(CompoundTag.class)) {
					ListTag biomePalette = Helper.tagFromCompound(Helper.tagFromCompound(section, "biomes"), "palette");
					if (biomePalette == null) {
						continue;
					}
					for (StringTag biomeName : biomePalette.iterateType(StringTag.class)) {
						if (identifier.matches(biomeName.getValue())) {
							names.add(biomeName.getValue());
							if (biomes.size() == names.size()) {
								return true;
							}
							continue filterLoop;
						}
					}
				}
			}
			return biomes.size() == names.size();
		}

		@Override
		public boolean matchAnyBiome(ChunkData data, Collection<BiomeRegistry.BiomeIdentifier> biomes) {
			ListTag sections = Helper.tagFromCompound(Helper.getRegion(data), "sections");
			if (sections == null) {
				return false;
			}

			for (BiomeRegistry.BiomeIdentifier identifier : biomes) {
				for (CompoundTag section : sections.iterateType(CompoundTag.class)) {
					ListTag biomePalette = Helper.tagFromCompound(Helper.tagFromCompound(section, "biomes"), "palette");
					if (biomePalette == null) {
						continue;
					}
					for (StringTag biomeName : biomePalette.iterateType(StringTag.class)) {
						if (identifier.matches(biomeName.getValue())) {
							return true;
						}
					}
				}
			}
			return false;
		}

		@Override
		public void changeBiome(ChunkData data, BiomeRegistry.BiomeIdentifier biome) {
			ListTag sections = Helper.tagFromCompound(Helper.getRegion(data), "sections");
			if (sections == null) {
				return;
			}

			for (CompoundTag section : sections.iterateType(CompoundTag.class)) {
				CompoundTag biomes = Helper.tagFromCompound(section, "biomes");
				if (biomes == null) {
					continue;
				}

				ListTag newBiomePalette = new ListTag();
				newBiomePalette.addString(biome.getName());
				biomes.put("palette", newBiomePalette);
				biomes.putLongArray("data", new long[1]);
			}
		}

		@Override
		public void forceBiome(ChunkData data, BiomeRegistry.BiomeIdentifier biome) {
			ListTag sections = Helper.tagFromCompound(Helper.getRegion(data), "sections");
			if (sections == null) {
				return;
			}

			for (CompoundTag section : sections.iterateType(CompoundTag.class)) {
				CompoundTag biomes = new CompoundTag();
				ListTag newBiomePalette = new ListTag();
				newBiomePalette.addString(biome.getName());
				biomes.put("palette", newBiomePalette);
				biomes.putLongArray("data", new long[1]);
				section.put("biomes", biomes);
			}
		}
	}

	@MCVersionImplementation(2844)
	public static class Sections extends ChunkFilter_15w32a.Sections {

		@Override
		public void deleteSections(ChunkData data, List<Range> ranges) {
			switch (Helper.getRegion(data).getString("Status")) {
			case "light", "spawn", "heightmaps", "full" -> Helper.getRegion(data).putString("Status", "features");
			default -> {return;}
			}
			ListTag sections = Helper.tagFromCompound(Helper.getRegion(data), "sections");
			if (sections == null) {
				return;
			}
			for (int i = 0; i < sections.size(); i++) {
				CompoundTag section = sections.getCompound(i);
				for (Range range : ranges) {
					if (range.contains(section.getInt("Y"))) {
						deleteSection(section);
					}
				}
			}
		}

		// only delete blocks, not biomes
		protected void deleteSection(CompoundTag section) {
			CompoundTag blockStates = section.getCompound("block_states");
			blockStates.remove("data");
			ListTag blockPalette = new ListTag();
			CompoundTag air = new CompoundTag();
			air.putString("Name", "minecraft:air");
			blockPalette.add(air);
			blockStates.put("palette", blockPalette);
			section.remove("BlockLight");
		}
	}

	@MCVersionImplementation(2844)
	public static class InhabitedTime implements ChunkFilter.InhabitedTime {

		@Override
		public LongTag getInhabitedTime(ChunkData data) {
			return Helper.tagFromCompound(Helper.getRegion(data), "InhabitedTime");
		}

		@Override
		public void setInhabitedTime(ChunkData data, long inhabitedTime) {
			CompoundTag root = Helper.getRegion(data);
			if (root != null) {
				root.putLong("InhabitedTime", inhabitedTime);
			}
		}
	}

	@MCVersionImplementation(2844)
	public static class Relocate extends ChunkFilter_21w06a.Relocate {

		@Override
		public boolean relocate(CompoundTag root, Point3i offset) {
			// adjust or set chunk position
			root.putInt("xPos", root.getInt("xPos") + offset.blockToChunk().getX());
			root.putInt("zPos", root.getInt("zPos") + offset.blockToChunk().getZ());

			// adjust tile entity positions
			ListTag tileEntities = Helper.tagFromCompound(root, "block_entities");
			if (tileEntities != null) {
				tileEntities.forEach(v -> catchAndLog(() -> applyOffsetToTileEntity((CompoundTag) v, offset)));
			}

			// adjust tile ticks
			ListTag tileTicks = Helper.tagFromCompound(root, "block_ticks");
			if (tileTicks != null) {
				tileTicks.forEach(v -> catchAndLog(() -> applyOffsetToTick((CompoundTag) v, offset)));
			}

			// adjust liquid ticks
			ListTag liquidTicks = Helper.tagFromCompound(root, "fluid_ticks");
			if (liquidTicks != null) {
				liquidTicks.forEach(v -> catchAndLog(() -> applyOffsetToTick((CompoundTag) v, offset)));
			}

			// adjust structures
			CompoundTag structures = Helper.tagFromCompound(root, "structures");
			if (structures != null) {
				catchAndLog(() -> applyOffsetToStructures(structures, offset));
			}

			catchAndLog(() -> Helper.applyOffsetToListOfShortTagLists(root, "PostProcessing", offset.blockToSection()));

			// adjust sections vertically
			ListTag sections = Helper.tagFromCompound(root, "sections");
			if (sections != null) {
				ListTag newSections = new ListTag();
				Range sectionRange = Helper.findSectionRange(root, sections);
				for (CompoundTag section : sections.iterateType(CompoundTag.class)) {
					if (applyOffsetToSection(section, offset.blockToSection(), sectionRange)) {
						newSections.add(section);
					}
				}
				root.put("sections", newSections);
			}

			return true;
		}

		@Override
		protected void applyOffsetToStructures(CompoundTag structures, Point3i offset) {
			Point3i chunkOffset = offset.blockToChunk();

			// update references
			CompoundTag references = Helper.tagFromCompound(structures, "References");
			if (references != null) {
				for (Map.Entry<String, Tag> entry : references) {
					long[] reference = silent(() -> ((LongArrayTag) entry.getValue()).getValue(), null);
					if (reference != null) {
						for (int i = 0; i < reference.length; i++) {
							int x = (int) (reference[i]);
							int z = (int) (reference[i] >> 32);
							reference[i] = ((long) (z + chunkOffset.getZ()) & 0xFFFFFFFFL) << 32 | (long) (x + chunkOffset.getX()) & 0xFFFFFFFFL;
						}
					}
				}
			}

			// update starts
			CompoundTag starts = Helper.tagFromCompound(structures, "starts");
			if (starts != null) {
				for (Map.Entry<String, Tag> entry : starts) {
					CompoundTag structure = silent(() -> (CompoundTag) entry.getValue(), null);
					if ("INVALID".equals(Helper.stringFromCompound(structure, "id"))) {
						continue;
					}
					Helper.applyIntIfPresent(structure, "ChunkX", chunkOffset.getX());
					Helper.applyIntIfPresent(structure, "ChunkZ", chunkOffset.getZ());
					Helper.applyOffsetToBB(Helper.intArrayFromCompound(structure, "BB"), offset);

					ListTag processed = Helper.tagFromCompound(structure, "Processed");
					if (processed != null) {
						for (CompoundTag chunk : processed.iterateType(CompoundTag.class)) {
							Helper.applyIntIfPresent(chunk, "X", chunkOffset.getX());
							Helper.applyIntIfPresent(chunk, "Z", chunkOffset.getZ());
						}
					}

					ListTag children = Helper.tagFromCompound(structure, "Children");
					if (children != null) {
						for (CompoundTag child : children.iterateType(CompoundTag.class)) {
							Helper.applyIntOffsetIfRootPresent(child, "TPX", "TPY", "TPZ", offset);
							Helper.applyIntOffsetIfRootPresent(child, "PosX", "PosY", "PosZ", offset);
							Helper.applyOffsetToBB(Helper.intArrayFromCompound(child, "BB"), offset);

							ListTag entrances = Helper.tagFromCompound(child, "Entrances");
							if (entrances != null) {
								entrances.forEach(e -> Helper.applyOffsetToBB(((IntArrayTag) e).getValue(), offset));
							}

							ListTag junctions = Helper.tagFromCompound(child, "junctions");
							if (junctions != null) {
								for (CompoundTag junction : junctions.iterateType(CompoundTag.class)) {
									Helper.applyIntOffsetIfRootPresent(junction, "source_x", "source_y", "source_z", offset);
								}
							}
						}
					}
				}
			}
		}
	}

	@MCVersionImplementation(2844)
	public static class Merge extends ChunkFilter_21w06a.Merge {

		@Override
		public void mergeChunks(CompoundTag source, CompoundTag destination, List<Range> ranges, int yOffset) {
			mergeCompoundTagLists(source, destination, ranges, yOffset, "sections", c -> ((CompoundTag) c).getInt("Y"));
			mergeCompoundTagLists(source, destination, ranges, yOffset, "block_entities", c -> ((CompoundTag) c).getInt("y") >> 4);
			mergeCompoundTagLists(source, destination, ranges, yOffset, "block_ticks", c -> ((CompoundTag) c).getInt("y") >> 4);
			mergeCompoundTagLists(source, destination, ranges, yOffset, "fluid_ticks", c -> ((CompoundTag) c).getInt("y") >> 4);
			mergeListTagLists(source, destination, ranges, yOffset, "PostProcessing");
			mergeStructures(source, destination, ranges, yOffset);
		}

		@Override
		protected void mergeStructures(CompoundTag source, CompoundTag destination, List<Range> ranges, int yOffset) {
			CompoundTag sourceStarts = Helper.tagFromCompound(Helper.tagFromCompound(source, "structures"), "starts", new CompoundTag());
			CompoundTag destinationStarts = Helper.tagFromCompound(Helper.tagFromCompound(destination, "structures"), "starts", new CompoundTag());

			if (!destinationStarts.isEmpty()) {
				// remove BBs from destination
				for (Map.Entry<String, Tag> start : destinationStarts) {
					ListTag children = Helper.tagFromCompound(start.getValue(), "Children", null);
					if (children != null) {
						child: for (int i = 0; i < children.size(); i++) {
							CompoundTag child = children.getCompound(i);
							int[] bb = Helper.intArrayFromCompound(child, "BB");
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
					if (children == null || children.isEmpty()) {
						int[] bb = Helper.intArrayFromCompound(start.getValue(), "BB");
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
				ListTag children = Helper.tagFromCompound(start.getValue(), "Children", null);
				if (children != null) {
					child:
					for (int i = 0; i < children.size(); i++) {
						CompoundTag child = children.getCompound(i);
						int[] bb = Helper.intArrayFromCompound(child, "BB");
						if (bb == null) {
							continue;
						}
						for (Range range : ranges) {
							if (range.contains(bb[1] >> 4 - yOffset) || range.contains(bb[4] >> 4 - yOffset)) {
								CompoundTag destinationStart = Helper.tagFromCompound(destinationStarts, start.getKey(), null);
								if (destinationStart == null || "INVALID".equals(destinationStart.getString("id"))) {
									destinationStart = ((CompoundTag) start.getValue()).copy();

									// we need to remove the children, we don't want all of them
									ListTag clonedDestinationChildren = Helper.tagFromCompound(destinationStart, "Children", null);
									if (clonedDestinationChildren != null) {
										clonedDestinationChildren.clear();
									}
									destinationStarts.put(start.getKey(), destinationStart);
								}

								ListTag destinationChildren = Helper.tagFromCompound(destinationStarts.get(start.getKey()), "Children", null);
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

	@MCVersionImplementation(2844)
	public static class TileEntities implements ChunkFilter.TileEntities {

		@Override
		public ListTag getTileEntities(ChunkData data) {
			return Helper.tagFromCompound(Helper.getRegion(data), "block_entities");
		}
	}

	@MCVersionImplementation(2844)
	public static class Structures implements ChunkFilter.Structures {

		@Override
		public CompoundTag getStructureReferences(ChunkData data) {
			return Helper.tagFromCompound(Helper.tagFromCompound(Helper.getRegion(data), "structures"), "References");
		}

		@Override
		public CompoundTag getStructureStarts(ChunkData data) {
			return Helper.tagFromCompound(Helper.tagFromCompound(Helper.getRegion(data), "structures"), "starts");
		}
	}

	@MCVersionImplementation(2844)
	public static class Status implements ChunkFilter.Status {

		@Override
		public StringTag getStatus(ChunkData data) {
			return Helper.tagFromCompound(Helper.getRegion(data), "Status");
		}

		@Override
		public void setStatus(ChunkData data, StatusRegistry.StatusIdentifier status) {
			if (Helper.getRegion(data) != null) {
				Helper.getRegion(data).putString("Status", status.getStatus());
			}
		}

		@Override
		public boolean matchStatus(ChunkData data, StatusRegistry.StatusIdentifier status) {
			StringTag tag = getStatus(data);
			if (tag == null) {
				return false;
			}
			return status.getStatus().equals(tag.getValue());
		}
	}

	@MCVersionImplementation(2844)
	public static class LastUpdate implements ChunkFilter.LastUpdate {

		@Override
		public LongTag getLastUpdate(ChunkData data) {
			return Helper.tagFromCompound(Helper.getRegion(data), "LastUpdate");
		}

		@Override
		public void setLastUpdate(ChunkData data, long lastUpdate) {
			if (Helper.getRegion(data) != null) {
				Helper.getRegion(data).putLong("LastUpdate", lastUpdate);
			}
		}
	}

	@MCVersionImplementation(2844)
	public static class Pos implements ChunkFilter.Pos {

		@Override
		public IntTag getXPos(ChunkData data) {
			return Helper.tagFromCompound(Helper.getRegion(data), "xPos");
		}

		@Override
		public IntTag getYPos(ChunkData data) {
			return Helper.tagFromCompound(Helper.getRegion(data), "yPos");
		}

		@Override
		public IntTag getZPos(ChunkData data) {
			return Helper.tagFromCompound(Helper.getRegion(data), "zPos");
		}
	}

	@MCVersionImplementation(2844)
	public static class LightPopulated implements ChunkFilter.LightPopulated {

		@Override
		public ByteTag getLightPopulated(ChunkData data) {
			return Helper.tagFromCompound(Helper.getRegion(data), "isLightOn");
		}

		@Override
		public void setLightPopulated(ChunkData data, byte lightPopulated) {
			if (Helper.getRegion(data) != null) {
				Helper.getRegion(data).putByte("isLightOn", lightPopulated);
			}
		}
	}

	@MCVersionImplementation(2844)
	public static class Blending implements ChunkFilter.Blending {

		@Override
		public void forceBlending(ChunkData data) {
			int min = 0, max = 0;
			CompoundTag root = Helper.getRegion(data);
			ListTag sections = Helper.tagFromCompound(root, "sections");
			if (sections == null) {
				return;
			}
			for (CompoundTag section : sections.iterateType(CompoundTag.class)) {
				int y = Helper.numberFromCompound(section, "Y", 0).intValue();
				min = Math.min(y, min);
				max = Math.max(y, max);
			}
			min = Math.min(min, -4);
			max = Math.max(max, 20);
			CompoundTag blendingData = new CompoundTag();
			blendingData.putInt("min_section", min);
			blendingData.putInt("max_section", max);
			root.put("blending_data", blendingData);
			root.remove("Heightmaps");
			root.remove("isLightOn");
		}
	}

	@MCVersionImplementation(2844)
	public static class Entities extends ChunkFilter_20w45a.Entities {

		@Override
		public void deleteEntities(ChunkData data, List<Range> ranges) {
			ListTag entities = Helper.tagFromLevelFromRoot(Helper.getEntities(data), "Entities", null);
			deleteEntities(entities, ranges);

			// delete proto-entities
			ListTag protoEntities = Helper.tagFromLevelFromRoot(Helper.getRegion(data), "entities", null);
			deleteEntities(protoEntities, ranges);
		}
	}
}
