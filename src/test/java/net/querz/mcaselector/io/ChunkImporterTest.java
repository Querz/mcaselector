package net.querz.mcaselector.io;

import net.querz.mcaselector.Config;
import net.querz.mcaselector.MCASelectorTestCase;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.property.DataProperty;
import org.apache.commons.io.FileUtils;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import static org.junit.Assert.*;

public class ChunkImporterTest extends MCASelectorTestCase {

	public void testNoSourceSelection_NoTargetSelection_NoOffset() throws IOException {
		Config.setWorldDir(new File("tmp/import/target"));

		FileUtils.copyDirectory(getResourceFile("import"), new File("tmp/import"));

		String source1before = calculateFileMD5(new File("tmp/import/source/r.0.-1.mca"));
		String source2before = calculateFileMD5(new File("tmp/import/source/r.-1.-1.mca"));
		String target2before = calculateFileMD5(new File("tmp/import/target/r.-1.-1.mca"));

		TestProgress progress;

		ChunkImporter.importChunks(
				new File("tmp/import/source"),
				progress = new TestProgress(() -> {
					System.out.println("DONE");
				}, 60),
				true, // headless
				true, // overwrite
				null, // sourceSelection
				null, // targetSelection
				null, // ranges
				new Point2i(0, 0),
				new DataProperty<>()
		);

		progress.join();

		String source1after = calculateFileMD5(new File("tmp/import/source/r.0.-1.mca"));
		String source2after = calculateFileMD5(new File("tmp/import/source/r.-1.-1.mca"));
		String target1after = calculateFileMD5(new File("tmp/import/target/r.0.-1.mca"));
		String target2after = calculateFileMD5(new File("tmp/import/target/r.-1.-1.mca"));

		assertEquals(source1before, source1after);
		assertEquals("CB01AFFCCC28DD4CFF94C48D337D4140", source1before);
		assertEquals("442C56C5849D37AD19DC54B280295921", source2before);
		assertEquals(source2before, source2after);
		assertEquals("CB01AFFCCC28DD4CFF94C48D337D4140", source1after);
		assertEquals("442C56C5849D37AD19DC54B280295921", source2after);
		assertNotEquals(target2before, target2after);

		assertChunkExists(new File("tmp/import/target/r.0.-1.mca"), new Point2i(10, -24));
		assertChunkExists(new File("tmp/import/target/r.-1.-1.mca"), new Point2i(-9, -4));
		assertChunkExists(new File("tmp/import/target/r.-1.-1.mca"), new Point2i(-9, -5));
		assertChunkExists(new File("tmp/import/target/r.-1.-1.mca"), new Point2i(-8, -5));
		assertChunkExists(new File("tmp/import/target/r.-1.-1.mca"), new Point2i(-8, -4));
		assertChunkExists(new File("tmp/import/target/r.-1.-1.mca"), new Point2i(-7, -6));
		assertChunkExists(new File("tmp/import/target/r.-1.-1.mca"), new Point2i(-7, -5));
		assertChunkExists(new File("tmp/import/target/r.-1.-1.mca"), new Point2i(-8, -6));

		assertChunkEquals(new File("tmp/import/source/r.-1.-1.mca"), new Point2i(-9, -4), new File("tmp/import/target/r.-1.-1.mca"), new Point2i(-9, -4));

	}

	private void assertChunkExists(File file, Point2i chunk) throws IOException {
		MCAFile f = MCAFile.read(file);
		assertNotNull(f.getLoadedChunkData(chunk));
		assertFalse(f.getLoadedChunkData(chunk).isEmpty());
		assertNotNull(f.getLoadedChunkData(chunk).getData());
	}

	private void assertChunkEquals(File expectedFile, Point2i expectedChunk, File actualFile, Point2i actualChunk) throws IOException {
		MCAFile expected = MCAFile.read(expectedFile);
		MCAFile actual = MCAFile.read(actualFile);

		MCAChunkData e = expected.getLoadedChunkData(expectedChunk);
		MCAChunkData a = actual.getLoadedChunkData(actualChunk);
		assertEquals(e.getData(), a.getData());
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
		FileUtils.deleteDirectory(new File("tmp"));
	}
}
