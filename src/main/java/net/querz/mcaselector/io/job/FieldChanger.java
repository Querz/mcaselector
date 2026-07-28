package net.querz.mcaselector.io.job;

import net.querz.mcaselector.changer.Field;
import net.querz.mcaselector.changer.FieldType;
import net.querz.mcaselector.config.ConfigProvider;
import net.querz.mcaselector.io.JobHandler;
import net.querz.mcaselector.io.RegionDirectories;
import net.querz.mcaselector.io.WorldDirectories;
import net.querz.mcaselector.io.mca.Region;
import net.querz.mcaselector.selection.Selection;
import net.querz.mcaselector.text.Translation;
import net.querz.mcaselector.util.point.Point2i;
import net.querz.mcaselector.util.progress.Progress;
import net.querz.mcaselector.util.progress.Timer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class FieldChanger {

	private static final Logger LOGGER = LogManager.getLogger(FieldChanger.class);

	private FieldChanger() {}

	public static void changeNBTFields(List<Field<?>> fields, boolean force, Selection selection, Progress progressChannel, boolean headless) {
		changeNBTFields(ConfigProvider.WORLD.getWorldDirs(), fields, force, selection, progressChannel, headless);
	}

	static void changeNBTFields(WorldDirectories world, List<Field<?>> fields, boolean force, Selection selection,
			Progress progressChannel, boolean headless) {
		boolean hasReplaceBlocks = fields.stream().anyMatch(field -> field.getType() == FieldType.REPLACE_BLOCKS);
		boolean replaceBlocksOnly = fields.size() == 1 && hasReplaceBlocks;
		RegionDirectories[] primaryRegions = world.listRegions(selection);
		if (replaceBlocksOnly) {
			primaryRegions = Arrays.stream(primaryRegions).filter(dirs -> dirs.getRegion() != null).toArray(RegionDirectories[]::new);
		}
		if (primaryRegions.length == 0) {
			if (headless) {
				progressChannel.done("no files");
			} else {
				progressChannel.done(Translation.DIALOG_PROGRESS_NO_FILES.toString());
			}
			return;
		}

		RegionDirectories[] relightCandidates = hasReplaceBlocks
				? listRelightCandidateRegions(world, selection)
				: new RegionDirectories[0];

		JobHandler.clearQueues();
		progressChannel.setMax(primaryRegions.length + relightCandidates.length);
		progressChannel.updateProgress(primaryRegions[0].getLocationAsFileName(), 0);

		PrimaryStageCoordinator coordinator = new PrimaryStageCoordinator(primaryRegions.length,
				changedChunks -> startRelightStage(relightCandidates, changedChunks, progressChannel));
		for (RegionDirectories dirs : primaryRegions) {
			JobHandler.addJob(new MCAFieldChangeProcessJob(dirs, fields, force, selection, replaceBlocksOnly,
					progressChannel, coordinator));
		}
	}

	private static RegionDirectories[] listRelightCandidateRegions(WorldDirectories world, Selection selection) {
		Selection candidateSelection = selection == null ? null : expandSelectionByOne(selection.getTrueSelection(world));
		return Arrays.stream(world.listRegions(candidateSelection))
				.filter(dirs -> dirs.getRegion() != null)
				.toArray(RegionDirectories[]::new);
	}

	private static void startRelightStage(RegionDirectories[] candidates, Set<Point2i> changedChunks,
			Progress progressChannel) {
		if (progressChannel.taskCancelled()) {
			return;
		}
		Map<Point2i, Set<Point2i>> targetsByRegion = groupByRegion(getAdjacentRelightChunks(changedChunks));
		for (RegionDirectories dirs : candidates) {
			Set<Point2i> targets = targetsByRegion.get(dirs.getLocation());
			if (targets == null || targets.isEmpty()) {
				progressChannel.incrementProgress(dirs.getLocationAsFileName());
				continue;
			}
			JobHandler.addJob(new MCARelightProcessJob(dirs, targets, progressChannel));
		}
	}

	static Selection expandSelectionByOne(Selection selection) {
		Selection expanded = new Selection();
		for (var entry : selection) {
			Point2i region = new Point2i(entry.getLongKey());
			if (entry.getValue() == null) {
				for (int index = 0; index < 1024; index++) {
					addSquare(expanded, new Point2i(index).add(region.regionToChunk()));
				}
			} else {
				for (int index : entry.getValue()) {
					addSquare(expanded, new Point2i(index).add(region.regionToChunk()));
				}
			}
		}
		return expanded;
	}

	static Set<Point2i> getAdjacentRelightChunks(Set<Point2i> changedChunks) {
		Set<Point2i> adjacent = new HashSet<>();
		for (Point2i center : changedChunks) {
			for (int x = -1; x <= 1; x++) {
				for (int z = -1; z <= 1; z++) {
					adjacent.add(center.add(x, z));
				}
			}
		}
		adjacent.removeAll(changedChunks);
		return adjacent;
	}

	static Map<Point2i, Set<Point2i>> groupByRegion(Set<Point2i> chunks) {
		Map<Point2i, Set<Point2i>> grouped = new HashMap<>();
		for (Point2i chunk : chunks) {
			grouped.computeIfAbsent(chunk.chunkToRegion(), ignored -> new HashSet<>()).add(chunk);
		}
		return grouped;
	}

	private static void addSquare(Selection selection, Point2i center) {
		for (int x = -1; x <= 1; x++) {
			for (int z = -1; z <= 1; z++) {
				selection.addChunk(center.add(x, z));
			}
		}
	}

	static final class PrimaryStageCoordinator {

		private int remaining;
		private boolean cancelled;
		private final Set<Point2i> savedChangedChunks = new HashSet<>();
		private final Consumer<Set<Point2i>> onComplete;

		PrimaryStageCoordinator(int remaining, Consumer<Set<Point2i>> onComplete) {
			this.remaining = remaining;
			this.onComplete = onComplete;
		}

		synchronized void primaryCompleted(Set<Point2i> changedChunks, boolean regionSaved) {
			if (regionSaved) {
				savedChangedChunks.addAll(changedChunks);
			}
			finishOne();
		}

		synchronized void primaryCancelled() {
			cancelled = true;
			finishOne();
		}

		private void finishOne() {
			if (remaining <= 0) {
				return;
			}
			remaining--;
			if (remaining == 0 && !cancelled) {
				onComplete.accept(Set.copyOf(savedChangedChunks));
			}
		}
	}

	public static class MCAFieldChangeProcessJob extends ProcessDataJob {

		private final Progress progressChannel;
		private final List<Field<?>> fields;
		private final boolean force;
		private final Selection selection;
		private final boolean regionOnly;
		private final PrimaryStageCoordinator coordinator;
		private final AtomicBoolean terminal = new AtomicBoolean();

		private MCAFieldChangeProcessJob(RegionDirectories dirs, List<Field<?>> fields, boolean force, Selection selection,
				boolean regionOnly, Progress progressChannel, PrimaryStageCoordinator coordinator) {
			super(dirs, PRIORITY_LOW);
			this.fields = fields;
			this.force = force;
			this.selection = selection;
			this.regionOnly = regionOnly;
			this.progressChannel = progressChannel;
			this.coordinator = coordinator;
			errorHandler = this::failUnexpectedly;
		}

		@Override
		public boolean execute() {
			if (progressChannel.taskCancelled()) {
				completeCancelled();
				return true;
			}
			if (selection != null && !selection.isAnyChunkInRegionSelected(getRegionDirectories().getLocation())) {
				LOGGER.debug("will not apply nbt changes to {}", getRegionDirectories().getLocationAsFileName());
				completePrimary(Set.of(), false);
				return true;
			}

			try {
				Region region = regionOnly
						? Region.loadRegionFile(getRegionDirectories())
						: Region.loadRegion(getRegionDirectories());
				Region.FieldChangeResult result = region.applyFieldChangesTracked(fields, force, selection,
						progressChannel::taskCancelled);
				if (result.cancelled()) {
					completeCancelled();
					return true;
				}
				if (!result.dirty()) {
					completePrimary(Set.of(), false);
					return true;
				}

				JobHandler.executeSaveData(new MCAFieldChangeSaveJob(getRegionDirectories(), region, regionOnly,
						result.replaceBlocksChangedChunks(), progressChannel, coordinator));
				return false;
			} catch (Exception ex) {
				LOGGER.warn("error changing fields in {}", getRegionDirectories().getLocationAsFileName(), ex);
				completePrimary(Set.of(), false);
				return true;
			}
		}

		@Override
		public void cancel() {
			progressChannel.cancelTask();
			completeCancelled();
		}

		private void failUnexpectedly(Throwable throwable) {
			LOGGER.warn("unexpected error changing fields in {}", getRegionDirectories().getLocationAsFileName(), throwable);
			done();
			completePrimary(Set.of(), false);
		}

		private void completePrimary(Set<Point2i> changedChunks, boolean regionSaved) {
			if (terminal.compareAndSet(false, true)) {
				coordinator.primaryCompleted(changedChunks, regionSaved);
				progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
			}
		}

		private void completeCancelled() {
			if (terminal.compareAndSet(false, true)) {
				coordinator.primaryCancelled();
			}
		}
	}

	public static class MCAFieldChangeSaveJob extends SaveDataJob<Region> {

		private final boolean regionOnly;
		private final Set<Point2i> changedChunks;
		private final Progress progressChannel;
		private final PrimaryStageCoordinator coordinator;
		private final AtomicBoolean terminal = new AtomicBoolean();

		private MCAFieldChangeSaveJob(RegionDirectories dirs, Region region, boolean regionOnly,
				Set<Point2i> changedChunks, Progress progressChannel, PrimaryStageCoordinator coordinator) {
			super(dirs, region);
			this.regionOnly = regionOnly;
			this.changedChunks = changedChunks;
			this.progressChannel = progressChannel;
			this.coordinator = coordinator;
			errorHandler = this::failUnexpectedly;
		}

		@Override
		public void execute() {
			Timer timer = new Timer();
			boolean regionSaved = false;
			try {
				regionSaved = getData().saveRegionWithTempFile();
				if (!regionOnly) {
					getData().savePoiWithTempFile();
					getData().saveEntitiesWithTempFile();
				}
			} catch (Exception ex) {
				LOGGER.warn("failed to save changed fields for {}", getRegionDirectories().getLocationAsFileName(), ex);
			} finally {
				completePrimary(regionSaved);
				LOGGER.debug("took {} to save data for {}", timer, getRegionDirectories().getLocationAsFileName());
			}
		}

		@Override
		public void cancel() {
			if (terminal.compareAndSet(false, true)) {
				progressChannel.cancelTask();
				coordinator.primaryCancelled();
			}
		}

		private void failUnexpectedly(Throwable throwable) {
			LOGGER.warn("unexpected error saving changed fields for {}", getRegionDirectories().getLocationAsFileName(), throwable);
			completePrimary(false);
		}

		private void completePrimary(boolean regionSaved) {
			if (terminal.compareAndSet(false, true)) {
				coordinator.primaryCompleted(changedChunks, regionSaved);
				progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
			}
		}
	}

	private static class MCARelightProcessJob extends ProcessDataJob {

		private final Set<Point2i> targets;
		private final Progress progressChannel;
		private final AtomicBoolean terminal = new AtomicBoolean();

		private MCARelightProcessJob(RegionDirectories dirs, Set<Point2i> targets, Progress progressChannel) {
			super(dirs, PRIORITY_LOW);
			this.targets = targets;
			this.progressChannel = progressChannel;
			errorHandler = this::failUnexpectedly;
		}

		@Override
		public boolean execute() {
			if (progressChannel.taskCancelled()) {
				terminal.set(true);
				return true;
			}
			try {
				Region region = Region.loadRegionFile(getRegionDirectories());
				Region.RelightResult result = region.relightChunks(targets, progressChannel::taskCancelled);
				if (result.cancelled()) {
					terminal.set(true);
					return true;
				}
				if (!result.dirty()) {
					complete();
					return true;
				}
				JobHandler.executeSaveData(new MCARelightSaveJob(getRegionDirectories(), region, progressChannel));
				return false;
			} catch (Exception ex) {
				LOGGER.warn("failed to prepare relight data for {}", getRegionDirectories().getLocationAsFileName(), ex);
				complete();
				return true;
			}
		}

		@Override
		public void cancel() {
			if (terminal.compareAndSet(false, true)) {
				progressChannel.cancelTask();
			}
		}

		private void failUnexpectedly(Throwable throwable) {
			LOGGER.warn("unexpected error preparing relight data for {}", getRegionDirectories().getLocationAsFileName(), throwable);
			done();
			complete();
		}

		private void complete() {
			if (terminal.compareAndSet(false, true)) {
				progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
			}
		}
	}

	private static class MCARelightSaveJob extends SaveDataJob<Region> {

		private final Progress progressChannel;
		private final AtomicBoolean terminal = new AtomicBoolean();

		private MCARelightSaveJob(RegionDirectories dirs, Region region, Progress progressChannel) {
			super(dirs, region);
			this.progressChannel = progressChannel;
			errorHandler = this::failUnexpectedly;
		}

		@Override
		public void execute() {
			try {
				getData().saveRegionWithTempFile();
			} catch (Exception ex) {
				LOGGER.warn("failed to save relight data for {}", getRegionDirectories().getLocationAsFileName(), ex);
			} finally {
				complete();
			}
		}

		@Override
		public void cancel() {
			if (terminal.compareAndSet(false, true)) {
				progressChannel.cancelTask();
			}
		}

		private void failUnexpectedly(Throwable throwable) {
			LOGGER.warn("unexpected error saving relight data for {}", getRegionDirectories().getLocationAsFileName(), throwable);
			complete();
		}

		private void complete() {
			if (terminal.compareAndSet(false, true)) {
				progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
			}
		}
	}
}
