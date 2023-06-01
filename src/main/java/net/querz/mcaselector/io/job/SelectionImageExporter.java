package net.querz.mcaselector.io.job;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import net.querz.mcaselector.config.ConfigProvider;
import net.querz.mcaselector.io.ByteArrayPointer;
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.io.ImageHelper;
import net.querz.mcaselector.io.JobHandler;
import net.querz.mcaselector.io.RegionDirectories;
import net.querz.mcaselector.io.mca.RegionMCAFile;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.progress.Progress;
import net.querz.mcaselector.selection.ChunkSet;
import net.querz.mcaselector.selection.SelectionData;
import net.querz.mcaselector.tile.OverlayPool;
import net.querz.mcaselector.tile.Tile;
import net.querz.mcaselector.tile.TileImage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

public final class SelectionImageExporter {

	private static final Logger LOGGER = LogManager.getLogger(SelectionImageExporter.class);

	private SelectionImageExporter() {}

	// returns the unfinished image that is currently written to. image is done when progressChannel is done.
	public static int[] exportSelectionImage(SelectionData data, OverlayPool overlayPool, Progress progressChannel) {
		JobHandler.clearQueues();

		progressChannel.setMax(data.size());
		progressChannel.updateProgress(FileHelper.createMCAFileName(data.getSelection().one()), 0);

		LOGGER.debug("creating image generation jobs for image: {}", data);

		int[] pixels = new int[(int) (data.getWidth() * 16 * data.getHeight() * 16)];

		Consumer<Throwable> errorHandler = t -> progressChannel.incrementProgress("error");

		for (Long2ObjectMap.Entry<ChunkSet> entry : data.getSelection()) {
			ExportSelectionImageProcessJob job = new ExportSelectionImageProcessJob(new Point2i(entry.getLongKey()), entry.getValue(), data, pixels, overlayPool, progressChannel);
			job.errorHandler = errorHandler;
			JobHandler.addJob(job);
		}

		return pixels;
	}

	private static class ExportSelectionImageProcessJob extends ProcessDataJob {

		private final int[] pixels;
		private final ChunkSet chunks;
		private final SelectionData data;
		private final Progress progressChannel;
		private final OverlayPool overlayPool;

		public ExportSelectionImageProcessJob(Point2i region, ChunkSet chunks, SelectionData data, int[] pixels, OverlayPool overlayPool, Progress progressChannel) {
			super(new RegionDirectories(region, null, null, null), PRIORITY_LOW);
			this.pixels = pixels;
			this.chunks = chunks;
			this.data = data;
			this.progressChannel = progressChannel;
			this.overlayPool = overlayPool;
		}

		@Override
		public boolean execute() {
			Image image = null;

			// test if the image is already in cache
			File cacheImage = FileHelper.createPNGFilePath(ConfigProvider.WORLD.getCacheDir(), 1, getRegionDirectories().getLocation());
			File regionFile = FileHelper.createRegionMCAFilePath(getRegionDirectories().getLocation());
			RegionMCAFile mcaFile = null;
			if (cacheImage.exists()) {
				// load cached image
				image = new Image(cacheImage.toURI().toString(), false);
			} else if (regionFile.exists()) {
				// generate image from region file

				byte[] data = load(regionFile);
				if (data == null) {
					progressChannel.incrementProgress(regionFile.getName());
					return true;
				}

				mcaFile = new RegionMCAFile(regionFile);
				try {
					mcaFile.load(new ByteArrayPointer(data));
				} catch (IOException ex) {
					progressChannel.incrementProgress(regionFile.getName());
					return true;
				}

				image = TileImage.generateImage(mcaFile, 1);
			}

			if (image == null) {
				progressChannel.incrementProgress(regionFile.getName());
				return true;
			}

			Image overlay = null;
			if (overlayPool != null && overlayPool.getParser() != null) {
				// load overlay image
				overlay = overlayPool.getImage(getRegionDirectories().getLocation(), mcaFile, null, null);
				// scale up
				BufferedImage scaled = ImageHelper.scaleImage(SwingFXUtils.fromFXImage(overlay, null), 512, ConfigProvider.WORLD.getSmoothOverlays());
				overlay = SwingFXUtils.toFXImage(scaled, null);
			}

			PixelReader pixelReader;

			// if we have an overlay, merge it
			if (overlay != null) {
				BufferedImage sourceBuf = SwingFXUtils.fromFXImage(image, null);
				Graphics2D graphics2D = sourceBuf.createGraphics();
				AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f);
				BufferedImage overlayBuf = SwingFXUtils.fromFXImage(overlay, null);
				graphics2D.setComposite(ac);
				graphics2D.drawImage(overlayBuf, 0, 0, null);
				pixelReader = SwingFXUtils.toFXImage(sourceBuf, null).getPixelReader();
			} else {
				 pixelReader = image.getPixelReader();
			}

			iterateChunks(chunks, getRegionDirectories().getLocation(), chunk -> {
				Point2i relBlock = chunk.asRelativeChunk().chunkToBlock();

				int[] pixelData = new int[256];
				pixelReader.getPixels(relBlock.getX(), relBlock.getZ(), Tile.CHUNK_SIZE, Tile.CHUNK_SIZE, PixelFormat.getIntArgbPreInstance(), pixelData, 0, Tile.CHUNK_SIZE);

				Point2i blockInSelection = chunk.sub(data.getMin()).chunkToBlock();

				for (int cx = 0; cx < Tile.CHUNK_SIZE; cx++) {
					for (int cz = 0; cz < Tile.CHUNK_SIZE; cz++) {
						int srcIndex = cz * Tile.CHUNK_SIZE + cx;
						int dstIndex = (blockInSelection.getZ() + cz) * (int) data.getWidth() * 16 + (blockInSelection.getX() + cx);
						pixels[dstIndex] = pixelData[srcIndex];
					}
				}
			});
			progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
			return true;
		}
	}

	private static void iterateChunks(ChunkSet chunks, Point2i region, Consumer<Point2i> chunkConsumer) {
		if (chunks == null) {
			Point2i regionChunk = region.regionToChunk();
			for (int x = regionChunk.getX(); x < regionChunk.getX() + 32; x++) {
				for (int z = regionChunk.getZ(); z < regionChunk.getZ() + 32; z++) {
					chunkConsumer.accept(new Point2i(x, z));
				}
			}
		} else {
			Point2i regionChunk = region.regionToChunk();
			chunks.forEach(chunk -> chunkConsumer.accept(regionChunk.add(new Point2i(chunk))));
		}
	}
}
