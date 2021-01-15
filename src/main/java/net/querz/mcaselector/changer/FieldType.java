package net.querz.mcaselector.changer;

import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.io.mca.RegionType;

import java.util.ArrayList;
import java.util.List;

public enum FieldType {

	LIGHT_POPULATED("LightPopulated", LightPopulatedField.class, false, false, RegionType.REGION),
	DATA_VERSION("DataVersion", DataVersionField.class, false, false, RegionType.REGION),
	INHABITED_TIME("InhabitedTime", InhabitedTimeField.class, false, false, RegionType.REGION),
	LAST_UPDATE("LastUpdate", LastUpdateField.class, false, false, RegionType.REGION),
	STATUS("Status", StatusField.class, false, true, RegionType.REGION),
	BIOME("Biome", BiomeField.class, false, false, RegionType.REGION),
	DELETE_ENTITIES("DeleteEntities", DeleteEntitiesField.class, false, false, RegionType.ENTITIES),
	DELETE_SECTIONS("DeleteSections", DeleteSectionsField.class, false, true, RegionType.REGION),
	STRUCTURE_REFERENCE("FixStructureReferences", ReferenceField.class, true, false, RegionType.REGION);

	private final String name;
	private final Class<? extends Field<?>> clazz;
	private final boolean headlessOnly;
	private final boolean clearCache;
	private final RegionType regionType;

	private static FieldType[] uiValues;

	static {
		List<FieldType> uiValues = new ArrayList<>(8);
		for (FieldType fieldType : values()) {
			if (!fieldType.headlessOnly) {
				uiValues.add(fieldType);
			}
		}
		FieldType.uiValues = uiValues.toArray(new FieldType[0]);
	}


	FieldType(String name, Class<? extends Field<?>> clazz, boolean headlessOnly, boolean clearCache, RegionType regionType) {
		this.name = name;
		this.clazz = clazz;
		this.headlessOnly = headlessOnly;
		this.clearCache = clearCache;
		this.regionType = regionType;
	}

	public Field<?> newInstance() {
		try {
			return clazz.newInstance();
		} catch (InstantiationException | IllegalAccessException ex) {
			Debug.dumpException("failed to create new change field instance", ex);
			return null;
		}
	}

	public static FieldType getByName(String name) {
		for (FieldType f : FieldType.values()) {
			if (f.name.equals(name)) {
				return f;
			}
		}
		return null;
	}

	public boolean requiresClearCache() {
		return clearCache;
	}

	public RegionType getRegionType() {
		return regionType;
	}

	@Override
	public String toString() {
		return name;
	}

	public static FieldType[] uiValues() {
		return uiValues;
	}
}
