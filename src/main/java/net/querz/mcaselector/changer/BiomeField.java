package net.querz.mcaselector.changer;

import net.querz.mcaselector.filter.BiomeFilter;
import net.querz.mcaselector.text.TextHelper;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.version.VersionController;
import net.querz.nbt.tag.CompoundTag;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class BiomeField extends Field<Integer> {

	private static Map<String, Integer> validNames = new HashMap<>();
	private String name;

	static {
		try (BufferedReader bis = new BufferedReader(
			new InputStreamReader(Objects.requireNonNull(BiomeFilter.class.getClassLoader().getResourceAsStream("biomes.csv"))))) {
			String line;
			while ((line = bis.readLine()) != null) {
				String[] split = line.split(";");
				if (split.length != 2) {
					Debug.dumpf("invalid biome mapping: %s", line);
					continue;
				}
				Integer id = TextHelper.parseInt(split[1], 10);
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
	public String toString() {
		return "Biome = " + name;
	}

	@Override
	public boolean parseNewValue(String s) {
		String low = s.toLowerCase();
		if (validNames.containsKey(low)) {
			setNewValue(validNames.get(low));
			name = low;
			return true;
		}
		return super.parseNewValue(s);
	}

	@Override
	public Integer getOldValue(CompoundTag root) {
		return null;
	}

	@Override
	public void change(CompoundTag root) {
		VersionController.getChunkFilter(root.getInt("DataVersion")).changeBiome(root, getNewValue());
	}

	@Override
	public void force(CompoundTag root) {
		VersionController.getChunkFilter(root.getInt("DataVersion")).forceBiome(root, getNewValue());
	}
}
