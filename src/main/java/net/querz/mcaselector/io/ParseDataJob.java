package net.querz.mcaselector.io;

import net.querz.mcaselector.Config;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.io.mca.EntitiesMCAFile;
import net.querz.mcaselector.io.mca.PoiMCAFile;
import net.querz.mcaselector.io.mca.RegionMCAFile;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.tiles.overlay.OverlayDataParser;
import net.querz.mcaselector.tiles.overlay.OverlayType;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.zip.GZIPOutputStream;

public class ParseDataJob extends LoadDataJob {

	private final RegionMCAFile regionMCAFile;
	private final BiConsumer<long[], UUID> dataCallback;
	private final Point2i region;
	private final UUID world;

	public ParseDataJob(RegionDirectories dirs, Point2i region, UUID world, RegionMCAFile regionMCAFile, BiConsumer<long[], UUID> dataCallback) {
		super(dirs);
		this.regionMCAFile = regionMCAFile;
		this.dataCallback = dataCallback;
		this.region = region;
		this.world = world;
	}

	@Override
	public void run() {
		execute();
	}

	@Override
	public void execute() {
		EntitiesMCAFile entitiesMCAFile = null;
		if (getRegionDirectories().getEntities() != null) {
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
		if (getRegionDirectories().getPoi() != null) {
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

		for (OverlayType parserType : OverlayType.values()) {
			OverlayDataParser parser = parserType.instance();
			File cacheFile = new File(Config.getCacheDirForWorldUUID(world), parser.name() + "/" + FileHelper.createDATFileName(region));
			if (!cacheFile.getParentFile().exists() && !cacheFile.getParentFile().mkdirs()) {
				Debug.errorf("failed to create cache directory for %s", cacheFile.getAbsolutePath());
				continue;
			}

			long[] data = new long[1024];
			for (int i = 0; i < 1024; i++) {
				ChunkData chunkData = new ChunkData(
						regionMCAFile.getChunk(i),
						poiMCAFile == null ? null : poiMCAFile.getChunk(i),
						entitiesMCAFile == null ? null : entitiesMCAFile.getChunk(i));
				try {
					data[i] = chunkData.parseData(parser);
				} catch (Exception ex) {
					Debug.dumpException("failed to parse chunk data at index " + i, ex);
				}
			}

			dataCallback.accept(data, world);
			try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(cacheFile)), 8192))) {
				for (long d : data) {
					dos.writeLong(d);
				}
			} catch (IOException ex) {
				Debug.dumpException("failed to write data cache file " + cacheFile, ex);
			}
		}
	}
}
