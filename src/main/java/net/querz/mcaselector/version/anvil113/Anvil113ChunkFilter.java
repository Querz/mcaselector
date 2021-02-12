package net.querz.mcaselector.version.anvil113;

import net.querz.mcaselector.version.ChunkFilter;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.ListTag;
import net.querz.nbt.tag.LongArrayTag;
import net.querz.nbt.tag.Tag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static net.querz.mcaselector.validation.ValidationHelper.*;

public class Anvil113ChunkFilter implements ChunkFilter {

	@Override
	public boolean matchBlockNames(CompoundTag data, String... names) {
		CompoundTag level = withDefault(() -> data.getCompoundTag("Level"), null);
		if (level == null) {
			return false;
		}
		Tag<?> rawSections = level.get("Sections");
		if (rawSections == null || rawSections.getID() == LongArrayTag.ID) {
			return false;
		}
		ListTag<CompoundTag> sections = catchClassCastException(((ListTag<?>) rawSections)::asCompoundTagList);
		if (sections == null) {
			return false;
		}
		int c = 0;
		nameLoop:
		for (String name : names) {
			for (CompoundTag t : sections) {
				ListTag<?> rawPalette = withDefault(() -> t.getListTag("Palette"), null);
				if (rawPalette == null) {
					continue;
				}
				ListTag<CompoundTag> palette = catchClassCastException(rawPalette::asCompoundTagList);
				if (palette == null) {
					continue;
				}
				for (CompoundTag p : palette) {
					if (name.equals(withDefault(() -> p.getString("Name"), null))) {
						c++;
						continue nameLoop;
					}
				}
			}
		}
		return names.length == c;
	}

	@Override
	public boolean matchBiomeIDs(CompoundTag data, int... ids) {
		if (!data.containsKey("Level") || withDefault(() -> data.getCompoundTag("Level").getIntArrayTag("Biomes"), null) == null) {
			return false;
		}

		filterLoop: for (int filterID : ids) {
			for (int dataID : data.getCompoundTag("Level").getIntArray("Biomes")) {
				if (filterID == dataID) {
					continue filterLoop;
				}
			}
			return false;
		}
		return true;
	}

	@Override
	public void changeBiome(CompoundTag data, int id) {
		if (!data.containsKey("Level") || withDefault(() -> data.getCompoundTag("Level").getIntArrayTag("Biomes"), null) == null) {
			return;
		}
		Arrays.fill(data.getCompoundTag("Level").getIntArray("Biomes"), id);
	}

	@Override
	public void forceBiome(CompoundTag data, int id) {
		if (data.containsKey("Level")) {
			int[] biomes = new int[256];
			Arrays.fill(biomes, id);
			data.getCompoundTag("Level").putIntArray("Biomes", biomes);
		}
	}

	@Override
	public void replaceBlocks(CompoundTag data, Map<String, String> replace) {
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

		// handle the special case when someone wants to replace air with something else
		if (replace.containsKey("minecraft:air")) {
			Map<Integer, CompoundTag> sectionMap = new HashMap<>();
			List<Integer> heights = new ArrayList<>(18);
			for (CompoundTag section : sections) {
				sectionMap.put((int) section.getByte("Y"), section);
				heights.add((int) section.getByte("Y"));
			}

			for (int y = 0; y < 16; y++) {
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


		for (CompoundTag section : sections) {
			Tag<?> rawPalette = section.getListTag("Palette");
			if (rawPalette == null || rawPalette.getID() != ListTag.ID) {
				continue;
			}

			ListTag<CompoundTag> palette = catchClassCastException(((ListTag<?>) rawPalette)::asCompoundTagList);
			if (palette == null) {
				continue;
			}

			for (int i = 0; i < palette.size(); i++) {
				CompoundTag blockState = palette.get(i);
				String rep = replace.get(withDefault(() -> blockState.getString("Name"), null));
				if (rep == null) {
					continue;
				}

				CompoundTag newBlockState = new CompoundTag();
				newBlockState.putString("Name", rep);
				palette.set(i, newBlockState);
			}
		}

		// delete tile entities with that name
		ListTag<CompoundTag> tileEntities = catchClassCastException(() -> level.getListTag("TileEntities").asCompoundTagList());
		if (tileEntities != null) {
			for (int i = 0; i < tileEntities.size(); i++) {
				CompoundTag tileEntity = tileEntities.get(i);
				String id = catchClassCastException(() -> tileEntity.getString("id"));
				if (replace.containsKey(id)) {
					tileEntities.remove(i);
					i--;
				}
			}
		}
	}

	protected CompoundTag createEmptySection(int y) {
		CompoundTag newSection = new CompoundTag();
		newSection.putByte("Y", (byte) y);
		newSection.putLongArray("BlockStates", new long[256]);
		ListTag<CompoundTag> newPalette = new ListTag<>(CompoundTag.class);
		CompoundTag newBlockState = new CompoundTag();
		newBlockState.putString("Name", "minecraft:air");
		newPalette.add(newBlockState);
		newSection.put("Palette", newPalette);
		return newSection;
	}
}
