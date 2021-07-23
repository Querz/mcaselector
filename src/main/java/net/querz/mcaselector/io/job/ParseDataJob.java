package net.querz.mcaselector.io.job;

import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.io.ByteArrayPointer;
import net.querz.mcaselector.io.RegionDirectories;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.io.mca.EntitiesMCAFile;
import net.querz.mcaselector.io.mca.PoiMCAFile;
import net.querz.mcaselector.io.mca.RegionMCAFile;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.progress.Timer;
import net.querz.mcaselector.tiles.Tile;
import net.querz.mcaselector.tiles.overlay.OverlayParser;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public class ParseDataJob extends ProcessDataJob {

	private static final Set<Point2i> loading = ConcurrentHashMap.newKeySet();

	private final BiConsumer<int[], UUID> dataCallback;
	private final UUID world;
	private final OverlayParser parser;
	private final Tile tile;

	public ParseDataJob(Tile tile, RegionDirectories dirs, UUID world, BiConsumer<int[], UUID> dataCallback, OverlayParser parser) {
		super(dirs);
		this.tile = tile;
		this.dataCallback = dataCallback;
		this.world = world;
		this.parser = parser;
		setLoading(tile, true);
	}

	public static boolean isLoading(Tile tile) {
		return loading.contains(tile.getLocation());
	}

	public static synchronized void setLoading(Tile tile, boolean loading) {
		if (loading) {
			ParseDataJob.loading.add(tile.getLocation());
		} else {
			ParseDataJob.loading.remove(tile.getLocation());
		}
	}

	public Tile getTile() {
		return tile;
	}

	@Override
	public void run() {
		execute();
	}

	@Override
	public void execute() {
		Timer t = new Timer();

		RegionMCAFile regionMCAFile = null;
		if (getRegionDirectories().getRegion() != null && getRegionDirectories().getRegion().exists() && getRegionDirectories().getRegion().length() > 0) {
			byte[] regionData = loadRegion();
			regionMCAFile = new RegionMCAFile(getRegionDirectories().getRegion());
			if (regionData != null) {
				// load EntitiesMCAFile
				ByteArrayPointer ptr = new ByteArrayPointer(regionData);
				try {
					regionMCAFile.load(ptr);
				} catch (IOException ex) {
					Debug.errorf("failed to read mca file header from %s", getRegionDirectories().getRegion());
				}
			}
		}

		EntitiesMCAFile entitiesMCAFile = null;
		if (getRegionDirectories().getEntities() != null && getRegionDirectories().getEntities().exists() && getRegionDirectories().getEntities().length() > 0) {
			byte[] entitiesData = loadEntities();
			entitiesMCAFile = new EntitiesMCAFile(getRegionDirectories().getEntities());
			if (entitiesData != null) {
				// load EntitiesMCAFile
				ByteArrayPointer ptr = new ByteArrayPointer(entitiesData);
				try {
					entitiesMCAFile.load(ptr);
				} catch (IOException ex) {
					Debug.errorf("failed to read mca file header from %s", getRegionDirectories().getEntities());
				}
			}
		}

		PoiMCAFile poiMCAFile = null;
		if (getRegionDirectories().getPoi() != null && getRegionDirectories().getPoi().exists() && getRegionDirectories().getPoi().length() > 0) {
			byte[] poiData = loadPoi();
			poiMCAFile = new PoiMCAFile(getRegionDirectories().getPoi());
			if (poiData != null) {
				// load PoiMCAFile
				ByteArrayPointer ptr = new ByteArrayPointer(poiData);
				try {
					poiMCAFile.load(ptr);
				} catch (IOException ex) {
					Debug.errorf("failed to read mca file header from %s", getRegionDirectories().getPoi());
				}
			}
		}

		if (regionMCAFile == null && poiMCAFile == null && entitiesMCAFile == null) {
			dataCallback.accept(null, world);
			Debug.dumpf("no data to load and parse for region %s", getRegionDirectories().getLocation());
			setLoading(tile, false);
			return;
		}

		int[] data = new int[1024];
		for (int i = 0; i < 1024; i++) {
			ChunkData chunkData = new ChunkData(
					regionMCAFile == null ? null : regionMCAFile.getChunk(i),
					poiMCAFile == null ? null : poiMCAFile.getChunk(i),
					entitiesMCAFile == null ? null : entitiesMCAFile.getChunk(i));
			try {
				data[i] = chunkData.parseData(parser);
			} catch (Exception ex) {
				Debug.dumpException("failed to parse chunk data at index " + i, ex);
			}
		}

		dataCallback.accept(data, world);
		setLoading(tile, false);

		Debug.dumpf("took %s to load and parse data for region %s", t, getRegionDirectories().getLocation());
	}

	@Override
	public void cancel() {
		setLoading(tile, false);
	}
}
