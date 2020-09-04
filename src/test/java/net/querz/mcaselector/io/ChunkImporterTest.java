package net.querz.mcaselector.io;

import net.querz.mcaselector.Config;
import net.querz.mcaselector.MCASelectorTestCase;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.property.DataProperty;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.ListTag;
import net.querz.nbt.tag.Tag;
import org.apache.commons.io.FileUtils;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ChunkImporterTest extends MCASelectorTestCase {


	/*
	* -1 -1 source:
	*
	* 		-9/-5	-8/-5
	*		-9/-4	-8/-4
	*
	* -1 -1 target
	*
	* 				-8/-6	-7/-6
	* 				-8/-5	-7/-5
	* */

	public void test_NoSourceSelection_NoTargetSelection_NoOffset() throws IOException {
		Config.setWorldDir(new File("tmp/import/target"));

		FileUtils.copyDirectory(getResourceFile("import"), new File("tmp/import"));

		String source1before = calculateFileMD5(new File("tmp/import/source/r.0.-1.mca"));
		String source2before = calculateFileMD5(new File("tmp/import/source/r.-1.-1.mca"));

		TestProgress progress;
		ChunkImporter.importChunks(
				new File("tmp/import/source"),
				progress = new TestProgress(() -> {}, 60),
				true, // headless
				true, // overwrite
				null, // sourceSelection
				null, // targetSelection
				null, // ranges
				new Point2i(0, 0), // offset
				new DataProperty<>()
		);

		progress.join();

		String source1after = calculateFileMD5(new File("tmp/import/source/r.0.-1.mca"));
		String source2after = calculateFileMD5(new File("tmp/import/source/r.-1.-1.mca"));

		// make sure that we never touch the source
		assertEquals(source1before, source1after);
		assertEquals("CB01AFFCCC28DD4CFF94C48D337D4140", source1before);
		assertEquals("442C56C5849D37AD19DC54B280295921", source2before);
		assertEquals(source2before, source2after);
		assertEquals("CB01AFFCCC28DD4CFF94C48D337D4140", source1after);
		assertEquals("442C56C5849D37AD19DC54B280295921", source2after);

		// make sure that all chunks exist in the new files
		assertChunkExists(new File("tmp/import/target/r.0.-1.mca"), new Point2i(10, -24));
		assertChunkExists(new File("tmp/import/target/r.-1.-1.mca"), new Point2i(-9, -4));
		assertChunkExists(new File("tmp/import/target/r.-1.-1.mca"), new Point2i(-9, -5));
		assertChunkExists(new File("tmp/import/target/r.-1.-1.mca"), new Point2i(-8, -5));
		assertChunkExists(new File("tmp/import/target/r.-1.-1.mca"), new Point2i(-8, -4));
		assertChunkExists(new File("tmp/import/target/r.-1.-1.mca"), new Point2i(-7, -6));
		assertChunkExists(new File("tmp/import/target/r.-1.-1.mca"), new Point2i(-7, -5));
		assertChunkExists(new File("tmp/import/target/r.-1.-1.mca"), new Point2i(-8, -6));

		// make sure that chunks were successfully copied to existing file
		assertChunkEquals(new File("tmp/import/source/r.-1.-1.mca"), new Point2i(-9, -4), new File("tmp/import/target/r.-1.-1.mca"), new Point2i(-9, -4));
		assertChunkEquals(new File("tmp/import/source/r.-1.-1.mca"), new Point2i(-9, -5), new File("tmp/import/target/r.-1.-1.mca"), new Point2i(-9, -5));
		assertChunkEquals(new File("tmp/import/source/r.-1.-1.mca"), new Point2i(-8, -4), new File("tmp/import/target/r.-1.-1.mca"), new Point2i(-8, -4));
		assertChunkEquals(new File("tmp/import/source/r.-1.-1.mca"), new Point2i(-8, -5), new File("tmp/import/target/r.-1.-1.mca"), new Point2i(-8, -5));

		// make sure that existing chunks are still there
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-8, -6), new File("tmp/import/target/r.-1.-1.mca"), new Point2i(-8, -6));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-7, -6), new File("tmp/import/target/r.-1.-1.mca"), new Point2i(-7, -6));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-7, -5), new File("tmp/import/target/r.-1.-1.mca"), new Point2i(-7, -5));

		// make sure that new mca file has been created and contains the required chunk
		assertChunkEquals(getResourceFile("import/source/r.0.-1.mca"), new Point2i(10, -24), new File("tmp/import/target/r.0.-1.mca"), new Point2i(10, -24));
	}

	public void test_NoSourceSelection_NoTargetSelection_Offset() throws IOException {
		Config.setWorldDir(new File("tmp/import/target"));

		FileUtils.copyDirectory(getResourceFile("import"), new File("tmp/import"));

		String source1before = calculateFileMD5(new File("tmp/import/source/r.0.-1.mca"));
		String source2before = calculateFileMD5(new File("tmp/import/source/r.-1.-1.mca"));

		TestProgress progress;
		ChunkImporter.importChunks(
				new File("tmp/import/source"),
				progress = new TestProgress(() -> {}, 60),
				true, // headless
				true, // overwrite
				null, // sourceSelection
				null, // targetSelection
				null, // ranges
				new Point2i(2, -2), // offset
				new DataProperty<>()
		);

		progress.join();

		String source1after = calculateFileMD5(new File("tmp/import/source/r.0.-1.mca"));
		String source2after = calculateFileMD5(new File("tmp/import/source/r.-1.-1.mca"));

		// make sure that we never touch the source
		assertEquals(source1before, source1after);
		assertEquals("CB01AFFCCC28DD4CFF94C48D337D4140", source1before);
		assertEquals("442C56C5849D37AD19DC54B280295921", source2before);
		assertEquals(source2before, source2after);
		assertEquals("CB01AFFCCC28DD4CFF94C48D337D4140", source1after);
		assertEquals("442C56C5849D37AD19DC54B280295921", source2after);

		// make sure that all chunks exist in the new files
		assertChunkExists(new File("tmp/import/target/r.0.-1.mca"), new Point2i(12, -26));
		assertChunkExists(new File("tmp/import/target/r.-1.-1.mca"), new Point2i(-7, -6));
		assertChunkExists(new File("tmp/import/target/r.-1.-1.mca"), new Point2i(-7, -7));
		assertChunkExists(new File("tmp/import/target/r.-1.-1.mca"), new Point2i(-6, -7));
		assertChunkExists(new File("tmp/import/target/r.-1.-1.mca"), new Point2i(-7, -6));
		assertChunkExists(new File("tmp/import/target/r.-1.-1.mca"), new Point2i(-7, -6));
		assertChunkExists(new File("tmp/import/target/r.-1.-1.mca"), new Point2i(-7, -5));
		assertChunkExists(new File("tmp/import/target/r.-1.-1.mca"), new Point2i(-8, -6));

		// make sure that chunks were successfully copied to existing file
		assertChunkEqualsIgnoreLocations(new File("tmp/import/source/r.-1.-1.mca"), new Point2i(-9, -4), new File("tmp/import/target/r.-1.-1.mca"), new Point2i(-7, -6));
		assertChunkEqualsIgnoreLocations(new File("tmp/import/source/r.-1.-1.mca"), new Point2i(-9, -5), new File("tmp/import/target/r.-1.-1.mca"), new Point2i(-7, -7));
		assertChunkEqualsIgnoreLocations(new File("tmp/import/source/r.-1.-1.mca"), new Point2i(-8, -4), new File("tmp/import/target/r.-1.-1.mca"), new Point2i(-6, -6));
		assertChunkEqualsIgnoreLocations(new File("tmp/import/source/r.-1.-1.mca"), new Point2i(-8, -5), new File("tmp/import/target/r.-1.-1.mca"), new Point2i(-6, -7));

		// make sure that existing chunks are still there
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-8, -6), new File("tmp/import/target/r.-1.-1.mca"), new Point2i(-8, -6));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-8, -5), new File("tmp/import/target/r.-1.-1.mca"), new Point2i(-8, -5));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-7, -5), new File("tmp/import/target/r.-1.-1.mca"), new Point2i(-7, -5));

		// make sure that new mca file has been created and contains the required chunk
		assertChunkEqualsIgnoreLocations(getResourceFile("import/source/r.0.-1.mca"), new Point2i(10, -24), new File("tmp/import/target/r.0.-1.mca"), new Point2i(12, -26));
	}

	public void test_NoSourceSelection_NoTargetSelection_Offset_NewFiles() throws IOException {
		Config.setWorldDir(new File("tmp/import/target"));

		FileUtils.copyDirectory(getResourceFile("import"), new File("tmp/import"));

		String source1before = calculateFileMD5(new File("tmp/import/source/r.0.-1.mca"));
		String source2before = calculateFileMD5(new File("tmp/import/source/r.-1.-1.mca"));

		TestProgress progress;
		ChunkImporter.importChunks(
				new File("tmp/import/source"),
				progress = new TestProgress(() -> {}, 60),
				true, // headless
				true, // overwrite
				null, // sourceSelection
				null, // targetSelection
				null, // ranges
				new Point2i(8, 4), // this moves the 4 chunks from -1/-1 to -1/-1, 0/-1, -1/0, 0/0, creating 2 new files and merging the 4th with 0/-1
				new DataProperty<>()
		);

		progress.join();

		String source1after = calculateFileMD5(new File("tmp/import/source/r.0.-1.mca"));
		String source2after = calculateFileMD5(new File("tmp/import/source/r.-1.-1.mca"));

		// make sure that we never touch the source
		assertEquals(source1before, source1after);
		assertEquals("CB01AFFCCC28DD4CFF94C48D337D4140", source1before);
		assertEquals("442C56C5849D37AD19DC54B280295921", source2before);
		assertEquals(source2before, source2after);
		assertEquals("CB01AFFCCC28DD4CFF94C48D337D4140", source1after);
		assertEquals("442C56C5849D37AD19DC54B280295921", source2after);

		// make sure that all chunks exist in the new files
		assertChunkExists(new File("tmp/import/target/r.0.-1.mca"), new Point2i(18, -20));
		assertChunkExists(new File("tmp/import/target/r.-1.-1.mca"), new Point2i(-8, -5));
		assertChunkExists(new File("tmp/import/target/r.-1.-1.mca"), new Point2i(-7, -6));
		assertChunkExists(new File("tmp/import/target/r.-1.-1.mca"), new Point2i(-8, -6));
		assertChunkExists(new File("tmp/import/target/r.-1.-1.mca"), new Point2i(-7, -5));

		// make sure that chunks were successfully copied to existing file
		assertChunkEqualsIgnoreLocations(new File("tmp/import/source/r.-1.-1.mca"), new Point2i(-9, -4), new File("tmp/import/target/r.-1.0.mca"), new Point2i(-1, 0));
		assertChunkEqualsIgnoreLocations(new File("tmp/import/source/r.-1.-1.mca"), new Point2i(-9, -5), new File("tmp/import/target/r.-1.-1.mca"), new Point2i(-1, -1));
		assertChunkEqualsIgnoreLocations(new File("tmp/import/source/r.-1.-1.mca"), new Point2i(-8, -4), new File("tmp/import/target/r.0.0.mca"), new Point2i(0, 0));
		assertChunkEqualsIgnoreLocations(new File("tmp/import/source/r.-1.-1.mca"), new Point2i(-8, -5), new File("tmp/import/target/r.0.-1.mca"), new Point2i(0, -1));

		// make sure that existing chunks are still there
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-8, -6), new File("tmp/import/target/r.-1.-1.mca"), new Point2i(-8, -6));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-7, -6), new File("tmp/import/target/r.-1.-1.mca"), new Point2i(-7, -6));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-7, -5), new File("tmp/import/target/r.-1.-1.mca"), new Point2i(-7, -5));

		// make sure that new mca file has been created and contains the required chunk
		assertChunkEqualsIgnoreLocations(getResourceFile("import/source/r.0.-1.mca"), new Point2i(10, -24), new File("tmp/import/target/r.0.-1.mca"), new Point2i(18, -20));
	}

	public void test_NoSourceSelection_TargetSelection_NoOffset() throws IOException {
		Config.setWorldDir(new File("tmp/import/target"));

		FileUtils.copyDirectory(getResourceFile("import"), new File("tmp/import"));

		String source1before = calculateFileMD5(new File("tmp/import/source/r.0.-1.mca"));
		String source2before = calculateFileMD5(new File("tmp/import/source/r.-1.-1.mca"));

		Map<Point2i, Set<Point2i>> targetSelection = new HashMap<>();
		targetSelection.put(new Point2i(-1, -1), Collections.singleton(new Point2i(-9, -4)));
		targetSelection.put(new Point2i(0, -1), null);

		TestProgress progress;
		ChunkImporter.importChunks(
				new File("tmp/import/source"),
				progress = new TestProgress(() -> {}, 60),
				true, // headless
				true, // overwrite
				null, // sourceSelection
				new SelectionData(targetSelection, false), // targetSelection
				null, // ranges
				new Point2i(0, 0), // offset
				new DataProperty<>()
		);

		progress.join();

		String source1after = calculateFileMD5(new File("tmp/import/source/r.0.-1.mca"));
		String source2after = calculateFileMD5(new File("tmp/import/source/r.-1.-1.mca"));

		// make sure that we never touch the source
		assertEquals(source1before, source1after);
		assertEquals("CB01AFFCCC28DD4CFF94C48D337D4140", source1before);
		assertEquals("442C56C5849D37AD19DC54B280295921", source2before);
		assertEquals(source2before, source2after);
		assertEquals("CB01AFFCCC28DD4CFF94C48D337D4140", source1after);
		assertEquals("442C56C5849D37AD19DC54B280295921", source2after);

		// make sure that all chunks exist in the new files
		assertChunkExists(new File("tmp/import/target/r.0.-1.mca"), new Point2i(10, -24));
		assertChunkExists(new File("tmp/import/target/r.-1.-1.mca"), new Point2i(-9, -4));
		assertNoChunkExists(new File("tmp/import/target/r.-1.-1.mca"), new Point2i(-9, -5));
		assertChunkExists(new File("tmp/import/target/r.-1.-1.mca"), new Point2i(-8, -5));
		assertNoChunkExists(new File("tmp/import/target/r.-1.-1.mca"), new Point2i(-8, -4));
		assertChunkExists(new File("tmp/import/target/r.-1.-1.mca"), new Point2i(-7, -6));
		assertChunkExists(new File("tmp/import/target/r.-1.-1.mca"), new Point2i(-7, -5));
		assertChunkExists(new File("tmp/import/target/r.-1.-1.mca"), new Point2i(-8, -6));

		// make sure that chunks were successfully copied to existing file
		assertChunkEquals(new File("tmp/import/source/r.-1.-1.mca"), new Point2i(-9, -4), new File("tmp/import/target/r.-1.-1.mca"), new Point2i(-9, -4));

		// make sure that existing chunks are still there
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-8, -5), new File("tmp/import/target/r.-1.-1.mca"), new Point2i(-8, -5));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-8, -6), new File("tmp/import/target/r.-1.-1.mca"), new Point2i(-8, -6));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-7, -6), new File("tmp/import/target/r.-1.-1.mca"), new Point2i(-7, -6));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-7, -5), new File("tmp/import/target/r.-1.-1.mca"), new Point2i(-7, -5));

		// make sure that new mca file has been created and contains the required chunk
		assertChunkEquals(getResourceFile("import/source/r.0.-1.mca"), new Point2i(10, -24), new File("tmp/import/target/r.0.-1.mca"), new Point2i(10, -24));
	}

	private void assertChunkExists(File file, Point2i chunk) throws IOException {
		MCAFile f = MCAFile.read(file);
		assertNotNull(f.getLoadedChunkData(chunk));
		assertFalse(f.getLoadedChunkData(chunk).isEmpty());
		assertNotNull(f.getLoadedChunkData(chunk).getData());
	}

	private void assertNoChunkExists(File file, Point2i chunk) throws IOException {
		MCAFile f = MCAFile.read(file);
		if (f.getLoadedChunkData(chunk) == null) {
			return;
		}
		assertTrue(f.getLoadedChunkData(chunk).isEmpty());
	}

	private void assertChunkEquals(File expectedFile, Point2i expectedChunk, File actualFile, Point2i actualChunk) throws IOException {
		MCAFile expected = MCAFile.read(expectedFile);
		MCAFile actual = MCAFile.read(actualFile);

		MCAChunkData e = expected.getLoadedChunkData(expectedChunk);
		MCAChunkData a = actual.getLoadedChunkData(actualChunk);
		// UUIDs are changed so we need to compare the data ourselves to ignore them
		assertTrue(nbtEqualsIgnoreFields(e.getData(), a.getData(), "UUID"));
	}

	private void assertChunkEqualsIgnoreLocations(File expectedFile, Point2i expectedChunk, File actualFile, Point2i actualChunk) throws IOException {
		MCAFile expected = MCAFile.read(expectedFile);
		MCAFile actual = MCAFile.read(actualFile);

		MCAChunkData e = expected.getLoadedChunkData(expectedChunk);
		MCAChunkData a = actual.getLoadedChunkData(actualChunk);

		assertTrue(nbtEqualsIgnoreFields(e.getData(), a.getData(),
				"UUID", "xPos", "zPos", "x", "z", "X", "Z", "posX", "posZ", "Pos", "xTile", "zTile",
				"SleepingX", "SleepingZ", "APX", "APZ", "BoundX", "BoundZ", "TileX", "TileZ", "pos"));
	}

	private boolean nbtEqualsIgnoreFields(Tag<?> a, Tag<?> b, String... ignored) {
		if (a.getID() != b.getID()) {
			return false;
		}
		switch (a.getID()) {
			case 0:
				return true;
			case 9: // ListTag
				if (((ListTag<?>) a).size() != ((ListTag<?>) b).size() || ((ListTag<?>) a).getTypeClass() != ((ListTag<?>) b).getTypeClass()) {
					return false;
				}
				for (int i = 0; i < ((ListTag<?>) a).size(); i++) {
					if (!nbtEqualsIgnoreFields(((ListTag<?>) a).get(i), ((ListTag<?>) b).get(i), ignored)) {
						return false;
					}
				}
				return true;
			case 10: // CompoundTag
				if (((CompoundTag) a).size() != ((CompoundTag) b).size()) {
					return false;
				}
				l: for (Map.Entry<String, Tag<?>> entry : (CompoundTag) a) {
					if (((CompoundTag) b).containsKey(entry.getKey())) {
						// skip ignored fields
						for (String i : ignored) {
							if (i.equals(entry.getKey())) {
								continue l;
							}
						}
						if (!nbtEqualsIgnoreFields(entry.getValue(), ((CompoundTag) b).get(entry.getKey()), ignored)) {
							return false;
						}
					} else {
						return false;
					}
				}
				return true;
			default:
				return a.equals(b);
		}
	}

	@Override
	public void setUp() {
		Config.setLoadThreads(1);
		Config.setProcessThreads(1);
		Config.setWriteThreads(1);
		Config.setMaxLoadedFiles(10);
		Config.setDebug(true);
		Config.setCacheDir(new File("tmp"));
		Config.getCacheDir().mkdirs();
		MCAFilePipe.init();
	}

	@Override
	public void tearDown() throws InterruptedException, TimeoutException, ExecutionException, IOException {
		FutureTask<Object> f = new FutureTask<>(() -> {}, null);
		MCAFilePipe.cancelAllJobs(f);
		f.get(60, TimeUnit.SECONDS);
//		FileUtils.deleteDirectory(new File("tmp"));
	}
}
