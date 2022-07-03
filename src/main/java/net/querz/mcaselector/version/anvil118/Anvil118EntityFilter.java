package net.querz.mcaselector.version.anvil118;

import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.range.Range;
import net.querz.mcaselector.version.NbtHelper;
import net.querz.mcaselector.version.anvil117.Anvil117EntityFilter;
import net.querz.nbt.ListTag;
import java.util.List;

public class Anvil118EntityFilter extends Anvil117EntityFilter {

	@Override
	public void deleteEntities(ChunkData data, List<Range> ranges) {
		if (data.entities() != null) {
			ListTag entities = NbtHelper.tagFromLevelFromRoot(data.entities().getData(), "Entities", null);
			deleteEntities(entities, ranges);
		}

		// delete proto-entities
		ListTag protoEntities = Snapshot118Helper.getProtoEntities(data.region().getData(), data.region().getData().getInt("DataVersion"));
		deleteEntities(protoEntities, ranges);
	}
}
