package net.querz.mcaselector.io.job;

import net.querz.mcaselector.io.RegionDirectories;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.io.mca.EntitiesMCAFile;
import net.querz.mcaselector.io.mca.PoiMCAFile;
import net.querz.mcaselector.io.mca.RegionMCAFile;
import net.querz.mcaselector.util.point.Point2i;
import net.querz.mcaselector.util.progress.Timer;
import net.querz.mcaselector.tile.Tile;
import net.querz.mcaselector.overlay.Overlay;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class ParseDataJob extends ProcessDataJob {

	private static final Logger LOGGER = LogManager.getLogger(ParseDataJob.class);

	private static final Set<Point2i> loading = ConcurrentHashMap.newKeySet();

	private final BiConsumer<int[], UUID> dataCallback;
	private final UUID world;
	private final RegionMCAFile region;
	private final PoiMCAFile poi;
	private final EntitiesMCAFile entities;
	private final Overlay parser;
	private final Tile tile;
	private final Supplier<Integer> prioritySupplier;

	public ParseDataJob(Tile tile, RegionDirectories dirs, UUID world, RegionMCAFile region, PoiMCAFile poi, EntitiesMCAFile entities, BiConsumer<int[], UUID> dataCallback, Overlay parser, Supplier<Integer> prioritySupplier) {
		super(dirs, PRIORITY_LOW);
		this.tile = tile;
		this.dataCallback = dataCallback;
		this.world = world;
		this.region = region;
		this.poi = poi;
		this.entities = entities;
		this.parser = parser;
		this.prioritySupplier = prioritySupplier;
		setLoading(tile, true);
	}

	public ParseDataJob(Tile tile, RegionDirectories dirs, UUID world, BiConsumer<int[], UUID> dataCallback, Overlay parser, Supplier<Integer> prioritySupplier) {
		super(dirs, PRIORITY_LOW);
		this.tile = tile;
		this.dataCallback = dataCallback;
		this.world = world;
		this.region = null;
		this.poi = null;
		this.entities = null;
		this.parser = parser;
		this.prioritySupplier = prioritySupplier;
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
	public boolean execute() {
		Timer t = new Timer();

		RegionMCAFile regionMCAFile = null;
		if (region != null) {
			regionMCAFile = region;
		} else if (getRegionDirectories().getRegion() != null && getRegionDirectories().getRegion().exists() && getRegionDirectories().getRegion().length() > 0) {
			regionMCAFile = new RegionMCAFile(getRegionDirectories().getRegion());
			// load RegionMCAFile
			try {
				regionMCAFile.load(false);
			} catch (IOException ex) {
				LOGGER.warn("failed to read mca file header from {}", getRegionDirectories().getRegion());
			}
		}

		EntitiesMCAFile entitiesMCAFile = null;
		if (entities != null) {
			entitiesMCAFile = entities;
		} else if (getRegionDirectories().getEntities() != null && getRegionDirectories().getEntities().exists() && getRegionDirectories().getEntities().length() > 0) {
			entitiesMCAFile = new EntitiesMCAFile(getRegionDirectories().getEntities());
			// load EntitiesMCAFile
			try {
				entitiesMCAFile.load(false);
			} catch (IOException ex) {
				LOGGER.warn("failed to read mca file header from {}", getRegionDirectories().getEntities());
			}
		}

		PoiMCAFile poiMCAFile = null;
		if (poi != null) {
			poiMCAFile = poi;
		} else if (getRegionDirectories().getPoi() != null && getRegionDirectories().getPoi().exists() && getRegionDirectories().getPoi().length() > 0) {
			poiMCAFile = new PoiMCAFile(getRegionDirectories().getPoi());
			// load PoiMCAFile
			try {
				poiMCAFile.load(false);
			} catch (IOException ex) {
				LOGGER.warn("failed to read mca file header from {}", getRegionDirectories().getPoi());
			}
		}

		if (regionMCAFile == null && poiMCAFile == null && entitiesMCAFile == null) {
			dataCallback.accept(null, world);
			LOGGER.debug("no data to load and parse for region {}", getRegionDirectories().getLocation());
			setLoading(tile, false);
			return true;
		}

		int[] data = new int[1024];
		for (int i = 0; i < 1024; i++) {
			ChunkData chunkData = new ChunkData(
					regionMCAFile == null ? null : regionMCAFile.getChunk(i),
					poiMCAFile == null ? null : poiMCAFile.getChunk(i),
					entitiesMCAFile == null ? null : entitiesMCAFile.getChunk(i),
					false);
			try {
				data[i] = chunkData.parseData(parser);
			} catch (Exception ex) {
				LOGGER.warn("failed to parse chunk data at index {}", i, ex);
			}
		}

		dataCallback.accept(data, world);
		setLoading(tile, false);

		LOGGER.debug("took {} to load and parse data for region {}", t, getRegionDirectories().getLocation());
		return true;
	}

	@Override
	public void cancel() {
		setLoading(tile, false);
	}

	@Override
	public int getPriority() {
		if (prioritySupplier == null) {
			return super.getPriority();
		}
		return super.getBasePriority() + prioritySupplier.get();
	}
}
