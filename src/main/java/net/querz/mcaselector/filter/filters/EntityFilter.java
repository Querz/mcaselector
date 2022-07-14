package net.querz.mcaselector.filter.filters;

import net.querz.mca.parsers.EntityParser;
import net.querz.mcaselector.filter.Comparator;
import net.querz.mcaselector.filter.FilterType;
import net.querz.mcaselector.filter.Operator;
import net.querz.mcaselector.filter.TextFilter;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.version.VersionController;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.ListTag;
import net.querz.nbt.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EntityFilter extends TextFilter<List<String>> {

	private static final Logger LOGGER = LogManager.getLogger(EntityParser.class);

	private static final Set<String> validNames = new HashSet<>();
	private static final Pattern entityNamePattern = Pattern.compile("^(?<space>[a-z_]*):?(?<id>[a-z_]*)$");

	static {
		try (BufferedReader bis = new BufferedReader(
				new InputStreamReader(Objects.requireNonNull(EntityFilter.class.getClassLoader().getResourceAsStream("mapping/all_entity_names.txt"))))) {
			String line;
			while ((line = bis.readLine()) != null) {
				validNames.add("minecraft:" + line);
			}
		} catch (IOException ex) {
			LOGGER.error("error reading mapping/all_entity_names.txt", ex);
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
	public boolean contains(List<String> value, ChunkData data) {
		ListTag entities = VersionController.getEntityFilter(data.getDataVersion()).getEntities(data);
		if (entities == null || entities.getID() == Tag.LONG_ARRAY) {
			return false;
		}
		nameLoop:
		for (String name : getFilterValue()) {
			for (CompoundTag entity : entities.iterateType(CompoundTag.TYPE)) {
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
		if (data.region() == null || data.region().getData() == null) {
			return false;
		}
		ListTag entities = VersionController.getEntityFilter(data.getDataVersion()).getEntities(data);
		if (entities == null || entities.getID() == Tag.LONG_ARRAY) {
			return false;
		}
		for (String name : getFilterValue()) {
			for (CompoundTag entity : entities.iterateType(CompoundTag.TYPE)) {
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

				if (!validNames.contains(name)) {
					if (name.startsWith("'") && name.endsWith("'") && name.length() >= 2 && !name.contains("\"")) {
						rawEntityNames[i] = name.substring(1, name.length() - 1);
						continue;
					}
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
