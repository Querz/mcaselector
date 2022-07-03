package net.querz.mcaselector.version.anvil119;

import net.querz.mcaselector.point.Point3i;
import net.querz.mcaselector.version.ChunkRelocator;
import net.querz.mcaselector.version.NbtHelper;
import net.querz.mcaselector.version.anvil117.Anvil117EntityRelocator;
import net.querz.nbt.*;
import java.util.Map;
import static net.querz.mcaselector.validation.ValidationHelper.silent;
import static net.querz.mcaselector.version.anvil117.Anvil117EntityRelocator.applyOffsetToEntity;

public class Anvil119ChunkRelocator implements ChunkRelocator {

	@Override
	public boolean relocate(CompoundTag root, Point3i offset) {
		// adjust or set chunk position
		root.putInt("xPos", root.getInt("xPos") + offset.blockToChunk().getX());
		root.putInt("zPos", root.getInt("zPos") + offset.blockToChunk().getZ());

		// adjust tile entity positions
		ListTag tileEntities = NbtHelper.tagFromCompound(root, "block_entities");
		if (tileEntities != null) {
			tileEntities.forEach(v -> applyOffsetToTileEntity((CompoundTag) v, offset));
		}

		// adjust tile ticks
		ListTag tileTicks = NbtHelper.tagFromCompound(root, "block_ticks");
		if (tileTicks != null) {
			tileTicks.forEach(v -> applyOffsetToTick((CompoundTag) v, offset));
		}

		// adjust liquid ticks
		ListTag liquidTicks = NbtHelper.tagFromCompound(root, "fluid_ticks");
		if (liquidTicks != null) {
			liquidTicks.forEach(v -> applyOffsetToTick((CompoundTag) v, offset));
		}

		// adjust structures
		CompoundTag structures = NbtHelper.tagFromCompound(root, "structures");
		if (structures != null) {
			applyOffsetToStructures(structures, offset);
		}

		NbtHelper.applyOffsetToListOfShortTagLists(root, "PostProcessing", offset.blockToSection());

		// adjust sections vertically
		ListTag sections = NbtHelper.tagFromCompound(root, "sections");
		if (sections != null) {
			ListTag newSections = new ListTag();
			int yMax = NbtHelper.findHighestSection(sections, -4);
			for (CompoundTag section : sections.iterateType(CompoundTag.TYPE)) {
				if (applyOffsetToSection(section, offset.blockToSection(), -4, yMax)) {
					newSections.add(section);
				}
			}
			root.put("sections", newSections);
		}

		return true;
	}

	private void applyOffsetToStructures(CompoundTag structures, Point3i offset) { // 1.13
		Point3i chunkOffset = offset.blockToChunk();

		// update references
		CompoundTag references = NbtHelper.tagFromCompound(structures, "References");
		if (references != null) {
			for (Map.Entry<String, Tag> entry : references) {
				long[] reference = silent(() -> ((LongArrayTag) entry.getValue()).getValue(), null);
				if (reference != null) {
					for (int i = 0; i < reference.length; i++) {
						int x = (int) (reference[i]);
						int z = (int) (reference[i] >> 32);
						reference[i] = ((long) (z + chunkOffset.getZ()) & 0xFFFFFFFFL) << 32 | (long) (x + chunkOffset.getX()) & 0xFFFFFFFFL;
					}
				}
			}
		}

		// update starts
		CompoundTag starts = NbtHelper.tagFromCompound(structures, "starts");
		if (starts != null) {
			for (Map.Entry<String, Tag> entry : starts) {
				CompoundTag structure = silent(() -> (CompoundTag) entry.getValue(), null);
				if ("INVALID".equals(NbtHelper.stringFromCompound(structure, "id"))) {
					continue;
				}
				NbtHelper.applyIntIfPresent(structure, "ChunkX", chunkOffset.getX());
				NbtHelper.applyIntIfPresent(structure, "ChunkZ", chunkOffset.getZ());
				NbtHelper.applyOffsetToBB(NbtHelper.intArrayFromCompound(structure, "BB"), offset);

				ListTag processed = NbtHelper.tagFromCompound(structure, "Processed");
				if (processed != null) {
					for (CompoundTag chunk : processed.iterateType(CompoundTag.TYPE)) {
						NbtHelper.applyIntIfPresent(chunk, "X", chunkOffset.getX());
						NbtHelper.applyIntIfPresent(chunk, "Z", chunkOffset.getZ());
					}
				}

				ListTag children = NbtHelper.tagFromCompound(structure, "Children");
				if (children != null) {
					for (CompoundTag child : children.iterateType(CompoundTag.TYPE)) {
						NbtHelper.applyIntOffsetIfRootPresent(child, "TPX", "TPY", "TPZ", offset);
						NbtHelper.applyIntOffsetIfRootPresent(child, "PosX", "PosY", "PosZ", offset);
						NbtHelper.applyOffsetToBB(NbtHelper.intArrayFromCompound(child, "BB"), offset);

						ListTag entrances = NbtHelper.tagFromCompound(child, "Entrances");
						if (entrances != null) {
							entrances.forEach(e -> NbtHelper.applyOffsetToBB(((IntArrayTag) e).getValue(), offset));
						}

						ListTag junctions = NbtHelper.tagFromCompound(child, "junctions");
						if (junctions != null) {
							for (CompoundTag junction : junctions.iterateType(CompoundTag.TYPE)) {
								NbtHelper.applyIntOffsetIfRootPresent(junction, "source_x", "source_y", "source_z", offset);
							}
						}
					}
				}
			}
		}
	}

	private void applyOffsetToTick(CompoundTag tick, Point3i offset) {
		NbtHelper.applyIntOffsetIfRootPresent(tick, "x", "y", "z", offset);
	}

	static void applyOffsetToTileEntity(CompoundTag tileEntity, Point3i offset) {
		if (tileEntity == null) {
			return;
		}

		NbtHelper.applyIntOffsetIfRootPresent(tileEntity, "x", "y", "z", offset);

		String id = NbtHelper.stringFromCompound(tileEntity, "id", "");
		switch (id) {
		case "minecraft:bee_nest":
		case "minecraft:beehive":
			CompoundTag flowerPos = NbtHelper.tagFromCompound(tileEntity, "FlowerPos");
			NbtHelper.applyIntOffsetIfRootPresent(flowerPos, "X", "Y", "Z", offset);
			ListTag bees = NbtHelper.tagFromCompound(tileEntity, "Bees");
			if (bees != null) {
				for (Tag bee : bees) {
					applyOffsetToEntity(NbtHelper.tagFromCompound(bee, "EntityData"), offset);
				}
			}
			break;
		case "minecraft:end_gateway":
			CompoundTag exitPortal = NbtHelper.tagFromCompound(tileEntity, "ExitPortal");
			NbtHelper.applyIntOffsetIfRootPresent(exitPortal, "X", "Y", "Z", offset);
			break;
		case "minecraft:structure_block":
			NbtHelper.applyIntOffsetIfRootPresent(tileEntity, "posX", "posY", "posZ", offset);
			break;
		case "minecraft:jukebox":
			CompoundTag recordItem = NbtHelper.tagFromCompound(tileEntity, "RecordItem");
			applyOffsetToItem(recordItem, offset);
			break;
		case "minecraft:lectern": // 1.14
			CompoundTag book = NbtHelper.tagFromCompound(tileEntity, "Book");
			applyOffsetToItem(book, offset);
			break;
		case "minecraft:mob_spawner":
			ListTag spawnPotentials = NbtHelper.tagFromCompound(tileEntity, "SpawnPotentials");
			if (spawnPotentials != null) {
				for (CompoundTag spawnPotential : spawnPotentials.iterateType(CompoundTag.TYPE)) {
					CompoundTag entity = NbtHelper.tagFromCompound(spawnPotential, "Entity");
					Anvil117EntityRelocator.applyOffsetToEntity(entity, offset);
				}
			}
		}

		ListTag items = NbtHelper.tagFromCompound(tileEntity, "Items");
		if (items != null) {
			items.forEach(i -> applyOffsetToItem((CompoundTag) i, offset));
		}
	}

	static void applyOffsetToItem(CompoundTag item, Point3i offset) {
		if (item == null) {
			return;
		}

		CompoundTag tag = NbtHelper.tagFromCompound(item, "tag");
		if (tag == null) {
			return;
		}

		String id = NbtHelper.stringFromCompound(item, "id", "");
		switch (id) {
		case "minecraft:compass":
			CompoundTag lodestonePos = NbtHelper.tagFromCompound(tag, "LodestonePos");
			NbtHelper.applyIntOffsetIfRootPresent(lodestonePos, "X", "Y", "Z", offset);
			break;
		}

		// recursively update all items in child containers
		CompoundTag blockEntityTag = NbtHelper.tagFromCompound(tag, "BlockEntityTag");
		ListTag items = NbtHelper.tagFromCompound(blockEntityTag, "Items");
		if (items != null) {
			items.forEach(i -> applyOffsetToItem((CompoundTag) i, offset));
		}
	}
}
