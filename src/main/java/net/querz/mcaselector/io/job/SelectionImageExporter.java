package net.querz.mcaselector.io.job;

import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import net.querz.mcaselector.Config;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.io.ByteArrayPointer;
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.io.MCAFilePipe;
import net.querz.mcaselector.io.RegionDirectories;
import net.querz.mcaselector.io.SelectionData;
import net.querz.mcaselector.io.SelectionHelper;
import net.querz.mcaselector.io.SelectionInfo;
import net.querz.mcaselector.io.mca.RegionMCAFile;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.progress.Progress;
import net.querz.mcaselector.tiles.Tile;
import net.querz.mcaselector.tiles.TileImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class SelectionImageExporter {

	private SelectionImageExporter() {}

	// returns the unfinished image that is currently written to. image is done when progressChannel is done.
	public static int[] exportSelectionImage(SelectionDataInfo selection, Progress progressChannel) {
		MCAFilePipe.clearQueues();

		progressChannel.setMax(selection.selection.size());
		Point2i first = selection.selection.entrySet().iterator().next().getKey();
		progressChannel.updateProgress(FileHelper.createMCAFileName(first), 0);

		Debug.dumpf("creating image generation jobs for image: %s", selection.selectionInfo);

		int[] pixels = new int[(int) selection.selectionInfo.getWidth() * 16 * (int) selection.selectionInfo.getHeight() * 16];

		for (Map.Entry<Point2i, Set<Point2i>> entry : selection.selection.entrySet()) {
			MCAFilePipe.addJob(new ExportSelectionImageLoadJob(entry.getKey(), entry.getValue(), selection.selectionInfo, pixels, progressChannel));
		}

		return pixels;
	}

	public static SelectionDataInfo calculateSelectionInfo(SelectionData selection) {
		Map<Point2i, Set<Point2i>> sel = SelectionHelper.getTrueSelection(selection);
		SelectionInfo selectionInfo = SelectionHelper.getSelectionInfo(sel);
		return new SelectionDataInfo(sel, selectionInfo);
	}

	public static class SelectionDataInfo {

		private final Map<Point2i, Set<Point2i>> selection;
		private final SelectionInfo selectionInfo;

		SelectionDataInfo(Map<Point2i, Set<Point2i>> selection, SelectionInfo selectionInfo) {
			this.selection = selection;
			this.selectionInfo = selectionInfo;
		}

		public Map<Point2i, Set<Point2i>> getSelectionData() {
			return selection;
		}

		public SelectionInfo getSelectionInfo() {
			return selectionInfo;
		}
	}

	private static class ExportSelectionImageLoadJob extends LoadDataJob {

		private final int[] pixels;
		private final Point2i region;
		private final Set<Point2i> chunks;
		private final SelectionInfo selectionInfo;
		private final Progress progressChannel;

		public ExportSelectionImageLoadJob(Point2i region, Set<Point2i> chunks, SelectionInfo selectionInfo, int[] pixels, Progress progressChannel) {
			super(new RegionDirectories(region, null, null, null));
			this.pixels = pixels;
			this.region = region;
			this.chunks = chunks;
			this.selectionInfo = selectionInfo;
			this.progressChannel = progressChannel;
		}

		@Override
		public void execute() {
			Image image = null;

			// test if the image is already in cache
			File cacheImage = FileHelper.createPNGFilePath(Config.getCacheDir(), 1, region);
			File regionFile = FileHelper.createRegionMCAFilePath(region);
			if (cacheImage.exists()) {
				// load cached image
				image = new Image(cacheImage.toURI().toString(), false);
			} else if (regionFile.exists()) {
				// generate image from region file

				byte[] data = load(regionFile);
				if (data == null) {
					progressChannel.incrementProgress(regionFile.getName());
					return;
				}

				RegionMCAFile mcaFile = new RegionMCAFile(regionFile);
				try {
					mcaFile.load(new ByteArrayPointer(data));
				} catch (IOException ex) {
					progressChannel.incrementProgress(regionFile.getName());
					return;
				}

				image = TileImage.createMCAImage(mcaFile);
			}

			if (image == null) {
				progressChannel.incrementProgress(regionFile.getName());
				return;
			}

			MCAFilePipe.executeProcessData(new ExportSelectionImageProcessJob(region, chunks, image, selectionInfo, pixels, progressChannel));
		}

	}

	private static class ExportSelectionImageProcessJob extends ProcessDataJob {

		private final int[] pixels;
		private final Set<Point2i> chunks;
		private final SelectionInfo selectionInfo;
		private final Progress progressChannel;
		private final Image source;

		public ExportSelectionImageProcessJob(Point2i region, Set<Point2i> chunks, Image source, SelectionInfo selectionInfo, int[] pixels, Progress progressChannel) {
			super(new RegionDirectories(region, null, null, null), null, null, null);
			this.pixels = pixels;
			this.chunks = chunks;
			this.selectionInfo = selectionInfo;
			this.progressChannel = progressChannel;
			this.source = source;
		}

		@Override
		public void execute() {
			PixelReader pixelReader = source.getPixelReader();

			iterateChunks(chunks, getRegionDirectories().getLocation(), chunk -> {
				Point2i relChunk = chunk.mod(32);
				int x = relChunk.getX() >= 0 ? relChunk.getX() : (32 + relChunk.getX());
				int z = relChunk.getZ() >= 0 ? relChunk.getZ() : (32 + relChunk.getZ());
				Point2i relBlock = new Point2i(x, z).chunkToBlock();

				int[] pixelData = new int[256];
				pixelReader.getPixels(relBlock.getX(), relBlock.getZ(), Tile.CHUNK_SIZE, Tile.CHUNK_SIZE, PixelFormat.getIntArgbPreInstance(), pixelData, 0, Tile.CHUNK_SIZE);

				Point2i blockInSelection = selectionInfo.getPointInSelection(chunk).chunkToBlock();

				for (int cx = 0; cx < Tile.CHUNK_SIZE; cx++) {
					for (int cz = 0; cz < Tile.CHUNK_SIZE; cz++) {
						int srcIndex = cz * Tile.CHUNK_SIZE + cx;
						int dstIndex = (blockInSelection.getZ() + cz) * (int) selectionInfo.getWidth() * 16 + (blockInSelection.getX() + cx);
						pixels[dstIndex] = pixelData[srcIndex];
					}
				}
			});
			progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
		}
	}

	private static void iterateChunks(Set<Point2i> chunks, Point2i region, Consumer<Point2i> chunkConsumer) {
		if (chunks == null) {
			Point2i regionChunk = region.regionToChunk();
			for (int x = regionChunk.getX(); x < regionChunk.getX() + 32; x++) {
				for (int z = regionChunk.getZ(); z < regionChunk.getZ() + 32; z++) {
					chunkConsumer.accept(new Point2i(x, z));
				}
			}
		} else {
			for (Point2i chunk : chunks) {
				chunkConsumer.accept(chunk);
			}
		}
	}
}
