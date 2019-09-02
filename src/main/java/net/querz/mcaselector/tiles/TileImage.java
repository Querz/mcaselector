package net.querz.mcaselector.tiles;

import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
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
import net.querz.mcaselector.ui.ImageHelper;
import net.querz.mcaselector.version.VersionController;
import java.io.File;

public class TileImage {

	public static void draw(Tile tile, GraphicsContext ctx, float scale, Point2f offset) {
		if (tile.isLoaded() && tile.image != null) {
			double size =  tile.getImage().getHeight() * (Tile.SIZE / tile.getImage().getWidth());
			ctx.drawImage(tile.getImage(), offset.getX(), offset.getY(), size / scale, size / scale);
			if (tile.marked) {
				//draw marked region
				ctx.setFill(Config.getRegionSelectionColor().makeJavaFXColor());
				ctx.fillRect(offset.getX(), offset.getY(), Tile.SIZE / scale, Tile.SIZE / scale);
			} else if (tile.markedChunks.size() > 0) {

				if (tile.markedChunksImage == null) {
					createMarkedChunksImage(tile, Tile.getZoomLevel(scale));
				}

				// apply markedChunksImage to ctx
				ctx.drawImage(tile.markedChunksImage, offset.getX(), offset.getY(), size / scale, size / scale);
			}
		} else {
			ctx.drawImage(ImageHelper.getEmptyTileImage(), offset.getX(), offset.getY(), Tile.SIZE / scale, Tile.SIZE / scale);
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

	public static Image generateImage(Tile tile, Runnable callback, byte[] rawData) {
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

		tile.image = createMCAImage(mcaFile, ptr);
		tile.loaded = true;

		callback.run();

		Debug.dumpf("took %s to generate image of %s", t, file.getName());

		return tile.image;
	}

	private static Image createMCAImage(MCAFile mcaFile, ByteArrayPointer ptr) {
		try {
			WritableImage finalImage = new WritableImage(Tile.SIZE, Tile.SIZE);
			PixelWriter writer = finalImage.getPixelWriter();

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

					drawChunkImage(data, cx * Tile.CHUNK_SIZE, cz * Tile.CHUNK_SIZE, writer);
				}
			}
			return finalImage;
		} catch (Exception ex) {
			Debug.error(ex);
		}
		return null;
	}

	private static void drawChunkImage(MCAChunkData chunkData, int x, int z, PixelWriter writer) {
		if (chunkData.getData() == null) {
			return;
		}
		int dataVersion = chunkData.getData().getInt("DataVersion");
		try {
			VersionController.getChunkDataProcessor(dataVersion).drawChunk(
					chunkData.getData(),
					VersionController.getColorMapping(dataVersion),
					x, z,
					writer
			);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
