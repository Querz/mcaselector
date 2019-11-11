package net.querz.mcaselector.tiles;

import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.*;
import net.querz.mcaselector.Config;
import net.querz.mcaselector.io.MCAFilePipe;
import net.querz.mcaselector.io.RegionImageGenerator;
import net.querz.mcaselector.ui.Window;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.key.KeyActivator;
import net.querz.mcaselector.point.Point2f;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.progress.Timer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class TileMap extends Canvas {

	private float scale = 1;	//higher --> -    lower --> +

	public static final float CHUNK_GRID_SCALE = 1.5f; //show chunk grid if scale is larger than this
	public static final int TILE_VISIBILITY_THRESHOLD = 2;

	private Window window;

	private GraphicsContext context;

	private Point2f offset = new Point2f();

	private Point2f previousMouseLocation = null;
	private Point2f firstMouseLocation = null;

	private Map<Point2i, Tile> tiles = new HashMap<>();
	private Set<Tile> visibleTiles = ConcurrentHashMap.newKeySet();

	private int selectedChunks = 0;
	private Point2i hoveredBlock = null;

	private boolean showChunkGrid = true;
	private boolean showRegionGrid = true;

	private List<Consumer<TileMap>> updateListener = new ArrayList<>(1);
	private List<Consumer<TileMap>> hoverListener = new ArrayList<>(1);

	private KeyActivator keyActivator = new KeyActivator();

	private long totalUpdates = 0;

	private boolean disabled = true;

	private boolean trackpadScrolling = false;

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
		keyActivator.registerAction(KeyCode.W, c -> offset = offset.sub(0, (c.contains(KeyCode.SHIFT) ? 10 : 5) * scale));
		keyActivator.registerAction(KeyCode.A, c -> offset = offset.sub((c.contains(KeyCode.SHIFT) ? 10 : 5) * scale, 0));
		keyActivator.registerAction(KeyCode.S, c -> offset = offset.add(0, (c.contains(KeyCode.SHIFT) ? 10 : 5) * scale));
		keyActivator.registerAction(KeyCode.D, c -> offset = offset.add((c.contains(KeyCode.SHIFT) ? 10 : 5) * scale, 0));
		keyActivator.registerGlobalAction(() -> Platform.runLater(this::update));
		this.setOnKeyPressed(this::onKeyPressed);
		this.setOnKeyReleased(this::onKeyReleased);
		offset = new Point2f(-((double) width / 2 - (double) Tile.SIZE / 2), -((double) height / 2 - (double) Tile.SIZE / 2));
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
				unloadTiles();
			}
			update();
		}
	}

	public static Point2f getRegionGridMin(Point2f offset, float scale) {
		Point2i min = offset.toPoint2i().blockToRegion();
		Point2i regionOffset = min.regionToBlock().sub((int) offset.getX(), (int) offset.getY());
		return new Point2f(regionOffset.getX() / scale, regionOffset.getY() / scale);
	}

	public static Point2f getChunkGridMin(Point2f offset, float scale) {
		Point2i min = offset.toPoint2i().blockToChunk();
		Point2i chunkOffset = min.chunkToBlock().sub((int) offset.getX(), (int) offset.getY());
		return new Point2f(chunkOffset.getX() / scale, chunkOffset.getY() / scale);
	}

	private void onKeyPressed(KeyEvent event) {
		if (event.getCode() == KeyCode.SHIFT) {
			keyActivator.pressActionKey(event.getCode());
		} else {
			keyActivator.pressKey(event.getCode());
		}
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
		runHoverListeners();
	}

	private void onMouseExited() {
		hoveredBlock = null;
		runHoverListeners();
	}

	private void onScroll(ScrollEvent event) {
		if (trackpadScrolling || event.isInertia()) {
			offset = offset.sub(new Point2f(event.getDeltaX(), event.getDeltaY()).mul(scale));
			update();
		} else {
			float oldScale = scale;
			scale -= event.getDeltaY() / 100;
			updateScale(oldScale);
		}
	}

	private void onScrollStarted() {
		trackpadScrolling = true;
	}

	private void onScrollFinished() {
		trackpadScrolling = false;
	}

	private void onZoom(ZoomEvent event) {
		float oldScale = scale;
		scale /= event.getZoomFactor();
		updateScale(oldScale);
	}

	private void onMousePressed(MouseEvent event) {
		if (!disabled) {
			firstMouseLocation = new Point2f(event.getX(), event.getY());

			if (event.getButton() == MouseButton.PRIMARY && !window.isKeyPressed(KeyCode.COMMAND)) {
				mark(event.getX(), event.getY(), true);
			} else if (event.getButton() == MouseButton.SECONDARY) {
				mark(event.getX(), event.getY(), false);
			}
			update();
		}
	}

	private void onMouseReleased() {
		previousMouseLocation = null;
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
			mark(event.getX(), event.getY(), true);
		} else if (!disabled && event.getButton() == MouseButton.SECONDARY) {
			mark(event.getX(), event.getY(), false);
		}
		update();
	}

	public void update() {
		Timer t = new Timer();
		runUpdateListeners();

		// removes jobs from queue that are no longer needed
		MCAFilePipe.validateJobs(j -> {
			if (j instanceof  RegionImageGenerator.MCAImageLoadJob) {
				RegionImageGenerator.MCAImageLoadJob job = (RegionImageGenerator.MCAImageLoadJob) j;
				if (!job.getTile().isVisible(this)) {
					Debug.dumpf("removing %s for tile %s from queue", job.getClass().getSimpleName(), job.getTile().getLocation());
					RegionImageGenerator.setLoading(job.getTile(), false);
					return true;
				}
			}
			return false;
		});

		// removes tiles from visibleTiles if they are no longer visible
		for (Tile tile : visibleTiles) {
			if (!tile.isVisible(this, TILE_VISIBILITY_THRESHOLD)) {
				visibleTiles.remove(tile);
				if (!RegionImageGenerator.isLoading(tile)) {
					tile.unload();
					if (!tile.isMarked() && tile.getMarkedChunks().size() == 0) {
						tiles.remove(tile.getLocation());
					}
				}
			}
		}
		draw(context);
		totalUpdates++;
		Debug.dumpf("update took: %s #%d", t, totalUpdates);
	}

	public void disable(boolean disabled) {
		this.disabled = disabled;
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
		runOnVisibleRegions(regions::add);
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
		selectedChunks = 0;
	}

	public void clearTile(Point2i p) {
		Tile tile = tiles.remove(p);
		if (tile != null) {
			visibleTiles.remove(tile);
			selectedChunks -= tile.getMarkedChunks().size();
			selectedChunks -= tile.isMarked() ? Tile.CHUNKS : 0;
			tile.unload();
		}
	}

	public void clearSelection() {
		for (Map.Entry<Point2i, Tile> entry : tiles.entrySet()) {
			entry.getValue().clearMarks();
		}
		selectedChunks = 0;
		update();
	}

	public void unloadTiles() {
		for (Tile tile : visibleTiles) {
			tile.unload();
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
			chunks.put(entry.getKey(), markedChunks);
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
			} else {
				for (Point2i chunk : entry.getValue()) {
					if (!tile.isMarked(chunk)) {
						selectedChunks++;
					}
					tile.mark(chunk);
				}
			}
		}
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
		a.setY(Math.min(a.getY(), b.getY()));
		b.setX(Math.max(aa.getX(), b.getX()));
		b.setY(Math.max(aa.getY(), b.getY()));
	}

	private void mark(double mouseX, double mouseY, boolean marked) {
		if (scale > CHUNK_GRID_SCALE) {
			Point2i regionBlock = getMouseRegionBlock(mouseX, mouseY);
			Point2i firstRegionBlock = getMouseRegionBlock(firstMouseLocation.getX(), firstMouseLocation.getY());
			sortPoints(firstRegionBlock, regionBlock);
			for (int x = firstRegionBlock.getX(); x <= regionBlock.getX(); x++) {
				for (int z = firstRegionBlock.getY(); z <= regionBlock.getY(); z++) {
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
				for (int z = firstChunkBlock.getY(); z <= chunkBlock.getY(); z++) {
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
			changedTiles.forEach(tile -> TileImage.createMarkedChunksImage(tile, getZoomLevel()));
		}
	}

	private void draw(GraphicsContext ctx) {
		ctx.setFill(Tile.EMPTY_CHUNK_BACKGROUND_COLOR.makeJavaFXColor());
		ctx.fillRect(0, 0, getWidth(), getHeight());
		runOnVisibleRegions(region -> {
			if (!tiles.containsKey(region)) {
				tiles.put(region, new Tile(region));
			}
			Tile tile = tiles.get(region);
			visibleTiles.add(tile);

			Point2i regionOffset = region.regionToBlock().sub((int) offset.getX(), (int) offset.getY());

			if (!tile.isLoaded() && !tile.isLoading()) {
				RegionImageGenerator.generate(tile, () -> Platform.runLater(this::update), this::getScale, false, false, null);
			}
			Point2f p = new Point2f(regionOffset.getX() / scale, regionOffset.getY() / scale);
			TileImage.draw(tile, ctx, scale, p);
		});

		if (showRegionGrid) {
			drawRegionGrid(ctx);
		}

		if (showChunkGrid && scale <= CHUNK_GRID_SCALE) {
			drawChunkGrid(ctx);
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
	private void runOnVisibleRegions(Consumer<Point2i> consumer) {
		Point2i min = offset.toPoint2i().blockToRegion();
		Point2i max = offset.add((float) getWidth() * scale, (float) getHeight() * scale).toPoint2i().blockToRegion();
		Point2i mid = min.regionToBlock().add(max.regionToBlock()).div(2).blockToRegion().regionToBlock().blockToRegion();
		int dir = 0; //0 = right, 1 = down, 2 = left, 3 = up
		int steps = 1;
		int xSteps = 0;
		int ySteps = 0;
		int step = 0;
		int x = mid.getX();
		int y = mid.getY();
		while ((x <= max.getX() || y <= max.getY()) && (x >= min.getX() || y >= min.getY())) {
			for (int i = 0; i < steps * 2; i++) {
				x = mid.getX() + xSteps;
				y = mid.getY() + ySteps;
				if (x <= max.getX() && x >= min.getX() && y <= max.getY() && y >= min.getY()) {
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
}
