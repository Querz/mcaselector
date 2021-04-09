package net.querz.mcaselector.filter;

import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.text.TextHelper;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.version.VersionController;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class BiomeFilter extends TextFilter<List<Integer>> {

	private static final Map<String, Integer> validNames = new HashMap<>();
	private static final Set<Integer> validIDs = new HashSet<>();

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
			Debug.dumpException("error reading mapping/all_biomes.txt for BiomeFilter", ex);
		}
	}

	public BiomeFilter() {
		this(Operator.AND, Comparator.CONTAINS, null);
	}

	private BiomeFilter(Operator operator, Comparator comparator, List<Integer> value) {
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
	public boolean contains(List<Integer> value, ChunkData data) {
		if (data.getRegion() == null) {
			return false;
		}
		return VersionController.getChunkFilter(data.getRegion().getData().getInt("DataVersion"))
				.matchBiomeIDs(data.getRegion().getData(), value);
	}

	@Override
	public boolean containsNot(List<Integer> value, ChunkData data) {
		return !contains(value, data);
	}

	@Override
	public boolean intersects(List<Integer> value, ChunkData data) {
		if (data.getRegion() == null) {
			return false;
		}
		return VersionController.getChunkFilter(data.getRegion().getData().getInt("DataVersion"))
				.matchAnyBiomeID(data.getRegion().getData(), value);
	}

	@Override
	public void setFilterValue(String raw) {
		String[] rawBiomeNames = raw.replace(" ", "").split(",");
		if (raw.isEmpty() || rawBiomeNames.length == 0) {
			setValid(false);
			setValue(null);
		} else {
			List<Integer> idList = new ArrayList<>();
			for (String name : rawBiomeNames) {
				boolean quoted = false;
				if (name.startsWith("'") && name.endsWith("'") && name.length() > 1) {
					name = name.substring(1, name.length() - 1);
					quoted = true;
				}

				if (name.matches("^[0-9]+$")) {
					try {
						int id = Integer.parseInt(name);
						if (quoted || validIDs.contains(id)) {
							idList.add(id);
						} else {
							setValid(false);
							setValue(null);
							return;
						}
					} catch (NumberFormatException ex) {
						setValid(false);
						setValue(null);
						return;
					}
				} else if (validNames.containsKey(name)) {
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
	public String toString() {
		return "Biome " + getComparator().getQueryString() + " \"" + getRawValue() + "\"";
	}

	@Override
	public Filter<List<Integer>> clone() {
		return new BiomeFilter(getOperator(), getComparator(), new ArrayList<>(value));
	}
}
