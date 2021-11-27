package net.querz.mcaselector.tiles;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.*;
import net.querz.mcaselector.Config;
import net.querz.mcaselector.io.*;
import net.querz.mcaselector.io.job.ParseDataJob;
import net.querz.mcaselector.io.job.RegionImageGenerator;
import net.querz.mcaselector.property.DataProperty;
import net.querz.mcaselector.tiles.overlay.OverlayParser;
import net.querz.mcaselector.ui.Color;
import net.querz.mcaselector.ui.DialogHelper;
import net.querz.mcaselector.ui.Window;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.key.KeyActivator;
import net.querz.mcaselector.point.Point2f;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.progress.Timer;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class TileMap extends Canvas implements ClipboardOwner {

	private float scale = 1;	// higher --> -    lower --> +

	public static final float CHUNK_GRID_SCALE = 1.5f; // show chunk grid if scale is larger than this
	public static final int TILE_VISIBILITY_THRESHOLD = 2;

	private final Window window;

	private final GraphicsContext context;

	private Point2f offset = new Point2f();

	private Point2f previousMouseLocation = null;
	private Point2f firstMouseLocation = null;

	private final Long2ObjectOpenHashMap<Tile> tiles = new Long2ObjectOpenHashMap<>();
	private Long2IntOpenHashMap tilePriorities = new Long2IntOpenHashMap();

	private int selectedChunks = 0;
	private Point2f mouseHoverLocation = null;
	private Point2i hoveredBlock = null;

	private boolean showChunkGrid = true;
	private boolean showRegionGrid = true;
	private boolean showNonexistentRegions;

	private final List<Consumer<TileMap>> updateListener = new ArrayList<>(1);
	private final List<Consumer<TileMap>> hoverListener = new ArrayList<>(1);

	private final KeyActivator keyActivator = new KeyActivator();

	private long totalDraws = 0;

	private boolean disabled = true;

	private boolean trackpadScrolling = false;

	private final ImagePool imgPool;
	private final OverlayPool overlayPool;

	private List<OverlayParser> overlayParsers = Collections.singletonList(null);
	private final ObjectProperty<OverlayParser> overlayParser = new SimpleObjectProperty<>(null);

	private Long2ObjectOpenHashMap<LongOpenHashSet> pastedChunks;
	private boolean pastedChunksInverted;
	private WorldDirectories pastedWorld;
	private Map<Point2i, Image> pastedChunksCache;
	private Point2i pastedChunksOffset;
	private Point2i firstPastedChunksOffset;

	private boolean selectionInverted = false;

	// used to scale existing images asynchronously
	private ScheduledExecutorService updateService;

	private ScheduledExecutorService drawService;
	private final AtomicBoolean drawRequested = new AtomicBoolean(false);

	public TileMap(Window window, int width, int height) {
		super(width, height);
		this.window = window;
		context = getGraphicsContext2D();
		context.setImageSmoothing(Config.smoothRendering());
		setFocusTraversable(true);
		this.setOnMousePressed(this::onMousePressed);
		this.setOnMouseReleased(e -> onMouseReleased());
		this.setOnMouseDragged(this::onMouseDragged);
		this.setOnMouseMoved(this::onMouseMoved);
		this.setOnMouseExited(e -> onMouseExited());
		this.setOnZoom(this::onZoom);
		this.setOnScroll(this::onScroll);
		this.setOnScrollStarted(e -> onScrollStarted());
		this.setOnScrollFinished(e -> onScrollFinished());
		this.setOnDragOver(this::onDragOver);
		this.setOnDragDropped(this::onDragDropped);
		keyActivator.registerAction(KeyCode.W, c -> offset = offset.sub(0, (c.contains(KeyCode.SHIFT) ? 10 : 5) * scale));
		keyActivator.registerAction(KeyCode.A, c -> offset = offset.sub((c.contains(KeyCode.SHIFT) ? 10 : 5) * scale, 0));
		keyActivator.registerAction(KeyCode.S, c -> offset = offset.add(0, (c.contains(KeyCode.SHIFT) ? 10 : 5) * scale));
		keyActivator.registerAction(KeyCode.D, c -> offset = offset.add((c.contains(KeyCode.SHIFT) ? 10 : 5) * scale, 0));
		keyActivator.registerAction(KeyCode.UP, c -> offset = offset.sub(0, (c.contains(KeyCode.SHIFT) ? 10 : 5) * scale));
		keyActivator.registerAction(KeyCode.LEFT, c -> offset = offset.sub((c.contains(KeyCode.SHIFT) ? 10 : 5) * scale, 0));
		keyActivator.registerAction(KeyCode.DOWN, c -> offset = offset.add(0, (c.contains(KeyCode.SHIFT) ? 10 : 5) * scale));
		keyActivator.registerAction(KeyCode.RIGHT, c -> offset = offset.add((c.contains(KeyCode.SHIFT) ? 10 : 5) * scale, 0));
		keyActivator.registerGlobalAction(this::draw);
		this.setOnKeyPressed(this::onKeyPressed);
		this.setOnKeyReleased(this::onKeyReleased);
		this.setOnKeyTyped(this::onKeyTyped);
		offset = new Point2f(-(double) width / 2, -(double) height / 2);

		overlayPool = new OverlayPool(this);
		imgPool = new ImagePool(this, Config.IMAGE_POOL_SIZE);

		setOverlays(Config.getOverlays());
		showNonexistentRegions = Config.showNonExistentRegions();

		RegionImageGenerator.setCacheEligibilityChecker(region -> {
			DataProperty<Boolean> eligible = new DataProperty<>(false);
			runOnVisibleRegions(r -> {
				if (region.equals(r)) {
					eligible.set(true);
				}
			}, new Point2f(), () -> scale, Config.getMaxLoadedFiles());
			return eligible.get();
		});

		initUpdateService();

		initDrawService();

		draw();
	}

	private void initUpdateService() {
		updateService = Executors.newSingleThreadScheduledExecutor();
		updateService.scheduleAtFixedRate(() -> {
			try {
				if (Config.getWorldDir() == null) {
					return;
				}

				// refresh tiles map
				tiles.values().removeIf(v -> {
					boolean visible = v.isVisible(this, TILE_VISIBILITY_THRESHOLD);

					// unload tile if it's not visible
					if (!visible) {
						v.unload(true, true);
					}

					return !visible && v.isObsolete();
				});

				// clean up all queues based on visible tiles
				JobHandler.validateJobs(j -> {
					if (j instanceof RegionImageGenerator.MCAImageProcessJob job) {
						if (!job.getTile().isVisible(this)) {
							Debug.dumpf("removing %s for tile %s from queue", job.getClass().getSimpleName(), job.getTile().getLocation());
							RegionImageGenerator.setLoading(job.getTile(), false);
							return true;
						}
					} else if (j instanceof ParseDataJob job) {
						if (!job.getTile().isVisible(this)) {
							ParseDataJob.setLoading(job.getTile(), false);
							Debug.dumpf("removing %s for tile %s from queue", job.getClass().getSimpleName(), job.getTile().getLocation());
							return true;
						}
					}
					return false;
				});

				// clean up pasted chunks cache
				if (pastedChunksCache != null) {
					pastedChunksCache.keySet().removeIf(img -> {
						Point2i o = offset.toPoint2i();
						Point2i min = o.sub(TILE_VISIBILITY_THRESHOLD * Tile.SIZE).blockToRegion().regionToBlock();
						Point2i max = new Point2i(
							(int) (o.getX() + getWidth() * scale),
							(int) (o.getZ() + getHeight() * scale)).add(TILE_VISIBILITY_THRESHOLD * Tile.SIZE).blockToRegion().regionToBlock();

						Point2i location = img.regionToBlock().add(pastedChunksOffset.chunkToBlock());

						return location.getX() < min.getX() || location.getZ() < min.getZ()
							|| location.getX() > max.getX() || location.getZ() > max.getZ();
					});
				}


				int zoomLevel = getZoomLevel();

				Long2IntOpenHashMap newTilePriorities = new Long2IntOpenHashMap(tilePriorities.size());

				DataProperty<Integer> priority = new DataProperty<>(1);

				runOnVisibleRegions(region -> {
					Tile tile = tiles.get(region.asLong());
					if (tile == null) {
						tile = new Tile(region);
						tiles.put(region.asLong(), tile);
					}

					newTilePriorities.put(region.asLong(), (int) priority.get());
					priority.set(priority.get() + 1);

					// load image
					if (tile.image != null) {
						if (tile.loaded) {
							// scale is right
							if (tile.getImageZoomLevel() != zoomLevel) {
								// image is larger than needed
								if (tile.getImageZoomLevel() < zoomLevel) {
									// scale down immediately
									tile.setImage(ImageHelper.scaleDownFXImage(tile.image, Tile.SIZE / zoomLevel));
									// DONE
								} else {
									imgPool.requestImage(tile, zoomLevel);
								}
							}
						} else {
							// if tile is not marked as loaded, but it has an image, we need to request a new image
							imgPool.requestImage(tile, zoomLevel);
						}
					} else {
						imgPool.requestImage(tile, zoomLevel);
					}

					// load overlay
					if (overlayParser.get() != null && !tile.isOverlayLoaded()) {
						overlayPool.requestImage(tile, overlayParser.get());
					}
				}, new Point2f(), () -> scale, Integer.MAX_VALUE);

				tilePriorities = newTilePriorities;

				Platform.runLater(this::runUpdateListeners);

			} catch (Exception ex) {
				Debug.dumpException("failed to update", ex);
			}
		}, 500, 500, TimeUnit.MILLISECONDS);
	}

	private void initDrawService() {
		drawService = Executors.newSingleThreadScheduledExecutor();
		drawService.scheduleAtFixedRate(() -> {
			if (!drawRequested.get()) {
				return;
			}

			Platform.runLater(() -> {
				Timer t = new Timer();
				draw(context);
				Debug.dumpfToConsoleOnly("draw #%d: %s", totalDraws++, t);
			});

			drawRequested.set(false);

		}, 1000 / 60, 1000 / 60, TimeUnit.MILLISECONDS);
	}

	public void reload() {
		runOnVisibleRegions(region -> {
			imgPool.discardCachedImage(region);
			Tile tile = tiles.get(region.asLong());
			if (tile != null) {
				tile.loaded = false;
			}
		}, new Point2f(), () -> scale, Integer.MAX_VALUE);
	}

	public int getTilePriority(Point2i region) {
		return tilePriorities.getOrDefault(region.asLong(), 9_999_999);
	}

	private void updateScale(float oldScale, Point2f center) {
		scale = scale < Config.MAX_SCALE ? Math.max(scale, Config.MIN_SCALE) : Config.MAX_SCALE;
		if (oldScale != scale) {
			// calculate the difference between the old max and the new max point
			Point2f diff = offset.add(center.getX() * oldScale, center.getY() * oldScale)
					.sub(offset.add(center.getX() * scale, center.getY() * scale));

			offset = offset.add(diff);

			if (Tile.getZoomLevel(oldScale) != Tile.getZoomLevel(scale)) {
				Debug.dumpf("zoom level changed from %d to %d", Tile.getZoomLevel(oldScale), Tile.getZoomLevel(scale));
				unloadTiles(false, false);
				// clear generator queue as well
				JobHandler.clearQueues();

				if (pastedChunksCache != null) {
					pastedChunksCache.clear();
				}
			}
			draw();
		}
	}

	public void nextOverlay() {
		if (disabled || overlayParser.get() == null) {
			return;
		}

		int index = overlayParsers.indexOf(overlayParser.get());

		OverlayParser parser;
		do {
			index++;
			if (index == overlayParsers.size()) {
				index = 0;
			}

		// repeat if the current parser is null, it is invalid or inactive or if the types are not the same
		} while ((parser = overlayParsers.get(index)) == null || !parser.isActive() || !parser.isValid() || parser.getType() != overlayParser.get().getType());

		setOverlay(parser);
		JobHandler.cancelParserQueue();
		draw();
	}

	public void nextOverlayType() {
		if (disabled) {
			return;
		}

		int index = overlayParsers.indexOf(overlayParser.get());

		OverlayParser parser;
		do {
			index++;
			if (index == overlayParsers.size()) {
				index = 0;
			}

			// repeat if the current parser is not null, it is inactive or invalid or the types are equal
		} while ((parser = overlayParsers.get(index)) != null && (!parser.isActive() || !parser.isValid() || overlayParser.get() != null && parser.getType() == overlayParser.get().getType()));

		setOverlay(parser);
		JobHandler.cancelParserQueue();
		draw();
	}

	public void setOverlays(List<OverlayParser> overlays) {
		if (overlays == null) {
			overlayParsers = Collections.singletonList(null);
			setOverlay(null);
			JobHandler.cancelParserQueue();
			return;
		}
		overlayParsers = new ArrayList<>(overlays.size() + 1);
		overlayParsers.addAll(overlays);
		overlayParsers.sort(Comparator.comparing(OverlayParser::getType));
		overlayParsers.add(null);
		setOverlay(null);
		JobHandler.cancelParserQueue();
	}

	public void setOverlay(OverlayParser overlay) {
		if (disabled) {
			return;
		}
		this.overlayParser.set(overlay);
		this.overlayPool.setParser(overlay);
		clearOverlay();
	}

	public void clearOverlay() {
		for (Tile tile : tiles.values()) {
			tile.overlay = null;
			tile.overlayLoaded = false;
		}
	}

	public OverlayParser getOverlay() {
		return overlayParser.get();
	}

	public ObjectProperty<OverlayParser> overlayParserProperty() {
		return overlayParser;
	}

	// returns a NEW copy of all current parsers
	public List<OverlayParser> getOverlayParsers() {
		List<OverlayParser> parsers = new ArrayList<>(overlayParsers.size() - 1);
		for (OverlayParser parser : overlayParsers) {
			if (parser != null) {
				parsers.add(parser.clone());
			}
		}
		return parsers;
	}

	public void setScale(float newScale) {
		scale = newScale;
		draw();
	}

	public static Point2f getRegionGridMin(Point2f offset, float scale) {
		Point2i min = offset.toPoint2i().blockToRegion();
		Point2f regionOffset = min.regionToBlock().toPoint2f().sub(offset.getX(), offset.getY());
		return new Point2f(regionOffset.getX() / scale, regionOffset.getY() / scale);
	}

	public static Point2f getChunkGridMin(Point2f offset, float scale) {
		Point2i min = offset.toPoint2i().blockToChunk();
		Point2f chunkOffset = min.chunkToBlock().toPoint2f().sub(offset.getX(), offset.getY());
		return new Point2f(chunkOffset.getX() / scale, chunkOffset.getY() / scale);
	}

	private void onKeyPressed(KeyEvent event) {
		if (event.getCode() == KeyCode.SHIFT) {
			keyActivator.pressActionKey(event.getCode());
		} else {
			keyActivator.pressKey(event.getCode());
		}

		if (event.getCode() == KeyCode.ESCAPE) {
			Debug.dumpf("cancelling chunk pasting");
			pastedChunks = null;
			pastedWorld = null;
			pastedChunksCache = null;
			pastedChunksOffset = null;
			draw();
		}

		if (event.getCode() == KeyCode.M) {
			dumpMetrics();
		}

		if (event.getCode() == KeyCode.J) {
			JobHandler.dumpMetrics();
		}

		if (event.getCode() == KeyCode.I) {
			imgPool.dumpMetrics();
		}

		switch (event.getCode()) {
			case DIGIT1 -> imgPool.mark(1);
			case DIGIT2 -> imgPool.mark(2);
			case DIGIT4 -> imgPool.mark(4);
			case DIGIT8 -> imgPool.mark(8);
		}

		// don't pass event to parent node if it would cause the tile map to lose focus
		if (KeyActivator.isArrowKey(event.getCode())) {
			event.consume();
		}
	}

	private void onKeyTyped(KeyEvent event) {
		if ("+".equals(event.getCharacter())) {
			zoomFactor(1.05, new Point2f(getWidth() / 2, getHeight() / 2));
		} else if ("-".equals(event.getCharacter())) {
			zoomFactor(0.95, new Point2f(getWidth() / 2, getHeight() / 2));
		}
	}

	private void zoomFactor(double factor, Point2f center) {
		float oldScale = scale;
		scale /= factor;
		updateScale(oldScale, center);
	}

	private void onKeyReleased(KeyEvent event) {
		if (event.getCode() == KeyCode.SHIFT) {
			keyActivator.releaseActionKey(event.getCode());
		} else {
			switch (event.getCode()) {
				case UP, DOWN, LEFT, RIGHT -> event.consume();
			}
			keyActivator.releaseKey(event.getCode());
		}
	}

	public void releaseAllKeys() {
		keyActivator.releaseAllKeys();
	}

	private void onMouseMoved(MouseEvent event) {
		hoveredBlock = getMouseBlock(event.getX(), event.getY());
		mouseHoverLocation = new Point2f(event.getX(), event.getY());
		runHoverListeners();
	}

	private void onMouseExited() {
		hoveredBlock = null;
		mouseHoverLocation = null;
		runHoverListeners();
	}

	private void onScroll(ScrollEvent event) {
		if (trackpadScrolling || event.isInertia()) {
			if (window.isKeyPressed(KeyCode.COMMAND)) {
				// zoom

				if (event.getDeltaY() > 0) {
					zoomFactor(1.03 + event.getDeltaY() / 1000, new Point2f(event.getX(), event.getY()));
				} else if (event.getDeltaY() < 0) {
					zoomFactor(0.97 + event.getDeltaY() / 1000, new Point2f(event.getX(), event.getY()));
				}

			} else {
				offset = offset.sub(new Point2f(event.getDeltaX(), event.getDeltaY()).mul(scale));
				draw();
			}
		} else {
			if (event.getDeltaY() > 0) {
				zoomFactor(1.03 + event.getDeltaY() / 1000, new Point2f(event.getX(), event.getY()));
			} else if (event.getDeltaY() < 0) {
				zoomFactor(0.97 + event.getDeltaY() / 1000, new Point2f(event.getX(), event.getY()));
			}
		}
	}

	private void onScrollStarted() {
		trackpadScrolling = true;
	}

	private void onScrollFinished() {
		trackpadScrolling = false;
	}

	private void onZoom(ZoomEvent event) {
		zoomFactor(event.getZoomFactor(), new Point2f(event.getX(), event.getY()));
	}

	private void onMousePressed(MouseEvent event) {
		if (!disabled) {
			firstMouseLocation = new Point2f(event.getX(), event.getY());
			firstPastedChunksOffset = pastedChunksOffset;

			if (event.getButton() == MouseButton.PRIMARY && !window.isKeyPressed(KeyCode.COMMAND) && pastedChunks == null) {
				mark(event.getX(), event.getY(), !selectionInverted);
			} else if (event.getButton() == MouseButton.SECONDARY) {
				mark(event.getX(), event.getY(), selectionInverted);
			}
			draw();
		}
	}

	private void onMouseReleased() {
		previousMouseLocation = null;
		firstPastedChunksOffset = null;
	}

	private void onMouseDragged(MouseEvent event) {
		Point2f mouseLocation = new Point2f(event.getX(), event.getY());
		if (event.getButton() == MouseButton.MIDDLE
				|| event.getButton() == MouseButton.PRIMARY && window.isKeyPressed(KeyCode.COMMAND)) {
			if (previousMouseLocation != null) {
				Point2f diff = mouseLocation.sub(previousMouseLocation);
				diff = diff.mul(-1);
				offset = offset.add(diff.mul(scale));
			}
			previousMouseLocation = mouseLocation;
		} else if (!disabled && event.getButton() == MouseButton.PRIMARY) {
			if (pastedChunks != null) {
				Point2f diff = mouseLocation.sub(firstMouseLocation).mul(scale);
				pastedChunksOffset = firstPastedChunksOffset.add(diff.toPoint2i().div(16));
			} else {
				mark(event.getX(), event.getY(), !selectionInverted);
			}
		} else if (!disabled && event.getButton() == MouseButton.SECONDARY) {
			mark(event.getX(), event.getY(), selectionInverted);
		}

		hoveredBlock = getMouseBlock(event.getX(), event.getY());
		runUpdateListeners();

		draw();
	}

	private void onDragOver(DragEvent event) {
		if (event.getGestureSource() != this && event.getDragboard().hasFiles()) {
			if (FileHelper.testWorldDirectoriesValid(event.getDragboard().getFiles(), null) != null) {
				event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
				event.consume();
			}
		}
	}

	private void onDragDropped(DragEvent event) {
		Dragboard db = event.getDragboard();
		if (db.hasFiles()) {

			WorldDirectories wd = FileHelper.testWorldDirectoriesValid(db.getFiles(), getWindow().getPrimaryStage());
			if (wd != null) {
				DialogHelper.setWorld(wd, this);
			}
			event.setDropCompleted(true);
		}
		event.consume();
	}

	public void redrawOverlays() {
		for (Tile tile : tiles.values()) {
			if (tile.markedChunksImage != null) {
				TileImage.createMarkedChunksImage(tile, getZoomLevel(), selectionInverted);
			}
		}
		if (pastedChunksCache != null) {
			pastedChunksCache.clear();
		}
	}

	public void draw() {
		drawRequested.set(true);
	}

	public void dumpMetrics() {
		Debug.dumpf("TileMap: width=%.2f, height=%.2f, tiles=%d, scale=%.5f, zoomLevel=%d, offset=%s", getWidth(), getHeight(), tiles.size(), scale, getZoomLevel(), offset);
		Debug.dump("Tiles:");
		for (Long2ObjectMap.Entry<Tile> tile : tiles.long2ObjectEntrySet()) {
			Debug.dumpf("  %s: loaded=%s, loading=%s, image=%s, marked=%s, overlay=%s, overlayLoaded=%s, overlayImgLoading=%s, visible=%s, cached=%s",
				new Point2i(tile.getLongKey()),
				tile.getValue().isLoaded(),
				RegionImageGenerator.isLoading(tile.getValue()),
				tile.getValue().getImage() == null ? null : (tile.getValue().getImage().getWidth() + "x" + tile.getValue().getImage().getHeight()),
				tile.getValue().isMarked() ? true : (tile.getValue().getMarkedChunks() != null && !tile.getValue().getMarkedChunks().isEmpty() ? tile.getValue().getMarkedChunks().size() : false),
				tile.getValue().overlay == null ? null : (tile.getValue().overlay.getWidth() + "x" + tile.getValue().overlay.getHeight()),
				tile.getValue().overlayLoaded,
				ParseDataJob.isLoading(tile.getValue()),
				tile.getValue().isVisible(this, TILE_VISIBILITY_THRESHOLD),
				FileHelper.createPNGFilePath(Config.getCacheDir(), getZoomLevel(), new Point2i(tile.getLongKey())).exists()
			);
		}
	}

	public void disable(boolean disabled) {
		this.disabled = disabled;
	}

	public boolean getDisabled() {
		return disabled;
	}

	public void setOnUpdate(Consumer<TileMap> listener) {
		updateListener.add(listener);
	}

	public void setOnHover(Consumer<TileMap> listener) {
		hoverListener.add(listener);
	}

	private void runUpdateListeners() {
		updateListener.forEach(c -> c.accept(this));
	}

	private void runHoverListeners() {
		hoverListener.forEach(c -> c.accept(this));
	}

	public Point2f getOffset() {
		return offset;
	}

	public float getScale() {
		return scale;
	}

	public int getZoomLevel() {
		return Tile.getZoomLevel(scale);
	}

	public void setShowRegionGrid(boolean showRegionGrid) {
		this.showRegionGrid = showRegionGrid;
		draw();
	}

	public void setShowChunkGrid(boolean showChunkGrid) {
		this.showChunkGrid = showChunkGrid;
		draw();
	}

	public void setShowNonexistentRegions(boolean showNonexistentRegions) {
		this.showNonexistentRegions = showNonexistentRegions;
		draw();
	}

	public void goTo(int x, int z) {
		offset = new Point2f(x - getWidth() * scale / 2, z - getHeight() * scale / 2);
		draw();
	}

	public int getSelectedChunks() {
		return selectedChunks;
	}

	public Point2i getHoveredBlock() {
		return hoveredBlock;
	}

	public ObjectOpenHashSet<Point2i> getVisibleRegions() {
		return getVisibleRegions(scale);
	}

	public ObjectOpenHashSet<Point2i> getVisibleRegions(float scale) {
		ObjectOpenHashSet<Point2i> regions = new ObjectOpenHashSet<>();
		runOnVisibleRegions(regions::add, new Point2f(), () -> scale, Integer.MAX_VALUE);
		return regions;
	}

	public int getVisibleTiles() {
		return 0;
	}

	public int getLoadedTiles() {
		return tiles.size();
	}

	public void clear() {
		tiles.clear();
		imgPool.clear();
		overlayPool.clear();
		selectedChunks = 0;
		selectionInverted = false;

		pastedChunks = null;
		pastedWorld = null;
		pastedChunksCache = null;
		pastedChunksOffset = null;
		pastedChunksInverted = false;
	}

	public void markAllTilesAsObsolete() {
		for (Tile tile : tiles.values()) {
			tile.setLoaded(false);
		}
		imgPool.clear();
	}

	public void clearTile(long p) {
		Tile tile = tiles.remove(p);
		if (tile != null) {
			selectedChunks -= tile.getMarkedChunks().size();
			selectedChunks -= tile.isMarked() ? Tile.CHUNKS : 0;
			imgPool.discardImage(tile.getLocation());
			overlayPool.discardData(tile.getLocation());
			tile.unload(true, true);
		}
	}

	public OverlayPool getOverlayPool() {
		return overlayPool;
	}

	public void clearSelection() {
		for (Tile tile : tiles.values()) {
			tile.clearMarks();
		}
		selectedChunks = 0;
		selectionInverted = false;
		draw();
	}

	public void invertSelection() {
		selectionInverted = !selectionInverted;
		redrawOverlays();
		draw();
	}

	public void setSelectionInverted(boolean inverted) {
		selectionInverted = inverted;
	}

	public boolean isSelectionInverted() {
		return selectionInverted;
	}

	public void unloadTiles(boolean overlay, boolean img) {
		for (Tile tile : tiles.values()) {
			tile.unload(overlay, img);
		}
	}

	public void setSmoothRendering(boolean smoothRendering) {
		context.setImageSmoothing(smoothRendering);
	}

	// will return a map of all chunks marked for deletion, mapped to regions.
	// if an entire region is marked for deletion, the value in the map will be null.
	// keys are region coordinates
	// values are chunk coordinates
	public Long2ObjectOpenHashMap<LongOpenHashSet> getMarkedChunks() {
		Long2ObjectOpenHashMap<LongOpenHashSet> chunks = new Long2ObjectOpenHashMap<>();

		for (Long2ObjectMap.Entry<Tile> entry : tiles.long2ObjectEntrySet()) {
			if (entry.getValue().isMarked()) {
				chunks.put(entry.getLongKey(), null);
				continue;
			}
			LongOpenHashSet markedChunks = entry.getValue().getMarkedChunks();
			if (markedChunks.size() == 0) {
				continue;
			}
			// cloning marked chunks for clipboard copy-pasting in the same instance
			chunks.put(entry.getLongKey(), new LongOpenHashSet(markedChunks));
		}
		return chunks;
	}

	public void setMarkedChunks(Long2ObjectOpenHashMap<LongOpenHashSet> chunks) {
		clearSelection();
		for (Long2ObjectMap.Entry<LongOpenHashSet> entry : chunks.long2ObjectEntrySet()) {
			long region = entry.getLongKey();
			Tile tile = tiles.get(region);
			if (tile == null) {
				tile = new Tile(new Point2i(region));
				tiles.put(region, tile);
			}
			if (entry.getValue() == null) {
				tile.mark(true);
				selectedChunks += Tile.CHUNKS;
			} else {
				for (long chunk : entry.getValue()) {
					tile.mark(chunk);
					selectedChunks++;
				}
			}
		}
	}

	public void addMarkedChunks(Long2ObjectOpenHashMap<LongOpenHashSet> chunks) {
		for (Long2ObjectMap.Entry<LongOpenHashSet> entry : chunks.long2ObjectEntrySet()) {
			long region = entry.getLongKey();
			Tile tile = tiles.get(region);
			if (tile == null) {
				tile = new Tile(new Point2i(region));
				tiles.put(region, tile);
			}
			if (entry.getValue() == null) {
				selectedChunks -= tile.getMarkedChunks().size();
				tile.mark(true);
				selectedChunks += Tile.CHUNKS;
			} else if (!tile.isMarked()) {
				for (long chunk : entry.getValue()) {
					if (!tile.isMarked(chunk)) {
						selectedChunks++;
					}
					tile.mark(chunk);
				}
			}
		}
	}

	public void setPastedChunks(Long2ObjectOpenHashMap<LongOpenHashSet> chunks, boolean inverted, Point2i min, Point2i max, WorldDirectories pastedWorld) {
		pastedChunks = chunks;
		pastedChunksInverted = inverted;
		this.pastedWorld = pastedWorld;
		if (chunks == null) {
			pastedChunksCache = null;
			pastedChunksOffset = null;
		} else {
			pastedChunksCache = new HashMap<>();
			Point2i offsetInChunks = offset.toPoint2i().blockToChunk(); // 0|0
			Point2i pastedMid = new Point2i((max.getX() - min.getX()) / 2, (max.getZ() - min.getZ()) / 2);
			Point2i originOffset = offsetInChunks.sub(min).sub(pastedMid);
			Point2f screenSizeInChunks = new Point2f(getWidth(), getHeight()).mul(scale).div(16);
			pastedChunksOffset = originOffset.add(screenSizeInChunks.div(2).toPoint2i());
		}
	}

	public Long2ObjectOpenHashMap<LongOpenHashSet> getPastedChunks() {
		return pastedChunks;
	}

	public boolean getPastedChunksInverted() {
		return pastedChunksInverted;
	}

	public boolean isInPastingMode() {
		return pastedChunks != null;
	}

	public WorldDirectories getPastedWorld() {
		return pastedWorld;
	}

	public Point2i getPastedChunksOffset() {
		return pastedChunksOffset;
	}

	private Point2i getMouseBlock(double x, double z) {
		int blockX = (int) (offset.getX() + x * scale);
		int blockZ = (int) (offset.getY() + z * scale);
		return new Point2i(blockX, blockZ);
	}

	private Point2i getMouseRegionBlock(double x, double z) {
		return getMouseBlock(x, z).blockToRegion();
	}

	private Point2i getMouseChunkBlock(double x, double z) {
		return getMouseBlock(x, z).blockToChunk();
	}

	private void sortPoints(Point2i a, Point2i b) {
		Point2i aa = a.clone();
		a.setX(Math.min(a.getX(), b.getX()));
		a.setZ(Math.min(a.getZ(), b.getZ()));
		b.setX(Math.max(aa.getX(), b.getX()));
		b.setZ(Math.max(aa.getZ(), b.getZ()));
	}

	private void mark(double mouseX, double mouseY, boolean marked) {
		if (scale > CHUNK_GRID_SCALE) {
			Point2i regionBlock = getMouseRegionBlock(mouseX, mouseY);
			Point2i firstRegionBlock = getMouseRegionBlock(firstMouseLocation.getX(), firstMouseLocation.getY());
			sortPoints(firstRegionBlock, regionBlock);
			for (int x = firstRegionBlock.getX(); x <= regionBlock.getX(); x++) {
				for (int z = firstRegionBlock.getZ(); z <= regionBlock.getZ(); z++) {
					Point2i region = new Point2i(x, z);
					Tile tile = tiles.get(region.asLong());
					if (tile == null) {
						tile = tiles.put(region.asLong(), new Tile(region));
					}
					if (tile != null) {
						if (tile.isMarked() && !marked) {
							selectedChunks -= Tile.CHUNKS;
						} else if (!tile.isMarked() && marked) {
							selectedChunks += Tile.CHUNKS - tile.getMarkedChunks().size();
						} else if (!tile.isMarked() && !marked) {
							selectedChunks -= tile.getMarkedChunks().size();
						}
						tile.mark(marked);
					}
				}
			}
		} else {
			Point2i chunkBlock = getMouseChunkBlock(mouseX, mouseY);
			Point2i firstChunkBlock = getMouseChunkBlock(firstMouseLocation.getX(), firstMouseLocation.getY());
			sortPoints(firstChunkBlock, chunkBlock);
			Set<Tile> changedTiles = new HashSet<>();
			for (int x = firstChunkBlock.getX(); x <= chunkBlock.getX(); x++) {
				for (int z = firstChunkBlock.getZ(); z <= chunkBlock.getZ(); z++) {
					Point2i chunk = new Point2i(x, z);
					long longChunk = chunk.asLong();
					Point2i region = chunk.chunkToRegion();
					Tile tile = tiles.get(region.asLong());
					if (tile == null) {
						tile = tiles.put(region.asLong(), new Tile(region));
					}
					if (tile != null) {
						if (tile.isMarked(longChunk) && !marked) {
							selectedChunks--;
							tile.unMark(chunk);
							changedTiles.add(tile);
						} else if (!tile.isMarked(longChunk) && marked) {
							selectedChunks++;
							tile.mark(longChunk);
							changedTiles.add(tile);
						}
					}
				}
			}
			changedTiles.forEach(tile -> TileImage.createMarkedChunksImage(tile, getZoomLevel(), selectionInverted));
		}
	}

	// only draws stuff that is in visible tiles
	private void draw(GraphicsContext ctx) {
		ctx.clearRect(0, 0, getWidth(), getHeight());
		runOnVisibleRegions(region -> {
			Tile tile = tiles.get(region.asLong());

			// use float calculations here to have smooth movement when scrolling
			Point2f canvasOffset = region.regionToBlock().toPoint2f().sub(offset).div(scale);

			TileImage.draw(tile, ctx, scale, canvasOffset, selectionInverted, overlayParser.get() != null, showNonexistentRegions);
		}, new Point2f(), () -> scale, Integer.MAX_VALUE);

		if (pastedChunks != null) {
			runOnVisibleRegions(region -> {
				Point2f regionOffset = region.regionToBlock().toPoint2f().sub(offset.getX(), offset.getY());
				Point2f p = regionOffset.div(scale).add(pastedChunksOffset.mul(16).toPoint2f().div(scale));
				drawPastedChunks(ctx, region, p);
			}, pastedChunksOffset.mul(16).toPoint2f(), () -> scale, Integer.MAX_VALUE);
		}

		if (showRegionGrid) {
			drawRegionGrid(ctx);
		}

		if (showChunkGrid && scale <= CHUNK_GRID_SCALE) {
			drawChunkGrid(ctx);
		}
	}

	private void drawPastedChunks(GraphicsContext ctx, Point2i region, Point2f pos) {
		int zoomLevel = getZoomLevel();

		ctx.setFill(Config.getPasteChunksColor().makeJavaFXColor());

		if (!pastedChunks.containsKey(region.asLong())) {
			if (pastedChunksInverted) {
				ctx.fillRect(pos.getX(), pos.getY(), Math.ceil(Tile.SIZE / scale), Math.ceil(Tile.SIZE / scale));
			}
			return;
		}

		if (!pastedChunksCache.containsKey(region)) {
			WritableImage image = new WritableImage(Tile.SIZE / zoomLevel, Tile.SIZE / zoomLevel);

			Canvas canvas = new Canvas(Tile.SIZE / (float) zoomLevel, Tile.SIZE / (float) zoomLevel);
			GraphicsContext ctx2 = canvas.getGraphicsContext2D();
			ctx2.setFill(Config.getPasteChunksColor().makeJavaFXColor());

			LongOpenHashSet chunks = pastedChunks.get(region.asLong());

			if (chunks == null) {
				if (!pastedChunksInverted) {
					ctx2.fillRect(0, 0, (float) Tile.SIZE / zoomLevel, (float) Tile.SIZE / zoomLevel);
				}
			} else {
				if (pastedChunksInverted) {
					chunks = SelectionData.createInvertedRegionSet(region, chunks);
				}
				if (chunks != null) {
					for (long chunk : chunks) {
						Point2i regionChunk = new Point2i(chunk).and(0x1F);
						ctx2.fillRect(
							regionChunk.getX() * Tile.CHUNK_SIZE / (float) zoomLevel,
							regionChunk.getZ() * Tile.CHUNK_SIZE / (float) zoomLevel,
							Tile.CHUNK_SIZE / (float) zoomLevel, Tile.CHUNK_SIZE / (float) zoomLevel);
					}
				}
			}

			SnapshotParameters params = new SnapshotParameters();
			params.setFill(Color.TRANSPARENT.makeJavaFXColor());

			canvas.snapshot(params, image);

			pastedChunksCache.put(region, image);

			ctx.drawImage(image, pos.getX(), pos.getY(), Tile.SIZE / scale, Tile.SIZE / scale);
		} else {
			ctx.drawImage(pastedChunksCache.get(region), pos.getX(), pos.getY(), Tile.SIZE / scale, Tile.SIZE / scale);
		}
	}

	private void drawRegionGrid(GraphicsContext ctx) {
		ctx.setLineWidth(Tile.GRID_LINE_WIDTH);
		ctx.setStroke(Tile.REGION_GRID_COLOR.makeJavaFXColor());

		Point2f p = getRegionGridMin(offset, scale);

		for (float x = p.getX(); x <= getWidth(); x += Tile.SIZE / scale) {
			ctx.strokeLine(x, 0, x, getHeight());
		}

		for (float y = p.getY(); y <= getHeight(); y += Tile.SIZE / scale) {
			ctx.strokeLine(0, y, getWidth(), y);
		}
	}

	private void drawChunkGrid(GraphicsContext ctx) {
		ctx.setLineWidth(Tile.GRID_LINE_WIDTH);
		ctx.setStroke(Tile.CHUNK_GRID_COLOR.makeJavaFXColor());

		Point2f p = getChunkGridMin(offset, scale);
		Point2f pReg = getRegionGridMin(offset, scale);

		for (float x = p.getX() + Tile.CHUNK_SIZE / scale; x <= getWidth(); x += Tile.CHUNK_SIZE / scale) {
			if (showRegionGrid && (int) (pReg.getX() + Tile.SIZE / scale) == (int) x) {
				pReg.setX(pReg.getX() + Tile.SIZE / scale);
				continue;
			}

			ctx.strokeLine(x, 0, x, getHeight());
		}

		for (float y = p.getY() + Tile.CHUNK_SIZE / scale; y <= getHeight(); y += Tile.CHUNK_SIZE / scale) {
			if (showRegionGrid && (int) (pReg.getY() + Tile.SIZE / scale) == (int) y) {
				pReg.setY(pReg.getY() + Tile.SIZE / scale);
				continue;
			}
			ctx.strokeLine(0, y, getWidth(), y);
		}
	}

	// performs an action on regions in a spiral pattern starting from the center of all visible regions in the TileMap.
	public void runOnVisibleRegions(Consumer<Point2i> consumer, Point2f additionalOffset, Supplier<Float> scaleSupplier, int limit) {
		float scale = scaleSupplier.get();
		Point2i min = offset.sub(additionalOffset).toPoint2i().blockToRegion();
		Point2i max = offset.sub(additionalOffset).add((float) getWidth() * scale, (float) getHeight() * scale).toPoint2i().blockToRegion();

		Point2i mid = min.regionToBlock().add(max.regionToBlock()).div(2).blockToRegion().regionToBlock().blockToRegion();
		int dir = 0; // 0 = right, 1 = down, 2 = left, 3 = up
		int steps = 1;
		int xSteps = 0;
		int ySteps = 0;
		int step = 0;
		int x = mid.getX();
		int y = mid.getZ();
		int count = 0;
		while ((x <= max.getX() || y <= max.getZ()) && (x >= min.getX() || y >= min.getZ())) {
			for (int i = 0; i < steps * 2; i++) {
				x = mid.getX() + xSteps;
				y = mid.getZ() + ySteps;
				if (x <= max.getX() && x >= min.getX() && y <= max.getZ() && y >= min.getZ()) {
					consumer.accept(new Point2i(x, y));
					count++;
					if (count == limit) {
						return;
					}
				}
				switch (dir) {
					case 0 -> xSteps++;
					case 1 -> ySteps++;
					case 2 -> xSteps--;
					case 3 -> ySteps--;
				}
				if (++step == steps) {
					step = 0;
					dir++;
					if (dir > 3) {
						dir = 0;
					}
				}
			}
			steps++;
		}
	}

	public Window getWindow() {
		return window;
	}

	@Override
	public void resize(double width, double height) {
		setWidth(width);
		setHeight(height);
		draw();
	}

	@Override
	public boolean isResizable() {
		return true;
	}

	@Override
	public double minHeight(double width) {
		return 0;
	}

	@Override
	public double minWidth(double height) {
		return 0;
	}

	@Override
	public double maxHeight(double width) {
		return Integer.MAX_VALUE;
	}

	@Override
	public double maxWidth(double height) {
		return Integer.MAX_VALUE;
	}

	@Override
	public void lostOwnership(Clipboard clipboard, Transferable transferable) {
		Debug.dump("TileMap lost ownership");
	}
}
