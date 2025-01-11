package net.querz.mcaselector.version.java_1_17;

import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.io.registry.BiomeRegistry;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.point.Point3i;
import net.querz.mcaselector.range.Range;
import net.querz.mcaselector.tile.Tile;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.Helper;
import net.querz.mcaselector.version.MCVersionImplementation;
import net.querz.mcaselector.version.java_1_15.ChunkFilter_19w36a;
import net.querz.mcaselector.version.java_1_16.ChunkFilter_20w17a;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.IntArrayTag;
import net.querz.nbt.ListTag;
import java.util.*;
import java.util.function.Predicate;

public class ChunkFilter_21w06a {

	@MCVersionImplementation(2694)
	public static class Biomes extends ChunkFilter_19w36a.Biomes {

		@Override
		public void forceBiome(ChunkData data, BiomeRegistry.BiomeIdentifier biome) {
			CompoundTag level = Helper.levelFromRoot(Helper.getRegion(data));
			if (level != null) {
				int[] biomes = new int[1536];
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

			if (destinationBiomes == null) {
				// if there is no destination, we will let minecraft set the biome
				destinationBiomes = new IntArrayTag(new int[1536]);
				Arrays.fill(destinationBiomes.getValue(), -1);
			}

			if (sourceBiomes == null) {
				// if there is no source biome, we set the biome to -1
				// merge biomes
				for (Range range : ranges) {
					int m = Math.min(range.getTo() + yOffset, 23);
					for (int i = Math.max(range.getFrom() + yOffset, -4); i <= m; i++) {
						setSectionBiomes(-1, destinationBiomes.getValue(), i);
					}
				}
			} else {
				for (Range range : ranges) {
					int m = Math.min(range.getTo() - yOffset, 23);
					for (int i = Math.max(range.getFrom() - yOffset, -4); i <= m; i++) {
						copySectionBiomes(sourceBiomes.getValue(), destinationBiomes.getValue(), i);
					}
				}
			}
		}

		@Override
		protected int getBiomeAt(int[] biomes, int biomeX, int biomeY, int biomeZ) {
			if (biomes == null || biomes.length != 1536) {
				return -1;
			}
			return biomes[getBiomeIndex(biomeX, biomeY, biomeZ)];
		}

		@Override
		protected void setBiomeAt(int[] biomes, int biomeX, int biomeY, int biomeZ, int biomeID) {
			if (biomes == null || biomes.length != 1536) {
				biomes = new int[1536];
				Arrays.fill(biomes, -1);
			}
			biomes[getBiomeIndex(biomeX, biomeY, biomeZ)] = biomeID;
		}

		@Override
		protected int getBiomeIndex(int x, int y, int z) {
			return (y + 4) * 24 + z * 4 + x;
		}
	}

	@MCVersionImplementation(2694)
	public static class Relocate extends ChunkFilter_20w45a.Relocate {

		@Override
		protected void applyOffsetToBiomes(IntArrayTag biomes, Point3i offset) {
			int[] biomesArray;
			if (biomes == null || (biomesArray = biomes.getValue()) == null || biomesArray.length != 1536) {
				return;
			}

			int[] newBiomes = new int[1536];

			for (int x = 0; x < 4; x++) {
				for (int z = 0; z < 4; z++) {
					for (int y = 0; y < 96; y++) {
						if (y + offset.getY() * 4 < 0 || y + offset.getY() * 4 > 95) {
							break;
						}
						int biome = biomesArray[y * 24 + z * 4 + x];
						newBiomes[(y + offset.getY() * 4) * 24 + z * 4 + x] = biome;
					}
				}
			}

			biomes.setValue(newBiomes);
		}
	}

	@MCVersionImplementation(2694)
	public static class Heightmap extends ChunkFilter_20w17a.Heightmap {

		@Override
		protected long[] getHeightMap(CompoundTag root, Predicate<CompoundTag> matcher) {
			ListTag sections = Helper.getSectionsFromLevelFromRoot(root, "Sections");
			if (sections == null) {
				return new long[37];
			}

			ListTag[] palettes = new ListTag[24];
			long[][] blockStatesArray = new long[24][];
			sections.forEach(s -> {
				ListTag p = Helper.tagFromCompound(s, "Palette");
				long[] b = Helper.longArrayFromCompound(s, "BlockStates");
				int y = Helper.numberFromCompound(s, "Y", -5).intValue();
				if (y >= -4 && y < 20 && p != null && b != null) {
					palettes[y + 4] = p;
					blockStatesArray[y + 4] = b;
				}
			});

			short[] heightmap = new short[256];

			// loop over x/z
			for (int cx = 0; cx < Tile.CHUNK_SIZE; cx++) {
				loop:
				for (int cz = 0; cz < Tile.CHUNK_SIZE; cz++) {
					for (int i = 23; i >= 0; i--) {
						ListTag palette = palettes[i];
						if (palette == null) {
							continue;
						}
						long[] blockStates = blockStatesArray[i];
						for (int cy = 15; cy >= 0; cy--) {
							int blockIndex = cy * Tile.CHUNK_SIZE * Tile.CHUNK_SIZE + cz * Tile.CHUNK_SIZE + cx;
							if (matcher.test(getBlockAt(blockIndex, blockStates, palette))) {
								heightmap[cz * Tile.CHUNK_SIZE + cx] = (short) (i * Tile.CHUNK_SIZE + cy + 1);
								continue loop;
							}
						}
					}
				}
			}
			return applyHeightMap(heightmap);
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

			int yMax = Helper.findHighestSection(sections, -4);

			// handle the special case when someone wants to replace air with something else
			if (replace.containsKey("minecraft:air")) {
				Map<Integer, CompoundTag> sectionMap = new HashMap<>();
				List<Integer> heights = new ArrayList<>(yMax + 5);
				for (CompoundTag section : sections.iterateType(CompoundTag.class)) {
					sectionMap.put(section.getInt("Y"), section);
					heights.add(section.getInt("Y"));
				}

				for (int y = -4; y <= yMax; y++) {
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
				if (y < 0 || y > 15) {
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
	}
}
