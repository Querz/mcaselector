package net.querz.mcaselector.io.mca;

import net.querz.mcaselector.changer.Field;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.tiles.overlay.OverlayParser;

import java.util.List;

public class ChunkData {

	private final int lastUpdated;
	private final RegionChunk region;
	private final EntitiesChunk entities;
	private final PoiChunk poi;

	public ChunkData(RegionChunk region, PoiChunk poi, EntitiesChunk entities) {
		this(0, region, poi, entities);
	}

	public ChunkData(int lastUpdated, RegionChunk region, PoiChunk poi, EntitiesChunk entities) {
		this.lastUpdated = lastUpdated;
		this.region = region;
		this.entities = entities;
		this.poi = poi;
	}

	public int getLastUpdated() {
		return lastUpdated;
	}

	public RegionChunk getRegion() {
		return region;
	}

	public EntitiesChunk getEntities() {
		return entities;
	}

	public PoiChunk getPoi() {
		return poi;
	}

	public boolean relocate(Point2i offset) {
		boolean result = true;
		if (region != null && region.getData() != null && region.getData().containsKey("DataVersion")) {
			result = region.relocate(offset);
		}
		if (poi != null && poi.getData() != null && poi.getData().containsKey("DataVersion")) {
			result = result && poi.relocate(offset);
		}
		if (entities != null && entities.getData() != null && entities.getData().containsKey("DataVersion")) {
			result = result && entities.relocate(offset);
		}
		return result;
	}

	public void applyFieldChanges(List<Field<?>> fields, boolean force) {
		for (Field<?> field : fields) {
			if (force) {
				field.force(this);
			} else {
				field.change(this);
			}
		}
	}

	public int parseData(OverlayParser parser) {
		return parser.parseValue(this);
	}
}
