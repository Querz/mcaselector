package net.querz.mcaselector.io.mca;

import net.querz.mcaselector.changer.Field;
import net.querz.mcaselector.changer.FieldType;
import net.querz.mcaselector.changer.fields.ReplaceBlocksField;
import net.querz.mcaselector.io.RegionDirectories;
import net.querz.mcaselector.selection.Selection;
import net.querz.mcaselector.util.point.Point2i;
import net.querz.mcaselector.version.VersionHandler;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.ListTag;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RegionFieldChangeTest {

	@BeforeAll
	static void initializeVersionHandlers() {
		VersionHandler.init();
	}

	@Test
	void tracksAndSavesOnlyActuallyChangedReplaceBlocksChunks(@TempDir Path tempDir) throws IOException {
		Point2i location = new Point2i(0, 0);
		RegionDirectories directories = createRegion(tempDir, location, modernRoot(location, "minecraft:stone"));
		Region region = Region.loadRegionFile(directories);
		Selection selection = new Selection();
		selection.addChunk(location);

		Region.FieldChangeResult result = region.applyFieldChangesTracked(
				List.of(parse("literal(minecraft:stone)=minecraft:dirt")), false, selection, () -> false);

		assertTrue(result.dirty());
		assertFalse(result.cancelled());
		assertEquals(Set.of(location), result.replaceBlocksChangedChunks());
		region.saveRegionWithTempFile();

		Region reloaded = Region.loadRegionFile(directories);
		assertEquals("minecraft:dirt", firstPaletteName(reloaded.getRegion().getChunkAt(location).getData()));
	}

	@Test
	void unmatchedReplaceBlocksLeavesRegionClean(@TempDir Path tempDir) throws IOException {
		Point2i location = new Point2i(0, 0);
		RegionDirectories directories = createRegion(tempDir, location, modernRoot(location, "minecraft:stone"));
		Region region = Region.loadRegionFile(directories);
		Selection selection = new Selection();
		selection.addChunk(location);

		Region.FieldChangeResult result = region.applyFieldChangesTracked(
				List.of(parse("literal(minecraft:diamond_block)=minecraft:dirt")), false, selection, () -> false);

		assertFalse(result.dirty());
		assertFalse(result.cancelled());
		assertTrue(result.replaceBlocksChangedChunks().isEmpty());
	}

	@Test
	void relightsOnlyExistingTargetsWhoseFlagIsStillSet(@TempDir Path tempDir) throws IOException {
		Point2i existing = new Point2i(32, 31);
		Point2i missing = new Point2i(33, 31);
		RegionDirectories directories = createRegion(tempDir, existing, modernRoot(existing, "minecraft:stone"));
		Region region = Region.loadRegionFile(directories);

		Region.RelightResult result = region.relightChunks(Set.of(existing, missing), () -> false);

		assertTrue(result.dirty());
		assertFalse(result.cancelled());
		region.saveRegionWithTempFile();

		Region reloaded = Region.loadRegionFile(directories);
		assertEquals(0, reloaded.getRegion().getChunkAt(existing).getData().getByte("isLightOn"));
		assertFalse(reloaded.relightChunks(Set.of(existing), () -> false).dirty());
	}

	@Test
	void preservesLegacyDirtySaveWhenALaterMixedFieldThrows(@TempDir Path tempDir) throws IOException {
		Point2i location = new Point2i(0, 0);
		RegionDirectories directories = createRegion(tempDir, location, modernRoot(location, "minecraft:stone"));
		Region region = Region.loadRegionFile(directories);
		Selection selection = new Selection();
		selection.addChunk(location);
		Field<Integer> mutating = testField(data -> data.region().getData().putInt("testMarker", 1));
		Field<Integer> throwing = testField(data -> { throw new IllegalStateException("expected"); });

		Region.FieldChangeResult result = region.applyFieldChangesTracked(
				List.of(mutating, throwing), false, selection, () -> false);

		assertTrue(result.dirty());
		assertEquals(1, region.getRegion().getChunkAt(location).getData().getInt("testMarker"));
	}

	@Test
	void replaceBlocksOnlyFailureAbortsTheRegionWithChunkCoordinates(@TempDir Path tempDir) throws IOException {
		Point2i first = new Point2i(0, 0);
		Point2i second = new Point2i(1, 0);
		RegionDirectories directories = createRegion(tempDir, first,
				modernRoot(first, "minecraft:stone"), second, modernRoot(second, "minecraft:stone"));
		Region region = Region.loadRegionFile(directories);
		Selection selection = new Selection();
		selection.addChunk(first);
		selection.addChunk(second);
		ReplaceBlocksField throwing = new ReplaceBlocksField() {
			private int calls;

			@Override
			public boolean applyWithResult(ChunkData data, boolean force) {
				if (++calls == 2) {
					throw new IllegalStateException("expected");
				}
				data.region().getData().putInt("testMarker", 1);
				return true;
			}
		};

		Region.FieldChangeException failure = assertThrows(Region.FieldChangeException.class,
				() -> region.applyFieldChangesTracked(List.of(throwing), false, selection, () -> false));

		assertEquals(second, failure.chunk());
		Region reloaded = Region.loadRegionFile(directories);
		assertFalse(reloaded.getRegion().getChunkAt(first).getData().containsKey("testMarker"));
	}

	private RegionDirectories createRegion(Path tempDir, Point2i chunkLocation, CompoundTag root) throws IOException {
		return createRegion(tempDir, chunkLocation, root, null, null);
	}

	private RegionDirectories createRegion(Path tempDir, Point2i chunkLocation, CompoundTag root,
			Point2i secondLocation, CompoundTag secondRoot) throws IOException {
		Point2i regionLocation = chunkLocation.chunkToRegion();
		File regionFile = tempDir.resolve("r." + regionLocation.getX() + "." + regionLocation.getZ() + ".mca").toFile();
		RegionMCAFile mca = new RegionMCAFile(regionFile);
		RegionChunk chunk = new RegionChunk(chunkLocation);
		chunk.setData(root);
		chunk.setCompressionType(CompressionType.ZLIB);
		mca.setChunkAt(chunkLocation, chunk);
		if (secondLocation != null) {
			RegionChunk second = new RegionChunk(secondLocation);
			second.setData(secondRoot);
			second.setCompressionType(CompressionType.ZLIB);
			mca.setChunkAt(secondLocation, second);
		}
		mca.saveWithTempFile();
		return new RegionDirectories(regionLocation, regionFile, null, null);
	}

	private ReplaceBlocksField parse(String value) {
		ReplaceBlocksField field = new ReplaceBlocksField();
		assertTrue(field.parseNewValue(value));
		return field;
	}

	private Field<Integer> testField(java.util.function.Consumer<ChunkData> change) {
		return new Field<>(FieldType.CUSTOM, 1) {
			@Override public Integer getOldValue(ChunkData data) { return null; }
			@Override public void change(ChunkData data) { change.accept(data); }
			@Override public void force(ChunkData data) { change(data); }
		};
	}

	private CompoundTag modernRoot(Point2i location, String blockName) {
		CompoundTag root = new CompoundTag();
		root.putInt("DataVersion", 4671);
		root.putInt("xPos", location.getX());
		root.putInt("zPos", location.getZ());
		root.putByte("isLightOn", (byte) 1);
		ListTag palette = new ListTag();
		CompoundTag state = new CompoundTag();
		state.putString("Name", blockName);
		palette.add(state);
		CompoundTag blockStates = new CompoundTag();
		blockStates.put("palette", palette);
		CompoundTag section = new CompoundTag();
		section.putByte("Y", (byte) 0);
		section.put("block_states", blockStates);
		ListTag sections = new ListTag();
		sections.add(section);
		root.put("sections", sections);
		return root;
	}

	private String firstPaletteName(CompoundTag root) {
		return root.getListTag("sections").getCompound(0)
				.getCompoundTag("block_states").getListTag("palette").getCompound(0).getString("Name");
	}
}
