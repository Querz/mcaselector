package net.querz.mcaselector.tiles;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import net.querz.mcaselector.Config;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.io.ByteArrayPointer;
import net.querz.mcaselector.io.MCAChunkData;
import net.querz.mcaselector.io.MCAFile;
import net.querz.mcaselector.point.Point2f;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.progress.Timer;
import net.querz.mcaselector.ui.Color;
import net.querz.mcaselector.io.ImageHelper;
import net.querz.mcaselector.version.VersionController;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class TileImage {

	private TileImage() {}

	public static void draw(Tile tile, GraphicsContext ctx, float scale, Point2f offset) {
		if (tile.isLoaded() && tile.image != null) {
			ctx.drawImage(tile.getImage(), offset.getX(), offset.getY(), Tile.SIZE / scale, Tile.SIZE / scale);
		} else {
			ctx.drawImage(ImageHelper.getEmptyTileImage(), offset.getX(), offset.getY(), Tile.SIZE / scale, Tile.SIZE / scale);
		}

		if (tile.marked) {
			//draw marked region
			ctx.setFill(Config.getRegionSelectionColor().makeJavaFXColor());
			ctx.fillRect(offset.getX(), offset.getY(), Tile.SIZE / scale, Tile.SIZE / scale);
		} else if (tile.markedChunks.size() > 0) {

			if (tile.markedChunksImage == null) {
				createMarkedChunksImage(tile, Tile.getZoomLevel(scale));
			}

			// apply markedChunksImage to ctx
			ctx.drawImage(tile.markedChunksImage, offset.getX(), offset.getY(), Tile.SIZE / scale, Tile.SIZE / scale);
		}
	}

	static void createMarkedChunksImage(Tile tile, int zoomLevel) {
		WritableImage wImage = new WritableImage(Tile.SIZE / zoomLevel, Tile.SIZE / zoomLevel);

		Canvas canvas = new Canvas(Tile.SIZE / (float) zoomLevel, Tile.SIZE / (float) zoomLevel);
		GraphicsContext ctx = canvas.getGraphicsContext2D();
		ctx.setFill(Config.getChunkSelectionColor().makeJavaFXColor());

		for (Point2i markedChunk : tile.markedChunks) {
			Point2i regionChunk = markedChunk.mod(Tile.SIZE_IN_CHUNKS);
			if (regionChunk.getX() < 0) {
				regionChunk.setX(regionChunk.getX() + Tile.SIZE_IN_CHUNKS);
			}
			if (regionChunk.getY() < 0) {
				regionChunk.setY(regionChunk.getY() + Tile.SIZE_IN_CHUNKS);
			}

			ctx.fillRect(regionChunk.getX() * Tile.CHUNK_SIZE / (float) zoomLevel, regionChunk.getY() * Tile.CHUNK_SIZE / (float) zoomLevel, Tile.CHUNK_SIZE / (float) zoomLevel, Tile.CHUNK_SIZE / (float) zoomLevel);
		}

		SnapshotParameters params = new SnapshotParameters();
		params.setFill(Color.TRANSPARENT.makeJavaFXColor());

		canvas.snapshot(params, wImage);

		tile.markedChunksImage = wImage;
	}

	public static Image generateImage(Tile tile, Consumer<Image> callback, Supplier<Float> scaleSupplier, byte[] rawData) {
		if (tile.loaded) {
			Debug.dump("region at " + tile.location + " already loaded");
			return tile.image;
		}

		Timer t = new Timer();

		File file = tile.getMCAFile();

		ByteArrayPointer ptr = new ByteArrayPointer(rawData);

		MCAFile mcaFile = MCAFile.readHeader(file, ptr);
		if (mcaFile == null) {
			Debug.error("error reading mca file " + file);
			//mark as loaded, we won't try to load this again
			tile.loaded = true;
			return tile.image;
		}
		Debug.dumpf("took %s to read mca file header of %s", t, file.getName());

		t.reset();

		Image image = createMCAImage(mcaFile, ptr);

		BufferedImage img = SwingFXUtils.fromFXImage(image, null);
		int zoomLevel = Tile.getZoomLevel(scaleSupplier.get());
		BufferedImage scaled = ImageHelper.scaleImage(img, (double) Tile.SIZE / zoomLevel);
		Image scaledImage = SwingFXUtils.toFXImage(scaled, null);

		tile.image = scaledImage;
		tile.loaded = true;

		callback.accept(scaledImage);

		Debug.dumpf("took %s to generate image of %s", t, file.getName());

		return image;
	}

	private static Image createMCAImage(MCAFile mcaFile, ByteArrayPointer ptr) {
		try {
			WritableImage finalImage = new WritableImage(Tile.SIZE, Tile.SIZE);
			PixelWriter writer = finalImage.getPixelWriter();
			int[] pixelBuffer = new int[Tile.SIZE * Tile.SIZE];
			short[] heights = new short[Tile.SIZE * Tile.SIZE];

			for (int cx = 0; cx < Tile.SIZE_IN_CHUNKS; cx++) {
				for (int cz = 0; cz < Tile.SIZE_IN_CHUNKS; cz++) {
					int index = cz  * Tile.SIZE_IN_CHUNKS + cx;

					MCAChunkData data = mcaFile.getChunkData(index);

					data.readHeader(ptr);

					try {
						data.loadData(ptr);
					} catch (Exception ex) {
						Debug.error(ex);
					}

					drawChunkImage(data, cx * Tile.CHUNK_SIZE, cz * Tile.CHUNK_SIZE, pixelBuffer, heights);
				}
			}

			shade(pixelBuffer, heights);

			writer.setPixels(0, 0, Tile.SIZE, Tile.SIZE, PixelFormat.getIntArgbPreInstance(), pixelBuffer,  0, Tile.SIZE);

			return finalImage;
		} catch (Exception ex) {
			Debug.error(ex);
		}
		return null;
	}

	private static void drawChunkImage(MCAChunkData chunkData, int x, int z, int[] pixelBuffer, short[] heights) {
		if (chunkData.getData() == null) {
			return;
		}
		int dataVersion = chunkData.getData().getInt("DataVersion");
		try {
			VersionController.getChunkDataProcessor(dataVersion).drawChunk(
					chunkData.getData(),
					VersionController.getColorMapping(dataVersion),
					x, z,
					pixelBuffer,
					heights
			);
		} catch (Exception ex) {
			Debug.dump(ex);
		}
	}

	private static void shade(int[] pixelBuffer, short[] heights) {
		int index = 0;
		for (int z = 0; z < Tile.SIZE; z++) {
			for (int x = 0; x < Tile.SIZE; x++, index++) {
				float xShade, zShade;

				if (pixelBuffer[index] == 0) {
					continue;
				}

				if(z == 0) {
					zShade = heights[index + Tile.SIZE] - heights[index];
				} else if (z == Tile.SIZE - 1) {
					zShade = heights[index] - heights[index - Tile.SIZE];
				} else {
					zShade = (heights[index + Tile.SIZE] - heights[index - Tile.SIZE]) * 2;
				}

				if (x == 0) {
					xShade = heights[index + 1] - heights[index];
				} else if (x == Tile.SIZE - 1) {
					xShade = heights[index] - heights[index - 1];
				} else {
					xShade = (heights[index + 1] - heights[index - 1]) * 2;
				}

				float shade = xShade + zShade;
				if (shade < -8) {
					shade = -8;
				}
				if (shade > 8) {
					shade = 8;
				}

				int altitudeShade = 16 * (heights[index] - 64) / 255;
				if (altitudeShade < -4) {
					altitudeShade = -4;
				}
				if (altitudeShade > 24) {
					altitudeShade = 24;
				}

				shade += altitudeShade;

				pixelBuffer[index] = Color.shade(pixelBuffer[index], (int) (shade * 8));
			}
		}
	}
}
