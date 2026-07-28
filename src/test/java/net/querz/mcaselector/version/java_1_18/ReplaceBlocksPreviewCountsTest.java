package net.querz.mcaselector.version.java_1_18;

import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.io.mca.RegionChunk;
import net.querz.mcaselector.util.point.Point2i;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.ListTag;
import org.junit.jupiter.api.Test;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ReplaceBlocksPreviewCountsTest {

	@Test
	void countsRulesSeparatelyWhileAggregateCountsBlockPositionsOnce() {
		Map<ChunkFilter.BlockReplaceSource, ChunkFilter.BlockReplaceData> replace = new LinkedHashMap<>();
		replace.put(ChunkFilter.BlockReplaceSource.literalName("minecraft:stone"), new ChunkFilter.BlockReplaceData("minecraft:dirt"));
		replace.put(ChunkFilter.BlockReplaceSource.regexName("minecraft:.*"), new ChunkFilter.BlockReplaceData("minecraft:gold_block"));

		ChunkFilter.BlockReplacePreviewData preview = new PreviewBlocks().preview(root(), sections(), replace);

		assertEquals(4096, preview.getBlocks());
		assertEquals(1, preview.getSections());
		assertEquals(4096, preview.getOverlappingBlocks());
		assertEquals(2, preview.getRules().size());
		assertEquals(1, preview.getRules().get(0).getIndex());
		assertEquals(ChunkFilter.BlockReplaceSourceType.LITERAL_NAME, preview.getRules().get(0).getSourceType());
		assertEquals("literal(minecraft:stone)", preview.getRules().get(0).getSourceText());
		assertEquals("minecraft:dirt", preview.getRules().get(0).getTargetText());
		assertEquals(4096, preview.getRules().get(0).getBlocks());
		assertEquals(2, preview.getRules().get(1).getIndex());
		assertEquals(ChunkFilter.BlockReplaceSourceType.REGEX_NAME, preview.getRules().get(1).getSourceType());
		assertEquals("regex(minecraft:.*)", preview.getRules().get(1).getSourceText());
		assertEquals("minecraft:gold_block", preview.getRules().get(1).getTargetText());
		assertEquals(4096, preview.getRules().get(1).getBlocks());
	}

	@Test
	void countsLightInvalidationOnlyForSectionsWithMatches() {
		Map<ChunkFilter.BlockReplaceSource, ChunkFilter.BlockReplaceData> replace = new LinkedHashMap<>();
		replace.put(ChunkFilter.BlockReplaceSource.literalName("minecraft:diamond_block"),
				new ChunkFilter.BlockReplaceData("minecraft:dirt"));

		ChunkFilter.BlockReplacePreviewData preview = new PreviewBlocks().preview(root(), sections(), replace);

		assertEquals(0, preview.getBlocks());
		assertEquals(0, preview.getSections());
		assertEquals(0, preview.getLightSections());
	}

	@Test
	void simpleRulesSkipBiomeAndTileLocationIndexesInPreviewAndExecution() {
		Map<ChunkFilter.BlockReplaceSource, ChunkFilter.BlockReplaceData> replace = new LinkedHashMap<>();
		replace.put(ChunkFilter.BlockReplaceSource.literalName("minecraft:stone"),
				new ChunkFilter.BlockReplaceData("minecraft:dirt"));
		CompoundTag root = root();
		root.putInt("DataVersion", 2844);
		root.put("sections", sections());
		RegionChunk region = new RegionChunk(new Point2i(0, 0));
		region.setData(root);
		TrackingBlocks blocks = new TrackingBlocks();

		blocks.preview(root, root.getListTag("sections"), replace);
		blocks.replaceBlocks(new ChunkData(new Point2i(0, 0), region, null, null, true), replace);

		assertEquals(0, blocks.biomeReads);
		assertEquals(0, blocks.tileLocationIndexBuilds);
	}

	@Test
	void sourceTileFiltersUseOriginalBlockEntityPresence() {
		Map<ChunkFilter.BlockReplaceSource, ChunkFilter.BlockReplaceData> replace = new LinkedHashMap<>();
		replace.put(
				ChunkFilter.BlockReplaceSource.literalName("minecraft:stone")
						.withTileEntityMode(ChunkFilter.BlockReplaceTileEntityMode.REQUIRE_TILE_ENTITY),
				new ChunkFilter.BlockReplaceData("minecraft:dirt"));
		replace.put(
				ChunkFilter.BlockReplaceSource.literalName("minecraft:stone")
						.withTileEntityMode(ChunkFilter.BlockReplaceTileEntityMode.EXCLUDE_TILE_ENTITY),
				new ChunkFilter.BlockReplaceData("minecraft:gold_block"));

		CompoundTag root = root();
		root.put("block_entities", tileEntities(tile("minecraft:chest", 0, 0, 0)));

		ChunkFilter.BlockReplacePreviewData preview = new PreviewBlocks().preview(root, sections(), replace);

		assertEquals(4096, preview.getBlocks());
		assertEquals(1, preview.getTileEntityRemovals());
		assertEquals(0, preview.getTileEntityAdditions());
		assertEquals(0, preview.getTileEntityUpdates());
		assertEquals(1, preview.getRules().get(0).getBlocks());
		assertEquals(4095, preview.getRules().get(1).getBlocks());
	}

	@Test
	void tileToTilePreviewCountsUpdates() {
		Map<ChunkFilter.BlockReplaceSource, ChunkFilter.BlockReplaceData> replace = new LinkedHashMap<>();
		replace.put(
				ChunkFilter.BlockReplaceSource.literalName("minecraft:stone")
						.withTileEntityMode(ChunkFilter.BlockReplaceTileEntityMode.REQUIRE_TILE_ENTITY),
				new ChunkFilter.BlockReplaceData("minecraft:barrel", tile("minecraft:barrel", 0, 0, 0)));

		CompoundTag root = root();
		root.put("block_entities", tileEntities(tile("minecraft:chest", 0, 0, 0)));

		ChunkFilter.BlockReplacePreviewData preview = new PreviewBlocks().preview(root, sections(), replace);

		assertEquals(1, preview.getBlocks());
		assertEquals(0, preview.getTileEntityAdditions());
		assertEquals(0, preview.getTileEntityRemovals());
		assertEquals(1, preview.getTileEntityUpdates());
		assertEquals(1, preview.getRules().get(0).getBlocks());
	}

	@Test
	void tileTargetReplacementRemovesExistingBlockEntitiesBeforeAddingNewOne() {
		Map<ChunkFilter.BlockReplaceSource, ChunkFilter.BlockReplaceData> replace = new LinkedHashMap<>();
		replace.put(
				ChunkFilter.BlockReplaceSource.literalName("minecraft:stone")
						.withTileEntityMode(ChunkFilter.BlockReplaceTileEntityMode.REQUIRE_TILE_ENTITY),
				new ChunkFilter.BlockReplaceData("minecraft:barrel", tile("minecraft:barrel", 0, 0, 0)));

		CompoundTag root = root();
		root.putInt("DataVersion", 2844);
		root.put("sections", sections());
		root.put("block_entities", tileEntities(
				tile("minecraft:chest", 0, 0, 0),
				tile("minecraft:chest", 0, 0, 0)));
		RegionChunk region = new RegionChunk(new Point2i(0, 0));
		region.setData(root);

		new ChunkFilter_21w43a.Blocks().replaceBlocks(new ChunkData(new Point2i(0, 0), region, null, null, true), replace);

		ListTag blockEntities = (ListTag) root.get("block_entities");
		assertEquals(1, blockEntities.size());
		CompoundTag tile = blockEntities.getCompound(0);
		assertEquals("minecraft:barrel", tile.getString("id"));
		assertEquals(0, tile.getInt("x"));
		assertEquals(0, tile.getInt("y"));
		assertEquals(0, tile.getInt("z"));
	}

	@Test
	void previewCountsOnlyBlocksInsideYRange() {
		Map<ChunkFilter.BlockReplaceSource, ChunkFilter.BlockReplaceData> replace = new LinkedHashMap<>();
		replace.put(
				ChunkFilter.BlockReplaceSource.literalName("minecraft:stone").withYRange(0, 0),
				new ChunkFilter.BlockReplaceData("minecraft:dirt"));

		ChunkFilter.BlockReplacePreviewData preview = new PreviewBlocks().preview(root(), sections(), replace);

		assertEquals(256, preview.getBlocks());
		assertEquals(1, preview.getSections());
		assertEquals(256, preview.getRules().get(0).getBlocks());
	}

	@Test
	void previewCompletesSyntheticAirSectionsOnlyInsideYRange() {
		Map<ChunkFilter.BlockReplaceSource, ChunkFilter.BlockReplaceData> replace = new LinkedHashMap<>();
		replace.put(
				ChunkFilter.BlockReplaceSource.literalName("minecraft:air").withYRange(16, 16),
				new ChunkFilter.BlockReplaceData("minecraft:glass"));

		ListTag sections = sections();
		sections.add(incompleteSection(1));

		ChunkFilter.BlockReplacePreviewData preview = new PreviewBlocks().preview(root(), sections, replace);

		assertEquals(256, preview.getBlocks());
		assertEquals(1, preview.getSections());
		assertEquals(1, preview.getCompletedAirSections());
		assertEquals(256, preview.getRules().get(0).getBlocks());
	}

	@Test
	void replaceBlocksChangesOnlyBlocksInsideYRange() {
		Map<ChunkFilter.BlockReplaceSource, ChunkFilter.BlockReplaceData> replace = new LinkedHashMap<>();
		replace.put(
				ChunkFilter.BlockReplaceSource.literalName("minecraft:stone").withYRange(0, 0),
				new ChunkFilter.BlockReplaceData("minecraft:dirt"));

		CompoundTag root = root();
		root.putInt("DataVersion", 2844);
		ListTag sections = sections();
		sections.getCompound(0).putByteArray("BlockLight", new byte[] { 1 });
		sections.getCompound(0).putByteArray("SkyLight", new byte[] { 1 });
		root.put("sections", sections);
		RegionChunk region = new RegionChunk(new Point2i(0, 0));
		region.setData(root);
		PreviewBlocks blocks = new PreviewBlocks();

		blocks.replaceBlocks(new ChunkData(new Point2i(0, 0), region, null, null, true), replace);

		assertEquals("minecraft:dirt", blocks.blockAt(root, 0));
		assertEquals("minecraft:stone", blocks.blockAt(root, 256));
		assertFalse(((ListTag) root.get("sections")).getCompound(0).containsKey("BlockLight"));
		assertFalse(((ListTag) root.get("sections")).getCompound(0).containsKey("SkyLight"));
	}

	@Test
	void earlyFlatPreviewAndExecutionRespectYAndBiomeConditions() {
		Map<ChunkFilter.BlockReplaceSource, ChunkFilter.BlockReplaceData> replace = new LinkedHashMap<>();
		replace.put(
				ChunkFilter.BlockReplaceSource.literalName("minecraft:stone")
						.withYRange(0, 3)
						.withBiomes(List.of("minecraft:forest")),
				new ChunkFilter.BlockReplaceData("minecraft:dirt"));

		CompoundTag level = new CompoundTag();
		level.putInt("xPos", 0);
		level.putInt("zPos", 0);
		ListTag sections = sectionsWithForestBiomeCell();
		sections.getCompound(0).putByteArray("BlockLight", new byte[] { 1 });
		sections.getCompound(0).putByteArray("SkyLight", new byte[] { 1 });
		level.put("Sections", sections);
		level.put("TileEntities", new ListTag());
		CompoundTag root = new CompoundTag();
		root.putInt("DataVersion", 2834);
		root.put("Level", level);
		EarlyPreviewBlocks blocks = new EarlyPreviewBlocks();

		ChunkFilter.BlockReplacePreviewData preview = blocks.preview(level, sections, replace);
		assertEquals(64, preview.getBlocks());
		assertEquals(64, preview.getRules().get(0).getBlocks());

		RegionChunk region = new RegionChunk(new Point2i(0, 0));
		region.setData(root);
		blocks.replaceBlocks(new ChunkData(new Point2i(0, 0), region, null, null, true), replace);

		assertEquals("minecraft:dirt", blocks.blockAt(level, 0));
		assertEquals("minecraft:stone", blocks.blockAt(level, 4));
		assertFalse(sections.getCompound(0).containsKey("BlockLight"));
		assertFalse(sections.getCompound(0).containsKey("SkyLight"));
	}

	@Test
	void earlyFlatBiomePaletteUsesThreeBitPackedIndices() {
		Map<ChunkFilter.BlockReplaceSource, ChunkFilter.BlockReplaceData> replace = new LinkedHashMap<>();
		replace.put(
				ChunkFilter.BlockReplaceSource.literalName("minecraft:stone")
						.withYRange(4, 7)
						.withBiomes(List.of("minecraft:forest")),
				new ChunkFilter.BlockReplaceData("minecraft:dirt"));

		CompoundTag level = new CompoundTag();
		level.putInt("xPos", 0);
		level.putInt("zPos", 0);
		ListTag sections = sections();
		sections.getCompound(0).put("biomes", packedBiomes(5, 20, 4));
		level.put("Sections", sections);
		level.put("TileEntities", new ListTag());
		CompoundTag root = new CompoundTag();
		root.putInt("DataVersion", 2834);
		root.put("Level", level);
		EarlyPreviewBlocks blocks = new EarlyPreviewBlocks();

		ChunkFilter.BlockReplacePreviewData preview = blocks.preview(level, sections, replace);
		assertEquals(64, preview.getBlocks());

		RegionChunk region = new RegionChunk(new Point2i(0, 0));
		region.setData(root);
		blocks.replaceBlocks(new ChunkData(new Point2i(0, 0), region, null, null, true), replace);

		assertEquals("minecraft:dirt", blocks.blockAt(level, blockIndexForBiomeCell(20)));
		assertEquals("minecraft:stone", blocks.blockAt(level, blockIndexForBiomeCell(16)));
	}

	@Test
	void modernBiomePaletteUsesFiveBitPackedIndices() {
		Map<ChunkFilter.BlockReplaceSource, ChunkFilter.BlockReplaceData> replace = new LinkedHashMap<>();
		replace.put(
				ChunkFilter.BlockReplaceSource.literalName("minecraft:stone")
						.withBiomes(List.of("minecraft:forest")),
				new ChunkFilter.BlockReplaceData("minecraft:dirt"));

		CompoundTag root = root();
		root.putInt("DataVersion", 2844);
		ListTag sections = sections();
		sections.getCompound(0).put("biomes", packedBiomes(17, 10, 16));
		root.put("sections", sections);
		RegionChunk region = new RegionChunk(new Point2i(0, 0));
		region.setData(root);
		PreviewBlocks blocks = new PreviewBlocks();

		ChunkFilter.BlockReplacePreviewData preview = blocks.preview(root, sections, replace);
		assertEquals(64, preview.getBlocks());

		blocks.replaceBlocks(new ChunkData(new Point2i(0, 0), region, null, null, true), replace);

		assertEquals("minecraft:dirt", blocks.blockAt(root, blockIndexForBiomeCell(10)));
		assertEquals("minecraft:stone", blocks.blockAt(root, blockIndexForBiomeCell(0)));
	}

	@Test
	void previewCountsOnlyBlocksInsideBiomeCell() {
		Map<ChunkFilter.BlockReplaceSource, ChunkFilter.BlockReplaceData> replace = new LinkedHashMap<>();
		replace.put(
				ChunkFilter.BlockReplaceSource.literalName("minecraft:stone").withBiomes(List.of("minecraft:forest")),
				new ChunkFilter.BlockReplaceData("minecraft:dirt"));

		ChunkFilter.BlockReplacePreviewData preview = new PreviewBlocks().preview(root(), sectionsWithForestBiomeCell(), replace);

		assertEquals(64, preview.getBlocks());
		assertEquals(1, preview.getSections());
		assertEquals(64, preview.getRules().get(0).getBlocks());
	}

	@Test
	void replaceBlocksChangesOnlyBlocksInsideBiomeCell() {
		Map<ChunkFilter.BlockReplaceSource, ChunkFilter.BlockReplaceData> replace = new LinkedHashMap<>();
		replace.put(
				ChunkFilter.BlockReplaceSource.literalName("minecraft:stone").withBiomes(List.of("minecraft:forest")),
				new ChunkFilter.BlockReplaceData("minecraft:dirt"));

		CompoundTag root = root();
		root.putInt("DataVersion", 2844);
		root.put("sections", sectionsWithForestBiomeCell());
		RegionChunk region = new RegionChunk(new Point2i(0, 0));
		region.setData(root);
		PreviewBlocks blocks = new PreviewBlocks();

		blocks.replaceBlocks(new ChunkData(new Point2i(0, 0), region, null, null, true), replace);

		assertEquals("minecraft:dirt", blocks.blockAt(root, 0));
		assertEquals("minecraft:stone", blocks.blockAt(root, 4));
	}

	@Test
	void biomeFilteredSyntheticAirSectionsFailClosedWhenBiomeIsUnknown() {
		Map<ChunkFilter.BlockReplaceSource, ChunkFilter.BlockReplaceData> forestReplace = new LinkedHashMap<>();
		forestReplace.put(
				ChunkFilter.BlockReplaceSource.literalName("minecraft:air").withYRange(16, 16).withBiomes(List.of("minecraft:forest")),
				new ChunkFilter.BlockReplaceData("minecraft:glass"));

		ListTag sections = sections();
		sections.add(incompleteSection(1));
		ChunkFilter.BlockReplacePreviewData forestPreview = new PreviewBlocks().preview(root(), sections, forestReplace);

		assertEquals(0, forestPreview.getBlocks());
		assertEquals(0, forestPreview.getCompletedAirSections());

		Map<ChunkFilter.BlockReplaceSource, ChunkFilter.BlockReplaceData> plainsReplace = new LinkedHashMap<>();
		plainsReplace.put(
				ChunkFilter.BlockReplaceSource.literalName("minecraft:air").withYRange(16, 16).withBiomes(List.of("minecraft:plains")),
				new ChunkFilter.BlockReplaceData("minecraft:glass"));

		ChunkFilter.BlockReplacePreviewData plainsPreview = new PreviewBlocks().preview(root(), sections, plainsReplace);

		assertEquals(0, plainsPreview.getBlocks());
		assertEquals(0, plainsPreview.getCompletedAirSections());

		CompoundTag root = root();
		root.putInt("DataVersion", 2844);
		root.put("sections", sections);
		RegionChunk region = new RegionChunk(new Point2i(0, 0));
		region.setData(root);
		new PreviewBlocks().replaceBlocks(new ChunkData(new Point2i(0, 0), region, null, null, true), plainsReplace);

		assertFalse(sections.getCompound(1).containsKey("block_states"));
	}

	@Test
	void syntheticAirDoesNotExposeInjectedPlainsToLaterRules() {
		Map<ChunkFilter.BlockReplaceSource, ChunkFilter.BlockReplaceData> replace = new LinkedHashMap<>();
		replace.put(
				ChunkFilter.BlockReplaceSource.literalName("minecraft:air").withYRange(16, 16),
				new ChunkFilter.BlockReplaceData("minecraft:glass"));
		replace.put(
				ChunkFilter.BlockReplaceSource.literalName("minecraft:air")
						.withYRange(16, 16)
						.withBiomes(List.of("minecraft:plains")),
				new ChunkFilter.BlockReplaceData("minecraft:gold_block"));

		ListTag sections = sections();
		sections.add(incompleteSection(1));
		PreviewBlocks blocks = new PreviewBlocks();
		ChunkFilter.BlockReplacePreviewData preview = blocks.preview(root(), sections, replace);

		assertEquals(256, preview.getBlocks());
		assertEquals(256, preview.getRules().get(0).getBlocks());
		assertEquals(0, preview.getRules().get(1).getBlocks());
		assertEquals(0, preview.getOverlappingBlocks());

		CompoundTag root = root();
		root.putInt("DataVersion", 2844);
		root.put("sections", sections);
		RegionChunk region = new RegionChunk(new Point2i(0, 0));
		region.setData(root);
		blocks.replaceBlocks(new ChunkData(new Point2i(0, 0), region, null, null, true), replace);

		assertEquals("minecraft:glass", blocks.blockAt(root, 1, 0));
	}

	@Test
	void trustedBiomePaletteStillAllowsSyntheticAirCompletion() {
		Map<ChunkFilter.BlockReplaceSource, ChunkFilter.BlockReplaceData> replace = new LinkedHashMap<>();
		replace.put(
				ChunkFilter.BlockReplaceSource.literalName("minecraft:air")
						.withYRange(16, 16)
						.withBiomes(List.of("minecraft:forest")),
				new ChunkFilter.BlockReplaceData("minecraft:glass"));

		ListTag sections = sections();
		CompoundTag incomplete = new CompoundTag();
		incomplete.putByte("Y", (byte) 1);
		incomplete.put("biomes", singletonBiomes("minecraft:forest"));
		sections.add(incomplete);
		PreviewBlocks blocks = new PreviewBlocks();

		ChunkFilter.BlockReplacePreviewData preview = blocks.preview(root(), sections, replace);
		assertEquals(256, preview.getBlocks());

		CompoundTag root = root();
		root.putInt("DataVersion", 2844);
		root.put("sections", sections);
		RegionChunk region = new RegionChunk(new Point2i(0, 0));
		region.setData(root);
		blocks.replaceBlocks(new ChunkData(new Point2i(0, 0), region, null, null, true), replace);

		assertEquals("minecraft:glass", blocks.blockAt(root, 1, 0));
	}

	@Test
	void earlyFlatSyntheticAirDoesNotMatchUnknownPlainsBiome() {
		Map<ChunkFilter.BlockReplaceSource, ChunkFilter.BlockReplaceData> replace = new LinkedHashMap<>();
		replace.put(
				ChunkFilter.BlockReplaceSource.literalName("minecraft:air")
						.withYRange(16, 16)
						.withBiomes(List.of("minecraft:plains")),
				new ChunkFilter.BlockReplaceData("minecraft:glass"));

		CompoundTag level = new CompoundTag();
		level.putInt("xPos", 0);
		level.putInt("zPos", 0);
		ListTag sections = sections();
		sections.add(incompleteSection(1));
		level.put("Sections", sections);
		level.put("TileEntities", new ListTag());
		CompoundTag root = new CompoundTag();
		root.putInt("DataVersion", 2834);
		root.put("Level", level);
		EarlyPreviewBlocks blocks = new EarlyPreviewBlocks();

		ChunkFilter.BlockReplacePreviewData preview = blocks.preview(level, sections, replace);
		assertEquals(0, preview.getBlocks());

		RegionChunk region = new RegionChunk(new Point2i(0, 0));
		region.setData(root);
		blocks.replaceBlocks(new ChunkData(new Point2i(0, 0), region, null, null, true), replace);

		assertFalse(sections.getCompound(1).containsKey("block_states"));
	}

	private CompoundTag root() {
		CompoundTag root = new CompoundTag();
		root.putInt("xPos", 0);
		root.putInt("zPos", 0);
		return root;
	}

	private ListTag sections() {
		ListTag sections = new ListTag();
		CompoundTag section = new CompoundTag();
		section.putByte("Y", (byte) 0);

		CompoundTag blockStates = new CompoundTag();
		ListTag palette = new ListTag();
		CompoundTag stone = new CompoundTag();
		stone.putString("Name", "minecraft:stone");
		palette.add(stone);
		blockStates.put("palette", palette);
		section.put("block_states", blockStates);

		sections.add(section);
		return sections;
	}

	private ListTag sectionsWithForestBiomeCell() {
		ListTag sections = sections();
		sections.getCompound(0).put("biomes", biomes("minecraft:plains", "minecraft:forest", 1L));
		return sections;
	}

	private CompoundTag biomes(String first, String second, long data) {
		CompoundTag biomes = new CompoundTag();
		ListTag palette = new ListTag();
		palette.addString(first);
		palette.addString(second);
		biomes.put("palette", palette);
		biomes.putLongArray("data", new long[] { data });
		return biomes;
	}

	private CompoundTag packedBiomes(int paletteSize, int selectedCell, int selectedPaletteIndex) {
		CompoundTag biomes = new CompoundTag();
		ListTag palette = new ListTag();
		for (int i = 0; i < paletteSize; i++) {
			palette.addString(i == selectedPaletteIndex ? "minecraft:forest" : "example:biome_" + i);
		}
		biomes.put("palette", palette);

		int bits = 32 - Integer.numberOfLeadingZeros(paletteSize - 1);
		int valuesPerLong = 64 / bits;
		long[] data = new long[Math.ceilDiv(64, valuesPerLong)];
		int longIndex = selectedCell / valuesPerLong;
		int bitOffset = selectedCell % valuesPerLong * bits;
		data[longIndex] |= (long) selectedPaletteIndex << bitOffset;
		biomes.putLongArray("data", data);
		return biomes;
	}

	private int blockIndexForBiomeCell(int biomeCell) {
		int x = biomeCell % 4 * 4;
		int z = biomeCell / 4 % 4 * 4;
		int y = biomeCell / 16 * 4;
		return y * 256 + z * 16 + x;
	}

	private CompoundTag incompleteSection(int y) {
		CompoundTag section = new CompoundTag();
		section.putByte("Y", (byte) y);
		section.put("biomes", new CompoundTag());
		return section;
	}

	private CompoundTag singletonBiomes(String biome) {
		CompoundTag biomes = new CompoundTag();
		ListTag palette = new ListTag();
		palette.addString(biome);
		biomes.put("palette", palette);
		return biomes;
	}

	private ListTag tileEntities(CompoundTag... tiles) {
		ListTag tileEntities = new ListTag();
		for (CompoundTag tile : tiles) {
			tileEntities.add(tile);
		}
		return tileEntities;
	}

	private CompoundTag tile(String id, int x, int y, int z) {
		CompoundTag tile = new CompoundTag();
		tile.putString("id", id);
		tile.putInt("x", x);
		tile.putInt("y", y);
		tile.putInt("z", z);
		return tile;
	}

	private static class PreviewBlocks extends ChunkFilter_21w43a.Blocks {

		protected ChunkFilter.BlockReplacePreviewData preview(CompoundTag root, ListTag sections, Map<ChunkFilter.BlockReplaceSource, ChunkFilter.BlockReplaceData> replace) {
			return previewReplaceBlocks(root, sections, "block_entities", replace);
		}

		private String blockAt(CompoundTag root, int index) {
			return blockAt(root, 0, index);
		}

		private String blockAt(CompoundTag root, int sectionIndex, int index) {
			CompoundTag section = ((ListTag) root.get("sections")).getCompound(sectionIndex);
			CompoundTag blockStates = section.getCompound("block_states");
			return getBlockAt(index, blockStates.getLongArray("data"), blockStates.getListTag("palette")).getString("Name");
		}
	}

	private static class TrackingBlocks extends PreviewBlocks {
		private int biomeReads;
		private int tileLocationIndexBuilds;

		@Override
		protected String getBiomeAt(CompoundTag section, int blockIndex) {
			biomeReads++;
			return super.getBiomeAt(section, blockIndex);
		}

		@Override
		protected java.util.Set<String> getTileEntityLocations(ListTag tileEntities) {
			tileLocationIndexBuilds++;
			return super.getTileEntityLocations(tileEntities);
		}
	}

	private static class EarlyPreviewBlocks extends ChunkFilter_21w37a.Blocks {

		private ChunkFilter.BlockReplacePreviewData preview(CompoundTag level, ListTag sections, Map<ChunkFilter.BlockReplaceSource, ChunkFilter.BlockReplaceData> replace) {
			return previewReplaceBlocks(level, sections, "TileEntities", replace);
		}

		private String blockAt(CompoundTag level, int index) {
			CompoundTag section = level.getListTag("Sections").getCompound(0);
			CompoundTag blockStates = section.getCompoundTag("block_states");
			return getBlockAt(index, blockStates.getLongArray("data"), blockStates.getListTag("palette")).getString("Name");
		}
	}
}
