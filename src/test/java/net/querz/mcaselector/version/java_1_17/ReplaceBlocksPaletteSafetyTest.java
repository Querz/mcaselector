package net.querz.mcaselector.version.java_1_17;

import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.io.mca.RegionChunk;
import net.querz.mcaselector.util.point.Point2i;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.ListTag;
import org.junit.jupiter.api.Test;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ReplaceBlocksPaletteSafetyTest {

	@Test
	void literalReplacementWorksInSinglePaletteSection() {
		CompoundTag root = paletteRoot();
		Map<ChunkFilter.BlockReplaceSource, ChunkFilter.BlockReplaceData> replace = new LinkedHashMap<>();
		replace.put(ChunkFilter.BlockReplaceSource.literalName("minecraft:stone"),
				new ChunkFilter.BlockReplaceData("minecraft:dirt"));

		replace(root, replace);

		assertEquals("minecraft:dirt", firstPaletteName(root));
	}

	@Test
	void contextualSourceFailsClosedInPalettePath() {
		CompoundTag root = paletteRoot();
		CompoundTag before = (CompoundTag) root.copy();
		Map<ChunkFilter.BlockReplaceSource, ChunkFilter.BlockReplaceData> replace = new LinkedHashMap<>();
		replace.put(ChunkFilter.BlockReplaceSource.literalName("minecraft:stone").withYRange(0, 0),
				new ChunkFilter.BlockReplaceData("minecraft:dirt"));

		replace(root, replace);

		assertEquals(before, root);
	}

	private void replace(CompoundTag root, Map<ChunkFilter.BlockReplaceSource, ChunkFilter.BlockReplaceData> replace) {
		RegionChunk region = new RegionChunk(new Point2i(0, 0));
		region.setData(root);
		new ChunkFilter_21w06a.Blocks().replaceBlocks(
				new ChunkData(new Point2i(0, 0), region, null, null, true), replace);
	}

	private CompoundTag paletteRoot() {
		CompoundTag root = new CompoundTag();
		CompoundTag level = new CompoundTag();
		level.putInt("xPos", 0);
		level.putInt("zPos", 0);
		ListTag sections = new ListTag();
		CompoundTag section = new CompoundTag();
		section.putByte("Y", (byte) 0);
		ListTag palette = new ListTag();
		CompoundTag stone = new CompoundTag();
		stone.putString("Name", "minecraft:stone");
		palette.add(stone);
		section.put("Palette", palette);
		sections.add(section);
		level.put("Sections", sections);
		root.put("Level", level);
		return root;
	}

	private String firstPaletteName(CompoundTag root) {
		return root.getCompoundTag("Level")
				.getListTag("Sections")
				.getCompound(0)
				.getListTag("Palette")
				.getCompound(0)
				.getString("Name");
	}
}
