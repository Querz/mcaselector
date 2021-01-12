package net.querz.mcaselector.filter;

import net.querz.mcaselector.debug.Debug;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.ListTag;
import net.querz.nbt.tag.LongArrayTag;
import net.querz.nbt.tag.Tag;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Matcher;

import static net.querz.mcaselector.validation.ValidationHelper.catchClassCastException;

public class StructureFilter extends TextFilter<List<String>> {

    private static final Set<String> validNames = new HashSet<>();

    static {
        try (BufferedReader bis = new BufferedReader(
                new InputStreamReader(Objects.requireNonNull(StructureFilter.class.getClassLoader().getResourceAsStream("mapping/all_structures.txt"))))) {
            String line;
            while ((line = bis.readLine()) != null) {
                validNames.add(line);
            }
        } catch (IOException ex) {
            Debug.dumpException("error reading mapping/all_structures.txt", ex);
        }
    }

    public StructureFilter() {
        this(Operator.AND, Comparator.CONTAINS, null);
    }

    private StructureFilter(Operator operator, Comparator comparator, List<String> value) {
        super(FilterType.STRUCTURES, operator, comparator, value);
        setRawValue(String.join(",", value == null ? new ArrayList<>(0) : value));
    }

    @Override
    public boolean contains(List<String> value, FilterData data) {
        CompoundTag rawStructures = data.getChunk().getCompoundTag("Level").getCompoundTag("Structures").getCompoundTag("References");

        for (String name : getFilterValue()) {
            Tag<?> structure = rawStructures.get(name);
            if (structure == null || structure.valueToString().equals("[]")) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean containsNot(List<String> value, FilterData data) {
        return !contains(value, data);
    }

    @Override
    public void setFilterValue(String raw) {
        String[] rawStructureNames = raw.replace(" ", "").split(",");
        if (raw.isEmpty() || rawStructureNames.length == 0) {
            setValid(false);
            setValue(null);
        } else {
            for (int i = 0; i < rawStructureNames.length; i++) {
                String name = rawStructureNames[i];

                if (!validNames.contains(name)) {
                    if (name.startsWith("'") && name.endsWith("'") && name.length() >= 2 && !name.contains("\"")) {
                        rawStructureNames[i] = name.substring(1, name.length() - 1);
                        continue;
                    }
                    setValue(null);
                    setValid(false);
                    return;
                }
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
    public StructureFilter clone() { return new StructureFilter(getOperator(), getComparator(), new ArrayList<>(value)); }
}
