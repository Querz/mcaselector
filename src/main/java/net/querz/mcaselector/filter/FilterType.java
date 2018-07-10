package net.querz.mcaselector.filter;

public enum FilterType {

	GROUP("Group", GroupFilter.class),
	DATA_VERSION("DataVersion", DataVersionFilter.class),
	INHABITED_TIME("InhabitedTime", InhabitedTimeFilter.class),
	X_POS("xPos", XPosFilter.class),
	Z_POS("zPos", ZPosFilter.class),
	LAST_UPDATE("LastUpdate", LastUpdateFilter.class),
	BLOCK("Block", BlockFilter.class);

	private String string;
	private Class<? extends Filter> clazz;

	FilterType(String string, Class<? extends Filter> clazz) {
		this.string = string;
		this.clazz = clazz;
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
}
