package net.querz.mcaselector.filter;

public enum FilterType {

	GROUP("Group", GroupFilter.class),
	DATA_VERSION("DataVersion", DataVersionFilter.class),
	INHABITED_TIME("InhabitedTime", InhabitedTimeFilter.class),
	X_POS("xPos", XPosFilter.class),
	Z_POS("zPos", ZPosFilter.class),
	LAST_UPDATE("LastUpdate", LastUpdateFilter.class);

	private String string;
	private Class clazz;

	FilterType(String string, Class<? extends Filter> clazz) {
		this.string = string;
		this.clazz = clazz;
	}

	@Override
	public String toString() {
		return string;
	}
}
