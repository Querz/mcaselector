package net.querz.mcaselector.filter;

import net.querz.mcaselector.filter.filters.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public enum FilterType {

	GROUP("Group", GroupFilter::new, Format.GROUP),
	NOT_GROUP("Not Group", () -> new GroupFilter(true), Format.GROUP),
	DATA_VERSION("DataVersion", DataVersionFilter::new, Format.NUMBER),
	INHABITED_TIME("InhabitedTime", InhabitedTimeFilter::new, Format.NUMBER),
	X_POS("xPos", XPosFilter::new, Format.NUMBER),
	Y_POS("yPos", YPosFilter::new, Format.NUMBER),
	Z_POS("zPos", ZPosFilter::new, Format.NUMBER),
	TIMESTAMP("Timestamp", TimestampFilter::new, Format.NUMBER),
	COMPRESSION("Compression", CompressionFilter::new, Format.TEXT),
	LAST_UPDATE("LastUpdate", LastUpdateFilter::new, Format.NUMBER),
	PALETTE("Palette", PaletteFilter::new, Format.TEXT),
	BIOME("Biome", BiomeFilter::new, Format.TEXT),
	STATUS("Status", StatusFilter::new, Format.TEXT),
	PLAYER_DATA("PlayerLocation", PlayerLocationFilter::new, Format.FILE),
	PLAYER_SPAWN("PlayerSpawn", PlayerSpawnFilter::new, Format.FILE),
	SELECTION("Selection", SelectionFilter::new, Format.FILE),
	LIGHT_POPULATED("LightPopulated", LightPopulatedFilter::new, Format.NUMBER),
	ENTITIES("Entities", EntityFilter::new, Format.TEXT),
	STRUCTURES("Structures", StructureFilter::new, Format.TEXT),
	ENTITY_AMOUNT("#Entities", EntityAmountFilter::new, Format.NUMBER),
	PROTO_ENTITY_AMOUNT("#ProtoEntities", ProtoEntityAmountFilter::new, Format.NUMBER),
	TILE_ENTITY_AMOUNT("#TileEntities", TileEntityAmountFilter::new, Format.NUMBER),
	CIRCLE("Circle", CircleFilter::new, Format.TEXT),
	BORDER("Border", BorderFilter::new, Format.NUMBER),
	CUSTOM("Custom", CustomFilter::new, Format.TEXT),
	SCRIPT("Script", CustomFilter::new, Format.TEXT, false);

	private final String string;
	private final Supplier<? extends Filter<?>> creator;
	private final Format format;
	private final boolean query;

	private static FilterType[] queryable;

	static {
		List<FilterType> queryable = new ArrayList<>(8);
		for (FilterType filterType : values()) {
			if (filterType.query) {
				queryable.add(filterType);
			}
		}
		FilterType.queryable = queryable.toArray(new FilterType[0]);
	}

	FilterType(String string, Supplier<? extends Filter<?>> creator, Format format) {
		this(string, creator, format, true);
	}

	FilterType(String string, Supplier<? extends Filter<?>> creator, Format format, boolean query) {
		this.string = string;
		this.creator = creator;
		this.format = format;
		this.query = query;
	}


	public static FilterType[] queryable() {
		return queryable;
	}

	public Format getFormat() {
		return format;
	}

	public Filter<?> create() {
		return creator.get();
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
		GROUP, NUMBER, TEXT, FILE
	}
}
