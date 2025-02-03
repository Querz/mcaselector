package net.querz.mcaselector.version.java_1_9;

import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.version.ColorMapping;
import net.querz.mcaselector.version.MCVersionImplementation;
import java.util.*;
import java.util.stream.Stream;

@MCVersionImplementation(100)
public class ColorMapping_15w32a implements ColorMapping<Integer, Integer> {

	private static final Map<Integer, Integer> mapping = FileHelper.loadFromResource("mapping/java_1_9/colors.csv", r -> {
		Map<Integer, Integer> map = new HashMap<>();
		try (Stream<String> lines = r.lines()) {
			lines.forEach(line -> {
				String[] split = line.split(";");
				int id = Integer.parseInt(split[0]);
				int data = Integer.parseInt(split[1]);
				int color = Integer.parseInt(split[2], 16);
				map.put((id << 4) | data, color | 0xFF000000);
			});
		}
		return map;
	});

	private static final Set<Integer> grass = new HashSet<>();
	private static final int[] biomeGrassTints = new int[256];
	private static final int[] biomeFoliageTints = new int[256];
	private static final int[] biomeWaterTints = new int[256];

	static {
		grass.addAll(List.of(2, 31, 104, 105, 175));
		Arrays.fill(biomeGrassTints, DEFAULT_GRASS_TINT);
		Arrays.fill(biomeFoliageTints, DEFAULT_FOLIAGE_TINT);
		Arrays.fill(biomeWaterTints, DEFAULT_WATER_TINT);
		FileHelper.loadFromResource("mapping/java_1_9/biome_colors.csv", r -> {
			try (Stream<String> lines = r.lines()) {
				lines.forEach(line -> {
					String[] split = line.split(";");
					int id = Integer.parseInt(split[0]);
					int grassColor = Integer.parseInt(split[1], 16);
					int foliageColor = Integer.parseInt(split[2], 16);
					int waterColor = Integer.parseInt(split[3], 16);
					biomeGrassTints[id] = grassColor;
					biomeFoliageTints[id] = foliageColor;
					biomeWaterTints[id] = waterColor;
				});
			}
			return null;
		});
	}

	@Override
	public int getRGB(Integer block, Integer biome) {
		return applyBiomeTint(block >> 4, biome, mapping.getOrDefault(block, 0xFF000000));
	}

	@Override
	public boolean isFoliage(Integer block) {
		return block == 18 || block == 106 || block == 161;
	}

	@Override
	public boolean isTransparent(Integer block) {
		return block == 0 || block == 166 || block == 217;
	}

	@Override
	public boolean isWater(Integer block) {
		return block == 8 || block == 9;
	}

	@Override
	public boolean isWaterlogged(Integer block) {
		return false;
	}

	private int applyBiomeTint(int id, int biome, int color) {
		if (grass.contains(id)) {
			return net.querz.mcaselector.version.mapping.color.ColorMapping.applyTint(color, biomeGrassTints[biome]);
		} else if (isFoliage(id)) {
			return net.querz.mcaselector.version.mapping.color.ColorMapping.applyTint(color, biomeFoliageTints[biome]);
		} else if (isWater(id)) {
			return net.querz.mcaselector.version.mapping.color.ColorMapping.applyTint(color, biomeWaterTints[biome]);
		}
		return color;
	}



}
