package net.querz.mcaselector.io.job;

import net.querz.mcaselector.selection.Selection;
import net.querz.mcaselector.util.point.Point2i;
import net.querz.mcaselector.version.ChunkFilter;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FieldChangerTest {

	@Test
	void returnsOnlyTheEightAdjacentRelightChunks() {
		Point2i changed = new Point2i(10, 20);

		Set<Point2i> adjacent = FieldChanger.getAdjacentRelightChunks(Set.of(changed));

		assertEquals(8, adjacent.size());
		assertFalse(adjacent.contains(changed));
		for (int x = 9; x <= 11; x++) {
			for (int z = 19; z <= 21; z++) {
				if (x != 10 || z != 20) {
					assertTrue(adjacent.contains(new Point2i(x, z)));
				}
			}
		}
	}

	@Test
	void adjacentRelightChunksCrossRegionBoundaries() {
		Point2i changed = new Point2i(31, 31);

		Set<Point2i> adjacent = FieldChanger.getAdjacentRelightChunks(Set.of(changed));

		assertEquals(8, adjacent.size());
		assertTrue(adjacent.contains(new Point2i(32, 31)));
		assertTrue(adjacent.contains(new Point2i(31, 32)));
		assertTrue(adjacent.contains(new Point2i(32, 32)));
	}

	@Test
	void adjacentRelightChunksExcludeEveryChangedCenter() {
		Point2i first = new Point2i(10, 20);
		Point2i second = new Point2i(11, 20);

		Set<Point2i> adjacent = FieldChanger.getAdjacentRelightChunks(Set.of(first, second));

		assertEquals(10, adjacent.size());
		assertFalse(adjacent.contains(first));
		assertFalse(adjacent.contains(second));
	}

	@Test
	void primaryCoordinatorWaitsForEveryRegionBeforeRelighting() {
		List<Set<Point2i>> completed = new ArrayList<>();
		FieldChanger.PrimaryStageCoordinator coordinator = new FieldChanger.PrimaryStageCoordinator(2, completed::add);
		Point2i changed = new Point2i(10, 20);

		coordinator.primaryCompleted(Set.of(changed), true);
		assertTrue(completed.isEmpty());

		coordinator.primaryCompleted(Set.of(), false);
		assertEquals(List.of(Set.of(changed)), completed);
	}

	@Test
	void primaryCoordinatorPublishesOnlySuccessfullySavedRegionChanges() {
		List<Set<Point2i>> completed = new ArrayList<>();
		FieldChanger.PrimaryStageCoordinator coordinator = new FieldChanger.PrimaryStageCoordinator(2, completed::add);

		coordinator.primaryCompleted(Set.of(new Point2i(1, 1)), false);
		coordinator.primaryCompleted(Set.of(new Point2i(2, 2)), true);

		assertEquals(List.of(Set.of(new Point2i(2, 2))), completed);
	}

	@Test
	void primaryCoordinatorDoesNotStartRelightAfterCancellation() {
		List<Set<Point2i>> completed = new ArrayList<>();
		FieldChanger.PrimaryStageCoordinator coordinator = new FieldChanger.PrimaryStageCoordinator(2, completed::add);

		coordinator.primaryCancelled();
		coordinator.primaryCompleted(Set.of(new Point2i(2, 2)), true);

		assertTrue(completed.isEmpty());
	}

	@Test
	void previewReportsPotentialAdjacentRelightChunksOutsideSelection() {
		Point2i affected = new Point2i(10, 20);
		Selection selection = new Selection();
		selection.addChunk(affected);
		ChunkFilter.BlockReplacePreviewData chunkPreview = ChunkFilter.BlockReplacePreviewData.supported();
		chunkPreview.addSection(1);
		ReplaceBlocksPreviewer.Result result = new ReplaceBlocksPreviewer.Result();

		result.addChunk(affected, chunkPreview);
		result.finish(selection);

		assertEquals(8, result.getPotentialAdjacentRelightChunks());
	}
}
