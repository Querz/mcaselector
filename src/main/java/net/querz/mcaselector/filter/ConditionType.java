package net.querz.mcaselector.filter;

public enum ConditionType {

	DATA_VERSION("DataVersion", DataVersionCondition.class),
	INHABITED_TIME("InhabitedTime", InhabitedTimeCondition.class),
	X_POS("xPos", XPosCondition.class),
	Z_POS("zPos", ZPosCondition.class);

	private String string;
	private Class<? extends Filter> clazz;

	ConditionType(String string, Class<? extends Filter> clazz) {
		this.string = string;
		this.clazz = clazz;
	}

	public static ConditionType match(Filter filter) {
		for (ConditionType t : ConditionType.values()) {
			if (t.clazz == filter.getClass()) {
				return t;
			}
		}
		return null;
	}

	@Override
	public String toString() {
		return string;
	}
}
