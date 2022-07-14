package net.querz.mcaselector.tile;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import net.querz.mcaselector.Config;
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.io.mca.Chunk;
import net.querz.mcaselector.io.mca.RegionMCAFile;
import net.querz.mcaselector.math.MathUtil;
import net.querz.mcaselector.point.Point2f;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.selection.ChunkSet;
import net.querz.mcaselector.selection.Selection;
import net.querz.mcaselector.ui.Color;
import net.querz.mcaselector.io.ImageHelper;
import net.querz.mcaselector.version.VersionController;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class TileImage {

	private static final Logger LOGGER = LogManager.getLogger(TileImage.class);

	private static final int[] corruptedChunkOverlay = new int[256];

	static {
		Image corrupted = FileHelper.getIconFromResources("img/corrupted");
		PixelReader pr = corrupted.getPixelReader();
		pr.getPixels(0, 0, 16, 16, PixelFormat.getIntArgbPreInstance(), corruptedChunkOverlay, 0, 16);
	}

	private TileImage() {}

	public static void draw(GraphicsContext ctx, Tile tile, float scale, Point2f offset, Selection selection, boolean overlay, boolean showNonexistentRegions) {
		if (tile == null || tile.image == null) {
			if (showNonexistentRegions) {
				ctx.drawImage(ImageHelper.getEmptyTileImage(), offset.getX(), offset.getY(), Tile.SIZE / scale, Tile.SIZE / scale);
			}
		}

		if (tile != null) {
			if (tile.image != null) {
				ctx.setImageSmoothing(Config.smoothRendering());
				ctx.drawImage(tile.image, offset.getX(), offset.getY(), Tile.SIZE / scale, Tile.SIZE / scale);
				ctx.setImageSmoothing(false);
			}

			if (overlay && tile.overlay != null) {
				ctx.setGlobalAlpha(0.5);
				ctx.setImageSmoothing(Config.smoothOverlays());
				ctx.drawImage(tile.getOverlay(), offset.getX(), offset.getY(), Tile.SIZE / scale, Tile.SIZE / scale);
				ctx.setGlobalAlpha(1);
				ctx.setImageSmoothing(false);
			}

			if (selection.isRegionSelected(tile.getLongLocation())) {
				ctx.setFill(Config.getRegionSelectionColor().makeJavaFXColor());
				ctx.fillRect(offset.getX(), offset.getY(), Tile.SIZE / scale, Tile.SIZE / scale);
			} else if (selection.isAnyChunkInRegionSelected(tile.getLongLocation())) {
				if (tile.markedChunksImage == null) {
					createMarkedChunksImage(tile, selection.getSelectedChunks(tile.getLocation()));
				}
				ctx.drawImage(tile.markedChunksImage, offset.getX(), offset.getY(), Tile.SIZE / scale, Tile.SIZE / scale);
			}

		} else if (selection.isInverted()) {
			ctx.setFill(Config.getRegionSelectionColor().makeJavaFXColor());
			ctx.fillRect(offset.getX(), offset.getY(), Tile.SIZE / scale, Tile.SIZE / scale);
		}
	}

	static void createMarkedChunksImage(Tile tile, ChunkSet selection) {
		if (selection == null) {
			tile.markedChunksImage = null;
			return;
		}
		WritableImage wImage = new WritableImage(32, 32);
		PixelWriter writer = wImage.getPixelWriter();

		javafx.scene.paint.Color chunkSelectionColor = Config.getChunkSelectionColor().makeJavaFXColor();

		selection.forEach(s -> {
			Point2i regionChunk = new Point2i(s);
			writer.setColor(regionChunk.getX(), regionChunk.getZ(), chunkSelectionColor);
		});

		tile.markedChunksImage = wImage;
	}

	public static Image generateImage(RegionMCAFile mcaFile, int scale) {

		int size = Tile.SIZE / scale;
		int chunkSize = Tile.CHUNK_SIZE / scale;
		int pixels = Tile.PIXELS / (scale * scale);

		try {

			WritableImage finalImage = new WritableImage(size, size);
			PixelWriter writer = finalImage.getPixelWriter();
			int[] pixelBuffer = new int[pixels];
			int[] waterPixels = Config.shade() && Config.shadeWater() && !Config.renderCaves() ? new int[pixels] : null;
			short[] terrainHeights = new short[pixels];
			short[] waterHeights = Config.shade() && Config.shadeWater() && !Config.renderCaves() ? new short[pixels] : null;

			for (int cx = 0; cx < Tile.SIZE_IN_CHUNKS; cx++) {
				for (int cz = 0; cz < Tile.SIZE_IN_CHUNKS; cz++) {
					int index = cz  * Tile.SIZE_IN_CHUNKS + cx;

					Chunk data = mcaFile.getChunk(index);

					if (data == null) {
						continue;
					}

					drawChunkImage(data, cx * chunkSize, cz * chunkSize, scale, pixelBuffer, waterPixels, terrainHeights, waterHeights);
				}
			}

			if (Config.renderCaves()) {
				flatShade(pixelBuffer, terrainHeights, scale);
			} else if (Config.shade() && !Config.renderLayerOnly()) {
				shade(pixelBuffer, waterPixels, terrainHeights, waterHeights, scale);
			}

			writer.setPixels(0, 0, size, size, PixelFormat.getIntArgbPreInstance(), pixelBuffer,  0, size);

			return finalImage;
		} catch (Exception ex) {
			LOGGER.warn("failed to create image for MCAFile {}", mcaFile.getFile().getName(), ex);
		}
		return null;
	}

	private static void drawChunkImage(Chunk chunkData, int x, int z, int scale, int[] pixelBuffer, int[] waterPixels, short[] terrainHeights, short[] waterHeights) {

		if (chunkData.getData() == null) {
			return;
		}
		int dataVersion = chunkData.getDataVersion();
		try {
			if (Config.renderCaves()) {
				VersionController.getChunkRenderer(dataVersion).drawCaves(
						chunkData.getData(),
						VersionController.getColorMapping(dataVersion),
						x, z, scale,
						pixelBuffer,
						terrainHeights,
						Config.getRenderHeight()
				);
			} else if (Config.renderLayerOnly()) {
				VersionController.getChunkRenderer(dataVersion).drawLayer(
						chunkData.getData(),
						VersionController.getColorMapping(dataVersion),
						x, z, scale,
						pixelBuffer,
						Config.getRenderHeight()
				);
			} else {
				VersionController.getChunkRenderer(dataVersion).drawChunk(
						chunkData.getData(),
						VersionController.getColorMapping(dataVersion),
						x, z, scale,
						pixelBuffer,
						waterPixels,
						terrainHeights,
						waterHeights,
						Config.shade() && Config.shadeWater(),
						Config.getRenderHeight()
				);
			}
		} catch (Exception ex) {
			LOGGER.warn("failed to draw chunk {}", chunkData.getAbsoluteLocation(), ex);

			// TODO: scale corrupted image
			for (int cx = 0; cx < Tile.CHUNK_SIZE; cx += scale) {
				for (int cz = 0; cz < Tile.CHUNK_SIZE; cz += scale) {
					int srcIndex = cz * Tile.CHUNK_SIZE + cx;
					int dstIndex = (z + cz / scale) * Tile.SIZE / scale + (x + cx / scale);
					pixelBuffer[dstIndex] = corruptedChunkOverlay[srcIndex];
					terrainHeights[dstIndex] = 64;
					waterHeights[dstIndex] = 64;
				}
			}
		}
	}

	private static void flatShade(int[] pixelBuffer, short[] terrainHeights, int scale) {
		int size = Tile.SIZE / scale;
		int index = 0;
		for (int z = 0; z < size; z++) {
			for (int x = 0; x < size; x++, index++) {
				int altitudeShade = MathUtil.clamp(16 * terrainHeights[index] / 64, -50, 50);
				pixelBuffer[index] = Color.shade(pixelBuffer[index], altitudeShade * 4);
			}
		}
	}

	private static void shade(int[] pixelBuffer, int[] waterPixels, short[] terrainHeights, short[] waterHeights, int scale) {
		if (!Config.shadeWater() || !Config.shade()) {
			waterHeights = terrainHeights;
		}

		int size = Tile.SIZE / scale;

		int index = 0;
		for (int z = 0; z < size; z++) {
			for (int x = 0; x < size; x++, index++) {
				float xShade, zShade;

				if (pixelBuffer[index] == 0) {
					continue;
				}

				if (terrainHeights[index] != waterHeights[index]) {
					float ratio = 0.5f - 0.5f / 40f * (float) ((waterHeights[index]) - (terrainHeights[index]));
					pixelBuffer[index] = Color.blend(pixelBuffer[index], waterPixels[index], ratio);
				} else {
					if (z == 0) {
						zShade = (waterHeights[index + size]) - (waterHeights[index]);
					} else if (z == size - 1) {
						zShade = (waterHeights[index]) - (waterHeights[index - size]);
					} else {
						zShade = ((waterHeights[index + size]) - (waterHeights[index - size])) * 2;
					}

					if (x == 0) {
						xShade = (waterHeights[index + 1]) - (waterHeights[index]);
					} else if (x == size - 1) {
						xShade = (waterHeights[index]) - (waterHeights[index - 1]);
					} else {
						xShade = ((waterHeights[index + 1]) - (waterHeights[index - 1])) * 2;
					}

					float shade = xShade + zShade;
					if (shade < -8) {
						shade = -8;
					}
					if (shade > 8) {
						shade = 8;
					}

					int altitudeShade = 16 * (waterHeights[index] - 64) / 255;
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
