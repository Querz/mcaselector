package net.querz.mcaselector.tile;

import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import net.querz.mcaselector.config.ConfigProvider;
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.io.JobHandler;
import net.querz.mcaselector.io.NamedThreadFactory;
import net.querz.mcaselector.io.db.CacheHandler;
import net.querz.mcaselector.io.job.ParseDataJob;
import net.querz.mcaselector.io.mca.EntitiesMCAFile;
import net.querz.mcaselector.io.mca.PoiMCAFile;
import net.querz.mcaselector.io.mca.RegionMCAFile;
import net.querz.mcaselector.util.point.Point2i;
import net.querz.mcaselector.util.property.DataProperty;
import net.querz.mcaselector.overlay.Overlay;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.iq80.leveldb.DBException;
import java.awt.*;
import java.io.IOException;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.AbstractMap;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class OverlayPool {

	private static final Logger LOGGER = LogManager.getLogger(OverlayPool.class);

	private final TileMap tileMap;
	private final Set<Point2i> noData = new HashSet<>();

    // used to load and render data asynchronously from db
    private final ThreadPoolExecutor overlayCacheLoaders;

	// used to load region data from db asynchronously to be displayed in the status bar
	private final ThreadPoolExecutor overlayValueLoader = new ThreadPoolExecutor(
			1, 1,
			0L, TimeUnit.MILLISECONDS,
			new LinkedBlockingQueue<>(),
			new NamedThreadFactory("overlayValuePool"));

    private Overlay parser;

    private Point2i hoveredRegion;
    private int[] hoveredRegionData;

    // Buffer for batching overlay writes to LevelDB
    private final Object batchLock = new Object();
    private final ArrayList<Map.Entry<Point2i, int[]>> batchBuffer = new ArrayList<>();
    private static final int BATCH_SIZE = 8;

    public OverlayPool(TileMap tileMap) {
        this.tileMap = tileMap;
        int pt = Math.max(1, net.querz.mcaselector.config.ConfigProvider.GLOBAL.getProcessThreads());
        int overlayThreads = Math.max(1, Math.min(4, pt));
        // Use virtual threads for I/O-bound overlay cache loads and add bounded queue for backpressure
        int queueCapacity = 1024;
        overlayCacheLoaders = new ThreadPoolExecutor(
                overlayThreads, overlayThreads,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(queueCapacity),
                Thread.ofVirtual().factory(),
                new ThreadPoolExecutor.DiscardOldestPolicy());
    }

	public Overlay getParser() {
		return parser;
	}

	public void setParser(Overlay overlay) {
		this.parser = overlay;
		if (overlay != null && overlay.isValid() && overlay.isActive()) {
			hoveredRegion = null;
			hoveredRegionData = null;
		}
	}

	public void requestImage(Tile tile, Overlay parser) {
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

		Overlay parserClone = parser.clone();

		overlayCacheLoaders.execute(() -> {
			int[] data = null;
			try {
				data = CacheHandler.getData(parserClone, tile.location);
			} catch (Exception ex) {
				LOGGER.warn("failed to load cached overlay data for region {}", tile.location, ex);
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
				JobHandler.executeParseData(new ParseDataJob(tile, FileHelper.createRegionDirectories(tile.location), ConfigProvider.WORLD.getWorldUUID(),
						(d, u) -> {
					if (u.equals(ConfigProvider.WORLD.getWorldUUID())) {
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

	public Image getImage(Point2i location, RegionMCAFile region, PoiMCAFile poi, EntitiesMCAFile entities) {
		try {
			int[] data = CacheHandler.getData(parser, location);
			if (data != null) {
				return parseColorGrades(data, parser.min(), parser.max(), parser.getMinHue(), parser.getMaxHue());
			}
		} catch (Exception ex) {
			LOGGER.warn("failed to load cached overlay data for region {}", location, ex);
			return null;
		}

		DataProperty<Image> image = new DataProperty<>();
		new ParseDataJob(
				new Tile(location),
				ConfigProvider.WORLD.getWorldDirs().makeRegionDirectories(location),
				ConfigProvider.WORLD.getWorldUUID(),
				region, poi, entities,
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
            // batch small groups of writes to improve throughput
            synchronized (batchLock) {
                batchBuffer.add(new AbstractMap.SimpleEntry<>(location, data));
                if (batchBuffer.size() >= BATCH_SIZE) {
                    Map<Point2i, int[]> batch = new HashMap<>();
                    for (Map.Entry<Point2i, int[]> e : batchBuffer) {
                        batch.put(e.getKey(), e.getValue());
                    }
                    batchBuffer.clear();
                    CacheHandler.setDataBatch(tileMap.getOverlay(), batch);
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("failed to cache data for region {}", location, ex);
        }
    }

	public void switchTo(String dbPath) {
		try {
			CacheHandler.switchTo(dbPath);
			hoveredRegion = null;
			hoveredRegionData = null;
		} catch (IOException ex) {
			LOGGER.warn("failed to switch cache db", ex);
		}
	}

	public void clear(boolean initCache) {
		try {
			CacheHandler.clear(ConfigProvider.WORLD.getCacheDBDir(), initCache);
			hoveredRegion = null;
			hoveredRegionData = null;
		} catch (IOException ex) {
			LOGGER.warn("failed to clear data cache", ex);
		}
		noData.clear();
	}

	public void discardData(Point2i region) {
		try {
			CacheHandler.deleteData(region);
			if (region.equals(hoveredRegion)) {
				hoveredRegion = null;
				hoveredRegionData = null;
			}
			LOGGER.debug("removed data for {} from data pool", region);
		} catch (IOException ex) {
			LOGGER.warn("failed to remove data from cache", ex);
		}
		noData.remove(region);
	}

	public void getHoveredChunkValue(Point2i chunk, Consumer<Integer> callback) {
		if (parser == null) {
			callback.accept(null);
		}
		Point2i region = chunk.chunkToRegion();
		Point2i normalizedChunk = chunk.asRelativeChunk();
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
					int[] regionData = CacheHandler.getData(parser, region);
					hoveredRegion = region;
					hoveredRegionData = regionData;
					if (regionData == null) {
						Platform.runLater(() -> callback.accept(null));
						return;
					}
					Platform.runLater(() -> callback.accept(regionData[normalizedChunk.getZ() * 32 + normalizedChunk.getX()]));
				} catch (IOException | DBException ex) {
					LOGGER.warn("failed to load data for overlay value", ex);
					Platform.runLater(() -> callback.accept(null));
				}
			});
		}
	}
}
