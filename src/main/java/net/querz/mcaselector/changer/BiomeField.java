package net.querz.mcaselector.changer;

import net.querz.mcaselector.filter.BiomeFilter;
import net.querz.mcaselector.util.Debug;
import net.querz.mcaselector.util.Helper;
import net.querz.mcaselector.version.VersionController;
import net.querz.nbt.CompoundTag;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class BiomeField extends Field<Integer> {

	private static Map<String, Integer> validNames = new HashMap<>();

	static {
		try (BufferedReader bis = new BufferedReader(
			new InputStreamReader(BiomeFilter.class.getClassLoader().getResourceAsStream("biomes.csv")))) {
			String line;
			while ((line = bis.readLine()) != null) {
				String[] split = line.split(";");
				if (split.length != 2) {
					Debug.dumpf("invalid biome mapping: %s", line);
					continue;
				}
				Integer id = Helper.parseInt(split[1], 10);
				if (id == null) {
					Debug.dumpf("invalid biome id: %s", line);
					continue;
				}
				validNames.put(split[0], id);
			}
		} catch (IOException ex) {
			Debug.error("error reading biomes.csv: ", ex.getMessage());
		}
	}

	public BiomeField() {
		super(FieldType.BIOME);
	}

	@Override
	public boolean parseNewValue(String s) {
		String low = s.toLowerCase();
		if (validNames.containsKey(low)) {
			setNewValue(validNames.get(low));
			return true;
		}
		return super.parseNewValue(s);
	}

	@Override
	public void change(CompoundTag root) {
		VersionController.getChunkFilter(root.getInt("DataVersion")).changeBiome(root, getNewValue());
	}

	@Override
	public void force(CompoundTag root) {
		change(root);
	}
}
