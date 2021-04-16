package net.querz.mcaselector.filter;

import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.validation.ValidationHelper;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.Tag;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class StructureFilter extends TextFilter<List<String>> {

	private static final Map<String, String> validNames = new HashMap<>();

	static {
		try (BufferedReader bis = new BufferedReader(
				new InputStreamReader(Objects.requireNonNull(StructureFilter.class.getClassLoader().getResourceAsStream("mapping/all_structures.txt"))))) {
			String line;
			while ((line = bis.readLine()) != null) {
				validNames.put(line.toLowerCase(), line);
			}
		} catch (IOException ex) {
			Debug.dumpException("error reading mapping/all_structures.txt", ex);
		}
		System.out.println(validNames);
	}

	public StructureFilter() {
		this(Operator.AND, Comparator.CONTAINS, null);
	}

	private StructureFilter(Operator operator, Comparator comparator, List<String> value) {
		super(FilterType.STRUCTURES, operator, comparator, value);
		setRawValue(String.join(",", value == null ? new ArrayList<>(0) : value));
	}

	@Override
	public boolean contains(List<String> value, ChunkData data) {
		CompoundTag rawStructures = data.getRegion().getData().getCompoundTag("Level").getCompoundTag("Structures").getCompoundTag("References");
		for (String name : value) {
			Tag<?> structure = rawStructures.get(name);
			if (structure == null || structure.valueToString().equals("[]")) {
				structure = rawStructures.get(validNames.get(name));
				if (structure == null || structure.valueToString().equals("[]")) {
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public boolean containsNot(List<String> value, ChunkData data) {
		return !contains(value, data);
	}

	@Override
	public boolean intersects(List<String> value, ChunkData data) {
		CompoundTag rawStructures = data.getRegion().getData().getCompoundTag("Level").getCompoundTag("Structures").getCompoundTag("References");

		for (String name : getFilterValue()) {
			long[] references = ValidationHelper.withDefaultSilent(() -> rawStructures.getLongArray(name), null);
			if (references != null && references.length > 0) {
				return true;
			}
			references = ValidationHelper.withDefaultSilent(() -> rawStructures.getLongArray(validNames.get(name)), null);
			if (references != null && references.length > 0) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void setFilterValue(String raw) {
		String[] rawStructureNames = raw.replace(" ", "").split(",");
		if (raw.isEmpty() || rawStructureNames.length == 0) {
			setValid(false);
			setValue(null);
		} else {
			for (int i = 0; i < rawStructureNames.length; i++) {
				String name = rawStructureNames[i].toLowerCase();
				if (!validNames.containsKey(rawStructureNames[i]) && (!validNames.containsKey(name) || !validNames.get(name).equals(rawStructureNames[i]))) {
					if (name.startsWith("'") && name.endsWith("'") && name.length() >= 2 && !name.contains("\"")) {
						rawStructureNames[i] = name.substring(1, name.length() - 1);
						continue;
					}
					setValue(null);
					setValid(false);
					return;
				}
				rawStructureNames[i] = name;
			}
			setValid(true);
			setValue(Arrays.asList(rawStructureNames));
			setRawValue(raw);
		}
	}

	@Override
	public String getFormatText() {
		return "<structure>[,<structure>,...]";
	}

	@Override
	public String toString() {
		return "Structures " + getComparator().getQueryString() + " \"" + getRawValue() + "\"";
	}

	@Override
	public StructureFilter clone() {
	    return new StructureFilter(getOperator(), getComparator(), new ArrayList<>(value));
	}
}
