package net.querz.mcaselector.io.mca;

import net.querz.mcaselector.changer.Field;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.point.Point3i;
import net.querz.mcaselector.overlay.Overlay;
import net.querz.nbt.CompoundTag;

import java.util.List;

public record ChunkData(RegionChunk region, PoiChunk poi, EntitiesChunk entities) {

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

	public int getDataVersion() {
		Chunk source;

		if (region != null && region.getData() != null) {
			source = region;
		} else if (entities != null && entities.getData() != null) {
			source = entities;
		} else {
			// MAINTAINER why not check poi data?
			// MAINTAINER fail fast?
			return 0;
		}

		return source.getData().getInt("DataVersion");
	}

}
