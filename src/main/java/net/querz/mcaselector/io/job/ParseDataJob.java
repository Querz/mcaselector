package net.querz.mcaselector.io.job;

import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.io.ByteArrayPointer;
import net.querz.mcaselector.io.RegionDirectories;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.io.mca.EntitiesMCAFile;
import net.querz.mcaselector.io.mca.PoiMCAFile;
import net.querz.mcaselector.io.mca.RegionMCAFile;
import net.querz.mcaselector.progress.Timer;
import net.querz.mcaselector.tiles.overlay.OverlayDataParser;
import net.querz.mcaselector.tiles.overlay.OverlayType;
import java.io.IOException;
import java.util.UUID;
import java.util.function.BiConsumer;

public class ParseDataJob extends LoadDataJob {

	private final BiConsumer<int[], UUID> dataCallback;
	private final UUID world;

	public ParseDataJob(RegionDirectories dirs, UUID world, BiConsumer<int[], UUID> dataCallback) {
		super(dirs);
		this.dataCallback = dataCallback;
		this.world = world;
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
			return;
		}

		for (OverlayType parserType : OverlayType.values()) {
			OverlayDataParser parser = parserType.instance();

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
		}

		Debug.dumpf("took %s to load and parse data for region %s", t, getRegionDirectories().getLocation());
	}
}
