package net.querz.mcaselector.version.anvil113;

import net.querz.mcaselector.ColorMapping;
import net.querz.nbt.CompoundTag;

public class Anvil113ColorMapping implements ColorMapping {

	@Override
	public int getRGB(Object o) {
		String name = ((CompoundTag) o).getString("Name");
		int i = name.indexOf(":");
		if (i != -1) {
			name = name.substring(i + 1);
		}
		switch (name) {
			case "flowing_water":
			case "water": return 0x0061ff;
			case "sand": return 0xffbf00;
		}
		return 0;
	}
}
