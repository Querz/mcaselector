package net.querz.mcaselector.overlay;

import com.google.gson.JsonObject;
import net.querz.mcaselector.io.mca.ChunkData;
import java.util.Arrays;
import java.util.UUID;

public abstract class Overlay implements Cloneable {

	private UUID id;

	private final OverlayType type;
	private boolean active;
	private Integer min;
	private Integer max;
	private String rawMin;
	private String rawMax;
	private String[] multiValues = null;
	private String rawMultiValues;
	private String rawMultiValuesShort;
	private float minHue = 0.66666667f; // blue
	private float maxHue = 0f; // red

	private transient String multiValuesID = "";

	public Overlay(OverlayType type) {
		this.type = type;
		id = UUID.randomUUID();
	}

	public OverlayType getType() {
		return type;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public boolean isActive() {
		return active;
	}

	public Integer min() {
		return min;
	}

	public Integer max() {
		return max;
	}

	public String minString() {
		return min == null ? "" : min + "";
	}

	public String maxString() {
		return max == null ? "" : max + "";
	}

	public boolean isValid() {
		return min != null && max != null && min < max;
	}

	public String getRawMin() {
		return rawMin;
	}

	public String getRawMax() {
		return rawMax;
	}

	public String getRawMultiValues() {
		return rawMultiValues;
	}

	public String getShortMultiValues() {
		if (rawMultiValuesShort != null) {
			return rawMultiValuesShort;
		}
		if (rawMultiValues == null) {
			return null;
		}
		if (rawMultiValues.length() > 50) {
			return rawMultiValuesShort = rawMultiValues.substring(0, 50) + "...";
		}
		return rawMultiValuesShort = rawMultiValues;
	}

	public void setRawMin(String rawMin) {
		this.rawMin = rawMin;
	}

	public void setRawMax(String rawMax) {
		this.rawMax = rawMax;
	}

	public void setRawMultiValues(String rawMultiValues) {
		this.rawMultiValues = rawMultiValues;
		this.rawMultiValuesShort = null;
	}

	public boolean setMinInt(Integer min) {
		this.min = min;
		return isValid();
	}

	public boolean setMaxInt(Integer max) {
		this.max = max;
		return isValid();
	}

	public float getMinHue() {
		return minHue;
	}

	public float getMaxHue() {
		return maxHue;
	}

	public void setMinHue(float minHue) {
		this.minHue = minHue;
	}

	public void setMaxHue(float maxHue) {
		this.maxHue = maxHue;
	}

	public void setMultiValues(String[] multiValues) {
		this.multiValues = multiValues;
		if (multiValues == null) {
			multiValuesID = "";
		} else {
			multiValuesID = UUID.nameUUIDFromBytes(String.join("", multiValues).getBytes()).toString().replace("-", "");
		}
	}

	public String getMultiValuesID() {
		return multiValuesID;
	}

	public abstract int parseValue(ChunkData chunkData);

	public abstract String name();

	public abstract boolean setMin(String raw);

	public abstract boolean setMax(String raw);

	// can be overwritten to set additional data points for a single overlay
	public boolean setMultiValuesString(String raw) {
		return true;
	}

	// can be overwritten to supply additional data points for a single overlay
	public String[] multiValues() {
		return multiValues;
	}

	public void writeCustomJSON(JsonObject obj) {}

	public void readCustomJSON(JsonObject obj) {}

	@Override
	public String toString() {
		return String.format("{min=%s, max=%s, active=%s, valid=%s, minHue=%f, maxHue=%f}", minString(), maxString(), active, isValid(), minHue, maxHue);
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof Overlay o)) {
			return false;
		}
		// type is equal
		return type == o.type
				// min are both null or equal
				&& (min == null && o.min == null || min != null && o.min != null && min.intValue() == o.min.intValue())
				// max are both null or equal
				&& (max == null && o.max == null || max != null && o.max != null && max.intValue() == o.max.intValue()
				// active is equal
				&& active == o.active
				// color range is equal
				&& minHue == o.minHue && maxHue == o.maxHue
				// their multiValues are equal
				&& Arrays.equals(multiValues(), o.multiValues()));
	}

	public boolean same(Object other) {
		if (!(other instanceof Overlay o)) {
			return false;
		}
		return id.equals(o.id);
	}

	@Override
	public Overlay clone() {
		Overlay clone = type.instance();
		clone.id = id;
		clone.min = min;
		clone.max = max;
		clone.multiValues = multiValues == null ? null : Arrays.copyOf(multiValues, multiValues.length);
		clone.rawMultiValues = rawMultiValues;
		clone.active = active;
		clone.rawMin = rawMin;
		clone.rawMax = rawMax;
		clone.minHue = minHue;
		clone.maxHue = maxHue;
		clone.multiValuesID = multiValuesID;
		return clone;
	}
}
