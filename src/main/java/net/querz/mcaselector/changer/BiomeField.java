package net.querz.mcaselector.changer;

import net.querz.mcaselector.filter.BiomeFilter;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.text.TextHelper;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.version.VersionController;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class BiomeField extends Field<Integer> {

	private static final Map<String, Integer> validNames = new HashMap<>();
	private static final Set<Integer> validIDs = new HashSet<>();
	private String name;

	static {
		try (BufferedReader bis = new BufferedReader(
			new InputStreamReader(Objects.requireNonNull(BiomeFilter.class.getClassLoader().getResourceAsStream("mapping/all_biomes.txt"))))) {
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
				validIDs.add(id);
			}
		} catch (IOException ex) {
			Debug.dumpException("error reading mapping/all_biomes.txt for BiomeField", ex);
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

		boolean quoted = false;
		if (low.startsWith("'") && low.endsWith("'") && low.length() > 1) {
			low = low.substring(1, low.length() - 1);
			quoted = true;
		}

		if (low.matches("^[0-9]+$")) {
			try {
				int id = Integer.parseInt(low);
				if (quoted || validIDs.contains(id)) {
					setNewValue(id);
					name = s;
					return true;
				}
			} catch (NumberFormatException ex) {
				// do nothing
			}
		} else if (low.equals("-1")) {
			setNewValue(-1);
			name = s;
			return true;
		}
		return super.parseNewValue(s);
	}

	@Override
	public Integer getOldValue(ChunkData data) {
		return null;
	}

	@Override
	public void change(ChunkData data) {
		VersionController.getChunkFilter(data.getRegion().getData().getInt("DataVersion")).changeBiome(data.getRegion().getData(), getNewValue());
	}

	@Override
	public void force(ChunkData data) {
		VersionController.getChunkFilter(data.getRegion().getData().getInt("DataVersion")).forceBiome(data.getRegion().getData(), getNewValue());
	}
}
