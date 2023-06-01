package net.querz.mcaselector.overlay;

import net.querz.mcaselector.exception.ParseException;

public class OverlayParser {

	private final String type;
	private final String min;
	private final String max;
	private final String data;
	private final float minHue;
	private final float maxHue;

	public OverlayParser(String type, String min, String max, String data, float minHue, float maxHue) {
		this.type = type;
		this.min = min;
		this.max = max;
		this.data = data;
		this.minHue = minHue;
		this.maxHue = maxHue;
	}

	public Overlay parse() throws ParseException {
		OverlayType t = OverlayType.getByName(type);
		if (t == null) {
			throw new ParseException("invalid overlay type \"" + type + "\"");
		}

		if (min == null) {
			throw new ParseException("missing mandatory minimum value for \"" + type + "\" overlay");
		}
		if (max == null) {
			throw new ParseException("missing mandatory maximum value for \"" + type + "\" overlay");
		}

		Overlay o = t.instance();
		o.setMin(min);
		o.setMax(max);
		o.setMultiValuesString(data);
		if (!o.isValid()) {
			throw new ParseException("failed to parse values for " + type + " overlay");
		}
		o.setMinHue(minHue);
		o.setMaxHue(maxHue);
		return o;
	}
}
