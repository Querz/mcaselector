package net.querz.mcaselector.version.anvil112;

import net.querz.mcaselector.text.TextHelper;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.version.ColorMapping;
import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Anvil112ColorMapping implements ColorMapping {

	private final Map<Integer, Integer> mapping = new HashMap<>();
	private final Set<Integer> grass = new HashSet<>();
	private final Set<Integer> foliage = new HashSet<>();

	public Anvil112ColorMapping() {
		// noinspection ConstantConditions
		try (BufferedReader bis = new BufferedReader(
				new InputStreamReader(Anvil112ColorMapping.class.getClassLoader().getResourceAsStream("mapping/112/colors.txt")))) {
			String line;
			while ((line = bis.readLine()) != null) {
				String[] elements = line.split(";");
				if (elements.length < 3 || elements.length > 4) {
					Debug.dumpf("invalid line in color file: \"%s\"", line);
					continue;
				}
				Integer id = TextHelper.parseInt(elements[0], 10);
				if (id == null || id < 0 || id > 255) {
					Debug.dumpf("invalid block id in color file: \"%s\"", elements[0]);
					continue;
				}
				Integer data = TextHelper.parseInt(elements[1], 10);
				if (data == null || data < 0 || data > 15) {
					Debug.dumpf("invalid block data in color file: \"%s\"", elements[1]);
					continue;
				}
				Integer color = TextHelper.parseInt(elements[2], 16);
				if (color == null || color < 0x0 || color > 0xFFFFFF) {
					Debug.dumpf("invalid color code in color file: \"%s\"", elements[2]);
				}
				mapping.put((id << 4) + data, color);

				if (elements.length == 4) {
					switch (elements[3]) {
						case "g":
							grass.add(id);
							break;
						case "f":
							foliage.add(id);
							break;
						default:
							throw new RuntimeException("invalid grass / foliage type " + elements[3]);
					}
				}
			}
		} catch (IOException ex) {
			throw new RuntimeException("failed to read mapping/112/colors.txt");
		}
	}

	@Override
	public int getRGB(Object blockID, int biome) {
		return applyBiomeTint((int) blockID >> 4, biome, mapping.getOrDefault((int) blockID, 0x000000));
	}

	private int applyBiomeTint(int id, int biome, int color) {
		if (grass.contains(id)) {
			return applyTint(color, biomeGrassTints[biome]);
		} else if (foliage.contains(id)) {
			return applyTint(color, biomeFoliageTints[biome]);
		} else if (id == 8 || id == 9) {
			return applyTint(color, biomeWaterTints[biome]);
		}
		return color;
	}
}
