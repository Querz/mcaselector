package net.querz.mcaselector.io.mca;

import net.querz.mcaselector.changer.Field;
import net.querz.mcaselector.util.point.Point2i;
import net.querz.mcaselector.util.point.Point3i;
import net.querz.mcaselector.overlay.Overlay;
import java.util.List;

public final class ChunkData {

	private final RegionChunk region;
	private final PoiChunk poi;
	private final EntitiesChunk entities;
	private final boolean selected;
	private final Point2i location;

	public ChunkData(RegionChunk region, PoiChunk poi, EntitiesChunk entities, boolean selected) {
		this.region = region;
		this.poi = poi;
		this.entities = entities;
		this.selected = selected;
		if (region != null && !region.isEmpty()) {
			location = region.getAbsoluteLocation();
		} else if (poi != null && !poi.isEmpty()) {
			location = poi.getAbsoluteLocation();
		} else if (entities != null && !entities.isEmpty()) {
			location = entities.getAbsoluteLocation();
		} else {
			location = null;
		}
	}

	public ChunkData(Point2i location, RegionChunk region, PoiChunk poi, EntitiesChunk entities, boolean selected) {
		this.region = region;
		this.poi = poi;
		this.entities = entities;
		this.selected = selected;
		this.location = location;
	}

	public boolean relocate(Point3i offset) {
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

	public int parseData(Overlay parser) {
		return parser.parseValue(this);
	}

	public RegionChunk region() {
		return region;
	}

	public PoiChunk poi() {
		return poi;
	}

	public EntitiesChunk entities() {
		return entities;
	}

	public boolean selected() {
		return selected;
	}

	public Point2i location() {
		return location;
	}
}
