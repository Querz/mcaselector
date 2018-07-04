package net.querz.mcaselector.tiles;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.*;
import net.querz.mcaselector.ui.Window;
import net.querz.mcaselector.util.Helper;
import net.querz.mcaselector.util.Point2f;
import net.querz.mcaselector.util.Point2i;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class TileMap extends Canvas {

	private float scale = 1;	//higher --> +    lower -->  -

	public static final float MAX_SCALE = 5;
	public static final float MIN_SCALE = 0.2f;
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

	private QueuedRegionImageGenerator qrig;

	public TileMap(Window window, int width, int height) {
		super(width, height);
		this.window = window;
		context = getGraphicsContext2D();
		this.setOnMousePressed(this::onMousePressed);
		this.setOnMouseReleased(e -> onMouseReleased());
		this.setOnMouseDragged(this::onMouseDragged);
		this.setOnScroll(this::onScroll);
		this.setOnMouseMoved(this::onMouseMoved);
		this.setOnMouseExited(e -> onMouseExited());
		qrig = new QueuedRegionImageGenerator(QueuedRegionImageGenerator.PROCESSOR_COUNT, this);
		update();
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
		float oldScale = scale;
		scale -= event.getDeltaY() / 100;
		scale = scale < MAX_SCALE ? (scale > MIN_SCALE ? scale : MIN_SCALE) : MAX_SCALE;

		//calculate the difference between the old max and the new max point
		Point2f diff = offset.add((float) getWidth() * oldScale, (float) getHeight() * oldScale)
				.sub(offset.add((float) getWidth() * scale, (float) getHeight() * scale));

		offset = offset.add(diff.div(2));
		update();
	}

	private void onMousePressed(MouseEvent event) {
		firstMouseLocation = new Point2f(event.getX(), event.getY());

		if (event.getButton() == MouseButton.PRIMARY && !window.isKeyPressed(KeyCode.COMMAND)) {
			mark(event.getX(), event.getY(), true);
		} else if (event.getButton() == MouseButton.SECONDARY) {
			mark(event.getX(), event.getY(), false);
		}
		update();
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
		} else if (event.getButton() == MouseButton.PRIMARY) {
			mark(event.getX(), event.getY(), true);
		} else if (event.getButton() == MouseButton.SECONDARY) {
			mark(event.getX(), event.getY(), false);
		}
		update();
	}

	public void update() {
		runUpdateListeners();
		qrig.validateJobs();
		for (Tile tile : visibleTiles) {
			if (!tile.isVisible(this, TILE_VISIBILITY_THRESHOLD)) {
				visibleTiles.remove(tile);
				tile.unload();
				if (!tile.isMarked() && tile.getMarkedChunks().size() == 0) {
					tiles.remove(tile.getLocation());
				}
			}
		}
		draw(context);
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
		runOnVisibleRegions(r -> regions.add(Helper.blockToRegion(r)));
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
		}
	}

	public void clearSelection() {
		for (Map.Entry<Point2i, Tile> entry : tiles.entrySet()) {
			entry.getValue().clearMarks();
		}
		selectedChunks = 0;
		update();
	}

	//will return a map of all chunks marked for deletion, mapped to regions.
	//if an entire region is marked for deletion, the value in the map will be null.
	//keys are region coordinates
	//values are chunk coordinates
	public Map<Point2i, Set<Point2i>> getMarkedChunks() {
		Map<Point2i, Set<Point2i>> chunks = new HashMap<>();

		for (Map.Entry<Point2i, Tile> entry : tiles.entrySet()) {
			if (entry.getValue().isMarked()) {
				chunks.put(Helper.blockToRegion(entry.getKey()), null);
				continue;
			}
			Set<Point2i> markedChunks = entry.getValue().getMarkedChunks();
			if (markedChunks.size() == 0) {
				continue;
			}
			Set<Point2i> markedChunksList = new HashSet<>(markedChunks.size());
			markedChunks.forEach(c -> markedChunksList.add(Helper.blockToChunk(c)));
			chunks.put(Helper.blockToRegion(entry.getKey()), markedChunksList);
		}
		return chunks;
	}

	public void setMarkedChunks(Map<Point2i, Set<Point2i>> chunks) {
		clearSelection();
		for (Map.Entry<Point2i, Set<Point2i>> entry : chunks.entrySet()) {
			Point2i region = Helper.regionToBlock(entry.getKey());
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
					tile.mark(Helper.chunkToBlock(chunk));
					selectedChunks++;
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
		return Helper.regionToBlock(Helper.blockToRegion(getMouseBlock(x, z)));
	}

	private Point2i getMouseChunkBlock(double x, double z) {
		return Helper.chunkToBlock(Helper.blockToChunk(getMouseBlock(x, z)));
	}

	private void sortPoints(Point2i a, Point2i b) {
		Point2i aa = a.clone();
		a.setX(a.getX() < b.getX() ? a.getX() : b.getX());
		a.setY(a.getY() < b.getY() ? a.getY() : b.getY());
		b.setX(aa.getX() < b.getX() ? b.getX() : aa.getX());
		b.setY(aa.getY() < b.getY() ? b.getY() : aa.getY());
	}

	private void mark(double mouseX, double mouseY, boolean marked) {
		if (scale > CHUNK_GRID_SCALE) {
			Point2i regionBlock = getMouseRegionBlock(mouseX, mouseY);
			Point2i firstRegionBlock = getMouseRegionBlock(firstMouseLocation.getX(), firstMouseLocation.getY());
			sortPoints(firstRegionBlock, regionBlock);
			for (int x = firstRegionBlock.getX(); x <= regionBlock.getX(); x += Tile.SIZE) {
				for (int z = firstRegionBlock.getY(); z <= regionBlock.getY(); z += Tile.SIZE) {
					Tile tile = tiles.get(new Point2i(x, z));
					if (tile != null && !tile.isEmpty()) {
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
			for (int x = firstChunkBlock.getX(); x <= chunkBlock.getX(); x += Tile.CHUNK_SIZE) {
				for (int z = firstChunkBlock.getY(); z <= chunkBlock.getY(); z += Tile.CHUNK_SIZE) {
					Point2i chunk = new Point2i(x, z);
					Tile tile = tiles.get(Helper.regionToBlock(Helper.blockToRegion(chunk)));
					if (tile != null) {
						if (tile.isMarked(chunk) && !marked && !tile.isEmpty()) {
							selectedChunks--;
							tile.unMark(chunk);
						} else if (!tile.isMarked(chunk) && marked && !tile.isEmpty()) {
							selectedChunks++;
							tile.mark(chunk);
						}
					}
				}
			}
		}
	}

	private void draw(GraphicsContext ctx) {
		ctx.setFill(Tile.EMPTY_CHUNK_BACKGROUND_COLOR);
		ctx.fillRect(0, 0, getWidth(), getHeight());
		runOnVisibleRegions(region -> {
			if (!tiles.containsKey(region)) {
				tiles.put(region, new Tile(region));
			}
			Tile tile = tiles.get(region);
			visibleTiles.add(tile);

			Point2i regionOffset = region.sub((int) offset.getX(), (int) offset.getY());

			if (!tile.isLoaded() && !tile.isLoading()) {
				qrig.addJob(tile);
			}
			Point2f p = new Point2f(regionOffset.getX() / scale, regionOffset.getY() / scale);
			tile.draw(ctx, scale, p, showRegionGrid, showChunkGrid);
		});
	}

	private void runOnVisibleRegions(Consumer<Point2i> consumer) {
		//regionLocation is the south-west-most visible region in the window
		Point2i regionLocation = Helper.regionToBlock(Helper.blockToRegion(offset.toPoint2i()));

		//get all tiles that are visible inside the window
		for (int x = regionLocation.getX(); x < offset.getX() + getWidth() * scale; x += Tile.SIZE) {
			for (int z = regionLocation.getY(); z < offset.getY() + getHeight() * scale; z += Tile.SIZE) {
				consumer.accept(new Point2i(x, z));
			}
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
