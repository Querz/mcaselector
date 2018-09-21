package net.querz.mcaselector.version.anvil113;

import net.querz.mcaselector.version.ChunkFilter;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.ListTag;

public class Anvil113ChunkFilter implements ChunkFilter {

	@Override
	public boolean matchBlockNames(CompoundTag data, String... names) {
		ListTag<CompoundTag> sections = data.getCompoundTag("Level").getListTag("Sections").asCompoundTagList();
		int c = 0;
		nameLoop:
		for (String name : names) {
			for (CompoundTag t : sections) {
				ListTag<CompoundTag> palette = t.getListTag("Palette").asCompoundTagList();
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
}
