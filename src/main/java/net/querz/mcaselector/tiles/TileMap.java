package net.querz.mcaselector.tiles;

import javafx.application.Platform;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.*;
import net.querz.mcaselector.Config;
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.io.MCAFilePipe;
import net.querz.mcaselector.io.job.ParseDataJob;
import net.querz.mcaselector.io.job.RegionImageGenerator;
import net.querz.mcaselector.io.SelectionData;
import net.querz.mcaselector.io.WorldDirectories;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class TileMap extends Canvas implements ClipboardOwner {

	private float scale = 1;	//higher --> -    lower --> +

	public static final float CHUNK_GRID_SCALE = 1.5f; //show chunk grid if scale is larger than this
	public static final int TILE_VISIBILITY_THRESHOLD = 2;

	private final Window window;

	private final GraphicsContext context;

	private Point2f offset = new Point2f();

	private Point2f previousMouseLocation = null;
	private Point2f firstMouseLocation = null;

	private final Map<Point2i, Tile> tiles = new HashMap<>();
	private final Set<Tile> visibleTiles = ConcurrentHashMap.newKeySet();

	private int selectedChunks = 0;
	private Point2f mouseHoverLocation = null;
	private Point2i hoveredBlock = null;

	private boolean showChunkGrid = true;
	private boolean showRegionGrid = true;
	private boolean showNonexistentRegions = true;

	private final List<Consumer<TileMap>> updateListener = new ArrayList<>(1);
	private final List<Consumer<TileMap>> hoverListener = new ArrayList<>(1);

	private final KeyActivator keyActivator = new KeyActivator();

	private long totalUpdates = 0;

	private boolean disabled = true;

	private boolean trackpadScrolling = false;

	private final ImagePool imgPool;
	private final OverlayPool overlayPool;

	private List<OverlayParser> overlayParsers = Collections.singletonList(null);
	private OverlayParser overlayParser = null;

	private Map<Point2i, Set<Point2i>> pastedChunks;
	private boolean pastedChunksInverted;
	private WorldDirectories pastedWorld;
	private Map<Point2i, Image> pastedChunksCache;
	private Point2i pastedChunksOffset;
	private Point2i firstPastedChunksOffset;

	private boolean selectionInverted = false;

	public TileMap(Window window, int width, int height) {
		super(width, height);
		this.window = window;
		context = getGraphicsContext2D();
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
		keyActivator.registerGlobalAction(() -> Platform.runLater(this::update));
		this.setOnKeyPressed(this::onKeyPressed);
		this.setOnKeyReleased(this::onKeyReleased);
		this.setOnKeyTyped(this::onKeyTyped);
		offset = new Point2f(-(double) width / 2, -(double) height / 2);

		overlayPool = new OverlayPool(this);
		imgPool = new ImagePool(this, Config.IMAGE_POOL_SIZE);

		setOverlays(Config.getOverlays());
		showNonexistentRegions = Config.showNonExistentRegions();

		update();
	}

	private void updateScale(float oldScale) {
		scale = scale < Config.MAX_SCALE ? Math.max(scale, Config.MIN_SCALE) : Config.MAX_SCALE;
		if (oldScale != scale) {
			//calculate the difference between the old max and the new max point
			Point2f diff = offset.add((float) getWidth() * oldScale, (float) getHeight() * oldScale)
					.sub(offset.add((float) getWidth() * scale, (float) getHeight() * scale));

			offset = offset.add(diff.div(2));

			if (Tile.getZoomLevel(oldScale) != Tile.getZoomLevel(scale)) {
				Debug.dumpf("zoom level changed from %d to %d", Tile.getZoomLevel(oldScale), Tile.getZoomLevel(scale));
				unloadTiles(false);
				if (pastedChunksCache != null) {
					pastedChunksCache.clear();
				}
			}
			update();
		}
	}

	public void nextOverlay() {
		if (disabled) {
			return;
		}

		int index = overlayParsers.indexOf(overlayParser);

		OverlayParser parser;
		do {
			index++;
			if (index == overlayParsers.size()) {
				index = 0;
			}

		// try until we find either null or a parser that is active and valid
		} while ((parser = overlayParsers.get(index)) != null && (!parser.isActive() || !parser.isValid()));

		setOverlay(parser);
		MCAFilePipe.clearParserQueue();
	}

	public void setOverlays(List<OverlayParser> overlays) {
		if (overlays == null) {
			overlayParsers = Collections.singletonList(null);
			setOverlay(null);
			MCAFilePipe.clearParserQueue();
			return;
		}
		overlayParsers = new ArrayList<>(overlays.size() + 1);
		overlayParsers.addAll(overlays);
		overlayParsers.add(null);
		setOverlay(null);
		MCAFilePipe.clearParserQueue();
	}

	public void setOverlay(OverlayParser overlay) {
		this.overlayParser = overlay;
		this.overlayPool.setParser(overlay);
		for (Tile tile : visibleTiles) {
			tile.overlay = null;
			tile.overlayLoaded = false;
		}
		update();
	}

	public OverlayParser getOverlay() {
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
		update();
	}

	public static Point2f getRegionGridMin(Point2f offset, float scale) {
		Point2i min = offset.toPoint2i().blockToRegion();
		Point2i regionOffset = min.regionToBlock().sub((int) offset.getX(), (int) offset.getY());
		return new Point2f(regionOffset.getX() / scale, regionOffset.getZ() / scale);
	}

	public static Point2f getChunkGridMin(Point2f offset, float scale) {
		Point2i min = offset.toPoint2i().blockToChunk();
		Point2i chunkOffset = min.chunkToBlock().sub((int) offset.getX(), (int) offset.getY());
		return new Point2f(chunkOffset.getX() / scale, chunkOffset.getZ() / scale);
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
			update();
		}

		if (event.getCode() == KeyCode.N) {
			dumpMetrics();
		}
	}

	private void onKeyTyped(KeyEvent event) {
		if ("+".equals(event.getCharacter())) {
			zoomFactor(1.05);
		} else if ("-".equals(event.getCharacter())) {
			zoomFactor(0.95);
		}
	}

	private void zoomFactor(double factor) {
		float oldScale = scale;
		scale /= factor;
		updateScale(oldScale);
	}

	private void onKeyReleased(KeyEvent event) {
		if (event.getCode() == KeyCode.SHIFT) {
			keyActivator.releaseActionKey(event.getCode());
		} else {
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
					zoomFactor(1.03 + event.getDeltaY() / 1000);
				} else if (event.getDeltaY() < 0) {
					zoomFactor(0.97 + event.getDeltaY() / 1000);
				}

			} else {
				offset = offset.sub(new Point2f(event.getDeltaX(), event.getDeltaY()).mul(scale));
				update();
			}
		} else {
			if (event.getDeltaY() > 0) {
				zoomFactor(1.03 + event.getDeltaY() / 1000);
			} else if (event.getDeltaY() < 0) {
				zoomFactor(0.97 + event.getDeltaY() / 1000);
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
		zoomFactor(event.getZoomFactor());
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
			update();
		}
	}

	private void onMouseReleased() {
		previousMouseLocation = null;
		firstPastedChunksOffset = null;
	}

	private void onMouseDragged(MouseEvent event) {
		if (event.getButton() == MouseButton.MIDDLE
				|| event.getButton() == MouseButton.PRIMARY && window.isKeyPressed(KeyCode.COMMAND)) {
			Point2f mouseLocation = new Point2f(event.getX(), event.getY());
			if (previousMouseLocation != null) {
				Point2f diff = mouseLocation.sub(previousMouseLocation);
				diff = diff.mul(-1);
				offset = offset.add(diff.mul(scale));
			}
			previousMouseLocation = mouseLocation;
		} else if (!disabled && event.getButton() == MouseButton.PRIMARY) {
			if (pastedChunks != null) {
				Point2f mouseLocation = new Point2f(event.getX(), event.getY());
				Point2f diff = mouseLocation.sub(firstMouseLocation).mul(scale);
				pastedChunksOffset = firstPastedChunksOffset.add(diff.toPoint2i().div(16));
			} else {
				mark(event.getX(), event.getY(), !selectionInverted);
			}
		} else if (!disabled && event.getButton() == MouseButton.SECONDARY) {
			mark(event.getX(), event.getY(), selectionInverted);
		}
		update();
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
		for (Map.Entry<Point2i, Tile> entry : tiles.entrySet()) {
			if (entry.getValue().markedChunksImage != null) {
				TileImage.createMarkedChunksImage(entry.getValue(), getZoomLevel(), selectionInverted);
			}
		}
		if (pastedChunksCache != null) {
			pastedChunksCache.clear();
		}
	}

	public void update() {
		Timer t = new Timer();

		// removes jobs from queue that are no longer needed
		MCAFilePipe.validateJobs(j -> {
			if (j instanceof RegionImageGenerator.MCAImageLoadJob) {
				RegionImageGenerator.MCAImageLoadJob job = (RegionImageGenerator.MCAImageLoadJob) j;
				if (!job.getTile().isVisible(this)) {
					Debug.dumpf("removing %s for tile %s from queue", job.getClass().getSimpleName(), job.getTile().getLocation());
					RegionImageGenerator.setLoading(job.getTile(), false);
					return true;
				}
			} else if (j instanceof ParseDataJob) {
				ParseDataJob job = (ParseDataJob) j;
				if (!job.getTile().isVisible(this)) {
					ParseDataJob.setLoading(job.getTile(), false);
					Debug.dumpf("removing %s for tile %s from queue", job.getClass().getSimpleName(), job.getTile().getLocation());
					return true;
				}
			}
			return false;
		});

		// remove tiles from visibleTiles if they are no longer visible
		for (Tile tile : visibleTiles) {
			if (!tile.isVisible(this, TILE_VISIBILITY_THRESHOLD)) {
				visibleTiles.remove(tile);
				// unload tile if it is currently loading neither the image nor the overlay
				if (!RegionImageGenerator.isLoading(tile) && !ParseDataJob.isLoading(tile)) {
					tile.unload(true);
					if (!tile.isMarked() && tile.getMarkedChunks().size() == 0) {
						tiles.remove(tile.getLocation());
					}
				}
			}
		}

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

		draw(context);
		totalUpdates++;
		if (mouseHoverLocation != null) {
			hoveredBlock = getMouseBlock(mouseHoverLocation.getX(), mouseHoverLocation.getY());
		}
		runUpdateListeners();
		Debug.dumpf("map update #%d: %s", totalUpdates, t);
	}

	public void dumpMetrics() {
		Debug.dumpf("TileMap: width=%.2f, height=%.2f, tiles=%d, visibleTiles=%d, scale=%.5f, offset=%s", getWidth(), getHeight(), tiles.size(), visibleTiles.size(), scale, offset);
		Debug.dump("Tiles:");
		for (Map.Entry<Point2i, Tile> tile : tiles.entrySet()) {
			Debug.dumpf("  %s: loaded=%s, image=%s, marked=%s, overlay=%s, overlayLoaded=%s, overlayImgLoading=%s, visible=%s, noMCA=%s",
				tile.getKey(),
				tile.getValue().isLoaded(),
				tile.getValue().getImage() == null ? null : (tile.getValue().getImage().getWidth() + "x" + tile.getValue().getImage().getHeight()),
				tile.getValue().isMarked() ? true : (tile.getValue().getMarkedChunks() != null && !tile.getValue().getMarkedChunks().isEmpty() ? tile.getValue().getMarkedChunks().size() : false),
				tile.getValue().overlay == null ? null : (tile.getValue().overlay.getWidth() + "x" + tile.getValue().overlay.getHeight()),
				tile.getValue().overlayLoaded,
				ParseDataJob.isLoading(tile.getValue()),
				tile.getValue().isVisible(this),
				imgPool.hasNoMCA(tile.getKey())
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
		update();
	}

	public void setShowChunkGrid(boolean showChunkGrid) {
		this.showChunkGrid = showChunkGrid;
		update();
	}

	public void setShowNonexistentRegions(boolean showNonexistentRegions) {
		this.showNonexistentRegions = showNonexistentRegions;
		update();
	}

	public void goTo(int x, int z) {
		offset = new Point2f(x - getWidth() * scale / 2, z - getHeight() * scale / 2);
		update();
	}

	public int getSelectedChunks() {
		return selectedChunks;
	}

	public Point2i getHoveredBlock() {
		return hoveredBlock;
	}

	public List<Point2i> getVisibleRegions() {
		List<Point2i> regions = new ArrayList<>();
		runOnVisibleRegions(regions::add, new Point2f());
		return regions;
	}

	public int getVisibleTiles() {
		return visibleTiles.size();
	}

	public int getLoadedTiles() {
		return tiles.size();
	}

	public void clear() {
		tiles.clear();
		visibleTiles.clear();
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

	public void clearTile(Point2i p) {
		Tile tile = tiles.remove(p);
		if (tile != null) {
			visibleTiles.remove(tile);
			selectedChunks -= tile.getMarkedChunks().size();
			selectedChunks -= tile.isMarked() ? Tile.CHUNKS : 0;
			imgPool.discardImage(tile.getLocation());
			overlayPool.discardData(tile.getLocation());
			tile.unload(true);
		}
	}

	public OverlayPool getOverlayPool() {
		return overlayPool;
	}

	public void clearSelection() {
		for (Map.Entry<Point2i, Tile> entry : tiles.entrySet()) {
			entry.getValue().clearMarks();
		}
		selectedChunks = 0;
		selectionInverted = false;
		update();
	}

	public void invertSelection() {
		selectionInverted = !selectionInverted;
		redrawOverlays();
		update();
	}

	public void setSelectionInverted(boolean inverted) {
		selectionInverted = inverted;
	}

	public boolean isSelectionInverted() {
		return selectionInverted;
	}

	public void unloadTiles(boolean overlay) {
		for (Tile tile : visibleTiles) {
			tile.unload(overlay);
		}
	}

	//will return a map of all chunks marked for deletion, mapped to regions.
	//if an entire region is marked for deletion, the value in the map will be null.
	//keys are region coordinates
	//values are chunk coordinates
	public Map<Point2i, Set<Point2i>> getMarkedChunks() {
		Map<Point2i, Set<Point2i>> chunks = new HashMap<>();

		for (Map.Entry<Point2i, Tile> entry : tiles.entrySet()) {
			if (entry.getValue().isMarked()) {
				chunks.put(entry.getKey(), null);
				continue;
			}
			Set<Point2i> markedChunks = entry.getValue().getMarkedChunks();
			if (markedChunks.size() == 0) {
				continue;
			}
			// cloning marked chunks for clipboard copy-pasting in the same instance
			chunks.put(entry.getKey(), new HashSet<>(markedChunks));
		}
		return chunks;
	}

	public void setMarkedChunks(Map<Point2i, Set<Point2i>> chunks) {
		clearSelection();
		for (Map.Entry<Point2i, Set<Point2i>> entry : chunks.entrySet()) {
			Point2i region = entry.getKey();
			Tile tile = tiles.get(region);
			if (tile == null) {
				tile = new Tile(region);
				tiles.put(region, tile);
			}
			if (entry.getValue() == null) {
				tile.mark(true);
				selectedChunks += Tile.CHUNKS;
			} else {
				for (Point2i chunk : entry.getValue()) {
					tile.mark(chunk);
					selectedChunks++;
				}
			}
		}
	}

	public void addMarkedChunks(Map<Point2i, Set<Point2i>> chunks) {
		for (Map.Entry<Point2i, Set<Point2i>> entry : chunks.entrySet()) {
			Point2i region = entry.getKey();
			Tile tile = tiles.get(region);
			if (tile == null) {
				tile = new Tile(region);
				tiles.put(region, tile);
			}
			if (entry.getValue() == null) {
				selectedChunks -= tile.getMarkedChunks().size();
				tile.mark(true);
				selectedChunks += Tile.CHUNKS;
			} else if (!tile.isMarked()) {
				for (Point2i chunk : entry.getValue()) {
					if (!tile.isMarked(chunk)) {
						selectedChunks++;
					}
					tile.mark(chunk);
				}
			}
		}
	}

	public void setPastedChunks(Map<Point2i, Set<Point2i>> chunks, boolean inverted, Point2i min, Point2i max, WorldDirectories pastedWorld) {
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

	public Map<Point2i, Set<Point2i>> getPastedChunks() {
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
					Tile tile = tiles.get(region);
					if (tile == null) {
						tile = tiles.put(region, new Tile(region));
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
					Tile tile = tiles.get(chunk.chunkToRegion());
					if (tile == null) {
						tile = tiles.put(chunk.chunkToRegion(), new Tile(chunk.chunkToRegion()));
					}
					if (tile != null) {
						if (tile.isMarked(chunk) && !marked) {
							selectedChunks--;
							tile.unMark(chunk);
							changedTiles.add(tile);
						} else if (!tile.isMarked(chunk) && marked) {
							selectedChunks++;
							tile.mark(chunk);
							changedTiles.add(tile);
						}
					}
				}
			}
			changedTiles.forEach(tile -> TileImage.createMarkedChunksImage(tile, getZoomLevel(), selectionInverted));
		}
	}

	private void draw(GraphicsContext ctx) {
		ctx.clearRect(0, 0, getWidth(), getHeight());
		int zoomLevel = getZoomLevel();
		runOnVisibleRegions(region -> {
			if (!tiles.containsKey(region)) {
				tiles.put(region, new Tile(region));
			}
			Tile tile = tiles.get(region);
			visibleTiles.add(tile);

			Point2i regionOffset = region.regionToBlock().sub((int) offset.getX(), (int) offset.getY());

			if (Config.getWorldDir() != null) {
				if (!tile.isLoaded() || !tile.matchesZoomLevel(zoomLevel)) {
					imgPool.requestImage(tile, zoomLevel);
				}

				if (overlayParser != null && !tile.isOverlayLoaded()) {
					overlayPool.requestImage(tile, overlayParser);
				}
			}

			Point2f p = new Point2f(regionOffset.getX() / scale, regionOffset.getZ() / scale);

			TileImage.draw(tile, ctx, scale, p, selectionInverted, overlayParser != null, showNonexistentRegions);

		}, new Point2f());

		if (pastedChunks != null) {
			runOnVisibleRegions(region -> {
				Point2i regionOffset = region.regionToBlock().sub((int) offset.getX(), (int) offset.getY());
				Point2f p = new Point2f(regionOffset.getX() / scale, regionOffset.getZ() / scale);
				p = p.add(pastedChunksOffset.mul(16).div(scale).toPoint2f());
				drawPastedChunks(ctx, region, p);
			}, pastedChunksOffset.mul(16).toPoint2f());
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

		if (!pastedChunks.containsKey(region)) {
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

			Set<Point2i> chunks = pastedChunks.get(region);

			if (chunks == null) {
				if (!pastedChunksInverted) {
					ctx2.fillRect(0, 0, (float) Tile.SIZE / zoomLevel, (float) Tile.SIZE / zoomLevel);
				}
			} else {
				if (pastedChunksInverted) {
					chunks = SelectionData.createInvertedRegionSet(region, chunks);
				}
				for (Point2i chunk : chunks) {
					Point2i regionChunk = chunk.and(0x1F);
					ctx2.fillRect(
							regionChunk.getX() * Tile.CHUNK_SIZE / (float) zoomLevel,
							regionChunk.getZ() * Tile.CHUNK_SIZE / (float) zoomLevel,
							Tile.CHUNK_SIZE / (float) zoomLevel, Tile.CHUNK_SIZE / (float) zoomLevel);
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

	//performs an action on regions in a spiral pattern starting from the center of all visible regions in the TileMap.
	private void runOnVisibleRegions(Consumer<Point2i> consumer, Point2f additionalOffset) {
		Point2i min = offset.sub(additionalOffset).toPoint2i().blockToRegion();
		Point2i max = offset.sub(additionalOffset).add((float) getWidth() * scale, (float) getHeight() * scale).toPoint2i().blockToRegion();

		Point2i mid = min.regionToBlock().add(max.regionToBlock()).div(2).blockToRegion().regionToBlock().blockToRegion();
		int dir = 0; //0 = right, 1 = down, 2 = left, 3 = up
		int steps = 1;
		int xSteps = 0;
		int ySteps = 0;
		int step = 0;
		int x = mid.getX();
		int y = mid.getZ();
		while ((x <= max.getX() || y <= max.getZ()) && (x >= min.getX() || y >= min.getZ())) {
			for (int i = 0; i < steps * 2; i++) {
				x = mid.getX() + xSteps;
				y = mid.getZ() + ySteps;
				if (x <= max.getX() && x >= min.getX() && y <= max.getZ() && y >= min.getZ()) {
					consumer.accept(new Point2i(x, y));
				}
				switch (dir) {
				case 0:
					xSteps++;
					break;
				case 1:
					ySteps++;
					break;
				case 2:
					xSteps--;
					break;
				case 3:
					ySteps--;
					break;
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
		update();
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
