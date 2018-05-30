package net.querz.mcaselector.version.anvil113;

import net.querz.mcaselector.ColorMapping;
import net.querz.nbt.CompoundTag;
import java.util.HashMap;
import java.util.Map;

public class Anvil113ColorMapping implements ColorMapping {

	private Map<String, BlockStateColorMapping> mapping = new HashMap<>();

	public Anvil113ColorMapping() {
		// note_block.pitch=1&powered=true&instrument=flute;01ab9f



	}

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
