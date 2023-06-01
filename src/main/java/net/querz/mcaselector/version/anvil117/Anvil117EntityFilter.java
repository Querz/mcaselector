package net.querz.mcaselector.version.anvil117;

import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.range.Range;
import net.querz.mcaselector.version.EntityFilter;
import net.querz.mcaselector.version.NbtHelper;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.ListTag;
import java.util.List;

public class Anvil117EntityFilter implements EntityFilter {

	@Override
	public void deleteEntities(ChunkData data, List<Range> ranges) {
		ListTag entities = NbtHelper.tagFromLevelFromRoot(data.entities().getData(), "Entities", null);
		deleteEntities(entities, ranges);

		// delete proto-entities
		ListTag protoEntities = NbtHelper.tagFromLevelFromRoot(data.region().getData(), "Entities", null);
		deleteEntities(protoEntities, ranges);
	}

	protected void deleteEntities(ListTag entities, List<Range> ranges) {
		if (entities == null) {
			return;
		}
		if (ranges == null) {
			entities.clear();
		} else {
			for (int i = 0; i < entities.size(); i++) {
				CompoundTag entity = entities.getCompound(i);
				for (Range range : ranges) {
					ListTag entityPos = NbtHelper.tagFromCompound(entity, "Pos");
					if (entityPos != null && entityPos.size() == 3) {
						if (range.contains(entityPos.getInt(1) >> 4)) {
							entities.remove(i);
							i--;
						}
					}
				}
			}
		}
	}

	@Override
	public ListTag getEntities(ChunkData data) {
		if (data.entities() == null) {
			return null;
		}
		return NbtHelper.tagFromCompound(data.entities().getData(), "Entities", null);
	}
}
