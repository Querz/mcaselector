package net.querz.mcaselector.io;

import net.querz.mcaselector.Config;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.property.DataProperty;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.ListTag;
import net.querz.nbt.tag.Tag;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import static net.querz.mcaselector.MCASelectorTestCase.*;
import static org.junit.Assert.*;

public class ChunkImporterTest {

	@Rule
	public TestName name = new TestName();

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

	@Test
	public void test_NoSourceSelection_NoTargetSelection_NoOffset() throws IOException {
		TestProgress progress;
		ChunkImporter.importChunks(
				tmpDir("source"),
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

		assertSourceFilesAreUnchanged();

		// make sure that all chunks exist in the new files
		assertChunkExists(tmpFile("target", "r.0.-1.mca"), new Point2i(10, -24));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-9, -4));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-9, -5));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -5));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -4));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -6));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -5));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -6));

		// make sure that chunks were successfully copied
		assertChunkEquals(tmpFile("source", "r.-1.-1.mca"), new Point2i(-9, -4), tmpFile("target", "r.-1.-1.mca"), new Point2i(-9, -4));
		assertChunkEquals(tmpFile("source", "r.-1.-1.mca"), new Point2i(-9, -5), tmpFile("target", "r.-1.-1.mca"), new Point2i(-9, -5));
		assertChunkEquals(tmpFile("source", "r.-1.-1.mca"), new Point2i(-8, -4), tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -4));
		assertChunkEquals(tmpFile("source", "r.-1.-1.mca"), new Point2i(-8, -5), tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -5));
		assertChunkEquals(tmpFile("source", "r.0.-1.mca"), new Point2i(10, -24), tmpFile("target", "r.0.-1.mca"), new Point2i(10, -24));

		// make sure that existing chunks are still there
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-8, -6), tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -6));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-7, -6), tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -6));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-7, -5), tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -5));
	}

	@Test
	public void test_NoSourceSelection_NoTargetSelection_Offset() throws IOException {
		TestProgress progress;
		ChunkImporter.importChunks(
				tmpDir("source"),
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

		assertSourceFilesAreUnchanged();

		// make sure that all chunks exist in the new files
		assertChunkExists(tmpFile("target", "r.0.-1.mca"), new Point2i(12, -26));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -6));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -7));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-6, -7));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -6));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -6));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -5));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -6));

		// make sure that chunks were successfully copied
		assertChunkEqualsIgnoreLocations(tmpFile("source", "r.-1.-1.mca"), new Point2i(-9, -4), tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -6));
		assertChunkEqualsIgnoreLocations(tmpFile("source", "r.-1.-1.mca"), new Point2i(-9, -5), tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -7));
		assertChunkEqualsIgnoreLocations(tmpFile("source", "r.-1.-1.mca"), new Point2i(-8, -4), tmpFile("target", "r.-1.-1.mca"), new Point2i(-6, -6));
		assertChunkEqualsIgnoreLocations(tmpFile("source", "r.-1.-1.mca"), new Point2i(-8, -5), tmpFile("target", "r.-1.-1.mca"), new Point2i(-6, -7));
		assertChunkEqualsIgnoreLocations(tmpFile("source", "r.0.-1.mca"), new Point2i(10, -24), tmpFile("target", "r.0.-1.mca"), new Point2i(12, -26));

		// make sure that existing chunks are still there
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-8, -6), tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -6));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-8, -5), tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -5));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-7, -5), tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -5));
	}

	@Test
	public void test_NoSourceSelection_NoTargetSelection_Offset_NewFiles() throws IOException {
		TestProgress progress;
		ChunkImporter.importChunks(
				tmpDir("source"),
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

		assertSourceFilesAreUnchanged();

		// make sure that all chunks exist in the new files
		assertChunkExists(tmpFile("target", "r.0.-1.mca"), new Point2i(18, -20));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -5));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -6));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -6));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -5));

		// make sure that chunks were successfully copied
		assertChunkEqualsIgnoreLocations(tmpFile("source", "r.-1.-1.mca"), new Point2i(-9, -4), tmpFile("target", "r.-1.0.mca"), new Point2i(-1, 0));
		assertChunkEqualsIgnoreLocations(tmpFile("source", "r.-1.-1.mca"), new Point2i(-9, -5), tmpFile("target", "r.-1.-1.mca"), new Point2i(-1, -1));
		assertChunkEqualsIgnoreLocations(tmpFile("source", "r.-1.-1.mca"), new Point2i(-8, -4), tmpFile("target", "r.0.0.mca"), new Point2i(0, 0));
		assertChunkEqualsIgnoreLocations(tmpFile("source", "r.-1.-1.mca"), new Point2i(-8, -5), tmpFile("target", "r.0.-1.mca"), new Point2i(0, -1));
		assertChunkEqualsIgnoreLocations(tmpFile("source", "r.0.-1.mca"), new Point2i(10, -24), tmpFile("target", "r.0.-1.mca"), new Point2i(18, -20));

		// make sure that existing chunks are still there
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-8, -6), tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -6));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-7, -6), tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -6));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-7, -5), tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -5));
	}

	@Test
	public void test_NoSourceSelection_TargetSelection_NoOffset() throws IOException {
		Map<Point2i, Set<Point2i>> targetSelection = new HashMap<>();
		targetSelection.put(new Point2i(-1, -1), Collections.singleton(new Point2i(-9, -4)));
		targetSelection.put(new Point2i(0, -1), null);

		TestProgress progress;
		ChunkImporter.importChunks(
				tmpDir("source"),
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

		assertSourceFilesAreUnchanged();

		// make sure that all chunks exist in the new files
		assertChunkExists(tmpFile("target", "r.0.-1.mca"), new Point2i(10, -24));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-9, -4));
		assertNoChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-9, -5));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -5));
		assertNoChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -4));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -6));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -5));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -6));

		// make sure that chunks were successfully copied
		assertChunkEquals(tmpFile("source", "r.-1.-1.mca"), new Point2i(-9, -4), tmpFile("target", "r.-1.-1.mca"), new Point2i(-9, -4));
		assertChunkEquals(tmpFile("source", "r.0.-1.mca"), new Point2i(10, -24), tmpFile("target", "r.0.-1.mca"), new Point2i(10, -24));

		// make sure that existing chunks are still there
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-8, -5), tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -5));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-8, -6), tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -6));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-7, -6), tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -6));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-7, -5), tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -5));
	}

	@Test
	public void test_NoSourceSelection_TargetSelection_Offset() throws IOException {
		Map<Point2i, Set<Point2i>> targetSelection = new HashMap<>();
		targetSelection.put(new Point2i(0, 0), Collections.singleton(new Point2i(0, 0)));
		targetSelection.put(new Point2i(0, -1), null);

		TestProgress progress;
		ChunkImporter.importChunks(
				tmpDir("source"),
				progress = new TestProgress(() -> {}, 60),
				true, // headless
				true, // overwrite
				null, // sourceSelection
				new SelectionData(targetSelection, false), // targetSelection
				null, // ranges
				new Point2i(9, 5), // offset
				new DataProperty<>()
		);

		progress.join();

		assertSourceFilesAreUnchanged();

		// make sure that all chunks exist in the new files
		assertChunkExists(tmpFile("target", "r.0.-1.mca"), new Point2i(19, -19));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -6));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -5));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -5));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -6));
		assertChunkExists(tmpFile("target", "r.0.0.mca"), new Point2i(0, 0));
		assertNoChunkExists(tmpFile("target", "r.0.0.mca"), new Point2i(1, 0));
		assertNoChunkExists(tmpFile("target", "r.0.0.mca"), new Point2i(1, 1));
		assertNoChunkExists(tmpFile("target", "r.0.0.mca"), new Point2i(0, 1));
		assertNoChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-9, -5));
		assertNoChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-9, -4));
		assertNoChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -4));

		// make sure that chunks were successfully copied
		assertChunkEqualsIgnoreLocations(tmpFile("source", "r.0.-1.mca"), new Point2i(10, -24), tmpFile("target", "r.0.-1.mca"), new Point2i(19, -19));
		assertChunkEqualsIgnoreLocations(tmpFile("source", "r.-1.-1.mca"), new Point2i(-9, -5), tmpFile("target", "r.0.0.mca"), new Point2i(0, 0));

		// make sure that existing chunks are still there
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-8, -5), tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -5));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-8, -6), tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -6));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-7, -6), tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -6));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-7, -5), tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -5));
	}

	@Test
	public void test_NoSourceSelection_InvertedTargetSelection_NoOffset() throws IOException {
		Map<Point2i, Set<Point2i>> targetSelection = new HashMap<>();
		targetSelection.put(new Point2i(-1, -1), Collections.singleton(new Point2i(-9, -4)));
		targetSelection.put(new Point2i(0, -1), null);

		TestProgress progress;
		ChunkImporter.importChunks(
				tmpDir("source"),
				progress = new TestProgress(() -> {}, 60),
				true, // headless
				true, // overwrite
				null, // sourceSelection
				new SelectionData(targetSelection, true), // targetSelection
				null, // ranges
				new Point2i(0, 0), // offset
				new DataProperty<>()
		);

		progress.join();

		assertSourceFilesAreUnchanged();

		// make sure that all chunks exist in the new files
		assertNoChunkExists(tmpFile("target", "r.0.-1.mca"), new Point2i(10, -24));
		assertNoChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-9, -4));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-9, -5));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -5));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -4));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -6));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -5));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -6));

		// make sure that chunks were successfully copied
		assertChunkEquals(tmpFile("source", "r.-1.-1.mca"), new Point2i(-9, -5), tmpFile("target", "r.-1.-1.mca"), new Point2i(-9, -5));
		assertChunkEquals(tmpFile("source", "r.-1.-1.mca"), new Point2i(-8, -4), tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -4));

		// make sure that existing chunks are still there
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-8, -5), tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -5));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-8, -6), tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -6));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-7, -6), tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -6));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-7, -5), tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -5));
	}

	@Test
	public void test_NoSourceSelection_InvertedTargetSelection_Offset() throws IOException {
		Map<Point2i, Set<Point2i>> targetSelection = new HashMap<>();
		targetSelection.put(new Point2i(0, 0), Collections.singleton(new Point2i(0, 0)));
		targetSelection.put(new Point2i(0, -1), null);

		TestProgress progress;
		ChunkImporter.importChunks(
				tmpDir("source"),
				progress = new TestProgress(() -> {}, 60),
				true, // headless
				true, // overwrite
				null, // sourceSelection
				new SelectionData(targetSelection, true), // targetSelection
				null, // ranges
				new Point2i(9, 5), // offset
				new DataProperty<>()
		);

		progress.join();

		assertSourceFilesAreUnchanged();

		// make sure that all chunks exist in the new files
		assertNoChunkExists(tmpFile("target", "r.0.-1.mca"), new Point2i(19, -19));
		assertNoChunkExists(tmpFile("target", "r.0.-1.mca"), new Point2i(10, -24));
		assertNoChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-9, -4));
		assertNoChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-9, -5));
		assertNoChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -4));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -6));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -5));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -6));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -5));
		assertChunkExists(tmpFile("target", "r.0.0.mca"), new Point2i(0, 1));
		assertChunkExists(tmpFile("target", "r.0.0.mca"), new Point2i(1, 1));
		assertChunkExists(tmpFile("target", "r.0.0.mca"), new Point2i(1, 0));
		assertNoChunkExists(tmpFile("target", "r.0.0.mca"), new Point2i(0, 0));

		// make sure that chunks were successfully copied
		assertChunkEqualsIgnoreLocations(tmpFile("source", "r.-1.-1.mca"), new Point2i(-9, -4), tmpFile("target", "r.0.0.mca"), new Point2i(0, 1));
		assertChunkEqualsIgnoreLocations(tmpFile("source", "r.-1.-1.mca"), new Point2i(-8, -4), tmpFile("target", "r.0.0.mca"), new Point2i(1, 1));
		assertChunkEqualsIgnoreLocations(tmpFile("source", "r.-1.-1.mca"), new Point2i(-8, -5), tmpFile("target", "r.0.0.mca"), new Point2i(1, 0));

		// make sure that existing chunks are still there
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-8, -5), tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -5));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-8, -6), tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -6));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-7, -6), tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -6));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-7, -5), tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -5));
	}

	@Test
	public void test_SourceSelection_NoTargetSelection_NoOffset() throws IOException {
		Map<Point2i, Set<Point2i>> sourceSelection = new HashMap<>();
		sourceSelection.put(new Point2i(-1, -1), Collections.singleton(new Point2i(-9, -4)));
		sourceSelection.put(new Point2i(0, -1), null);

		TestProgress progress;
		ChunkImporter.importChunks(
				tmpDir("source"),
				progress = new TestProgress(() -> {}, 60),
				true, // headless
				true, // overwrite
				new SelectionData(sourceSelection, false), // sourceSelection
				null, // targetSelection
				null, // ranges
				new Point2i(0, 0), // offset
				new DataProperty<>()
		);

		progress.join();

		assertSourceFilesAreUnchanged();

		// make sure that all chunks exist in the new files
		assertChunkExists(tmpFile("target", "r.0.-1.mca"), new Point2i(10, -24));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-9, -4));
		assertNoChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-9, -5));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -5));
		assertNoChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -4));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -6));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -5));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -6));

		// make sure that chunks were successfully copied
		assertChunkEquals(tmpFile("source", "r.-1.-1.mca"), new Point2i(-9, -4), tmpFile("target", "r.-1.-1.mca"), new Point2i(-9, -4));
		assertChunkEquals(tmpFile("source", "r.0.-1.mca"), new Point2i(10, -24), tmpFile("target", "r.0.-1.mca"), new Point2i(10, -24));

		// make sure that existing chunks are still there
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-8, -5), tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -5));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-8, -6), tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -6));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-7, -6), tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -6));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-7, -5), tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -5));
	}

	@Test
	public void test_SourceSelection_NoTargetSelection_Offset() throws IOException {
		Map<Point2i, Set<Point2i>> sourceSelection = new HashMap<>();
		sourceSelection.put(new Point2i(-1, -1), Collections.singleton(new Point2i(-9, -4)));
		sourceSelection.put(new Point2i(0, -1), null);

		TestProgress progress;
		ChunkImporter.importChunks(
				tmpDir("source"),
				progress = new TestProgress(() -> {}, 60),
				true, // headless
				true, // overwrite
				new SelectionData(sourceSelection, false), // sourceSelection
				null, // targetSelection
				null, // ranges
				new Point2i(9, 4), // offset
				new DataProperty<>()
		);

		progress.join();

		assertSourceFilesAreUnchanged();

		// make sure that all chunks exist in the new files
		assertChunkExists(tmpFile("target", "r.0.-1.mca"), new Point2i(19, -20));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -6));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -5));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -5));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -6));
		assertChunkExists(tmpFile("target", "r.0.0.mca"), new Point2i(0, 0));
		assertNoChunkExists(tmpFile("target", "r.0.0.mca"), new Point2i(1, 0));
		assertNoChunkExists(tmpFile("target", "r.0.0.mca"), new Point2i(1, 1));
		assertNoChunkExists(tmpFile("target", "r.0.0.mca"), new Point2i(0, 1));
		assertNoChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-9, -5));
		assertNoChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-9, -4));
		assertNoChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -4));

		// make sure that chunks were successfully copied
		assertChunkEqualsIgnoreLocations(tmpFile("source", "r.0.-1.mca"), new Point2i(10, -24), tmpFile("target", "r.0.-1.mca"), new Point2i(19, -20));
		assertChunkEqualsIgnoreLocations(tmpFile("source", "r.-1.-1.mca"), new Point2i(-9, -4), tmpFile("target", "r.0.0.mca"), new Point2i(0, 0));

		// make sure that existing chunks are still there
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-8, -5), tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -5));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-8, -6), tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -6));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-7, -6), tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -6));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-7, -5), tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -5));
	}

	@Test
	public void test_SourceSelection_TargetSelection_NoOffset() throws IOException {
		Map<Point2i, Set<Point2i>> sourceSelection = new HashMap<>();
		sourceSelection.put(new Point2i(-1, -1), new HashSet<>(Arrays.asList(new Point2i(-9, -4), new Point2i(-8, -4))));
		sourceSelection.put(new Point2i(0, -1), null);

		Map<Point2i, Set<Point2i>> targetSelection = new HashMap<>();
		targetSelection.put(new Point2i(-1, -1), Collections.singleton(new Point2i(-8, -4)));
		targetSelection.put(new Point2i(0, -1), null);

		TestProgress progress;
		ChunkImporter.importChunks(
				tmpDir("source"),
				progress = new TestProgress(() -> {}, 60),
				true, // headless
				true, // overwrite
				new SelectionData(sourceSelection, false), // sourceSelection
				new SelectionData(targetSelection, false), // targetSelection
				null, // ranges
				new Point2i(0, 0), // offset
				new DataProperty<>()
		);

		progress.join();

		assertSourceFilesAreUnchanged();

		// make sure that all chunks exist in the new files
		assertChunkExists(tmpFile("target", "r.0.-1.mca"), new Point2i(10, -24));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -4));
		assertNoChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-9, -4));
		assertNoChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-9, -5));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -5));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -6));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -5));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -6));

		// make sure that chunks were successfully copied
		assertChunkEquals(tmpFile("source", "r.-1.-1.mca"), new Point2i(-8, -4), tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -4));
		assertChunkEquals(tmpFile("source", "r.0.-1.mca"), new Point2i(10, -24), tmpFile("target", "r.0.-1.mca"), new Point2i(10, -24));

		// make sure that existing chunks are still there
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-8, -5), tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -5));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-8, -6), tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -6));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-7, -6), tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -6));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-7, -5), tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -5));
	}

	@Test
	public void test_SourceSelection_TargetSelection_NoOffset_NoTargetRegion() throws IOException {
		Map<Point2i, Set<Point2i>> sourceSelection = new HashMap<>();
		sourceSelection.put(new Point2i(-1, -1), new HashSet<>(Arrays.asList(new Point2i(-9, -4), new Point2i(-8, -4))));
		sourceSelection.put(new Point2i(0, -1), null);

		Map<Point2i, Set<Point2i>> targetSelection = new HashMap<>();
		targetSelection.put(new Point2i(-1, -1), Collections.singleton(new Point2i(-8, -4)));
		// don't select this target region and see what happens

		TestProgress progress;
		ChunkImporter.importChunks(
				tmpDir("source"),
				progress = new TestProgress(() -> {}, 60),
				true, // headless
				true, // overwrite
				new SelectionData(sourceSelection, false), // sourceSelection
				new SelectionData(targetSelection, false), // targetSelection
				null, // ranges
				new Point2i(0, 0), // offset
				new DataProperty<>()
		);

		progress.join();

		assertSourceFilesAreUnchanged();

		// make sure that all chunks exist in the new files
		assertNoChunkExists(tmpFile("target", "r.0.-1.mca"), new Point2i(10, -24));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -4));
		assertNoChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-9, -4));
		assertNoChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-9, -5));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -5));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -6));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -5));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -6));

		// make sure that chunks were successfully copied
		assertChunkEquals(tmpFile("source", "r.-1.-1.mca"), new Point2i(-8, -4), tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -4));

		// make sure that existing chunks are still there
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-8, -5), tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -5));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-8, -6), tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -6));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-7, -6), tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -6));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-7, -5), tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -5));
	}

	@Test
	public void test_SourceSelection_TargetSelection_Offset() throws IOException {
		Map<Point2i, Set<Point2i>> sourceSelection = new HashMap<>();
		sourceSelection.put(new Point2i(-1, -1), new HashSet<>(Arrays.asList(new Point2i(-9, -4), new Point2i(-8, -4), new Point2i(-9, -5))));
		sourceSelection.put(new Point2i(0, -1), null);

		Map<Point2i, Set<Point2i>> targetSelection = new HashMap<>();
		targetSelection.put(new Point2i(0, 0), new HashSet<>(Arrays.asList(new Point2i(0, 0), new Point2i(1, 1))));
		targetSelection.put(new Point2i(0, -1), Collections.singleton(new Point2i(19, -19)));

		TestProgress progress;
		ChunkImporter.importChunks(
				tmpDir("source"),
				progress = new TestProgress(() -> {}, 60),
				true, // headless
				true, // overwrite
				new SelectionData(sourceSelection, false), // sourceSelection
				new SelectionData(targetSelection, false), // targetSelection
				null, // ranges
				new Point2i(9, 5), // offset
				new DataProperty<>()
		);

		progress.join();

		assertSourceFilesAreUnchanged();

		// make sure that all chunks exist in the new files
		assertChunkExists(tmpFile("target", "r.0.-1.mca"), new Point2i(19, -19));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -6));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -5));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -6));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -5));
		assertNoChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-9, -5));
		assertNoChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-9, -4));
		assertNoChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -4));
		assertNoChunkExists(tmpFile("target", "r.0.0.mca"), new Point2i(1, 0));
		assertNoChunkExists(tmpFile("target", "r.0.0.mca"), new Point2i(0, 1));
		assertChunkExists(tmpFile("target", "r.0.0.mca"), new Point2i(0, 0));
		assertChunkExists(tmpFile("target", "r.0.0.mca"), new Point2i(1, 1));

		// make sure that chunks were successfully copied to existing file
		assertChunkEqualsIgnoreLocations(tmpFile("source", "r.-1.-1.mca"), new Point2i(-9, -5), tmpFile("target", "r.0.0.mca"), new Point2i(0, 0));
		assertChunkEqualsIgnoreLocations(tmpFile("source", "r.-1.-1.mca"), new Point2i(-8, -4), tmpFile("target", "r.0.0.mca"), new Point2i(1, 1));
		assertChunkEqualsIgnoreLocations(tmpFile("source", "r.0.-1.mca"), new Point2i(10, -24), tmpFile("target", "r.0.-1.mca"), new Point2i(19, -19));

		// make sure that existing chunks are still there
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-8, -5), tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -5));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-8, -6), tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -6));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-7, -6), tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -6));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-7, -5), tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -5));
	}

	@Test
	public void test_SourceSelection_InvertedTargetSelection_NoOffset() throws IOException {
		Map<Point2i, Set<Point2i>> sourceSelection = new HashMap<>();
		sourceSelection.put(new Point2i(-1, -1), new HashSet<>(Arrays.asList(new Point2i(-9, -4), new Point2i(-8, -4))));
		sourceSelection.put(new Point2i(0, -1), null);

		Map<Point2i, Set<Point2i>> targetSelection = new HashMap<>();
		targetSelection.put(new Point2i(-1, -1), Collections.singleton(new Point2i(-8, -4)));
		targetSelection.put(new Point2i(0, -1), null);

		TestProgress progress;
		ChunkImporter.importChunks(
				tmpDir("source"),
				progress = new TestProgress(() -> {}, 60),
				true, // headless
				true, // overwrite
				new SelectionData(sourceSelection, false), // sourceSelection
				new SelectionData(targetSelection, true), // targetSelection
				null, // ranges
				new Point2i(0, 0), // offset
				new DataProperty<>()
		);

		progress.join();

		assertSourceFilesAreUnchanged();

		// make sure that all chunks exist in the new files
		assertNoChunkExists(tmpFile("target", "r.0.-1.mca"), new Point2i(10, -24));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -6));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -5));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -6));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -5));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-9, -4));
		assertNoChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-9, -5));
		assertNoChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -4));

		// make sure that chunks were successfully copied
		assertChunkEquals(tmpFile("source", "r.-1.-1.mca"), new Point2i(-9, -4), tmpFile("target", "r.-1.-1.mca"), new Point2i(-9, -4));

		// make sure that existing chunks are still there
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-8, -5), tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -5));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-8, -6), tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -6));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-7, -6), tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -6));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-7, -5), tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -5));
	}

	@Test
	public void test_SourceSelection_InvertedTargetSelection_Offset() throws IOException {
		Map<Point2i, Set<Point2i>> sourceSelection = new HashMap<>();
		sourceSelection.put(new Point2i(-1, -1), new HashSet<>(Arrays.asList(new Point2i(-9, -4), new Point2i(-8, -4))));
		sourceSelection.put(new Point2i(0, -1), null);

		Map<Point2i, Set<Point2i>> targetSelection = new HashMap<>();
		targetSelection.put(new Point2i(0, 0), new HashSet<>(Arrays.asList(new Point2i(1, 0), new Point2i(0, 1))));
		targetSelection.put(new Point2i(0, -1), null);

		TestProgress progress;
		ChunkImporter.importChunks(
				tmpDir("source"),
				progress = new TestProgress(() -> {}, 60),
				true, // headless
				true, // overwrite
				new SelectionData(sourceSelection, false), // sourceSelection
				new SelectionData(targetSelection, true), // targetSelection
				null, // ranges
				new Point2i(9, 5), // offset
				new DataProperty<>()
		);

		progress.join();

		assertSourceFilesAreUnchanged();

		// make sure that all chunks exist in the new files
		assertNoChunkExists(tmpFile("target", "r.0.-1.mca"), new Point2i(19, -19));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -6));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -5));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -6));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -5));
		assertChunkExists(tmpFile("target", "r.0.0.mca"), new Point2i(1, 1));
		assertNoChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-9, -5));
		assertNoChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -4));
		assertNoChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-9, -4));
		assertNoChunkExists(tmpFile("target", "r.0.0.mca"), new Point2i(0, 0));
		assertNoChunkExists(tmpFile("target", "r.0.0.mca"), new Point2i(1, 0));
		assertNoChunkExists(tmpFile("target", "r.0.0.mca"), new Point2i(0, 1));

		// make sure that chunks were successfully copied
		assertChunkEqualsIgnoreLocations(tmpFile("source", "r.-1.-1.mca"), new Point2i(-8, -4), tmpFile("target", "r.0.0.mca"), new Point2i(1, 1));

		// make sure that existing chunks are still there
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-8, -5), tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -5));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-8, -6), tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -6));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-7, -6), tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -6));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-7, -5), tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -5));
	}

	@Test
	public void test_InvertedSourceSelection_NoTargetSelection_NoOffset() throws IOException {
		Map<Point2i, Set<Point2i>> sourceSelection = new HashMap<>();
		sourceSelection.put(new Point2i(-1, -1), Collections.singleton(new Point2i(-9, -4)));
		sourceSelection.put(new Point2i(0, -1), null);

		TestProgress progress;
		ChunkImporter.importChunks(
				tmpDir("source"),
				progress = new TestProgress(() -> {}, 60),
				true, // headless
				true, // overwrite
				new SelectionData(sourceSelection, true), // sourceSelection
				null, // targetSelection
				null, // ranges
				new Point2i(0, 0), // offset
				new DataProperty<>()
		);

		progress.join();

		assertSourceFilesAreUnchanged();

		// make sure that all chunks exist in the new files
		assertNoChunkExists(tmpFile("target", "r.0.-1.mca"), new Point2i(10, -24));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -6));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -5));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -6));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -5));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-9, -5));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -4));
		assertNoChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-9, -4));

		// make sure that chunks were successfully copied
		assertChunkEquals(tmpFile("source", "r.-1.-1.mca"), new Point2i(-9, -5), tmpFile("target", "r.-1.-1.mca"), new Point2i(-9, -5));
		assertChunkEquals(tmpFile("source", "r.-1.-1.mca"), new Point2i(-8, -4), tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -4));

		// make sure that existing chunks are still there
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-8, -5), tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -5));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-8, -6), tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -6));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-7, -6), tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -6));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-7, -5), tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -5));
	}

	@Test
	public void test_InvertedSourceSelection_NoTargetSelection_NoOffset_TargetRegion() throws IOException {
		Map<Point2i, Set<Point2i>> sourceSelection = new HashMap<>();
		sourceSelection.put(new Point2i(-1, -1), Collections.singleton(new Point2i(-9, -4)));

		TestProgress progress;
		ChunkImporter.importChunks(
				tmpDir("source"),
				progress = new TestProgress(() -> {}, 60),
				true, // headless
				true, // overwrite
				new SelectionData(sourceSelection, true), // sourceSelection
				null, // targetSelection
				null, // ranges
				new Point2i(0, 0), // offset
				new DataProperty<>()
		);

		progress.join();

		assertSourceFilesAreUnchanged();

		// make sure that all chunks exist in the new files
		assertChunkExists(tmpFile("target", "r.0.-1.mca"), new Point2i(10, -24));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -6));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -5));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -6));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -5));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-9, -5));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -4));
		assertNoChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-9, -4));

		// make sure that chunks were successfully copied
		assertChunkEquals(tmpFile("source", "r.-1.-1.mca"), new Point2i(-9, -5), tmpFile("target", "r.-1.-1.mca"), new Point2i(-9, -5));
		assertChunkEquals(tmpFile("source", "r.-1.-1.mca"), new Point2i(-8, -4), tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -4));
		assertChunkEquals(tmpFile("source", "r.0.-1.mca"), new Point2i(10, -24), tmpFile("target", "r.0.-1.mca"), new Point2i(10, -24));

		// make sure that existing chunks are still there
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-8, -5), tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -5));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-8, -6), tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -6));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-7, -6), tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -6));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-7, -5), tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -5));
	}

	@Test
	public void test_InvertedSourceSelection_NoTargetSelection_Offset() throws IOException {
		Map<Point2i, Set<Point2i>> sourceSelection = new HashMap<>();
		sourceSelection.put(new Point2i(-1, -1), Collections.singleton(new Point2i(-9, -4)));
		sourceSelection.put(new Point2i(0, -1), null);

		TestProgress progress;
		ChunkImporter.importChunks(
				tmpDir("source"),
				progress = new TestProgress(() -> {}, 60),
				true, // headless
				true, // overwrite
				new SelectionData(sourceSelection, true), // sourceSelection
				null, // targetSelection
				null, // ranges
				new Point2i(9, 5), // offset
				new DataProperty<>()
		);

		progress.join();

		assertSourceFilesAreUnchanged();

		// make sure that all chunks exist in the new files
		assertNoChunkExists(tmpFile("target", "r.0.-1.mca"), new Point2i(19, -19));
		assertNoChunkExists(tmpFile("target", "r.0.-1.mca"), new Point2i(10, -24));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -6));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -5));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -6));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -5));
		assertChunkExists(tmpFile("target", "r.0.0.mca"), new Point2i(0, 0));
		assertChunkExists(tmpFile("target", "r.0.0.mca"), new Point2i(1, 0));
		assertChunkExists(tmpFile("target", "r.0.0.mca"), new Point2i(1, 1));
		assertNoChunkExists(tmpFile("target", "r.0.0.mca"), new Point2i(0, 1));
		assertNoChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-9, -5));
		assertNoChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-9, -4));
		assertNoChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -4));

		// make sure that chunks were successfully copied
		assertChunkEqualsIgnoreLocations(tmpFile("source", "r.-1.-1.mca"), new Point2i(-9, -5), tmpFile("target", "r.0.0.mca"), new Point2i(0, 0));
		assertChunkEqualsIgnoreLocations(tmpFile("source", "r.-1.-1.mca"), new Point2i(-8, -5), tmpFile("target", "r.0.0.mca"), new Point2i(1, 0));
		assertChunkEqualsIgnoreLocations(tmpFile("source", "r.-1.-1.mca"), new Point2i(-8, -4), tmpFile("target", "r.0.0.mca"), new Point2i(1, 1));

		// make sure that existing chunks are still there
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-8, -5), tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -5));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-8, -6), tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -6));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-7, -6), tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -6));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-7, -5), tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -5));
	}

	@Test
	public void test_InvertedSourceSelection_TargetSelection_NoOffset() throws IOException {
		Map<Point2i, Set<Point2i>> sourceSelection = new HashMap<>();
		sourceSelection.put(new Point2i(-1, -1), Collections.singleton(new Point2i(-9, -4)));
		sourceSelection.put(new Point2i(0, -1), null);

		Map<Point2i, Set<Point2i>> targetSelection = new HashMap<>();
		targetSelection.put(new Point2i(-1, -1), new HashSet<>(Arrays.asList(new Point2i(-8, -4), new Point2i(-8, -5))));
		targetSelection.put(new Point2i(0, -1), null);

		TestProgress progress;
		ChunkImporter.importChunks(
				tmpDir("source"),
				progress = new TestProgress(() -> {}, 60),
				true, // headless
				true, // overwrite
				new SelectionData(sourceSelection, true), // sourceSelection
				new SelectionData(targetSelection, false), // targetSelection
				null, // ranges
				new Point2i(0, 0), // offset
				new DataProperty<>()
		);

		progress.join();

		assertSourceFilesAreUnchanged();

		// make sure that all chunks exist in the new files
		assertNoChunkExists(tmpFile("target", "r.0.-1.mca"), new Point2i(10, -24));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -6));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -5));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -6));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -5));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -4));
		assertNoChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-9, -5));
		assertNoChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-9, -4));

		// make sure that chunks were successfully copied
		assertChunkEquals(tmpFile("source", "r.-1.-1.mca"), new Point2i(-8, -4), tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -4));

		// make sure that existing chunks are still there
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-8, -5), tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -5));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-8, -6), tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -6));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-7, -6), tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -6));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-7, -5), tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -5));
	}

	@Test
	public void test_InvertedSourceSelection_TargetSelection_NoOffset_WithFile() throws IOException {
		Map<Point2i, Set<Point2i>> sourceSelection = new HashMap<>();
		sourceSelection.put(new Point2i(-1, -1), Collections.singleton(new Point2i(-9, -4)));

		Map<Point2i, Set<Point2i>> targetSelection = new HashMap<>();
		targetSelection.put(new Point2i(-1, -1), new HashSet<>(Arrays.asList(new Point2i(-8, -4), new Point2i(-8, -5))));
		targetSelection.put(new Point2i(0, -1), null);

		TestProgress progress;
		ChunkImporter.importChunks(
				tmpDir("source"),
				progress = new TestProgress(() -> {}, 60),
				true, // headless
				true, // overwrite
				new SelectionData(sourceSelection, true), // sourceSelection
				new SelectionData(targetSelection, false), // targetSelection
				null, // ranges
				new Point2i(0, 0), // offset
				new DataProperty<>()
		);

		progress.join();

		assertSourceFilesAreUnchanged();

		// make sure that all chunks exist in the new files
		assertChunkExists(tmpFile("target", "r.0.-1.mca"), new Point2i(10, -24));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -6));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -5));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -6));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -5));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -4));
		assertNoChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-9, -5));
		assertNoChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-9, -4));

		// make sure that chunks were successfully copied
		assertChunkEquals(tmpFile("source", "r.-1.-1.mca"), new Point2i(-8, -4), tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -4));
		assertChunkEquals(tmpFile("source", "r.0.-1.mca"), new Point2i(10, -24), tmpFile("target", "r.0.-1.mca"), new Point2i(10, -24));

		// make sure that existing chunks are still there
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-8, -5), tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -5));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-8, -6), tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -6));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-7, -6), tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -6));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-7, -5), tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -5));
	}

	@Test
	public void test_InvertedSourceSelection_TargetSelection_Offset() throws IOException {
		Map<Point2i, Set<Point2i>> sourceSelection = new HashMap<>();
		sourceSelection.put(new Point2i(-1, -1), Collections.singleton(new Point2i(-9, -4)));
		sourceSelection.put(new Point2i(0, -1), null);

		Map<Point2i, Set<Point2i>> targetSelection = new HashMap<>();
		targetSelection.put(new Point2i(0, 0), Collections.singleton(new Point2i(0, 0)));
		targetSelection.put(new Point2i(0, -1), null);

		TestProgress progress;
		ChunkImporter.importChunks(
				tmpDir("source"),
				progress = new TestProgress(() -> {}, 60),
				true, // headless
				true, // overwrite
				new SelectionData(sourceSelection, true), // sourceSelection
				new SelectionData(targetSelection, false), // targetSelection
				null, // ranges
				new Point2i(8, 4), // offset
				new DataProperty<>()
		);

		progress.join();

		assertSourceFilesAreUnchanged();

		// make sure that all chunks exist in the new files
		assertNoChunkExists(tmpFile("target", "r.0.-1.mca"), new Point2i(18, -20));
		assertNoChunkExists(tmpFile("target", "r.0.-1.mca"), new Point2i(10, -24));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -6));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -5));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -6));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -5));
		assertChunkExists(tmpFile("target", "r.0.0.mca"), new Point2i(0, 0));
		assertNoChunkExists(tmpFile("target", "r.-1.0.mca"), new Point2i(-1, 0));
		assertNoChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-1, -1));
		assertChunkExists(tmpFile("target", "r.0.-1.mca"), new Point2i(0, -1));

		// make sure that chunks were successfully copied
		assertChunkEqualsIgnoreLocations(tmpFile("source", "r.-1.-1.mca"), new Point2i(-8, -4), tmpFile("target", "r.0.0.mca"), new Point2i(0, 0));
		assertChunkEqualsIgnoreLocations(tmpFile("source", "r.-1.-1.mca"), new Point2i(-8, -5), tmpFile("target", "r.0.-1.mca"), new Point2i(0, -1));

		// make sure that existing chunks are still there
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-8, -5), tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -5));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-8, -6), tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -6));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-7, -6), tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -6));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-7, -5), tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -5));
	}

	@Test
	public void test_InvertedSourceSelection_TargetSelection_Offset_WithFile() throws IOException {
		Map<Point2i, Set<Point2i>> sourceSelection = new HashMap<>();
		sourceSelection.put(new Point2i(-1, -1), Collections.singleton(new Point2i(-9, -4)));

		Map<Point2i, Set<Point2i>> targetSelection = new HashMap<>();
		targetSelection.put(new Point2i(0, 0), Collections.singleton(new Point2i(0, 0)));

		TestProgress progress;
		ChunkImporter.importChunks(
				tmpDir("source"),
				progress = new TestProgress(() -> {}, 60),
				true, // headless
				true, // overwrite
				new SelectionData(sourceSelection, true), // sourceSelection
				new SelectionData(targetSelection, false), // targetSelection
				null, // ranges
				new Point2i(8, 4), // offset
				new DataProperty<>()
		);

		progress.join();

		assertSourceFilesAreUnchanged();

		// make sure that all chunks exist in the new files
		assertNoChunkExists(tmpFile("target", "r.0.-1.mca"), new Point2i(18, -20));
		assertNoChunkExists(tmpFile("target", "r.0.-1.mca"), new Point2i(10, -24));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -6));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -5));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -6));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -5));
		assertChunkExists(tmpFile("target", "r.0.0.mca"), new Point2i(0, 0));
		assertNoChunkExists(tmpFile("target", "r.-1.0.mca"), new Point2i(-1, 0));
		assertNoChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-1, -1));
		assertNoChunkExists(tmpFile("target", "r.0.-1.mca"), new Point2i(0, -1));

		// make sure that chunks were successfully copied
		assertChunkEqualsIgnoreLocations(tmpFile("source", "r.-1.-1.mca"), new Point2i(-8, -4), tmpFile("target", "r.0.0.mca"), new Point2i(0, 0));

		// make sure that existing chunks are still there
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-8, -5), tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -5));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-8, -6), tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -6));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-7, -6), tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -6));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-7, -5), tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -5));
	}

	@Test
	public void test_InvertedSourceSelection_InvertedTargetSelection_NoOffset() throws IOException {
		Map<Point2i, Set<Point2i>> sourceSelection = new HashMap<>();
		sourceSelection.put(new Point2i(-1, -1), Collections.singleton(new Point2i(-9, -4)));
		sourceSelection.put(new Point2i(0, -1), null);

		Map<Point2i, Set<Point2i>> targetSelection = new HashMap<>();
		targetSelection.put(new Point2i(-1, -1), new HashSet<>(Arrays.asList(new Point2i(-8, -4), new Point2i(-8, -5))));
		targetSelection.put(new Point2i(0, -1), null);

		TestProgress progress;
		ChunkImporter.importChunks(
				tmpDir("source"),
				progress = new TestProgress(() -> {}, 60),
				true, // headless
				true, // overwrite
				new SelectionData(sourceSelection, true), // sourceSelection
				new SelectionData(targetSelection, true), // targetSelection
				null, // ranges
				new Point2i(0, 0), // offset
				new DataProperty<>()
		);

		progress.join();

		assertSourceFilesAreUnchanged();

		// make sure that all chunks exist in the new files
		assertNoChunkExists(tmpFile("target", "r.0.-1.mca"), new Point2i(10, -24));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -6));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -5));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -6));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -5));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-9, -5));
		assertNoChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -4));
		assertNoChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-9, -4));

		// make sure that chunks were successfully copied
		assertChunkEquals(tmpFile("source", "r.-1.-1.mca"), new Point2i(-9, -5), tmpFile("target", "r.-1.-1.mca"), new Point2i(-9, -5));

		// make sure that existing chunks are still there
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-8, -5), tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -5));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-8, -6), tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -6));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-7, -6), tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -6));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-7, -5), tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -5));
	}

	@Test
	public void test_InvertedSourceSelection_InvertedTargetSelection_NoOffset_WithFile() throws IOException {
		Map<Point2i, Set<Point2i>> sourceSelection = new HashMap<>();
		sourceSelection.put(new Point2i(-1, -1), Collections.singleton(new Point2i(-9, -4)));

		Map<Point2i, Set<Point2i>> targetSelection = new HashMap<>();
		targetSelection.put(new Point2i(-1, -1), new HashSet<>(Arrays.asList(new Point2i(-8, -4), new Point2i(-8, -5))));

		TestProgress progress;
		ChunkImporter.importChunks(
				tmpDir("source"),
				progress = new TestProgress(() -> {}, 60),
				true, // headless
				true, // overwrite
				new SelectionData(sourceSelection, true), // sourceSelection
				new SelectionData(targetSelection, true), // targetSelection
				null, // ranges
				new Point2i(0, 0), // offset
				new DataProperty<>()
		);

		progress.join();

		assertSourceFilesAreUnchanged();

		// make sure that all chunks exist in the new files
		assertChunkExists(tmpFile("target", "r.0.-1.mca"), new Point2i(10, -24));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -6));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -5));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -6));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -5));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-9, -5));
		assertNoChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -4));
		assertNoChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-9, -4));

		// make sure that chunks were successfully copied
		assertChunkEquals(tmpFile("source", "r.-1.-1.mca"), new Point2i(-9, -5), tmpFile("target", "r.-1.-1.mca"), new Point2i(-9, -5));
		assertChunkEquals(tmpFile("source", "r.0.-1.mca"), new Point2i(10, -24), tmpFile("target", "r.0.-1.mca"), new Point2i(10, -24));

		// make sure that existing chunks are still there
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-8, -5), tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -5));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-8, -6), tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -6));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-7, -6), tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -6));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-7, -5), tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -5));
	}

	@Test
	public void test_InvertedSourceSelection_InvertedTargetSelection_Offset() throws IOException {
		Map<Point2i, Set<Point2i>> sourceSelection = new HashMap<>();
		sourceSelection.put(new Point2i(-1, -1), Collections.singleton(new Point2i(-9, -4)));
		sourceSelection.put(new Point2i(0, -1), null);

		Map<Point2i, Set<Point2i>> targetSelection = new HashMap<>();
		targetSelection.put(new Point2i(-1, -1), Collections.singleton(new Point2i(-1, -1)));
		targetSelection.put(new Point2i(0, -1), null);

		TestProgress progress;
		ChunkImporter.importChunks(
				tmpDir("source"),
				progress = new TestProgress(() -> {}, 60),
				true, // headless
				true, // overwrite
				new SelectionData(sourceSelection, true), // sourceSelection
				new SelectionData(targetSelection, true), // targetSelection
				null, // ranges
				new Point2i(8, 4), // offset
				new DataProperty<>()
		);

		progress.join();

		assertSourceFilesAreUnchanged();

		// make sure that all chunks exist in the new files
		assertNoChunkExists(tmpFile("target", "r.0.-1.mca"), new Point2i(10, -24));
		assertNoChunkExists(tmpFile("target", "r.0.-1.mca"), new Point2i(18, -20));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -6));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -5));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -6));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -5));

		assertNoChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-1, -1));
		assertNoChunkExists(tmpFile("target", "r.-1.0.mca"), new Point2i(-1, 0));
		assertNoChunkExists(tmpFile("target", "r.0.-1.mca"), new Point2i(0, -1));
		assertChunkExists(tmpFile("target", "r.0.0.mca"), new Point2i(0, 0));

		// make sure that chunks were successfully copied
		assertChunkEqualsIgnoreLocations(tmpFile("source", "r.-1.-1.mca"), new Point2i(-8, -4), tmpFile("target", "r.0.0.mca"), new Point2i(0, 0));

		// make sure that existing chunks are still there
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-8, -5), tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -5));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-8, -6), tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -6));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-7, -6), tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -6));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-7, -5), tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -5));
	}

	@Test
	public void test_InvertedSourceSelection_InvertedTargetSelection_Offset_WithFile() throws IOException {
		Map<Point2i, Set<Point2i>> sourceSelection = new HashMap<>();
		sourceSelection.put(new Point2i(-1, -1), Collections.singleton(new Point2i(-9, -4)));

		Map<Point2i, Set<Point2i>> targetSelection = new HashMap<>();
		targetSelection.put(new Point2i(-1, -1), Collections.singleton(new Point2i(-1, -1)));

		TestProgress progress;
		ChunkImporter.importChunks(
				tmpDir("source"),
				progress = new TestProgress(() -> {}, 60),
				true, // headless
				true, // overwrite
				new SelectionData(sourceSelection, true), // sourceSelection
				new SelectionData(targetSelection, true), // targetSelection
				null, // ranges
				new Point2i(8, 4), // offset
				new DataProperty<>()
		);

		progress.join();

		assertSourceFilesAreUnchanged();

		// make sure that all chunks exist in the new files
		assertNoChunkExists(tmpFile("target", "r.0.-1.mca"), new Point2i(10, -24));
		assertChunkExists(tmpFile("target", "r.0.-1.mca"), new Point2i(18, -20));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -6));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -5));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -6));
		assertChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -5));

		assertNoChunkExists(tmpFile("target", "r.-1.-1.mca"), new Point2i(-1, -1));
		assertNoChunkExists(tmpFile("target", "r.-1.0.mca"), new Point2i(-1, 0));
		assertChunkExists(tmpFile("target", "r.0.-1.mca"), new Point2i(0, -1));
		assertChunkExists(tmpFile("target", "r.0.0.mca"), new Point2i(0, 0));

		// make sure that chunks were successfully copied
		assertChunkEqualsIgnoreLocations(tmpFile("source", "r.-1.-1.mca"), new Point2i(-8, -4), tmpFile("target", "r.0.0.mca"), new Point2i(0, 0));
		assertChunkEqualsIgnoreLocations(tmpFile("source", "r.0.-1.mca"), new Point2i(10, -24), tmpFile("target", "r.0.-1.mca"), new Point2i(18, -20));

		// make sure that existing chunks are still there
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-8, -5), tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -5));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-8, -6), tmpFile("target", "r.-1.-1.mca"), new Point2i(-8, -6));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-7, -6), tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -6));
		assertChunkEquals(getResourceFile("import/target/r.-1.-1.mca"), new Point2i(-7, -5), tmpFile("target", "r.-1.-1.mca"), new Point2i(-7, -5));
	}

// ---------------------------------------------------------------------------------------------------------------------

	private void assertSourceFilesAreUnchanged() {
		String source1before = calculateFileMD5(getResourceFile("import/source/r.-1.-1.mca"));
		String source2before = calculateFileMD5(getResourceFile("import/source/r.0.-1.mca"));
		String source1after = calculateFileMD5(tmpFile("source", "r.-1.-1.mca"));
		String source2after = calculateFileMD5(tmpFile("source", "r.0.-1.mca"));
		assertEquals(source1before, source1after);
		assertEquals(source2before, source2after);
	}

	private void assertChunkExists(File file, Point2i chunk) throws IOException {
		assertTrue(file.exists());
		MCAFile f = MCAFile.read(file);
		assertNotNull(f.getLoadedChunkData(chunk));
		assertFalse(f.getLoadedChunkData(chunk).isEmpty());
		assertNotNull(f.getLoadedChunkData(chunk).getData());
	}

	private void assertNoChunkExists(File file, Point2i chunk) throws IOException {
		if (!file.exists()) {
			return;
		}
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

	private File tmpFile(String dir, String file) {
		return new File("tmp/" + name.getMethodName() + "/import/" + dir, file);
	}

	private File tmpDir(String dir) {
		return new File("tmp/" + name.getMethodName() + "/import/" + dir);
	}

	@Before
	public void before() throws IOException {
		FileUtils.copyDirectory(getResourceFile("import"), new File("tmp/" + name.getMethodName() + "/import"));
		Config.setWorldDir(new File("tmp/" + name.getMethodName() + "/import/target"));
		Config.setLoadThreads(1);
		Config.setProcessThreads(1);
		Config.setWriteThreads(1);
		Config.setMaxLoadedFiles(10);
		Config.setDebug(true);
		Config.setCacheDir(new File("tmp/" + name.getMethodName()));
		Config.getCacheDir().mkdirs();
		MCAFilePipe.init();
	}

	@After
	public void after() throws InterruptedException, TimeoutException, ExecutionException {
		FutureTask<Object> f = new FutureTask<>(() -> {}, null);
		MCAFilePipe.cancelAllJobs(f);
		f.get(60, TimeUnit.SECONDS);
	}

	@AfterClass
	public static void afterClass() throws IOException {
		FileUtils.deleteDirectory(new File("tmp"));
	}
}
