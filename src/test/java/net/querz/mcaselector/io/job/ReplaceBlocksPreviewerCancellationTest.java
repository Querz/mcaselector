package net.querz.mcaselector.io.job;

import net.querz.mcaselector.changer.fields.ReplaceBlocksField;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.io.mca.Region;
import net.querz.mcaselector.io.mca.RegionChunk;
import net.querz.mcaselector.util.point.Point2i;
import net.querz.mcaselector.util.progress.Progress;
import net.querz.mcaselector.version.VersionHandler;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.ListTag;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class ReplaceBlocksPreviewerCancellationTest {

	@BeforeAll
	static void initializeVersionHandlers() {
		VersionHandler.init();
	}

	@Test
	void cancellationStopsScanningAtTheNextChunkBoundary() {
		TestProgress progress = new TestProgress();
		CancellingRegion region = new CancellingRegion(progress, chunk());
		ReplaceBlocksField field = new ReplaceBlocksField();
		field.parseNewValue("literal(minecraft:stone)=minecraft:dirt");

		try {
			Method scanRegion = ReplaceBlocksPreviewer.class.getDeclaredMethod(
					"scanRegion", Region.class, Point2i.class, ReplaceBlocksField.class,
					net.querz.mcaselector.selection.Selection.class,
					ReplaceBlocksPreviewer.Result.class, Progress.class);
			scanRegion.setAccessible(true);
			scanRegion.invoke(null, region, new Point2i(0, 0), field, null,
					new ReplaceBlocksPreviewer.Result(), progress);
		} catch (ReflectiveOperationException ex) {
			fail("preview scan must accept and check the cancellation token", ex);
		}

		assertEquals(1, region.getChunkCalls);
	}

	private RegionChunk chunk() {
		CompoundTag root = new CompoundTag();
		root.putInt("DataVersion", 4671);
		root.putInt("xPos", 0);
		root.putInt("zPos", 0);
		CompoundTag stone = new CompoundTag();
		stone.putString("Name", "minecraft:stone");
		ListTag palette = new ListTag();
		palette.add(stone);
		CompoundTag blockStates = new CompoundTag();
		blockStates.put("palette", palette);
		CompoundTag section = new CompoundTag();
		section.putByte("Y", (byte) 0);
		section.put("block_states", blockStates);
		ListTag sections = new ListTag();
		sections.add(section);
		root.put("sections", sections);
		RegionChunk chunk = new RegionChunk(new Point2i(0, 0));
		chunk.setData(root);
		return chunk;
	}

	private static class CancellingRegion extends Region {

		private final TestProgress progress;
		private final RegionChunk chunk;
		private int getChunkCalls;

		private CancellingRegion(TestProgress progress, RegionChunk chunk) {
			this.progress = progress;
			this.chunk = chunk;
		}

		@Override
		public ChunkData getChunkDataAt(Point2i location, boolean selected) {
			getChunkCalls++;
			progress.cancelTask();
			return new ChunkData(location, chunk, null, null, selected);
		}
	}

	private static class TestProgress implements Progress {

		private volatile boolean cancelled;

		@Override public void setMax(int max) {}
		@Override public void updateProgress(String msg, int progress) {}
		@Override public void done(String msg) {}
		@Override public boolean taskCancelled() { return cancelled; }
		@Override public void cancelTask() { cancelled = true; }
		@Override public void incrementProgress(String msg) {}
		@Override public void incrementProgress(String msg, int progress) {}
		@Override public void setMessage(String msg) {}
	}
}
