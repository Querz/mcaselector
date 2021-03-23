package net.querz.mcaselector.tiles;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import net.querz.mcaselector.Config;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.io.ByteArrayPointer;
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.io.mca.Chunk;
import net.querz.mcaselector.io.mca.RegionMCAFile;
import net.querz.mcaselector.point.Point2f;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.progress.Timer;
import net.querz.mcaselector.ui.Color;
import net.querz.mcaselector.io.ImageHelper;
import net.querz.mcaselector.version.VersionController;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public final class TileImage {

	private static final int[] corruptedChunkOverlay = new int[256];

	static {
		Image corrupted = FileHelper.getIconFromResources("img/corrupted");
		PixelReader pr = corrupted.getPixelReader();
		pr.getPixels(0, 0, 16, 16, PixelFormat.getIntArgbPreInstance(), corruptedChunkOverlay, 0, 16);
	}

	private TileImage() {}

	public static void draw(Tile tile, GraphicsContext ctx, float scale, Point2f offset, boolean selectionInverted, boolean overlay) {
		if (tile.image != null) {
			ctx.drawImage(tile.getImage(), offset.getX(), offset.getY(), Tile.SIZE / scale, Tile.SIZE / scale);

			if (overlay && tile.overlay != null) {
				ctx.setGlobalAlpha(0.5);
				ctx.drawImage(tile.getOverlay(), offset.getX(), offset.getY(), Tile.SIZE / scale, Tile.SIZE / scale);
				ctx.setGlobalAlpha(1);
			}
		} else {
			ctx.drawImage(ImageHelper.getEmptyTileImage(), offset.getX(), offset.getY(), Tile.SIZE / scale, Tile.SIZE / scale);
		}

		if (tile.marked && tile.markedChunks.isEmpty() && !selectionInverted || !tile.marked && tile.markedChunks.isEmpty() && selectionInverted) {
			//draw marked region
			ctx.setFill(Config.getRegionSelectionColor().makeJavaFXColor());
			ctx.fillRect(offset.getX(), offset.getY(), Tile.SIZE / scale, Tile.SIZE / scale);
		} else if (tile.markedChunks.size() > 0) {

			if (tile.markedChunksImage == null) {
				createMarkedChunksImage(tile, Tile.getZoomLevel(scale), selectionInverted);
			}

			// apply markedChunksImage to ctx
			ctx.drawImage(tile.markedChunksImage, offset.getX(), offset.getY(), Tile.SIZE / scale, Tile.SIZE / scale);
		}
	}

	static void createMarkedChunksImage(Tile tile, int zoomLevel, boolean inverted) {
		WritableImage wImage = new WritableImage(Tile.SIZE / zoomLevel, Tile.SIZE / zoomLevel);

		Canvas canvas = new Canvas(Tile.SIZE / (float) zoomLevel, Tile.SIZE / (float) zoomLevel);
		GraphicsContext ctx = canvas.getGraphicsContext2D();
		ctx.setFill(Config.getChunkSelectionColor().makeJavaFXColor());

		if (inverted) {
			ctx.fillRect(0, 0, Tile.SIZE, Tile.SIZE);
		}

		for (Point2i markedChunk : tile.markedChunks) {
			Point2i regionChunk = markedChunk.mod(Tile.SIZE_IN_CHUNKS);
			if (regionChunk.getX() < 0) {
				regionChunk.setX(regionChunk.getX() + Tile.SIZE_IN_CHUNKS);
			}
			if (regionChunk.getZ() < 0) {
				regionChunk.setZ(regionChunk.getZ() + Tile.SIZE_IN_CHUNKS);
			}

			if (inverted) {
				ctx.clearRect(regionChunk.getX() * Tile.CHUNK_SIZE / (float) zoomLevel, regionChunk.getZ() * Tile.CHUNK_SIZE / (float) zoomLevel, Tile.CHUNK_SIZE / (float) zoomLevel, Tile.CHUNK_SIZE / (float) zoomLevel);
			} else {
				ctx.fillRect(regionChunk.getX() * Tile.CHUNK_SIZE / (float) zoomLevel, regionChunk.getZ() * Tile.CHUNK_SIZE / (float) zoomLevel, Tile.CHUNK_SIZE / (float) zoomLevel, Tile.CHUNK_SIZE / (float) zoomLevel);
			}
		}

		SnapshotParameters params = new SnapshotParameters();
		params.setFill(Color.TRANSPARENT.makeJavaFXColor());

		canvas.snapshot(params, wImage);

		tile.markedChunksImage = wImage;
	}

	public static Image generateImage(Tile tile, UUID world, BiConsumer<Image, UUID> callback, Supplier<Float> scaleSupplier, RegionMCAFile mcaFile) {
		Timer t = new Timer();

		Image image = createMCAImage(mcaFile);

		return image;
	}

	public static Image createMCAImage(RegionMCAFile mcaFile) {
		try {
			WritableImage finalImage = new WritableImage(Tile.SIZE, Tile.SIZE);
			PixelWriter writer = finalImage.getPixelWriter();
			int[] pixelBuffer = new int[Tile.PIXELS];
			int[] waterPixels = Config.shade() && Config.shadeWater() ? new int[Tile.PIXELS] : null;
			byte[] terrainHeights = new byte[Tile.PIXELS];
			byte[] waterHeights = Config.shade() && Config.shadeWater() ? new byte[Tile.PIXELS] : null;

			for (int cx = 0; cx < Tile.SIZE_IN_CHUNKS; cx++) {
				for (int cz = 0; cz < Tile.SIZE_IN_CHUNKS; cz++) {
					int index = cz  * Tile.SIZE_IN_CHUNKS + cx;

					Chunk data = mcaFile.getChunk(index);

					if (data == null) {
						continue;
					}

					drawChunkImage(data, cx * Tile.CHUNK_SIZE, cz * Tile.CHUNK_SIZE, pixelBuffer, waterPixels, terrainHeights, waterHeights);
				}
			}

			if (Config.shade()) {
				shade(pixelBuffer, waterPixels, terrainHeights, waterHeights);
			}

			writer.setPixels(0, 0, Tile.SIZE, Tile.SIZE, PixelFormat.getIntArgbPreInstance(), pixelBuffer,  0, Tile.SIZE);

			return finalImage;
		} catch (Exception ex) {
			Debug.dumpException("failed to create image for MCAFile " + mcaFile.getFile().getName(), ex);
		}
		return null;
	}

	private static void drawChunkImage(Chunk chunkData, int x, int z, int[] pixelBuffer, int[] waterPixels, byte[] terrainHeights, byte[] waterHeights) {
		if (chunkData.getData() == null) {
			return;
		}
		int dataVersion = chunkData.getData().getInt("DataVersion");
		try {
			VersionController.getChunkRenderer(dataVersion).drawChunk(
					chunkData.getData(),
					VersionController.getColorMapping(dataVersion),
					x, z,
					pixelBuffer,
					waterPixels,
					terrainHeights,
					waterHeights,
					Config.shade() && Config.shadeWater()
			);
		} catch (Exception ex) {
			Debug.dumpException("failed to draw chunk " + chunkData.getAbsoluteLocation(), ex);

			for (int cx = 0; cx < Tile.CHUNK_SIZE; cx++) {
				for (int cz = 0; cz < Tile.CHUNK_SIZE; cz++) {
					int srcIndex = cz * Tile.CHUNK_SIZE + cx;
					int dstIndex = (z + cz) * Tile.SIZE + (x + cx);
					pixelBuffer[dstIndex] = corruptedChunkOverlay[srcIndex];
					terrainHeights[dstIndex] = 64;
					waterHeights[dstIndex] = 64;
				}
			}
		}
	}

	private static void shade(int[] pixelBuffer, int[] waterPixels, byte[] terrainHeights, byte[] waterHeights) {
		if (!Config.shadeWater() || !Config.shade()) {
			waterHeights = terrainHeights;
		}

		int index = 0;
		for (int z = 0; z < Tile.SIZE; z++) {
			for (int x = 0; x < Tile.SIZE; x++, index++) {
				float xShade, zShade;

				if (pixelBuffer[index] == 0) {
					continue;
				}

				if (terrainHeights[index] != waterHeights[index]) {
					float ratio = 0.5f - 0.5f / 40f * (float) ((waterHeights[index] & 0xFF) - (terrainHeights[index] & 0xFF));
					pixelBuffer[index] = Color.blend(pixelBuffer[index], waterPixels[index], ratio);
				} else {
					if (z == 0) {
						zShade = (waterHeights[index + Tile.SIZE] & 0xFF) - (waterHeights[index] & 0xFF);
					} else if (z == Tile.SIZE - 1) {
						zShade = (waterHeights[index] & 0xFF) - (waterHeights[index - Tile.SIZE] & 0xFF);
					} else {
						zShade = ((waterHeights[index + Tile.SIZE] & 0xFF) - (waterHeights[index - Tile.SIZE] & 0xFF)) * 2;
					}

					if (x == 0) {
						xShade = (waterHeights[index + 1] & 0xFF) - (waterHeights[index] & 0xFF);
					} else if (x == Tile.SIZE - 1) {
						xShade = (waterHeights[index] & 0xFF) - (waterHeights[index - 1] & 0xFF);
					} else {
						xShade = ((waterHeights[index + 1] & 0xFF) - (waterHeights[index - 1] & 0xFF)) * 2;
					}

					float shade = xShade + zShade;
					if (shade < -8) {
						shade = -8;
					}
					if (shade > 8) {
						shade = 8;
					}

					int altitudeShade = 16 * ((waterHeights[index] & 0xFF) - 64) / 255;
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
}
