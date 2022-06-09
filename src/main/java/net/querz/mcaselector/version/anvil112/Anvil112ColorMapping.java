package net.querz.mcaselector.version.anvil112;

import net.querz.mcaselector.text.TextHelper;
import net.querz.mcaselector.version.ColorMapping;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.*;
import java.util.*;

public class Anvil112ColorMapping implements ColorMapping {

	private static final Logger LOGGER = LogManager.getLogger(Anvil112ColorMapping.class);

	private final Map<Integer, Integer> mapping = new HashMap<>();
	private final Set<Integer> grass = new HashSet<>();
	private final Set<Integer> foliage = new HashSet<>();

	private final int[] biomeGrassTints = new int[256];
	private final int[] biomeFoliageTints = new int[256];
	private final int[] biomeWaterTints = new int[256];

	public Anvil112ColorMapping() {
		try (BufferedReader bis = new BufferedReader(
				new InputStreamReader(Objects.requireNonNull(Anvil112ColorMapping.class.getClassLoader().getResourceAsStream("mapping/112/colors.txt"))))) {
			String line;
			while ((line = bis.readLine()) != null) {
				String[] elements = line.split(";");
				if (elements.length < 3 || elements.length > 4) {
					LOGGER.error("invalid line in color file: \"{}\"", line);
					continue;
				}
				Integer id = TextHelper.parseInt(elements[0], 10);
				if (id == null || id < 0 || id > 255) {
					LOGGER.error("invalid block id in color file: \"{}\"", elements[0]);
					continue;
				}
				Integer data = TextHelper.parseInt(elements[1], 10);
				if (data == null || data < 0 || data > 15) {
					LOGGER.error("invalid block data in color file: \"{}\"", elements[1]);
					continue;
				}
				Integer color = TextHelper.parseInt(elements[2], 16);
				if (color == null || color < 0x0 || color > 0xFFFFFF) {
					LOGGER.error("invalid color code in color file: \"{}\"", elements[2]);
					continue;
				}
				mapping.put((id << 4) + data, color | 0xFF000000);

				if (elements.length == 4) {
					switch (elements[3]) {
						case "g" -> grass.add(id);
						case "f" -> foliage.add(id);
						default -> throw new RuntimeException("invalid grass / foliage type " + elements[3]);
					}
				}
			}
		} catch (IOException ex) {
			throw new RuntimeException("failed to read mapping/112/colors.txt");
		}

		Arrays.fill(biomeGrassTints, DEFAULT_GRASS_TINT);
		Arrays.fill(biomeFoliageTints, DEFAULT_FOLIAGE_TINT);
		Arrays.fill(biomeWaterTints, DEFAULT_WATER_TINT);

		try (BufferedReader bis = new BufferedReader(
			new InputStreamReader(Objects.requireNonNull(ColorMapping.class.getClassLoader().getResourceAsStream("mapping/112/biome_colors.txt"))))) {

			String line;
			while ((line = bis.readLine()) != null) {
				String[] elements = line.split(";");
				if (elements.length != 4) {
					LOGGER.error("invalid line in biome color file: \"{}\"", line);
					continue;
				}

				int biomeID = Integer.parseInt(elements[0]);
				int grassColor = Integer.parseInt(elements[1], 16);
				int foliageColor = Integer.parseInt(elements[2], 16);
				int waterColor = Integer.parseInt(elements[3], 16);

				biomeGrassTints[biomeID] = grassColor;
				biomeFoliageTints[biomeID] = foliageColor;
				biomeWaterTints[biomeID] = waterColor;
			}
		} catch (IOException ex) {
			throw new RuntimeException("failed to read mapping/112/biome_colors.txt");
		}
	}

	@Override
	public int getRGB(Object blockID, int biome) {
		return applyBiomeTint((int) blockID >> 4, biome, mapping.getOrDefault((int) blockID, 0xFF000000));
	}

	@Override
	public int getRGB(Object o, String biome) {
		throw new UnsupportedOperationException("color mapping for 1.12 does not support biome names");
	}

	@Override
	public boolean isFoliage(Object id) {
		return switch ((int) id) {
			case 18, 106, 161 -> true;
			default -> false;
		};
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
