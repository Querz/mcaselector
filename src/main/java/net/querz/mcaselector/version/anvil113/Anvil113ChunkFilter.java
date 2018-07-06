package net.querz.mcaselector.version.anvil113;

import net.querz.mcaselector.version.ChunkFilter;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.ListTag;
import net.querz.nbt.Tag;

public class Anvil113ChunkFilter implements ChunkFilter {

	@Override
	public boolean matchBlockNames(CompoundTag data, String... names) {
		ListTag sections = (ListTag) ((CompoundTag) data.get("Level")).get("Sections");
		for (Tag t : sections.getValue()) {
			ListTag palette = (ListTag) ((CompoundTag) t).get("Palette");
			for (Tag p : palette.getValue()) {
				for (String name : names) {
					if (((CompoundTag) p).getString("Name").equals(name)) {
						return true;
					}
				}
			}
		}
		return false;
	}
}
