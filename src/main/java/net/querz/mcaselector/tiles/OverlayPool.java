package net.querz.mcaselector.tiles;

import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import net.querz.mcaselector.Config;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.io.JobHandler;
import net.querz.mcaselector.io.NamedThreadFactory;
import net.querz.mcaselector.io.db.CacheDBController;
import net.querz.mcaselector.io.job.ParseDataJob;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.property.DataProperty;
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

	// used to load and render data asynchronously from db
	private final ThreadPoolExecutor overlayCacheLoaders = new ThreadPoolExecutor(
			4, 4,
			0L, TimeUnit.MILLISECONDS,
			new LinkedBlockingQueue<>(),
			new NamedThreadFactory("overlayCachePool"));

	// used to load region data from db asynchronously to be displayed in the status bar
	private final ThreadPoolExecutor overlayValueLoader = new ThreadPoolExecutor(
			1, 1,
			0L, TimeUnit.MILLISECONDS,
			new LinkedBlockingQueue<>(),
			new NamedThreadFactory("overlayValuePool"));

	private final CacheDBController dataCache = CacheDBController.getInstance();
	private OverlayParser parser;

	private Point2i hoveredRegion;
	private int[] hoveredRegionData;

	public OverlayPool(TileMap tileMap) {
		this.tileMap = tileMap;
	}

	public OverlayParser getParser() {
		return parser;
	}

	public void setParser(OverlayParser overlay) {
		this.parser = overlay;
		if (overlay != null && overlay.isValid() && overlay.isActive()) {
			try {
				dataCache.initTables(Collections.singletonList(overlay));
				hoveredRegion = null;
				hoveredRegionData = null;
			} catch (SQLException ex) {
				Debug.dumpException("failed to create table for overlay " + overlay, ex);
			}
		}
	}

	public void requestImage(Tile tile, OverlayParser parser) {
		if (parser == null || !parser.isActive() || !parser.isValid()) {
			return;
		}

		// check if data for this region exists
		if (noData.contains(tile.location)) {
			return;
		}

		if (ParseDataJob.isLoading(tile)) {
			// skip if we are already loading this tile
			return;
		}

		ParseDataJob.setLoading(tile, true);

		OverlayParser parserClone = parser.clone();

		overlayCacheLoaders.execute(() -> {
			int[] data = null;
			try {
				data = dataCache.getData(parserClone, tile.location);
			} catch (Exception ex) {
				Debug.dumpException("failed to load cached overlay data for region " + tile.location, ex);
			}

			if (data != null) {
				Image overlay = parseColorGrades(data, parserClone.min(), parserClone.max(), parserClone.getMinHue(), parserClone.getMaxHue());
				if (parserClone.equals(this.parser)) {
					tile.overlay = overlay;
					tile.overlayLoaded = true;
					tileMap.draw();
				}
				ParseDataJob.setLoading(tile, false);
			} else {
				// calculate data
				JobHandler.executeParseData(new ParseDataJob(tile, FileHelper.createRegionDirectories(tile.location), Config.getWorldUUID(),
						(d, u) -> {
					if (u.equals(Config.getWorldUUID())) {
						if (d == null) {
							noData.add(tile.location);
							tile.overlayLoaded = true;
							return;
						}
						if (parserClone.equals(this.parser)) {
							push(tile.location, d);
							tile.overlay = parseColorGrades(d, parser.min(), parser.max(), parser.getMinHue(), parser.getMaxHue());
							tile.overlayLoaded = true;
							tileMap.draw();
						}
					}
				}, parser, () -> tileMap.getTilePriority(tile.location)));
			}
		});
	}

	public Image getImage(Point2i location) {
		try {
			int[] data = dataCache.getData(parser, location);
			if (data != null) {
				return parseColorGrades(data, parser.min(), parser.max(), parser.getMinHue(), parser.getMaxHue());
			}
		} catch (Exception ex) {
			Debug.dumpException("failed to load cached overlay data for region " + location, ex);
			return null;
		}

		DataProperty<Image> image = new DataProperty<>();
		new ParseDataJob(
				new Tile(location),
				Config.getWorldDirs().makeRegionDirectories(location),
				Config.getWorldUUID(),
				(i, u) -> {
					if (i != null) {
						image.set(parseColorGrades(i, parser.min(), parser.max(), parser.getMinHue(), parser.getMaxHue()));
					}
				},
				parser,
				null
		).execute();
		return image.get();
	}

	private static Image parseColorGrades(int[] data, int min, int max, float minHue, float maxHue) {
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
			dataCache.setData(tileMap.getOverlay(), location, data);
		} catch (Exception ex) {
			Debug.dumpException("failed to cache data for region " + location, ex);
		}
	}

	public void switchTo(String dbPath) {
		try {
			dataCache.switchTo(dbPath, tileMap.getOverlayParsers());
			hoveredRegion = null;
			hoveredRegionData = null;
		} catch (SQLException ex) {
			Debug.dumpException("failed to switch cache db", ex);
		}
	}

	public void clear() {
		try {
			dataCache.clear(tileMap.getOverlayParsers());
			hoveredRegion = null;
			hoveredRegionData = null;
		} catch (Exception ex) {
			Debug.dumpException("failed to clear data cache", ex);
		}
		noData.clear();
	}

	public void discardData(Point2i region) {
		try {
			dataCache.deleteData(region);
			if (region.equals(hoveredRegion)) {
				hoveredRegion = null;
				hoveredRegionData = null;
			}
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
				callback.accept(hoveredRegionData[normalizedChunk.getZ() * 32 + normalizedChunk.getX()]);
			} else {
				callback.accept(null);
			}
		} else {
			overlayValueLoader.getQueue().clear(); // no need to load anything else
			overlayValueLoader.execute(() -> {
				try {
					int[] regionData = dataCache.getData(parser, region);
					hoveredRegion = region;
					hoveredRegionData = regionData;
					if (regionData == null) {
						Platform.runLater(() -> callback.accept(null));
						return;
					}
					Platform.runLater(() -> callback.accept(regionData[normalizedChunk.getZ() * 32 + normalizedChunk.getX()]));
				} catch (IOException | SQLException ex) {
					Debug.dumpException("failed to load data for overlay value", ex);
					Platform.runLater(() -> callback.accept(null));
				}
			});
		}
	}
}
