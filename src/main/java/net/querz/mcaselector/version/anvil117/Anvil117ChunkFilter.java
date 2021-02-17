package net.querz.mcaselector.version.anvil117;

import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.point.Point3i;
import net.querz.mcaselector.validation.ValidationHelper;
import net.querz.mcaselector.version.anvil116.Anvil116ChunkFilter;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.ListTag;
import net.querz.nbt.tag.LongArrayTag;
import net.querz.nbt.tag.Tag;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static net.querz.mcaselector.validation.ValidationHelper.catchClassCastException;
import static net.querz.mcaselector.validation.ValidationHelper.withDefault;

public class Anvil117ChunkFilter extends Anvil116ChunkFilter {

	@Override
	public void forceBiome(CompoundTag data, int id) {
		if (data.containsKey("Level")) {
			int[] biomes = ValidationHelper.withDefault(() -> data.getCompoundTag("Level").getIntArray("Biomes"), null);
			if (biomes != null && (biomes.length == 1024 || biomes.length == 1536)) {
				biomes = new int[biomes.length];
			} else {
				biomes = new int[1536];
			}
			Arrays.fill(biomes, id);
			data.getCompoundTag("Level").putIntArray("Biomes", biomes);
		}
	}

	@Override
	public void replaceBlocks(CompoundTag data, Map<String, BlockReplaceData> replace) {
		CompoundTag level = withDefault(() -> data.getCompoundTag("Level"), null);
		if (level == null) {
			return;
		}
		Tag<?> rawSections = level.get("Sections");
		if (rawSections == null || rawSections.getID() == LongArrayTag.ID) {
			return;
		}
		ListTag<CompoundTag> sections = catchClassCastException(((ListTag<?>) rawSections)::asCompoundTagList);
		if (sections == null) {
			return;
		}

		Point2i pos = withDefault(() -> new Point2i(level.getInt("xPos"), level.getInt("zPos")).chunkToBlock(), null);
		if (pos == null) {
			return;
		}

		// handle the special case when someone wants to replace air with something else
		if (replace.containsKey("minecraft:air")) {
			Map<Integer, CompoundTag> sectionMap = new HashMap<>();
			List<Integer> heights = new ArrayList<>(26);
			for (CompoundTag section : sections) {
				sectionMap.put((int) section.getByte("Y"), section);
				heights.add((int) section.getByte("Y"));
			}

			for (int y = -4; y < 20; y++) {
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

		ListTag<CompoundTag> tileEntities = catchClassCastException(() -> level.getListTag("TileEntities").asCompoundTagList());
		if (tileEntities == null) {
			tileEntities = new ListTag<>(CompoundTag.class);
		}

		for (CompoundTag section : sections) {
			Tag<?> rawPalette = section.getListTag("Palette");
			if (rawPalette == null || rawPalette.getID() != ListTag.ID) {
				continue;
			}

			ListTag<CompoundTag> palette = catchClassCastException(((ListTag<?>) rawPalette)::asCompoundTagList);
			if (palette == null) {
				continue;
			}

			long[] blockStates = catchClassCastException(() -> section.getLongArray("BlockStates"));
			if (blockStates == null) {
				continue;
			}

			int y = section.getByte("Y");

			for (int i = 0; i < 4096; i++) {
				CompoundTag blockState = getBlockAt(i, blockStates, palette);

				BlockReplaceData replacement = replace.get(blockState.getString("Name"));
				if (replacement != null) {

					try {
						blockStates = setBlockAt(i, replacement.getState(), blockStates, palette);
					} catch (Exception ex) {
						throw new RuntimeException("failed to set block in section " + y, ex);
					}

					Point3i location = indexToLocation(i).add(pos.getX(), y * 16, pos.getZ());

					if (replacement.getTile() != null) {
						CompoundTag tile = replacement.getTile().clone();
						tile.putInt("x", location.getX());
						tile.putInt("y", location.getY());
						tile.putInt("z", location.getZ());
						tileEntities.add(tile);
					} else if (tileEntities.size() != 0) {
						for (int t = 0; t < tileEntities.size(); t++) {
							CompoundTag tile = tileEntities.get(t);
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
