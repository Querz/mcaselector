package net.querz.mcaselector.filter;

import net.querz.mcaselector.util.Debug;
import net.querz.mcaselector.util.Helper;
import net.querz.mcaselector.version.VersionController;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BiomeFilter extends TextFilter<List<Integer>> {

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

	public BiomeFilter() {
		this(Operator.AND, Comparator.CONTAINS, null);
	}

	public BiomeFilter(Operator operator, Comparator comparator, List<Integer> value) {
		super(FilterType.BIOME, operator, comparator, value);
		if (value == null) {
			setRawValue("");
		} else {
			StringBuilder sb = new StringBuilder();
			boolean first = true;
			for (int id : value) {
				sb.append(first ? "" : ",");
				for (Map.Entry<String, Integer> entry : validNames.entrySet()) {
					if (entry.getValue() == id) {
						sb.append(entry.getKey());
						break;
					}
				}
				first = false;
			}
			setRawValue(sb.toString());
		}
	}

	@Override
	public String getFormatText() {
		return "<biome>[,<biome>,...]";
	}

	@Override
	public boolean contains(List<Integer> value, FilterData data) {
		return VersionController.getChunkFilter(data.getChunk().getInt("DataVersion")).matchBiomeIDs(data.getChunk(), value.stream().mapToInt(i->i).toArray());
	}

	@Override
	public boolean containsNot(List<Integer> value, FilterData data) {
		return !contains(value, data);
	}

	@Override
	public void setFilterValue(String raw) {
		String[] rawBiomeNames = raw.replace(" ", "").split(",");
		if (raw.isEmpty() || rawBiomeNames.length == 0) {
			setValid(false);
			setValue(null);
		} else {
			List<Integer> idList = new ArrayList<>();
			for (int i = 0; i < rawBiomeNames.length; i++) {
				String name = rawBiomeNames[i];
				if (!validNames.containsKey(name)) {
					setValid(false);
					setValue(null);
					return;
				}
				if (validNames.containsKey(name)) {
					idList.add(validNames.get(name));
				} else {
					setValid(false);
					setValue(null);
					return;
				}
			}
			setValid(true);
			setValue(idList);
			setRawValue(raw);
		}
	}

	@Override
	public String toString(FilterData data) {
		return "BiomeFilter " + getComparator() + " <data>";
	}

	@Override
	public String toString() {
		return "Biome " + getComparator() + " " + (getFilterValue() != null ? Arrays.toString(getFilterValue().toArray()) : "null");
	}

	@Override
	public Filter<List<Integer>> clone() {
		return new BiomeFilter(getOperator(), getComparator(), new ArrayList<>(value));
	}
}
