package net.querz.mcaselector.version.anvil113;

import net.querz.mcaselector.version.ChunkFilter;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.ListTag;
import java.util.Arrays;
import static net.querz.mcaselector.validation.ValidationHelper.*;

public class Anvil113ChunkFilter implements ChunkFilter {

	@Override
	public boolean matchBlockNames(CompoundTag data, String... names) {
		ListTag<CompoundTag> sections = withDefault(() -> data.getCompoundTag("Level").getListTag("Sections").asCompoundTagList(), null);
		if (sections == null) {
			return false;
		}
		int c = 0;
		nameLoop:
		for (String name : names) {
			for (CompoundTag t : sections) {
				ListTag<CompoundTag> palette = withDefault(() -> t.getListTag("Palette").asCompoundTagList(), null);
				if (palette == null) {
					continue;
				}
				for (CompoundTag p : palette) {
					if (("minecraft:" + name).equals(withDefault(() -> p.getString("Name"), null))) {
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
