package net.querz.mcaselector.version.anvil113;

import net.querz.mcaselector.version.ChunkFilter;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.ListTag;
import net.querz.nbt.tag.LongArrayTag;
import net.querz.nbt.tag.Tag;
import java.util.Arrays;
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
}
