package net.querz.mcaselector.version.anvil112;

import net.querz.mcaselector.range.Range;
import net.querz.mcaselector.version.ChunkMerger;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.ListTag;
import java.util.List;
import java.util.function.Function;
import static net.querz.mcaselector.validation.ValidationHelper.withDefault;

public class Anvil112ChunkMerger implements ChunkMerger {

	@Override
	public void mergeChunks(CompoundTag source, CompoundTag destination, List<Range> ranges) {
		mergeCompoundTagLists(source, destination, ranges, "Sections", c -> c.getNumber("Y").intValue());
		mergeCompoundTagLists(source, destination, ranges, "Entities", c -> c.getListTag("Pos").asDoubleTagList().get(1).asInt() >> 4);
		mergeCompoundTagLists(source, destination, ranges, "TileEntities", c -> c.getInt("y") >> 4);
		mergeCompoundTagLists(source, destination, ranges, "TileTicks", c -> c.getInt("y") >> 4);
		mergeCompoundTagLists(source, destination, ranges, "LiquidTicks", c -> c.getInt("y") >> 4);
		mergeListTagLists(source, destination, ranges, "Lights");
		mergeListTagLists(source, destination, ranges, "LiquidsToBeTicked");
		mergeListTagLists(source, destination, ranges, "ToBeTicked");
		mergeListTagLists(source, destination, ranges, "PostProcessing");

		// we need to fix entity UUIDs, because Minecraft doesn't like duplicates
		fixEntityUUIDs(destination);
	}

	protected void fixEntityUUIDs(CompoundTag root) {
		ListTag<CompoundTag> entities = withDefault(() -> root.getCompoundTag("Level").getListTag("Entities").asCompoundTagList(), null);
		if (entities == null) {
			return;
		}
		entities.forEach(this::fixEntityUUID);
	}

	protected void fixEntityUUID(CompoundTag entity) {
		if (entity.containsKey("UUIDMost")) {
			entity.putLong("UUIDMost", random.nextLong());
		}
		if (entity.containsKey("UUIDLeast")) {
			entity.putLong("UUIDLeast", random.nextLong());
		}
		if (entity.containsKey("Passengers")) {
			ListTag<CompoundTag> passengers = withDefault(() -> entity.getListTag("Passengers").asCompoundTagList(), null);
			if (passengers != null) {
				passengers.forEach(this::fixEntityUUID);
			}
		}
	}

	private void mergeListTagLists(CompoundTag source, CompoundTag destination, List<Range> ranges, String name) {
		ListTag<ListTag<?>> def = new ListTag<>(ListTag.class);
		ListTag<ListTag<?>> sourceList = withDefault(() -> source.getCompoundTag("Level").getListTag(name).asListTagList(), def);
		ListTag<ListTag<?>> destinationList = withDefault(() -> destination.getCompoundTag("Level").getListTag(name).asListTagList(), sourceList);

		if (sourceList.size() != destinationList.size()) {
			return;
		}

		for (Range range : ranges) {
			int m = Math.min(range.getTo(), sourceList.size() - 1);
			for (int i = Math.max(range.getFrom(), 0); i <= m; i++) {
				destinationList.set(i, sourceList.get(i));
			}
		}

		initLevel(destination).put(name, destinationList);
	}

	private void mergeCompoundTagLists(CompoundTag source, CompoundTag destination, List<Range> ranges, String name, Function<CompoundTag, Integer> ySupplier) {
		ListTag<CompoundTag> sourceElements = withDefault(() -> source.getCompoundTag("Level").getListTag(name).asCompoundTagList(), new ListTag<>(CompoundTag.class));
		ListTag<CompoundTag> destinationElements = withDefault(() -> destination.getCompoundTag("Level").getListTag(name).asCompoundTagList(), new ListTag<>(CompoundTag.class));

		initLevel(destination).put(name, mergeLists(sourceElements, destinationElements, ranges, ySupplier));
	}

	private CompoundTag initLevel(CompoundTag c) {
		CompoundTag level = c.getCompoundTag("Level");
		if (level == null) {
			c.put("Level", level = new CompoundTag());
		}
		return level;
	}
}
