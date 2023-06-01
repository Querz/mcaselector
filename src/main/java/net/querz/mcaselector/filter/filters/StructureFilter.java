package net.querz.mcaselector.filter.filters;

import net.querz.mcaselector.filter.Comparator;
import net.querz.mcaselector.filter.FilterType;
import net.querz.mcaselector.filter.Operator;
import net.querz.mcaselector.filter.TextFilter;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.io.registry.StructureRegistry;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.VersionController;
import net.querz.nbt.*;
import java.util.*;

public class StructureFilter extends TextFilter<List<String>> {

	public StructureFilter() {
		this(Operator.AND, net.querz.mcaselector.filter.Comparator.CONTAINS, null);
	}

	private StructureFilter(Operator operator, Comparator comparator, List<String> value) {
		super(FilterType.STRUCTURES, operator, comparator, value);
		setRawValue(String.join(",", value == null ? new ArrayList<>(0) : value));
	}

	@Override
	public boolean contains(List<String> value, ChunkData data) {
		if (data.region() == null || data.region().getData() == null) {
			return false;
		}
		ChunkFilter chunkFilter = VersionController.getChunkFilter(data.getDataVersion());
		CompoundTag references = chunkFilter.getStructureReferences(data.region().getData());
		if (references == null) {
			return false;
		}

		main:
		for (String name : value) {
			for (String alt : StructureRegistry.getAlts(name)) {
				LongArrayTag longArrayStructure = references.getLongArrayTag(alt);
				if (longArrayStructure != null && !longArrayStructure.isEmpty()) {
					continue main;
				}
			}
			return false;
		}
		return true;
	}

	@Override
	public boolean containsNot(List<String> value, ChunkData data) {
		return !contains(value, data);
	}

	@Override
	public boolean intersects(List<String> value, ChunkData data) {
		if (data.region() == null || data.region().getData() == null) {
			return false;
		}
		ChunkFilter chunkFilter = VersionController.getChunkFilter(data.getDataVersion());
		CompoundTag references = chunkFilter.getStructureReferences(data.region().getData());
		if (references == null) {
			return false;
		}
		for (String name : getFilterValue()) {
			for (String alt : StructureRegistry.getAlts(name)) {
				LongArrayTag longArrayStructure = references.getLongArrayTag(alt);
				if (longArrayStructure != null && !longArrayStructure.isEmpty()) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public void setFilterValue(String raw) {
		// allow minecraft:name
		// allow name (if it's a valid name)
		// allow Name

		String[] values = raw.split(",");
		if (values.length == 0) {
			setValid(false);
			setValue(null);
			return;
		}

		for (int i = 0; i < values.length; i++) {
			String name = values[i] = values[i].trim();

			// allow custom structure names
			if (name.startsWith("'") && name.endsWith("'") && name.length() >= 2) {
				values[i] = name.substring(1, name.length() - 1);
				continue;
			}

			if (!StructureRegistry.isValidName(name)) {
				setValue(null);
				setValid(false);
				return;
			}
		}

		setValid(true);
		setValue(Arrays.asList(values));
		setRawValue(raw);
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
