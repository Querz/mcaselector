package net.querz.mcaselector.version.anvil113;

import net.querz.nbt.CompoundTag;
import net.querz.nbt.Tag;
import java.util.*;

public class BlockStateColorMapping {

	//format example:
	// note_block.pitch=1&powered=true&instrument=flute;01ab9f
	// properties will be sorted alphabetically and will only be found that way

	private Map<Object, Integer> mapping = new HashMap<>();
	private int defaultColor = 0x000000;

	public BlockStateColorMapping(int defaultColor) {
		this.defaultColor = defaultColor;
	}

	//instrument=flute&pitch=1&powered=true
	public void add(String properties, int color) {
		String[] p = properties.split("&");
		Arrays.sort(p);
		mapping.put(String.join("&", p), color);
	}

	public int getColor(CompoundTag tag) {
		String m;
		if ((m = tag.getString("mcaselector.colormapping")).isEmpty()) {
			Set<String> s = new TreeSet<>(String::compareTo);
			for (Tag t : tag.getValue().values()) {
				s.add(t.getName() + "=" + t.getValue());
			}
			m = String.join("&", s);
			tag.setString("mcaselector.colormapping", m);
		}
		return mapping.getOrDefault(m, defaultColor);
	}
}
