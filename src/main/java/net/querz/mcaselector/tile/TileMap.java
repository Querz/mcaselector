package net.querz.mcaselector.tile;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import net.querz.mcaselector.config.Config;
import net.querz.mcaselector.config.ConfigProvider;
import net.querz.mcaselector.io.*;
import net.querz.mcaselector.io.job.ParseDataJob;
import net.querz.mcaselector.io.job.RegionImageGenerator;
import net.querz.mcaselector.property.DataProperty;
import net.querz.mcaselector.selection.ChunkSet;
import net.querz.mcaselector.selection.Selection;
import net.querz.mcaselector.selection.SelectionData;
import net.querz.mcaselector.overlay.Overlay;
import net.querz.mcaselector.ui.DialogHelper;
import net.querz.mcaselector.ui.ProgressTask;
import net.querz.mcaselector.ui.Window;
import net.querz.mcaselector.point.Point2f;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.progress.Timer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class TileMap extends Canvas implements ClipboardOwner {

	private static final Logger LOGGER = LogManager.getLogger(TileMap.class);

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
	private boolean showCoordinates = false;
	private boolean showNonexistentRegions;

	private final List<Consumer<TileMap>> updateListener = new ArrayList<>(1);
	private final List<Consumer<TileMap>> hoverListener = new ArrayList<>(1);

	private final KeyActivator keyActivator = new KeyActivator();

	private long totalDraws = 0;

	private boolean disabled = true;

	private boolean trackpadScrolling = false;

	private final ImagePool imgPool;
	private final OverlayPool overlayPool;

	private List<Overlay> overlays = Collections.singletonList(null);
	private final ObjectProperty<Overlay> overlayParser = new SimpleObjectProperty<>(null);

	private Selection pastedChunks;
	private WorldDirectories pastedWorld;
	private Map<Point2i, Image> pastedChunksCache;
	private Point2i pastedChunksOffset;
	private Point2i firstPastedChunksOffset;

	private Selection selection = new Selection();

	// used to scale existing images asynchronously
	private ScheduledExecutorService updateService;

	private ScheduledExecutorService drawService;
	private final AtomicBoolean drawRequested = new AtomicBoolean(false);

	private boolean unsavedSelection = false;

	public TileMap(Window window, int width, int height) {
		super(width, height);
		this.window = window;
		context = getGraphicsContext2D();
		context.setImageSmoothing(ConfigProvider.WORLD.getSmoothRendering());
		context.setFont(Font.font("Monospaced", FontWeight.BOLD, null, 16));
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

		setOverlays(ConfigProvider.OVERLAY.getOverlays());
		showNonexistentRegions = ConfigProvider.WORLD.getShowNonexistentRegions();

		RegionImageGenerator.setCacheEligibilityChecker(region -> {
			DataProperty<Boolean> eligible = new DataProperty<>(false);
			runOnVisibleRegions(r -> {
				if (region.equals(r)) {
					eligible.set(true);
				}
			}, new Point2f(), () -> scale, ConfigProvider.GLOBAL.getMaxLoadedFiles());
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
				if (ConfigProvider.WORLD.getRegionDir() == null) {
					return;
				}

				// refresh tiles map
				tiles.values().removeIf(v -> {
					boolean visible = v.isVisible(this, TILE_VISIBILITY_THRESHOLD);

					// unload tile if it's not visible
					if (!visible) {
						v.unload(true, true);
					}

					return !visible && v.getImage() == null;
				});

				// clean up all queues based on visible tiles
				JobHandler.validateJobs(j -> {
					if (j instanceof RegionImageGenerator.MCAImageProcessJob job) {
						if (!job.getTile().isVisible(this)) {
							LOGGER.debug("removing {} for tile {} from queue", job.getClass().getSimpleName(), job.getTile().getLocation());
							RegionImageGenerator.setLoading(job.getTile(), false);
							return true;
						}
					} else if (j instanceof ParseDataJob job) {
						if (!job.getTile().isVisible(this)) {
							ParseDataJob.setLoading(job.getTile(), false);
							LOGGER.debug("removing {} for tile {} from queue", job.getClass().getSimpleName(), job.getTile().getLocation());
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
				LOGGER.warn("failed to update", ex);
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
				LOGGER.trace("draw #{}: {}", totalDraws++, t);
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
				LOGGER.debug("zoom level changed from {} to {}", Tile.getZoomLevel(oldScale), Tile.getZoomLevel(scale));
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

		int index = overlays.indexOf(overlayParser.get());

		Overlay parser;
		do {
			index++;
			if (index == overlays.size()) {
				index = 0;
			}

		// repeat if the current parser is null, it is invalid or inactive or if the types are not the same
		} while ((parser = overlays.get(index)) == null || !parser.isActive() || !parser.isValid() || parser.getType() != overlayParser.get().getType());

		setOverlay(parser);
		JobHandler.cancelParserQueue();
		draw();
	}

	public void nextOverlayType() {
		if (disabled) {
			return;
		}

		int index = overlays.indexOf(overlayParser.get());

		Overlay parser;
		do {
			index++;
			if (index == overlays.size()) {
				index = 0;
			}

			// repeat if the current parser is not null, it is inactive or invalid or the types are equal
		} while ((parser = overlays.get(index)) != null && (!parser.isActive() || !parser.isValid() || overlayParser.get() != null && parser.getType() == overlayParser.get().getType()));

		setOverlay(parser);
		JobHandler.cancelParserQueue();
		draw();
	}

	public void setOverlays(List<Overlay> overlays) {
		if (overlays == null) {
			this.overlays = Collections.singletonList(null);
			setOverlay(null);
			JobHandler.cancelParserQueue();
			return;
		}
		this.overlays = new ArrayList<>(overlays.size() + 1);
		this.overlays.addAll(overlays);
		this.overlays.sort(Comparator.comparing(Overlay::getType));
		this.overlays.add(null);
		setOverlay(null);
		JobHandler.cancelParserQueue();
	}

	public void setOverlay(Overlay overlay) {
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

	public Overlay getOverlay() {
		return overlayParser.get();
	}

	public ObjectProperty<Overlay> overlayParserProperty() {
		return overlayParser;
	}

	// returns a NEW copy of all current overlays
	public List<Overlay> getOverlays() {
		List<Overlay> overlays = new ArrayList<>(this.overlays.size() - 1);
		for (Overlay parser : this.overlays) {
			if (parser != null) {
				overlays.add(parser.clone());
			}
		}
		return overlays;
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
			LOGGER.debug("cancelling chunk pasting");
			pastedChunks = null;
			pastedWorld = null;
			pastedChunksCache = null;
			pastedChunksOffset = null;
			draw();
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
				mark(event.getX(), event.getY(), true);
			} else if (event.getButton() == MouseButton.SECONDARY) {
				mark(event.getX(), event.getY(), false);
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
				mark(event.getX(), event.getY(), true);
			}
		} else if (!disabled && event.getButton() == MouseButton.SECONDARY) {
			mark(event.getX(), event.getY(), false);
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
				DialogHelper.setWorld(wd, List.of(wd.getRegion().getParentFile()), this, window.getPrimaryStage());
			}
			event.setDropCompleted(true);
		}
		event.consume();
	}

	public void redrawOverlays() {
		for (Tile tile : tiles.values()) {
			if (tile.markedChunksImage != null) {
				TileImage.createMarkedChunksImage(tile, selection.getSelectedChunks(tile.getLocation()));
			}
		}
		if (pastedChunksCache != null) {
			pastedChunksCache.clear();
		}
	}

	public void draw() {
		drawRequested.set(true);
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

	public void setShowCoordinates(boolean showCoordinates) {
		this.showCoordinates = showCoordinates;
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
		clear(null);
	}

	public void clear(ProgressTask loadWorldTask) {
		tiles.clear();
		imgPool.clear(loadWorldTask);
		overlayPool.clear();

		pastedChunks = null;
		pastedWorld = null;
		pastedChunksCache = null;
		pastedChunksOffset = null;
	}

	public void markAllTilesAsObsolete() {
		for (Tile tile : tiles.values()) {
			tile.setLoaded(false);
		}
		imgPool.clear(null);
	}

	public void clearTile(long p) {
		Tile tile = tiles.remove(p);
		if (tile != null) {
			tile.unload(true, true);
		}
		imgPool.discardImage(new Point2i(p));
		overlayPool.discardData(new Point2i(p));
	}

	public OverlayPool getOverlayPool() {
		return overlayPool;
	}

	public void clearSelection() {
		selection = new Selection();
		selectedChunks = 0;
		unsavedSelection = false;

		for (Tile tile : tiles.values()) {
			tile.clearMarkedChunksImage();
		}
		draw();
	}

	public void invertSelection() {
		selection.setInverted(!selection.isInverted());
		redrawOverlays();
		draw();
	}

	public void invertRegionsWithSelection() {
		selection.invertAll();
		selectedChunks = selection.count();
		unsavedSelection = true;
		runUpdateListeners();
		redrawOverlays();
		draw();
	}

	public void unloadTiles(boolean overlay, boolean img) {
		for (Tile tile : tiles.values()) {
			tile.unload(overlay, img);
		}
	}

	public void setSmoothRendering(boolean smoothRendering) {
		context.setImageSmoothing(smoothRendering);
	}

	public Selection getSelection() {
		return selection;
	}

	public void addSelection(Selection selection) {
		int selectedBefore = selectedChunks;
		this.selection.merge(selection);
		selectedChunks = this.selection.count();
		// reset selection image of tile
		for (Long2ObjectMap.Entry<ChunkSet> e : selection) {
			Tile tile = tiles.get(e.getLongKey());
			if (tile != null) {
				tile.clearMarkedChunksImage();
			}
		}
		unsavedSelection = !selection.isEmpty() || selectedBefore == selectedChunks && unsavedSelection;
	}

	public void setSelection(Selection selection) {
		this.selection = selection;
		selectedChunks = selection.count();
		unsavedSelection = !selection.isEmpty();
	}

	public void setPastedChunks(SelectionData data) {
		pastedChunks = data.getSelection();
		this.pastedWorld = data.getWorld();
		if (data.getSelection() == null) {
			pastedChunksCache = null;
			pastedChunksOffset = null;
		} else {
			pastedChunksCache = new HashMap<>();
			Point2i offsetInChunks = offset.toPoint2i().blockToChunk(); // 0|0
			Point2i pastedMid = new Point2i((data.getMax().getX() - data.getMin().getX()) / 2, (data.getMax().getZ() - data.getMin().getZ()) / 2);
			Point2i originOffset = offsetInChunks.sub(data.getMin()).sub(pastedMid);
			Point2f screenSizeInChunks = new Point2f(getWidth(), getHeight()).mul(scale).div(16);
			pastedChunksOffset = originOffset.add(screenSizeInChunks.div(2).toPoint2i());
		}
	}

	public boolean hasUnsavedSelection() {
		return unsavedSelection;
	}

	public void setSelectionSaved() {
		unsavedSelection = false;
	}

	public Selection getPastedChunks() {
		return pastedChunks;
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
		int blockX = (int) Math.floor(offset.getX() + x * scale);
		int blockZ = (int) Math.floor(offset.getY() + z * scale);
		return new Point2i(blockX, blockZ);
	}

	private Point2i getMouseRegion(double x, double z) {
		return getMouseBlock(x, z).blockToRegion();
	}

	private Point2i getMouseChunk(double x, double z) {
		return getMouseBlock(x, z).blockToChunk();
	}

	private void sortPoints(Point2i a, Point2i b) {
		Point2i aa = a.clone();
		a.setX(Math.min(a.getX(), b.getX()));
		a.setZ(Math.min(a.getZ(), b.getZ()));
		b.setX(Math.max(aa.getX(), b.getX()));
		b.setZ(Math.max(aa.getZ(), b.getZ()));
	}

	private void mark(double mouseX, double mouseY, boolean mark) {
		boolean paintMode = window.isKeyPressed(KeyCode.SHIFT);
		if (paintMode) {
			firstMouseLocation = new Point2f(mouseX, mouseY);
		}
		int selectedBefore = selectedChunks;
		if (scale > CHUNK_GRID_SCALE) {
			Point2i mouseRegion = getMouseRegion(mouseX, mouseY);
			Point2i firstRegion = paintMode ? mouseRegion : getMouseRegion(firstMouseLocation.getX(), firstMouseLocation.getY());
			sortPoints(firstRegion, mouseRegion);
			for (int x = firstRegion.getX(); x <= mouseRegion.getX(); x++) {
				for (int z = firstRegion.getZ(); z <= mouseRegion.getZ(); z++) {
					Point2i region = new Point2i(x, z);
					if (mark) {
						int diff = selection.addRegion(region.asLong());
						selectedChunks += selection.isInverted() ? -diff : diff;
					} else {
						int diff = selection.removeRegion(region.asLong());
						selectedChunks += selection.isInverted() ? diff : -diff;
					}
				}
			}
		} else {
			Point2i mouseChunk = getMouseChunk(mouseX, mouseY);
			Point2i firstChunk = paintMode ? mouseChunk : getMouseChunk(firstMouseLocation.getX(), firstMouseLocation.getY());
			sortPoints(firstChunk, mouseChunk);
			for (int x = firstChunk.getX(); x <= mouseChunk.getX(); x++) {
				for (int z = firstChunk.getZ(); z <= mouseChunk.getZ(); z++) {
					Point2i chunk = new Point2i(x, z);
					Point2i region = chunk.chunkToRegion();

					if (mark) {
						if (!selection.isChunkSelected(x, z)) {
							selection.addChunk(chunk);
							if (selection.isInverted()) {
								selectedChunks--;
							} else {
								selectedChunks++;
							}
							resetMarkedChunksImage(region);
						}
					} else {
						if (selection.isChunkSelected(x, z)) {
							selection.removeChunk(chunk);
							if (selection.isInverted()) {
								selectedChunks++;
							} else {
								selectedChunks--;
							}
							resetMarkedChunksImage(region);
						}
					}
				}
			}
		}
		unsavedSelection = !selection.isEmpty() || selectedBefore == selectedChunks && unsavedSelection;
	}

	private void resetMarkedChunksImage(Point2i region) {
		Tile tile = tiles.get(region.asLong());
		if (tile == null) {
			tile = tiles.put(region.asLong(), new Tile(region));
		}
		tile.markedChunksImage = null;
	}

	// only draws stuff that is in visible tiles
	private void draw(GraphicsContext ctx) {
		ctx.clearRect(0, 0, getWidth(), getHeight());
		runOnVisibleRegions(region -> {
			Tile tile = tiles.get(region.asLong());

			// use float calculations here to have smooth movement when scrolling
			Point2f canvasOffset = region.regionToBlock().toPoint2f().sub(offset).div(scale);

			TileImage.draw(ctx, tile, scale, canvasOffset, selection, overlayParser.get() != null, showNonexistentRegions);
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

		if (showCoordinates) {
			if (scale < 1.2) {
				drawChunkCoordinates(ctx);
			} else {
				drawRegionCoordinates(ctx);
			}
		}
	}

	private void drawRegionCoordinates(GraphicsContext ctx) {
		ctx.setFill(Tile.COORDINATES_COLOR.makeJavaFXColor());

		Point2f p = getRegionGridMin(offset, scale);

		int multiplier = 1;
		if (scale > 7) {
			multiplier = 4;
		} else if (scale > 4) {
			multiplier = 2;
		}

		float step = Tile.SIZE / scale;
		float halfStep = Tile.SIZE / (scale * 2);
		int mul = multiplier * Tile.SIZE;
		boolean oldStep = true;
		Point2f first = p;

		for (float x = first.getX(); x <= getWidth(); x += step) {
			for (float y = first.getY(); y <= getHeight(); y += step) {
				Point2i region = getMouseRegion(x + halfStep, y + halfStep).regionToBlock();
				if (!oldStep || region.getX() % mul == 0 && region.getZ() % mul == 0) {
					ctx.fillText(region.getX() + "," + region.getZ(), x + 2, y + 16);
					if (oldStep) {
						step *= multiplier;
						oldStep = false;
						first = new Point2f(x, y);
					}
				}
			}
		}

		for (float y = first.getY(); y <= getHeight(); y += step) {
			Point2i region = getMouseRegion(first.getX() - step + halfStep, y + halfStep).regionToBlock();
			ctx.fillText(region.getX() + "," + region.getZ(), first.getX() - step + 2, y + 16);
		}
	}

	private void drawChunkCoordinates(GraphicsContext ctx) {
		ctx.setFill(Tile.COORDINATES_COLOR.makeJavaFXColor());

		Point2f p = getRegionGridMin(offset, scale);

		int multiplier = 1;
		if (scale > 0.8) {
			multiplier = 16;
		} else if (scale > 0.4) {
			multiplier = 8;
		} else if (scale > 0.2) {
			multiplier = 4;
		} else if (scale > 0.1) {
			multiplier = 2;
		}

		float step = Tile.CHUNK_SIZE / scale;
		float halfStep = Tile.CHUNK_SIZE / (scale * 2);
		int mul = multiplier * Tile.CHUNK_SIZE;
		boolean oldStep = true;
		Point2f first = p;

		for (float x = first.getX(); x <= getWidth(); x += step) {
			for (float y = first.getY(); y <= getHeight(); y += step) {
				Point2i chunk = getMouseChunk(x + halfStep, y + halfStep).chunkToBlock();

				if (!oldStep || chunk.getX() % mul == 0 && chunk.getZ() % mul == 0) {
					ctx.fillText(chunk.getX() + "," + chunk.getZ(), x + 2, y + 16);
					if (oldStep) {
						step *= multiplier;
						oldStep = false;
						first = new Point2f(x, y);
					}
				}
			}
		}
	}

	private void drawPastedChunks(GraphicsContext ctx, Point2i region, Point2f pos) {
		javafx.scene.paint.Color color = ConfigProvider.GLOBAL.getPasteChunksColor().makeJavaFXColor();
		ctx.setFill(color);

		if (pastedChunks.isRegionSelected(region.asLong())) {
			ctx.fillRect(pos.getX(), pos.getY(), Math.ceil(Tile.SIZE / scale), Math.ceil(Tile.SIZE / scale));
			return;
		}

		if (!pastedChunksCache.containsKey(region)) {
			WritableImage wImage = new WritableImage(Tile.SIZE_IN_CHUNKS, Tile.SIZE_IN_CHUNKS);
			PixelWriter writer = wImage.getPixelWriter();

			ChunkSet chunks = pastedChunks.getSelectedChunks(region);

			for (int chunk : chunks) {
				Point2i regionChunk = new Point2i(chunk);
				writer.setColor(regionChunk.getX(), regionChunk.getZ(), color);
			}

			pastedChunksCache.put(region, wImage);

			ctx.drawImage(wImage, pos.getX(), pos.getY(), Tile.SIZE / scale, Tile.SIZE / scale);
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
		LOGGER.debug("TileMap lost ownership");
	}
}
