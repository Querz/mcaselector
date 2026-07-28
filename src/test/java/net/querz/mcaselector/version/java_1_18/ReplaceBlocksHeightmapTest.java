package net.querz.mcaselector.version.java_1_18;

import net.querz.nbt.CompoundTag;
import net.querz.nbt.ListTag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReplaceBlocksHeightmapTest {

	private static final String TEST_HEIGHTMAP = "TEST_HEIGHTMAP";
	private static final int SECTION_COUNT = 24;
	private static final int EXPECTED_HEIGHT = SECTION_COUNT * 16;

	@Test
	void modernFlatHeightmapWritesToRootAndPacksEveryColumn() {
		CompoundTag root = new CompoundTag();
		root.put("sections", heightmapSections());

		new ModernHeightmap().calculate(root);

		CompoundTag heightmaps = root.getCompoundTag("Heightmaps");
		assertNotNull(heightmaps);
		assertPackedHeightmap(heightmaps.getLongArray(TEST_HEIGHTMAP));
	}

	@Test
	void earlyFlatHeightmapReadsNestedBlockStatesAndWritesToLevel() {
		CompoundTag root = new CompoundTag();
		CompoundTag level = new CompoundTag();
		level.put("Sections", heightmapSections());
		root.put("Level", level);

		new EarlyHeightmap().calculate(root);

		CompoundTag heightmaps = level.getCompoundTag("Heightmaps");
		assertNotNull(heightmaps);
		assertPackedHeightmap(heightmaps.getLongArray(TEST_HEIGHTMAP));
	}

	private void assertPackedHeightmap(long[] heightmap) {
		int bits = 32 - Integer.numberOfLeadingZeros(EXPECTED_HEIGHT);
		int valuesPerLong = 64 / bits;
		assertEquals(Math.ceilDiv(256, valuesPerLong), heightmap.length);
		long mask = (1L << bits) - 1;
		for (int index = 0; index < 256; index++) {
			int longIndex = index / valuesPerLong;
			int bitOffset = index % valuesPerLong * bits;
			assertEquals(EXPECTED_HEIGHT, heightmap[longIndex] >>> bitOffset & mask, "column " + index);
		}
	}

	private ListTag heightmapSections() {
		ListTag sections = new ListTag();
		sections.add(singleBlockSection(-4, "minecraft:air"));
		sections.add(singleBlockSection(19, "minecraft:stone"));
		return sections;
	}

	private CompoundTag singleBlockSection(int y, String name) {
		CompoundTag section = new CompoundTag();
		section.putByte("Y", (byte) y);
		CompoundTag blockStates = new CompoundTag();
		ListTag palette = new ListTag();
		CompoundTag state = new CompoundTag();
		state.putString("Name", name);
		palette.add(state);
		blockStates.put("palette", palette);
		section.put("block_states", blockStates);
		return section;
	}

	private static class ModernHeightmap extends ChunkFilter_21w43a.Heightmap {

		private void calculate(CompoundTag root) {
			long[] heightmap = getHeightMap(root, state -> "minecraft:stone".equals(state.getString("Name")));
			setHeightMap(root, TEST_HEIGHTMAP, heightmap);
		}
	}

	private static class EarlyHeightmap extends ChunkFilter_21w37a.Heightmap {

		private void calculate(CompoundTag root) {
			long[] heightmap = getHeightMap(root, state -> "minecraft:stone".equals(state.getString("Name")));
			setHeightMap(root, TEST_HEIGHTMAP, heightmap);
		}
	}
}
