package net.querz.mcaselector.version.anvil113;

import net.querz.mcaselector.version.ChunkFilter;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.ListTag;

import java.util.Arrays;

public class Anvil113ChunkFilter implements ChunkFilter {

	@Override
	public boolean matchBlockNames(CompoundTag data, String... names) {
		ListTag<CompoundTag> sections = data.getCompoundTag("Level").getListTag("Sections").asCompoundTagList();
		int c = 0;
		nameLoop:
		for (String name : names) {
			for (CompoundTag t : sections) {
				ListTag<?> genericPalette = t.getListTag("Palette");
				if (genericPalette == null) {
					continue;
				}
				ListTag<CompoundTag> palette = genericPalette.asCompoundTagList();
				for (CompoundTag p : palette) {
					if (p.getString("Name").equals("minecraft:" + name)) {
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
		if (!data.containsKey("Level") || !data.getCompoundTag("Level").containsKey("Biomes")) {
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
}
