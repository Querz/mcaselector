package net.querz.mcaselector.version.java_1_17;

import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.util.point.Point2i;
import net.querz.mcaselector.util.point.Point3i;
import net.querz.mcaselector.util.range.Range;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.Helper;
import net.querz.mcaselector.version.MCVersionImplementation;
import net.querz.mcaselector.version.java_1_15.ChunkFilter_19w36a;
import net.querz.mcaselector.version.java_1_16.ChunkFilter_20w17a;
import net.querz.mcaselector.version.mapping.registry.BiomeRegistry;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.IntArrayTag;
import net.querz.nbt.ListTag;
import net.querz.nbt.Tag;

import java.util.*;
import java.util.function.Predicate;
import static net.querz.mcaselector.util.validation.ValidationHelper.*;

public class ChunkFilter_21w06a {

	@MCVersionImplementation(2694)
	public static class Biomes extends ChunkFilter_19w36a.Biomes {

		@Override
		public void forceBiome(ChunkData data, BiomeRegistry.BiomeIdentifier biome) {
			CompoundTag level = Helper.levelFromRoot(Helper.getRegion(data));
			int yMin = Helper.intFromCompound(level, "yPos", -4);
			int yMax = Helper.findHighestSection(Helper.tagFromCompound(level, "Sections"), yMin);
			if (level != null) {
				int[] biomes = new int[(yMax - yMin) * 64];
				Arrays.fill(biomes, (byte) biome.getID());
				level.putIntArray("Biomes", biomes);
			}
		}
	}

	@MCVersionImplementation(2694)
	public static class Merge extends ChunkFilter_20w45a.Merge {

		@Override
		protected void mergeBiomes(CompoundTag source, CompoundTag destination, List<Range> ranges, int yOffset) {
			IntArrayTag sourceBiomes = Helper.tagFromLevelFromRoot(source, "Biomes");
			IntArrayTag destinationBiomes = Helper.tagFromLevelFromRoot(destination, "Biomes");

			int yMin = Helper.intFromCompound(Helper.levelFromRoot(destination), "yPos", -4);
			int yMax = Helper.findHighestSection(Helper.tagFromLevelFromRoot(destination, "Sections"), yMin);

			if (destinationBiomes == null) {
				// if there is no destination, we will let minecraft set the biome
				destinationBiomes = new IntArrayTag(new int[(yMax - yMin) * 64]);
				Arrays.fill(destinationBiomes.getValue(), -1);
			}

			if (sourceBiomes == null) {
				// if there is no source biome, we set the biome to -1
				// merge biomes
				for (Range range : ranges) {
					int m = Math.min(range.getTo() + yOffset, yMax - yMin -1);
					for (int i = Math.max(range.getFrom() + yOffset, yMin); i <= m; i++) {
						setSectionBiomes(-1, destinationBiomes.getValue(), i, yMin, yMax);
					}
				}
			} else {
				for (Range range : ranges) {
					int m = Math.min(range.getTo() - yOffset, yMax - yMin -1);
					for (int i = Math.max(range.getFrom() - yOffset, yMin); i <= m; i++) {
						copySectionBiomes(sourceBiomes.getValue(), destinationBiomes.getValue(), i, yMin, yMax);
					}
				}
			}
		}

		protected void copySectionBiomes(int[] sourceBiomes, int[] destinationBiomes, int sectionY, int yMin, int yMax) {
			for (int y = 0; y < 4; y++) {
				int biomeY = sectionY * 4 + y;
				for (int x = 0; x < 4; x++) {
					for (int z = 0; z < 4; z++) {
						setBiomeAt(destinationBiomes, x, biomeY, z, getBiomeAt(sourceBiomes, x, biomeY, z, yMin, yMax), yMin, yMax);
					}
				}
			}
		}

		protected void setSectionBiomes(int biome, int[] destinationBiomes, int sectionY, int yMin, int yMax) {
			for (int y = 0; y < 4; y++) {
				int biomeY = sectionY * 4 + y;
				for (int x = 0; x < 4; x++) {
					for (int z = 0; z < 4; z++) {
						setBiomeAt(destinationBiomes, x, biomeY, z, biome, yMin, yMax);
					}
				}
			}
		}

		protected int getBiomeAt(int[] biomes, int biomeX, int biomeY, int biomeZ, int yMin, int yMax) {
			if (biomes == null || biomes.length != (yMax - yMin) * 64) {
				return -1;
			}
			return biomes[getBiomeIndex(biomeX, biomeY, biomeZ, yMin, yMax)];
		}

		protected void setBiomeAt(int[] biomes, int biomeX, int biomeY, int biomeZ, int biomeID, int yMin, int yMax) {
			if (biomes == null || biomes.length != (yMax - yMin) * 64) {
				biomes = new int[(yMax - yMin) * 64];
				Arrays.fill(biomes, -1);
			}
			biomes[getBiomeIndex(biomeX, biomeY, biomeZ, yMin, yMax)] = biomeID;
		}

		protected int getBiomeIndex(int x, int y, int z, int yMin, int yMax) {
			return (y - yMin) * (yMax - yMin) + z * 4 + x;
		}
	}

	@MCVersionImplementation(2694)
	public static class Relocate extends ChunkFilter_20w45a.Relocate {

		@Override
		public boolean relocate(CompoundTag root, Point3i offset) {
			CompoundTag level = Helper.tagFromCompound(root, "Level");
			if (level == null) {
				return false;
			}

			// adjust or set chunk position
			level.putInt("xPos", level.getInt("xPos") + offset.blockToChunk().getX());
			level.putInt("zPos", level.getInt("zPos") + offset.blockToChunk().getZ());

			// adjust tile entity positions
			ListTag tileEntities = Helper.tagFromCompound(level, "TileEntities");
			if (tileEntities != null) {
				tileEntities.forEach(v -> catchAndLog(() -> applyOffsetToTileEntity((CompoundTag) v, offset)));
			}

			// adjust tile ticks
			ListTag tileTicks = Helper.tagFromCompound(level, "TileTicks");
			if (tileTicks != null) {
				tileTicks.forEach(v -> catchAndLog(() -> applyOffsetToTick((CompoundTag) v, offset)));
			}

			// adjust liquid ticks
			ListTag liquidTicks = Helper.tagFromCompound(level, "LiquidTicks");
			if (liquidTicks != null) {
				liquidTicks.forEach(v -> catchAndLog(() -> applyOffsetToTick((CompoundTag) v, offset)));
			}

			// adjust structures
			CompoundTag structures = Helper.tagFromCompound(level, "Structures");
			if (structures != null) {
				catchAndLog(() -> applyOffsetToStructures(structures, offset));
			}

			// Lights
			catchAndLog(() -> Helper.applyOffsetToListOfShortTagLists(level, "Lights", offset.blockToSection()));

			// LiquidsToBeTicked
			catchAndLog(() -> Helper.applyOffsetToListOfShortTagLists(level, "LiquidsToBeTicked", offset.blockToSection()));

			// ToBeTicked
			catchAndLog(() -> Helper.applyOffsetToListOfShortTagLists(level, "ToBeTicked", offset.blockToSection()));

			// PostProcessing
			catchAndLog(() -> Helper.applyOffsetToListOfShortTagLists(level, "PostProcessing", offset.blockToSection()));

			// adjust sections vertically
			ListTag sections = Helper.getSectionsFromLevelFromRoot(root, "Sections");
			if (sections != null) {
				ListTag newSections = new ListTag();
				int yMin = Helper.intFromCompound(level, "yPos", -4);
				int yMax = Helper.findHighestSection(sections, yMin);
				for (CompoundTag section : sections.iterateType(CompoundTag.class)) {
					if (applyOffsetToSection(section, offset.blockToSection(), yMin, yMax)) {
						newSections.add(section);
					}
				}
				level.put("Sections", newSections);

				// Biomes
				catchAndLog(() -> applyOffsetToBiomes(Helper.tagFromCompound(level, "Biomes"), offset.blockToSection(), yMax - yMin));
			}

			return true;
		}

		@Override
		protected void applyOffsetToBiomes(IntArrayTag biomes, Point3i offset, int numSections) {
			int[] biomesArray;
			int l = numSections * 64;
			if (biomes == null || (biomesArray = biomes.getValue()) == null || biomesArray.length != l) {
				return;
			}

			int[] newBiomes = new int[l];

			for (int x = 0; x < 4; x++) {
				for (int z = 0; z < 4; z++) {
					for (int y = 0; y < numSections * 4; y++) {
						if (y + offset.getY() * 4 < 0 || y + offset.getY() * 4 >= numSections * 4) {
							break;
						}
						int biome = biomesArray[y * numSections + z * 4 + x];
						newBiomes[(y + offset.getY() * 4) * numSections + z * 4 + x] = biome;
					}
				}
			}

			biomes.setValue(newBiomes);
		}
	}

	@MCVersionImplementation(2694)
	public static class Heightmap extends ChunkFilter_20w45a.Heightmap {

		@Override
		protected long[] getHeightMap(CompoundTag root, Predicate<CompoundTag> matcher) {
			ListTag sections = Helper.getSectionsFromLevelFromRoot(root, "Sections");
			if (sections == null) {
				return new long[37];
			}

			int yMin = Helper.intFromCompound(Helper.levelFromRoot(root), "yPos", -4);
			int yMax = Helper.findHighestSection(Helper.tagFromLevelFromRoot(root, "Sections"), yMin);

			ListTag[] palettes = new ListTag[yMax - yMin];
			long[][] blockStatesArray = new long[yMax - yMin][];
			sections.forEach(s -> {
				ListTag p = Helper.tagFromCompound(s, "Palette");
				long[] b = Helper.longArrayFromCompound(s, "BlockStates");
				int y = Helper.numberFromCompound(s, "Y", yMin - 1).intValue();
				if (y >= yMin && y <= yMax && p != null && b != null) {
					palettes[y - yMin] = p;
					blockStatesArray[y - yMin] = b;
				}
			});

			short[] heightmap = new short[256];

			// loop over x/z
			for (int cx = 0; cx < 16; cx++) {
				loop:
				for (int cz = 0; cz < 16; cz++) {
					for (int i = palettes.length - 1; i >= 0; i--) {
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

			int bits = 32 - Integer.numberOfLeadingZeros((yMax - yMin) * 16);
			return applyHeightMap(heightmap, bits);
		}

		protected long[] applyHeightMap(short[] rawHeightmap, int bits) {
			long[] data = new long[Math.ceilDiv(256, 64 / bits)];
			int index = 0;
			for (int i = 0; i < data.length; i++) {
				long l = 0L;
				for (int j = 0; j < bits - 1 && index < 256; j++, index++) {
					l += ((long) rawHeightmap[index] << (bits * j));
				}
				data[i] = l;
			}
			return data;
		}
	}

	@MCVersionImplementation(2694)
	public static class Blocks extends ChunkFilter_20w17a.Blocks {

		@Override
		public void replaceBlocks(ChunkData data, Map<String, ChunkFilter.BlockReplaceData> replace) {
			CompoundTag level = Helper.levelFromRoot(Helper.getRegion(data));
			ListTag sections = Helper.tagFromLevelFromRoot(level, "Sections");
			if (sections == null) {
				return;
			}

			Point2i pos = Helper.point2iFromCompound(level, "xPos", "zPos");
			if (pos == null) {
				return;
			}
			pos = pos.chunkToBlock();

			int yMin = Helper.intFromCompound(level, "yPos", -4);
			int yMax = Helper.findHighestSection(sections, yMin);

			// handle the special case when someone wants to replace air with something else
			if (replace.containsKey("minecraft:air")) {
				Map<Integer, CompoundTag> sectionMap = new HashMap<>();
				List<Integer> heights = new ArrayList<>(yMax - yMin + 1);
				for (CompoundTag section : sections.iterateType(CompoundTag.class)) {
					sectionMap.put(section.getInt("Y"), section);
					heights.add(section.getInt("Y"));
				}

				for (int y = yMin; y <= yMax; y++) {
					if (!sectionMap.containsKey(y)) {
						sectionMap.put(y, createEmptySection(y));
						heights.add(y);
					} else {
						CompoundTag section = sectionMap.get(y);
						if (!section.containsKey("BlockStates") || !section.containsKey("Palette")) {
							sectionMap.put(y, createEmptySection(y));
						}
					}
				}

				heights.sort(Integer::compareTo);
				sections.clear();

				for (int height : heights) {
					sections.add(sectionMap.get(height));
				}
			}

			ListTag tileEntities = Helper.tagFromCompound(level, "TileEntities", null);
			if (tileEntities == null) {
				tileEntities = new ListTag();
			}

			for (CompoundTag section : sections.iterateType(CompoundTag.class)) {
				ListTag palette = Helper.tagFromCompound(section, "Palette", null);
				if (palette == null) {
					continue;
				}

				long[] blockStates = Helper.longArrayFromCompound(section, "BlockStates");
				if (blockStates == null) {
					continue;
				}

				int y = Helper.numberFromCompound(section, "Y", -1).intValue();
				if (y < yMin || y > yMax) {
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

				section.putLongArray("BlockStates", blockStates);
			}

			level.put("TileEntities", tileEntities);
		}

		@Override
		protected int filterSections(Tag sectionA, Tag sectionB) {
			return Helper.numberFromCompound(sectionB, "Y", Integer.MIN_VALUE).intValue() - Helper.numberFromCompound(sectionA, "Y", Integer.MIN_VALUE).intValue();
		}
	}
}
