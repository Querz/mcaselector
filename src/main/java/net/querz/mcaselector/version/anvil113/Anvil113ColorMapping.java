package net.querz.mcaselector.version.anvil113;

import net.querz.mcaselector.text.TextHelper;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.version.ColorMapping;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.StringTag;
import net.querz.nbt.tag.Tag;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import static net.querz.mcaselector.validation.ValidationHelper.*;

public class Anvil113ColorMapping implements ColorMapping {

	//value can either be an Integer (color) or a BlockStateMapping
	private Map<String, Object> mapping = new TreeMap<>();

	public Anvil113ColorMapping() {
		// note_block:pitch=1,powered=true,instrument=flute;01ab9f
		// noinspection ConstantConditions
		try (BufferedReader bis = new BufferedReader(
				new InputStreamReader(Anvil113ColorMapping.class.getClassLoader().getResourceAsStream("colors113.csv")))) {
			String line;
			while ((line = bis.readLine()) != null) {
				String[] elements = line.split(";");
				if (elements.length != 2) {
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
					Debug.dumpf("Invalid color code in color file: \"%s\"", elements[1]);
				}

				if (blockData.length == 1) {
					//default block color, set value to Integer color
					mapping.put("minecraft:" + blockData[0], color);
				} else {
					BlockStateMapping bsm;
					if (mapping.containsKey("minecraft:" + blockData[0])) {
						bsm = (BlockStateMapping) mapping.get("minecraft:" + blockData[0]);
					} else {
						bsm = new BlockStateMapping();
						mapping.put("minecraft:" + blockData[0], bsm);
					}
					Set<String> conditions = new HashSet<>(Arrays.asList(blockData[1].split(",")));
					bsm.blockStateMapping.put(conditions, color);
				}
			}
		} catch (IOException ex) {
			throw new RuntimeException("Unable to open color file");
		}
	}

	@Override
	public int getRGB(Object o) {
		if (isWaterlogged((CompoundTag) o)) {
			return (int) mapping.get("minecraft:water");
		}
		Object value = mapping.get(withDefault(() -> ((CompoundTag) o).getString("Name"), ""));
		if (value instanceof Integer) {
			return (int) value;
		} else if (value instanceof BlockStateMapping) {
			return ((BlockStateMapping) value).getColor(withDefault(() -> ((CompoundTag) o).getCompoundTag("Properties"), null));
		}
		return 0x000000;
	}

	private boolean isWaterlogged(CompoundTag data) {
		return data.get("Properties") != null && "true".equals(withDefault(() -> data.getCompoundTag("Properties").getString("waterlogged"), null));
	}

	private static class BlockStateMapping {

		private Map<Set<String>, Integer> blockStateMapping = new HashMap<>();

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
