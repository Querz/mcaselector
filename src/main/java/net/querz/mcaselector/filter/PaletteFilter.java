package net.querz.mcaselector.filter;

import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.version.VersionController;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class PaletteFilter extends TextFilter<List<String>> {

	private static final Set<String> validNames = new HashSet<>();

	static {
		try (BufferedReader bis = new BufferedReader(
				new InputStreamReader(Objects.requireNonNull(PaletteFilter.class.getClassLoader().getResourceAsStream("mapping/all_block_names.txt"))))) {
			String line;
			while ((line = bis.readLine()) != null) {
				validNames.add(line);
			}
		} catch (IOException ex) {
			Debug.dumpException("error reading mapping/all_block_names.txt", ex);
		}
	}

	public PaletteFilter() {
		this(Operator.AND, Comparator.CONTAINS, null);
	}

	private PaletteFilter(Operator operator, Comparator comparator, List<String> value) {
		super(FilterType.PALETTE, operator, comparator, value);
		setRawValue(String.join(",", value == null ? new ArrayList<>(0) : value));
	}

	@Override
	public boolean contains(List<String> value, FilterData data) {
		if (data.getRegion() == null) {
			return false;
		}
		return VersionController.getChunkFilter(data.getRegion().getData().getInt("DataVersion"))
				.matchBlockNames(data.getRegion().getData(), value.toArray(new String[0]));
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
				} else {
					rawBlockNames[i] = "minecraft:" + rawBlockNames[i];
				}
			}
			setValid(true);
			setValue(Arrays.asList(rawBlockNames));
			setRawValue(raw);
		}
	}

	@Override
	public String getFormatText() {
		return "<block>[,<block>,...]";
	}

	@Override
	public String toString() {
		return "Palette " + getComparator().getQueryString() + " \"" + getRawValue() + "\"";
	}

	@Override
	public PaletteFilter clone() {
		return new PaletteFilter(getOperator(), getComparator(), new ArrayList<>(value));
	}
}
