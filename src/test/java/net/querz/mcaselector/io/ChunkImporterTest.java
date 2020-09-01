package net.querz.mcaselector.io;

import junit.framework.TestCase;
import net.querz.mcaselector.Config;
import net.querz.mcaselector.MCASelectorTestCase;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.progress.Progress;
import net.querz.mcaselector.property.DataProperty;

import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

public class ChunkImporterTest extends MCASelectorTestCase {

	public void testNoSourceSelection_NoTargetSelection_NoOffset() {
		Config.setWorldDir(getResourceFile("import/target"));
		MCAFilePipe.init();

		ChunkImporter.importChunks(
				getResourceFile("import/source"),
				new TestProgress(() -> {

				}, 60),
				true, // headless
				true, // overwrite
				null, // sourceSelection
				null, // targetSelection
				null, // ranges
				new Point2i(0, 0),
				new DataProperty<>()
		);
	}

	@Override
	public void setUp() {
		Config.setLoadThreads(1);
		Config.setProcessThreads(1);
		Config.setWriteThreads(1);
		Config.setMaxLoadedFiles(10);
		Config.setCacheDir(new File("tmp"));
	}

	@Override
	public void tearDown() throws InterruptedException, TimeoutException, ExecutionException {
		FutureTask<Object> f = new FutureTask<>(() -> {}, null);
		MCAFilePipe.cancelAllJobs(f);
		f.get(60, TimeUnit.SECONDS);
	}
}
