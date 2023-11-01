package net.querz.mcaselector.version.anvil117;

import net.querz.mcaselector.point.Point3i;
import net.querz.mcaselector.version.ChunkRelocator;
import net.querz.mcaselector.version.Helper;
import net.querz.nbt.*;
import java.util.Map;
import static net.querz.mcaselector.validation.ValidationHelper.silent;
import static net.querz.mcaselector.version.anvil117.Anvil117EntityRelocator.*;

public class Anvil117ChunkRelocator implements ChunkRelocator {

	@Override
	public boolean relocate(CompoundTag root, Point3i offset) {
		CompoundTag level = Helper.tagFromCompound(root, "Level");
		if (level == null) {
			return false;
		}

		// adjust or set chunk position
		level.putInt("xPos", level.getInt("xPos") + offset.blockToChunk().getX());
		level.putInt("zPos", level.getInt("zPos") + offset.blockToChunk().getZ());

		// adjust tile entity positions
		ListTag tileEntities = Helper.tagFromCompound(level, "TileEntities");
		if (tileEntities != null) {
			tileEntities.forEach(v -> applyOffsetToTileEntity((CompoundTag) v, offset));
		}

		// adjust tile ticks
		ListTag tileTicks = Helper.tagFromCompound(level, "TileTicks");
		if (tileTicks != null) {
			tileTicks.forEach(v -> applyOffsetToTick((CompoundTag) v, offset));
		}

		// adjust liquid ticks
		ListTag liquidTicks = Helper.tagFromCompound(level, "LiquidTicks");
		if (liquidTicks != null) {
			liquidTicks.forEach(v -> applyOffsetToTick((CompoundTag) v, offset));
		}

		// adjust structures
		CompoundTag structures = Helper.tagFromCompound(level, "Structures");
		if (structures != null) {
			applyOffsetToStructures(structures, offset);
		}

		// Biomes
		applyOffsetToBiomes(Helper.tagFromCompound(level, "Biomes"), offset.blockToSection());

		// Lights
		Helper.applyOffsetToListOfShortTagLists(level, "Lights", offset.blockToSection());

		// LiquidsToBeTicked
		Helper.applyOffsetToListOfShortTagLists(level, "LiquidsToBeTicked", offset.blockToSection());

		// ToBeTicked
		Helper.applyOffsetToListOfShortTagLists(level, "ToBeTicked", offset.blockToSection());

		// PostProcessing
		Helper.applyOffsetToListOfShortTagLists(level, "PostProcessing", offset.blockToSection());

		// adjust sections vertically
		ListTag sections = Helper.getSectionsFromLevelFromRoot(root, "Sections");
		if (sections != null) {
			ListTag newSections = new ListTag();
			for (CompoundTag section : sections.iterateType(CompoundTag.class)) {
				if (applyOffsetToSection(section, offset.blockToSection(), 0, 15)) {
					newSections.add(section);
				}
			}
			level.put("Sections", newSections);
		}

		return true;
	}

	private void applyOffsetToBiomes(IntArrayTag biomes, Point3i offset) {
		int[] biomesArray;
		if (biomes == null || (biomesArray = biomes.getValue()) == null || (biomesArray.length != 1024 && biomesArray.length != 1536)) {
			return;
		}

		int[] newBiomes = new int[biomesArray.length];
		int maxY = biomesArray.length / 16;

		for (int x = 0; x < 4; x++) {
			for (int z = 0; z < 4; z++) {
				for (int y = 0; y < maxY; y++) {
					if (y + offset.getY() * 4 < 0 || y + offset.getY() * 4 > maxY - 1) {
						break;
					}
					int biome = biomesArray[y * 16 + z * 4 + x];
					newBiomes[(y + offset.getY() * 4) * 16 + z * 4 + x] = biome;
				}
			}
		}

		biomes.setValue(newBiomes);
	}

	private void applyOffsetToStructures(CompoundTag structures, Point3i offset) { // 1.13
		Point3i chunkOffset = offset.blockToChunk();

		// update references
		CompoundTag references = Helper.tagFromCompound(structures, "References");
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
		CompoundTag starts = Helper.tagFromCompound(structures, "Starts");
		if (starts != null) {
			for (Map.Entry<String, Tag> entry : starts) {
				CompoundTag structure = silent(() -> (CompoundTag) entry.getValue(), null);
				if ("INVALID".equals(Helper.stringFromCompound(structure, "id"))) {
					continue;
				}
				Helper.applyIntIfPresent(structure, "ChunkX", chunkOffset.getX());
				Helper.applyIntIfPresent(structure, "ChunkZ", chunkOffset.getZ());
				Helper.applyOffsetToBB(Helper.intArrayFromCompound(structure, "BB"), offset);

				ListTag processed = Helper.tagFromCompound(structure, "Processed");
				if (processed != null) {
					for (CompoundTag chunk : processed.iterateType(CompoundTag.class)) {
						Helper.applyIntIfPresent(chunk, "X", chunkOffset.getX());
						Helper.applyIntIfPresent(chunk, "Z", chunkOffset.getZ());
					}
				}

				ListTag children = Helper.tagFromCompound(structure, "Children");
				if (children != null) {
					for (CompoundTag child : children.iterateType(CompoundTag.class)) {
						Helper.applyIntOffsetIfRootPresent(child, "TPX", "TPY", "TPZ", offset);
						Helper.applyIntOffsetIfRootPresent(child, "PosX", "PosY", "PosZ", offset);
						Helper.applyOffsetToBB(Helper.intArrayFromCompound(child, "BB"), offset);

						ListTag entrances = Helper.tagFromCompound(child, "Entrances");
						if (entrances != null) {
							entrances.forEach(e -> Helper.applyOffsetToBB(((IntArrayTag) e).getValue(), offset));
						}

						ListTag junctions = Helper.tagFromCompound(child, "junctions");
						if (junctions != null) {
							for (CompoundTag junction : junctions.iterateType(CompoundTag.class)) {
								Helper.applyIntOffsetIfRootPresent(junction, "source_x", "source_y", "source_z", offset);
							}
						}
					}
				}
			}
		}
	}

	private void applyOffsetToTick(CompoundTag tick, Point3i offset) {
		Helper.applyIntOffsetIfRootPresent(tick, "x", "y", "z", offset);
	}

	static void applyOffsetToTileEntity(CompoundTag tileEntity, Point3i offset) {
		if (tileEntity == null) {
			return;
		}

		Helper.applyIntOffsetIfRootPresent(tileEntity, "x", "y", "z", offset);

		String id = Helper.stringFromCompound(tileEntity, "id", "");
		switch (id) {
		case "minecraft:bee_nest":
		case "minecraft:beehive":
			CompoundTag flowerPos = Helper.tagFromCompound(tileEntity, "FlowerPos");
			Helper.applyIntOffsetIfRootPresent(flowerPos, "X", "Y", "Z", offset);
			ListTag bees = Helper.tagFromCompound(tileEntity, "Bees");
			if (bees != null) {
				for (CompoundTag bee : bees.iterateType(CompoundTag.class)) {
					applyOffsetToEntity(Helper.tagFromCompound(bee, "EntityData"), offset);
				}
			}
			break;
		case "minecraft:end_gateway":
			CompoundTag exitPortal = Helper.tagFromCompound(tileEntity, "ExitPortal");
			Helper.applyIntOffsetIfRootPresent(exitPortal, "X", "Y", "Z", offset);
			break;
		case "minecraft:structure_block":
			Helper.applyIntOffsetIfRootPresent(tileEntity, "posX", "posY", "posZ", offset);
			break;
		case "minecraft:jukebox":
			CompoundTag recordItem = Helper.tagFromCompound(tileEntity, "RecordItem");
			applyOffsetToItem(recordItem, offset);
			break;
		case "minecraft:lectern": // 1.14
			CompoundTag book = Helper.tagFromCompound(tileEntity, "Book");
			applyOffsetToItem(book, offset);
			break;
		case "minecraft:mob_spawner":
			ListTag spawnPotentials = Helper.tagFromCompound(tileEntity, "SpawnPotentials");
			if (spawnPotentials != null) {
				for (CompoundTag spawnPotential : spawnPotentials.iterateType(CompoundTag.class)) {
					CompoundTag entity = Helper.tagFromCompound(spawnPotential, "Entity");
					applyOffsetToEntity(entity, offset);
				}
			}
		}

		ListTag items = Helper.tagFromCompound(tileEntity, "Items");
		if (items != null) {
			items.forEach(i -> applyOffsetToItem((CompoundTag) i, offset));
		}
	}

	static void applyOffsetToItem(CompoundTag item, Point3i offset) {
		if (item == null) {
			return;
		}

		CompoundTag tag = Helper.tagFromCompound(item, "tag");
		if (tag == null) {
			return;
		}

		String id = Helper.stringFromCompound(item, "id", "");
		switch (id) {
		case "minecraft:compass":
			CompoundTag lodestonePos = Helper.tagFromCompound(tag, "LodestonePos");
			Helper.applyIntOffsetIfRootPresent(lodestonePos, "X", "Y", "Z", offset);
			break;
		}

		// recursively update all items in child containers
		CompoundTag blockEntityTag = Helper.tagFromCompound(tag, "BlockEntityTag");
		ListTag items = Helper.tagFromCompound(blockEntityTag, "Items");
		if (items != null) {
			items.forEach(i -> applyOffsetToItem((CompoundTag) i, offset));
		}
	}
}
