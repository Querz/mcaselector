package net.querz.mcaselector.changer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum FieldType {
	LIGHT_POPULATED("LightPopulated", LightPopulatedField.class),
	DATA_VERSION("DataVersion", DataVersionField.class),
	INHABITED_TIME("InhabitedTime", InhabitedTimeField.class);

	private String name;
	private Class<? extends Field> clazz;

	FieldType(String name, Class<? extends Field> clazz) {
		this.name = name;
		this.clazz = clazz;
	}

	public Field<?> newInstance() {
		try {
			return clazz.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static List<Field<?>> instantiateAll() {
		List<Field<?>> list = new ArrayList<>(values().length);
		Arrays.stream(values()).forEach(f -> list.add(f.newInstance()));
		return list;
	}

	@Override
	public String toString() {
		return name;
	}
}
