package net.querz.mcaselector.filter;

import net.querz.mcaselector.debug.Debug;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.ListTag;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class EntityFilter extends TextFilter<List<String>> {

	private static Set<String> validNames = new HashSet<>();

	static {
		try (BufferedReader bis = new BufferedReader(
				new InputStreamReader(Objects.requireNonNull(EntityFilter.class.getClassLoader().getResourceAsStream("entity-names.csv"))))) {
			String line;
			while ((line = bis.readLine()) != null) {
				validNames.add(line);
			}
		} catch (IOException ex) {
			Debug.error("error reading entity-names.csv: ", ex.getMessage());
		}
	}

	public EntityFilter() {
		this(Operator.AND, Comparator.CONTAINS, null);
	}

	private EntityFilter(Operator operator, Comparator comparator, List<String> value) {
		super(FilterType.ENTITIES, operator, comparator, value);
		setRawValue(String.join(",", value == null ? new ArrayList<>(0) : value));
	}

	@Override
	public boolean contains(List<String> value, FilterData data) {
		ListTag<CompoundTag> entities = data
				.getChunk()
				.getCompoundTag("Level")
				.getListTag("Entities")
				.asCompoundTagList();
		nameLoop: for (String name : getFilterValue()) {
			for (CompoundTag entity : entities) {
				String id = entity.getString("id");
				// cut away the minecraft: part
				if (id.length() > 10 && name.equals(id.substring(10))) {
					continue nameLoop;
				}
			}
			return false;
		}
		return true;
	}

	@Override
	public boolean containsNot(List<String> value, FilterData data) {
		return !contains(value, data);
	}

	@Override
	public void setFilterValue(String raw) {
		String[] rawBlockNames = raw.replace(" ", "").split(",");
		if (raw.isEmpty() || rawBlockNames.length == 0) {
			setValid(false);
			setValue(null);
		} else {
			for (int i = 0; i < rawBlockNames.length; i++) {
				String name = rawBlockNames[i];
				if (!validNames.contains(name)) {
					if (name.startsWith("'") && name.endsWith("'") && name.length() >= 2 && !name.contains("\"")) {
						rawBlockNames[i] = name.substring(1, name.length() - 1);
						continue;
					}
					setValue(null);
					setValid(false);
					return;
				}
			}
			setValid(true);
			setValue(Arrays.asList(rawBlockNames));
			setRawValue(raw);
		}
	}

	@Override
	public String getFormatText() {
		return "<entity>[,<entity>,...]";
	}

	@Override
	public String toString(FilterData data) {
		return "EntityFilter " + getComparator().getQueryString() + " <data>";
	}

	@Override
	public String toString() {
		return "Entities " + getComparator().getQueryString() + " \"" + getRawValue() + "\"";
	}

	@Override
	public EntityFilter clone() {
		return new EntityFilter(getOperator(), getComparator(), new ArrayList<>(value));
	}
}
