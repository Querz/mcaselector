package net.querz.mcaselector.filter.filters;

import net.querz.mcaselector.filter.*;
import net.querz.mcaselector.filter.Comparator;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.VersionHandler;
import net.querz.mcaselector.version.mapping.registry.BiomeRegistry;
import java.util.*;

public class BiomeFilter extends TextFilter<List<BiomeRegistry.BiomeIdentifier>> {

	public BiomeFilter() {
		this(Operator.AND, net.querz.mcaselector.filter.Comparator.CONTAINS, null);
	}

	private BiomeFilter(Operator operator, Comparator comparator, List<BiomeRegistry.BiomeIdentifier> value) {
		super(FilterType.BIOME, operator, comparator, value);
		if (value == null) {
			setRawValue("");
		} else {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < value.size(); i++) {
				if (i > 0) {
					sb.append(",");
				}
				sb.append(value.get(i));
			}
			setRawValue(sb.toString());
		}
	}

	@Override
	public String getFormatText() {
		return "<biome>[,<biome>,...]";
	}

	@Override
	public boolean contains(List<BiomeRegistry.BiomeIdentifier> value, ChunkData data) {
		return VersionHandler.getImpl(data, ChunkFilter.Biomes.class).matchBiomes(data, value);
	}

	@Override
	public boolean containsNot(List<BiomeRegistry.BiomeIdentifier> value, ChunkData data) {
		return !contains(value, data);
	}

	@Override
	public boolean intersects(List<BiomeRegistry.BiomeIdentifier> value, ChunkData data) {
		return VersionHandler.getImpl(data, ChunkFilter.Biomes.class).matchAnyBiome(data, value);
	}

	@Override
	public void setFilterValue(String raw) {
		String[] rawBiomeNames = raw.replace(" ", "").split(",");
		if (raw.isEmpty() || rawBiomeNames.length == 0) {
			setValid(false);
			setValue(null);
		} else {
			List<BiomeRegistry.BiomeIdentifier> nameList = new ArrayList<>();
			// name,id,'name','id'
			// make sure that name exists before converting to BiomeIdentifier
			// make sure that id exists before converting to BiomeIdentifier


			for (String name : rawBiomeNames) {
				boolean quoted = false;
				if (name.startsWith("'") && name.endsWith("'") && name.length() > 2) {
					name = name.substring(1, name.length() - 1);
					quoted = true;
				} else if (!name.matches("^[0-9]+$") && !name.startsWith("minecraft:")) {
					name = "minecraft:" + name;
				}

				if (name.matches("^[0-9]+$")) {
					try {
						int id = Integer.parseInt(name);
						if (quoted || BiomeRegistry.isValidID(id)) {
							nameList.add(new BiomeRegistry.BiomeIdentifier(id));
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
				} else if (quoted || BiomeRegistry.isValidName(name)) {
					nameList.add(new BiomeRegistry.BiomeIdentifier(name));
				} else {
					setValid(false);
					setValue(null);
					return;
				}
			}
			setValid(true);
			setValue(nameList);
			setRawValue(raw);
		}
	}

	@Override
	public String toString() {
		return "Biome " + getComparator().getQueryString() + " \"" + getRawValue() + "\"";
	}

	@Override
	public Filter<List<BiomeRegistry.BiomeIdentifier>> clone() {
		return new BiomeFilter(getOperator(), getComparator(), new ArrayList<>(value));
	}
}
