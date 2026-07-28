package net.querz.mcaselector.io.job;

import net.querz.mcaselector.changer.fields.ReplaceBlocksField;
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.io.JobHandler;
import net.querz.mcaselector.io.RegionDirectories;
import net.querz.mcaselector.io.WorldDirectories;
import net.querz.mcaselector.io.mca.CompressionType;
import net.querz.mcaselector.io.mca.Region;
import net.querz.mcaselector.io.mca.RegionChunk;
import net.querz.mcaselector.io.mca.RegionMCAFile;
import net.querz.mcaselector.selection.Selection;
import net.querz.mcaselector.util.point.Point2i;
import net.querz.mcaselector.util.progress.Progress;
import net.querz.mcaselector.version.VersionHandler;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.ListTag;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FieldChangerIntegrationTest {

	@BeforeAll
	static void initializeVersionHandlers() {
		VersionHandler.init();
	}

	@Test
	void savesTheChangedChunkThenRelightsExistingCrossRegionNeighborsWithoutSidecars(@TempDir Path tempDir)
			throws Exception {
		WorldDirectories world = createWorld(tempDir);
		Point2i center = new Point2i(31, 31);
		List<Point2i> chunks = squareAround(center);
		writeRegionChunks(world, chunks);
		Map<Path, FileSnapshot> sidecars = writeSidecarSentinels(world, chunks);
		Selection selection = new Selection();
		selection.addChunk(center);

		FieldChanger.changeNBTFields(world, List.of(parse("literal(minecraft:stone)=minecraft:dirt")), false,
				selection, new TestProgress(), true);
		waitForJobs();

		for (Point2i chunk : chunks) {
			CompoundTag root = loadChunk(world, chunk);
			assertEquals(0, root.getByte("isLightOn"), "relight flag for " + chunk);
			assertEquals(chunk.equals(center) ? "minecraft:dirt" : "minecraft:stone", firstPaletteName(root));
		}
		for (Map.Entry<Path, FileSnapshot> sidecar : sidecars.entrySet()) {
			assertSnapshot(sidecar.getKey(), sidecar.getValue());
		}
	}

	@Test
	void zeroMatchDoesNotRewriteAnyRegionOrCreateSidecars(@TempDir Path tempDir) throws Exception {
		WorldDirectories world = createWorld(tempDir);
		Point2i center = new Point2i(31, 31);
		List<Point2i> chunks = squareAround(center);
		writeRegionChunks(world, chunks);
		ageRegionFiles(world);
		Map<Path, FileSnapshot> before = snapshotRegionFiles(world);
		Selection selection = new Selection();
		selection.addChunk(center);

		FieldChanger.changeNBTFields(world, List.of(parse("literal(minecraft:diamond_block)=minecraft:dirt")), false,
				selection, new TestProgress(), true);
		waitForJobs();

		assertEquals(before.keySet(), snapshotRegionFiles(world).keySet());
		for (Map.Entry<Path, FileSnapshot> file : before.entrySet()) {
			assertSnapshot(file.getKey(), file.getValue());
		}
		assertTrue(isDirectoryEmpty(world.getPoi().toPath()));
		assertTrue(isDirectoryEmpty(world.getEntities().toPath()));
	}

	private WorldDirectories createWorld(Path root) throws IOException {
		Path region = Files.createDirectories(root.resolve("region"));
		Path poi = Files.createDirectories(root.resolve("poi"));
		Path entities = Files.createDirectories(root.resolve("entities"));
		return new WorldDirectories(region.toFile(), poi.toFile(), entities.toFile());
	}

	private void writeRegionChunks(WorldDirectories world, List<Point2i> chunks) throws IOException {
		Map<Point2i, List<Point2i>> grouped = new HashMap<>();
		for (Point2i chunk : chunks) {
			grouped.computeIfAbsent(chunk.chunkToRegion(), ignored -> new ArrayList<>()).add(chunk);
		}
		for (Map.Entry<Point2i, List<Point2i>> entry : grouped.entrySet()) {
			File file = new File(world.getRegion(), FileHelper.createMCAFileName(entry.getKey()));
			RegionMCAFile mca = new RegionMCAFile(file);
			for (Point2i location : entry.getValue()) {
				RegionChunk chunk = new RegionChunk(location);
				chunk.setData(modernRoot(location));
				chunk.setCompressionType(CompressionType.ZLIB);
				mca.setChunkAt(location, chunk);
			}
			mca.saveWithTempFile();
		}
	}

	private Map<Path, FileSnapshot> writeSidecarSentinels(WorldDirectories world, List<Point2i> chunks) throws IOException {
		Map<Path, FileSnapshot> result = new HashMap<>();
		for (Point2i region : chunks.stream().map(Point2i::chunkToRegion).distinct().toList()) {
			String name = FileHelper.createMCAFileName(region);
			Path poi = world.getPoi().toPath().resolve(name);
			Path entities = world.getEntities().toPath().resolve(name);
			Files.writeString(poi, "poi sentinel " + region);
			Files.writeString(entities, "entities sentinel " + region);
			setOldModifiedTime(poi);
			setOldModifiedTime(entities);
			result.put(poi, snapshot(poi));
			result.put(entities, snapshot(entities));
		}
		return result;
	}

	private Map<Path, FileSnapshot> snapshotRegionFiles(WorldDirectories world) throws IOException {
		Map<Path, FileSnapshot> result = new HashMap<>();
		try (var files = Files.list(world.getRegion().toPath())) {
			for (Path file : files.toList()) {
				result.put(file, snapshot(file));
			}
		}
		return result;
	}

	private void ageRegionFiles(WorldDirectories world) throws IOException {
		try (var files = Files.list(world.getRegion().toPath())) {
			for (Path file : files.toList()) {
				setOldModifiedTime(file);
			}
		}
	}

	private void setOldModifiedTime(Path file) throws IOException {
		Files.setLastModifiedTime(file, FileTime.from(Instant.parse("2020-01-01T00:00:00Z")));
	}

	private FileSnapshot snapshot(Path file) throws IOException {
		return new FileSnapshot(Files.readAllBytes(file), Files.getLastModifiedTime(file));
	}

	private void assertSnapshot(Path file, FileSnapshot expected) throws IOException {
		assertArrayEquals(expected.contents(), Files.readAllBytes(file));
		assertEquals(expected.modified(), Files.getLastModifiedTime(file));
	}

	private boolean isDirectoryEmpty(Path directory) throws IOException {
		try (var files = Files.list(directory)) {
			return files.findAny().isEmpty();
		}
	}

	private CompoundTag loadChunk(WorldDirectories world, Point2i location) throws IOException {
		RegionDirectories dirs = world.makeRegionDirectories(location.chunkToRegion());
		Region region = Region.loadRegionFile(dirs);
		return region.getRegion().getChunkAt(location).getData();
	}

	private void waitForJobs() throws InterruptedException {
		Instant deadline = Instant.now().plus(Duration.ofSeconds(10));
		while (JobHandler.getActiveJobs() > 0 && Instant.now().isBefore(deadline)) {
			Thread.sleep(10);
		}
		assertEquals(0, JobHandler.getActiveJobs(), "field change jobs did not finish");
	}

	private List<Point2i> squareAround(Point2i center) {
		List<Point2i> result = new ArrayList<>();
		for (int x = -1; x <= 1; x++) {
			for (int z = -1; z <= 1; z++) {
				result.add(center.add(x, z));
			}
		}
		return result;
	}

	private ReplaceBlocksField parse(String value) {
		ReplaceBlocksField field = new ReplaceBlocksField();
		assertTrue(field.parseNewValue(value));
		return field;
	}

	private CompoundTag modernRoot(Point2i location) {
		CompoundTag root = new CompoundTag();
		root.putInt("DataVersion", 4671);
		root.putInt("xPos", location.getX());
		root.putInt("zPos", location.getZ());
		root.putByte("isLightOn", (byte) 1);
		CompoundTag state = new CompoundTag();
		state.putString("Name", "minecraft:stone");
		ListTag palette = new ListTag();
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

	private static final class TestProgress implements Progress {

		private final AtomicInteger current = new AtomicInteger();
		private volatile boolean cancelled;

		@Override public void setMax(int max) {}
		@Override public void updateProgress(String msg, int progress) { current.set(progress); }
		@Override public void done(String msg) {}
		@Override public boolean taskCancelled() { return cancelled; }
		@Override public void cancelTask() { cancelled = true; }
		@Override public void incrementProgress(String msg) { current.incrementAndGet(); }
		@Override public void incrementProgress(String msg, int progress) { current.addAndGet(progress); }
		@Override public void setMessage(String msg) {}
	}

	private record FileSnapshot(byte[] contents, FileTime modified) {}
}
