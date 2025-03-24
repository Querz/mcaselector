package net.querz.mcaselector.filter.filters;

import net.querz.mcaselector.filter.Comparator;
import net.querz.mcaselector.filter.FilterType;
import net.querz.mcaselector.filter.Operator;
import net.querz.mcaselector.filter.TextFilter;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.VersionHandler;
import net.querz.mcaselector.version.mapping.registry.EntityRegistry;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.ListTag;
import net.querz.nbt.Tag;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EntityFilter extends TextFilter<List<String>> {

	private static final Pattern entityNamePattern = Pattern.compile("^(?<space>[a-z_]*):?(?<id>[a-z_]*)$");

	public EntityFilter() {
		this(Operator.AND, Comparator.CONTAINS, null);
	}

	private EntityFilter(Operator operator, Comparator comparator, List<String> value) {
		super(FilterType.ENTITIES, operator, comparator, value);
		setRawValue(String.join(",", value == null ? new ArrayList<>(0) : value));
	}

	@Override
	public boolean contains(List<String> value, ChunkData data) {
		ListTag entities = VersionHandler.getImpl(data, ChunkFilter.Entities.class).getEntities(data);
		if (entities == null || entities.getType() == Tag.Type.LONG_ARRAY) {
			return false;
		}
		nameLoop:
		for (String name : getFilterValue()) {
			for (CompoundTag entity : entities.iterateType(CompoundTag.class)) {
				String id = entity.getString("id");
				if (name.equals(id)) {
					continue nameLoop;
				}
			}
			return false;
		}
		return true;
	}

	@Override
	public boolean intersects(List<String> value, ChunkData data) {
		ListTag entities = VersionHandler.getImpl(data, ChunkFilter.Entities.class).getEntities(data);
		if (entities == null || entities.getType() == Tag.Type.LONG_ARRAY) {
			return false;
		}
		for (String name : getFilterValue()) {
			for (CompoundTag entity : entities.iterateType(CompoundTag.class)) {
				String id = entity.getString("id");
				if (name.equals(id)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public boolean containsNot(List<String> value, ChunkData data) {
		return !contains(value, data);
	}

	@Override
	public void setFilterValue(String raw) {
		String[] rawEntityNames = raw.replace(" ", "").split(",");
		if (raw.isEmpty() || rawEntityNames.length == 0) {
			setValid(false);
			setValue(null);
		} else {
			for (int i = 0; i < rawEntityNames.length; i++) {
				String name = rawEntityNames[i];
				Matcher m = entityNamePattern.matcher(name);
				if (m.matches()) {
					if (m.group("id").isEmpty()) {
						name = "minecraft:" + m.group("space");
						rawEntityNames[i] = name;
					}
				}
				if (name.startsWith("'") && name.endsWith("'") && name.length() >= 2 && !name.contains("\"")) {
					rawEntityNames[i] = name.substring(1, name.length() - 1);
					continue;
				}
				if (!EntityRegistry.isValidName(name)) {
					setValue(null);
					setValid(false);
					return;
				}
			}
			setValid(true);
			setValue(Arrays.asList(rawEntityNames));
			setRawValue(raw);
		}
	}

	@Override
	public String getFormatText() {
		return "<entity>[,<entity>,...]";
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
