package net.querz.mcaselector.tiles;

import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import net.querz.mcaselector.Config;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.io.MCAFilePipe;
import net.querz.mcaselector.io.db.CacheDBController;
import net.querz.mcaselector.io.job.ParseDataJob;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.tiles.overlay.OverlayParser;
import java.awt.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class OverlayPool {

	private final TileMap tileMap;
	private final Set<Point2i> noData = new HashSet<>();
	private final ThreadPoolExecutor overlayCacheLoaders = new ThreadPoolExecutor(
			4, 4,
			0L, TimeUnit.MILLISECONDS,
			new LinkedBlockingQueue<>());
	private final ThreadPoolExecutor overlayValueLoader = new ThreadPoolExecutor(
			1, 1,
			0L, TimeUnit.MILLISECONDS,
			new LinkedBlockingQueue<>());

	// when key present, but value null: no cache file
	// when key not present: look if region exists
	private final CacheDBController dataCache = new CacheDBController();
	private OverlayParser parser;

	private final Object hoveredRegionLock = new Object();
	private Point2i hoveredRegion;
	private int[] hoveredRegionData;

	public OverlayPool(TileMap tileMap) {
		this.tileMap = tileMap;
	}

	public void setParser(OverlayParser overlay) {
		this.parser = overlay;
		if (overlay != null) {
			try {
				dataCache.initTables(Collections.singletonList(overlay));
			} catch (SQLException ex) {
				Debug.dumpException("failed to create table for overlay " + overlay, ex);
			}
		}
	}

	public void requestImage(Tile tile, OverlayParser parser) {
		// check if data for this region exists
		if (noData.contains(tile.location)) {
			return;
		}

		if (ParseDataJob.isLoading(tile)) {
			// skip if we are already loading this tile
			return;
		}

		if (parser == null) {
			return;
		}

		ParseDataJob.setLoading(tile, true);

		overlayCacheLoaders.execute(() -> {
			int[] data = null;
			try {
				data = dataCache.getData(parser, null, tile.location);
			} catch (Exception ex) {
				Debug.dumpException("failed to load cached overlay data for region " + tile.location, ex);
			}

			if (data != null) {
				tile.overlay = parseColorGrades(data, parser.min(), parser.max(), parser.getMinHue(), parser.getMaxHue());
				tile.overlayLoaded = true;
				ParseDataJob.setLoading(tile, false);
				Platform.runLater(tileMap::update);
			} else {
				// calculate data
				MCAFilePipe.executeParseData(new ParseDataJob(tile, FileHelper.createRegionDirectories(tile.location), Config.getWorldUUID(), (d, u) -> {
					Platform.runLater(() -> {
						if (u.equals(Config.getWorldUUID())) {
							if (d == null) {
								noData.add(tile.location);
								tile.overlayLoaded = true;
								return;
							}
							if (parser.equals(this.parser)) {
								push(tile.location, d);
								tile.overlay = parseColorGrades(d, parser.min(), parser.max(), parser.getMinHue(), parser.getMaxHue());
								tile.overlayLoaded = true;
								tileMap.update();
							}
						}
					});
				}, parser));
			}
		});
	}

	private Image parseColorGrades(int[] data, int min, int max, float minHue, float maxHue) {
		int[] colors = new int[1024];
		for (int i = 0; i < 1024; i++) {
			colors[i] = getColorGrade(data[i], min, max, minHue, maxHue);
		}

		WritableImage image = new WritableImage(32, 32);
		image.getPixelWriter().setPixels(0, 0, 32, 32, PixelFormat.getIntArgbPreInstance(), colors, 0, 32);

		return image;
	}

	private static int getColorGrade(int value, int min, int max, float minHue, float maxHue) {
		if (value <= min) {
			return Color.HSBtoRGB(minHue, 1, 1);
		}
		if (value >= max) {
			return Color.HSBtoRGB(maxHue, 1, 1);
		}

		float percent = (float) (value - min) / (max - min);
		float hue = minHue + percent * (maxHue - minHue);

		return Color.HSBtoRGB(hue, 1, 1);
	}

	public void push(Point2i location, int[] data) {
		try {
			dataCache.setData(tileMap.getOverlay(), null, location, data);
		} catch (Exception ex) {
			Debug.dumpException("failed to cache data for region " + location, ex);
		}
	}

	public void switchTo(String dbPath) {
		try {
			dataCache.switchTo(dbPath, tileMap.getOverlayParsers());
		} catch (SQLException ex) {
			Debug.dumpException("failed to switch cache db", ex);
		}
	}

	public void clear() {
		try {
			dataCache.clear(tileMap.getOverlayParsers());
		} catch (Exception ex) {
			Debug.dumpException("failed to clear data cache", ex);
		}
		noData.clear();
	}

	public void discardData(Point2i region) {
		try {
			dataCache.deleteData(region);
			Debug.dumpf("removed data for %s from data pool", region);
		} catch (SQLException ex) {
			Debug.dumpException("failed to remove data from cache", ex);
		}
		noData.remove(region);
	}

	public void getHoveredChunkValue(Point2i chunk, Consumer<Integer> callback) {
		if (parser == null) {
			callback.accept(null);
		}
		Point2i region = chunk.chunkToRegion();
		Point2i normalizedChunk = chunk.normalizeChunkInRegion();
		if (region.equals(hoveredRegion)) {
			if (hoveredRegionData != null) {
				callback.accept(hoveredRegionData[normalizedChunk.getX() * 32 + normalizedChunk.getZ()]);
			} else {
				callback.accept(null);
			}
		} else {
			overlayValueLoader.getQueue().clear(); // no need to load anything else
			overlayValueLoader.execute(() -> {
				try {
					int[] regionData = dataCache.getData(parser, null, region);
					hoveredRegion = region;
					hoveredRegionData = regionData;
					if (regionData == null) {
						Platform.runLater(() -> callback.accept(null));
						return;
					}
					Platform.runLater(() -> callback.accept(regionData[normalizedChunk.getX() * 32 + normalizedChunk.getZ()]));
				} catch (IOException | SQLException ex) {
					Debug.dumpException("failed to load data for overlay value", ex);
					Platform.runLater(() -> callback.accept(null));
				}
			});
		}
	}
}
