package net.querz.mcaselector.filter;

public enum FilterType {

	GROUP("Group", GroupFilter.class, Format.GROUP),
	DATA_VERSION("DataVersion", DataVersionFilter.class, Format.NUMBER),
	INHABITED_TIME("InhabitedTime", InhabitedTimeFilter.class, Format.NUMBER),
	X_POS("xPos", XPosFilter.class, Format.NUMBER),
	Z_POS("zPos", ZPosFilter.class, Format.NUMBER),
	LAST_UPDATE("LastUpdate", LastUpdateFilter.class, Format.NUMBER),
	PALETTE("Palette", PaletteFilter.class, Format.TEXT),
	BIOME("Biome", BiomeFilter.class, Format.TEXT),
	STATUS("Status", StatusFilter.class, Format.TEXT),
	LIGHT_POPULATED("LightPopulated", LightPopulatedFilter.class, Format.NUMBER),
	ENTITIES("Entities", EntityFilter.class, Format.TEXT),
	ENTITY_AMOUNT("# Entities", EntityAmountFilter.class, Format.NUMBER);

	private String string;
	private Class<? extends Filter> clazz;
	private Format format;

	FilterType(String string, Class<? extends Filter> clazz, Format format) {
		this.string = string;
		this.clazz = clazz;
		this.format = format;
	}

	public Format getFormat() {
		return format;
	}

	public Filter create() {
		try {
			return clazz.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public String toString() {
		return string;
	}

	public static FilterType getByName(String name) {
		for (FilterType t : FilterType.values()) {
			if (t.string.equals(name)) {
				return t;
			}
		}
		return null;
	}

	public enum Format {
		GROUP, NUMBER, TEXT
	}
}
