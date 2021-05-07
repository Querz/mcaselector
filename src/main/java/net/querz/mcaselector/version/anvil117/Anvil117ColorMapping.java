package net.querz.mcaselector.version.anvil117;

import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.text.TextHelper;
import net.querz.mcaselector.version.ColorMapping;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.StringTag;
import net.querz.nbt.tag.Tag;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import static net.querz.mcaselector.validation.ValidationHelper.withDefault;

public class Anvil117ColorMapping implements ColorMapping {

	//value can either be an Integer (color) or a BlockStateMapping
	private final Map<String, Object> mapping = new TreeMap<>();
	private final Set<String> grass = new HashSet<>();
	private final Set<String> foliage = new HashSet<>();
	private final int[] biomeGrassTints = new int[256];
	private final int[] biomeFoliageTints = new int[256];

	public Anvil117ColorMapping() {
		// note_block:pitch=1,powered=true,instrument=flute;01ab9f
		// noinspection ConstantConditions
		try (BufferedReader bis = new BufferedReader(
				new InputStreamReader(Anvil117ColorMapping.class.getClassLoader().getResourceAsStream("mapping/117/colors.txt")))) {
			String line;
			while ((line = bis.readLine()) != null) {
				String[] elements = line.split(";");
				if (elements.length < 2 || elements.length > 3) {
					Debug.dumpf("invalid line in color file: \"%s\"", line);
					continue;
				}
				String[] blockData = elements[0].split(":");
				if (blockData.length > 2) {
					Debug.dumpf("invalid line in color file: \"%s\"", line);
					continue;
				}
				Integer color = TextHelper.parseInt(elements[1], 16);
				if (color == null || color < 0x0 || color > 0xFFFFFF) {
					Debug.dumpf("invalid color code in color file: \"%s\"", elements[1]);
				}

				if (blockData.length == 1) {
					//default block color, set value to Integer color
					mapping.put("minecraft:" + blockData[0], color);
				} else {
					Anvil117ColorMapping.BlockStateMapping bsm;
					if (mapping.containsKey("minecraft:" + blockData[0])) {
						bsm = (Anvil117ColorMapping.BlockStateMapping) mapping.get("minecraft:" + blockData[0]);
					} else {
						bsm = new Anvil117ColorMapping.BlockStateMapping();
						mapping.put("minecraft:" + blockData[0], bsm);
					}
					Set<String> conditions = new HashSet<>(Arrays.asList(blockData[1].split(",")));
					bsm.blockStateMapping.put(conditions, color);
				}
				if (elements.length == 3) {
					switch (elements[2]) {
						case "g":
							grass.add("minecraft:" + blockData[0]);
							break;
						case "f":
							foliage.add("minecraft:" + blockData[0]);
							break;
						default:
							throw new RuntimeException("invalid grass / foliage type " + elements[2]);
					}
				}
			}
		} catch (IOException ex) {
			throw new RuntimeException("failed to read mapping/117/colors.txt");
		}

		try (BufferedReader bis = new BufferedReader(
				new InputStreamReader(Anvil117ColorMapping.class.getClassLoader().getResourceAsStream("mapping/all_biome_colors.txt")))) {

			String line;
			while ((line = bis.readLine()) != null) {
				String[] elements = line.split(";");
				if (elements.length != 3) {
					Debug.dumpf("invalid line in biome color file: \"%s\"", line);
					continue;
				}

				int biomeID = Integer.parseInt(elements[0]);
				int grassColor = Integer.parseInt(elements[1], 16);
				int foliageColor = Integer.parseInt(elements[2], 16);

				biomeGrassTints[biomeID] = grassColor;
				biomeFoliageTints[biomeID] = foliageColor;
			}

		} catch (IOException ex) {
			throw new RuntimeException("failed to read mapping/all_biome_colors.txt");
		}
	}

	@Override
	public int getRGB(Object o) {
		String name = withDefault(() -> ((CompoundTag) o).getString("Name"), "");
		Object value = mapping.get(name);

		if (value instanceof Integer) {
			if (grass.contains(name)) {
				return applyTint((int) value, biomeGrassTints[37]);
			} else if (foliage.contains(name)) {
				return applyTint((int) value, biomeFoliageTints[37]);
			}
			return (int) value;
		} else if (value instanceof Anvil117ColorMapping.BlockStateMapping) {
			int color = ((Anvil117ColorMapping.BlockStateMapping) value).getColor(withDefault(() -> ((CompoundTag) o).getCompoundTag("Properties"), null));
			if (grass.contains(name)) {
				return applyTint(color, biomeGrassTints[37]);
			} else if (foliage.contains(name)) {
				return applyTint(color, biomeFoliageTints[37]);
			}
		}
		return 0x000000;
	}

	private int applyTint(int color, int tint) {
		int nr = (tint >> 16 & 0xFF) * (color >> 16 & 0xFF) / 255;
		int ng = (tint >> 8 & 0xFF) * (color >> 8 & 0xFF) / 255;
		int nb = (tint & 0xFF) * (color & 0xFF) / 255;
		return nr << 16 | ng << 8 | nb;
	}

	private static class BlockStateMapping {

		private final Map<Set<String>, Integer> blockStateMapping = new HashMap<>();

		public int getColor(CompoundTag properties) {
			if (properties != null) {
				for (Map.Entry<String, Tag<?>> property : properties.entrySet()) {
					Map<Set<String>, Integer> clone = new HashMap<>(blockStateMapping);
					for (Map.Entry<Set<String>, Integer> blockState : blockStateMapping.entrySet()) {
						String value = property.getKey() + "=" + ((StringTag) property.getValue()).getValue();
						if (!blockState.getKey().contains(value)) {
							clone.remove(blockState.getKey());
						}
					}
					Iterator<Map.Entry<Set<String>, Integer>> it = clone.entrySet().iterator();
					if (it.hasNext()) {
						return it.next().getValue();
					}
				}
			}
			return 0x000000;
		}
	}
}
