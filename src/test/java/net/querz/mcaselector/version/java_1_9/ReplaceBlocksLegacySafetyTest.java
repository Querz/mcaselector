package net.querz.mcaselector.version.java_1_9;

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

class ReplaceBlocksLegacySafetyTest {

	@Test
	void literalNameReplacementStillWorks() {
		CompoundTag root = classicRoot(true);
		Map<ChunkFilter.BlockReplaceSource, ChunkFilter.BlockReplaceData> replace = new LinkedHashMap<>();
		replace.put(ChunkFilter.BlockReplaceSource.literalName("minecraft:stone"),
				new ChunkFilter.BlockReplaceData("minecraft:dirt"));

		replace(root, replace);

		assertEquals(3, firstBlockId(root));
	}

	@Test
	void contextualAndUnsupportedSourcesFailClosed() {
		CompoundTag contextualRoot = classicRoot(true);
		CompoundTag contextualBefore = (CompoundTag) contextualRoot.copy();
		Map<ChunkFilter.BlockReplaceSource, ChunkFilter.BlockReplaceData> contextual = new LinkedHashMap<>();
		contextual.put(ChunkFilter.BlockReplaceSource.literalName("minecraft:stone").withYRange(0, 0),
				new ChunkFilter.BlockReplaceData("minecraft:dirt"));

		replace(contextualRoot, contextual);
		assertEquals(contextualBefore, contextualRoot);

		CompoundTag regexRoot = classicRoot(true);
		CompoundTag regexBefore = (CompoundTag) regexRoot.copy();
		Map<ChunkFilter.BlockReplaceSource, ChunkFilter.BlockReplaceData> regex = new LinkedHashMap<>();
		regex.put(ChunkFilter.BlockReplaceSource.regexName("minecraft:.*"),
				new ChunkFilter.BlockReplaceData("minecraft:dirt"));

		replace(regexRoot, regex);
		assertEquals(regexBefore, regexRoot);
	}

	@Test
	void contextualAirRuleDoesNotCreateLegacySections() {
		CompoundTag root = classicRoot(false);
		Map<ChunkFilter.BlockReplaceSource, ChunkFilter.BlockReplaceData> replace = new LinkedHashMap<>();
		replace.put(ChunkFilter.BlockReplaceSource.literalName("minecraft:air").withYRange(0, 0),
				new ChunkFilter.BlockReplaceData("minecraft:glass"));

		replace(root, replace);

		assertEquals(0, root.getCompoundTag("Level").getListTag("Sections").size());
	}

	private void replace(CompoundTag root, Map<ChunkFilter.BlockReplaceSource, ChunkFilter.BlockReplaceData> replace) {
		RegionChunk region = new RegionChunk(new Point2i(0, 0));
		region.setData(root);
		new ChunkFilter_15w32a.Blocks().replaceBlocks(
				new ChunkData(new Point2i(0, 0), region, null, null, true), replace);
	}

	private CompoundTag classicRoot(boolean includeSection) {
		CompoundTag root = new CompoundTag();
		CompoundTag level = new CompoundTag();
		ListTag sections = new ListTag();
		if (includeSection) {
			CompoundTag section = new CompoundTag();
			section.putByte("Y", (byte) 0);
			byte[] blocks = new byte[4096];
			java.util.Arrays.fill(blocks, (byte) 1);
			section.putByteArray("Blocks", blocks);
			section.putByteArray("Data", new byte[2048]);
			sections.add(section);
		}
		level.put("Sections", sections);
		root.put("Level", level);
		return root;
	}

	private int firstBlockId(CompoundTag root) {
		return root.getCompoundTag("Level").getListTag("Sections").getCompound(0).getByteArray("Blocks")[0] & 0xFF;
	}
}
