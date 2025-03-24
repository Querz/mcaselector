package net.querz.mcaselector.changer;

import net.querz.mcaselector.changer.fields.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public enum FieldType {

	LIGHT_POPULATED("LightPopulated", LightPopulatedField::new, false, false),
	DATA_VERSION("DataVersion", DataVersionField::new, false, false),
	INHABITED_TIME("InhabitedTime", InhabitedTimeField::new, false, false),
	TIMESTAMP("Timestamp", TimestampField::new, false, false),
	COMPRESSION("Compression", CompressionField::new, false, false),
	LAST_UPDATE("LastUpdate", LastUpdateField::new, false, false),
	STATUS("Status", StatusField::new, false, true),
	BIOME("Biome", BiomeField::new, false, false),
	REPLACE_BLOCKS("ReplaceBlocks", ReplaceBlocksField::new, false, true),
	DELETE_ENTITIES("DeleteEntities", DeleteEntitiesField::new, false, false),
	DELETE_SECTIONS("DeleteSections", DeleteSectionsField::new, false, true),
	FIX_STATUS("FixStatus", FixStatusField::new, false, true),
	FIX_HEIGHTMAPS("FixHeightmaps", FixHeightmapsField::new, false, false),
	DELETE_STRUCTURE("DeleteStructureReference", DeleteStructureField::new, false, false),
	STRUCTURE_REFERENCE("FixStructureReferences", ReferenceField::new, true, false),
	PREVENT_RETROGEN("PreventRetrogen", PreventRetrogenField::new, false, false),
	FORCE_BLEND("ForceBlend", ForceBlendField::new, false, false),
	CUSTOM("Custom", CustomField::new, false, false),
	SCRIPT("Script", ScriptField::new, true, false);

	private final String name;
	private final Supplier<? extends Field<?>> constructor;
	private final boolean headlessOnly;
	private final boolean clearCache;

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


	FieldType(String name, Supplier<? extends Field<?>> constructor, boolean headlessOnly, boolean clearCache) {
		this.name = name;
		this.constructor = constructor;
		this.headlessOnly = headlessOnly;
		this.clearCache = clearCache;
	}

	public Field<?> newInstance() {
		return constructor.get();
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

	@Override
	public String toString() {
		return name;
	}

	public static FieldType[] uiValues() {
		return uiValues;
	}
}
