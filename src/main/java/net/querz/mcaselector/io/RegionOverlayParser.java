package net.querz.mcaselector.io;

import net.querz.mcaselector.Config;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.io.mca.RegionChunk;
import net.querz.mcaselector.io.mca.RegionMCAFile;
import net.querz.mcaselector.tiles.OverlayDataParser;
import net.querz.mcaselector.tiles.OverlayDataParserType;
import net.querz.mcaselector.tiles.Tile;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

public class RegionOverlayParser {

	private static final OverlayDataParser[] parsers = new OverlayDataParser[OverlayDataParserType.values().length];

	static {
		OverlayDataParserType[] values = OverlayDataParserType.values();
		for (int i = 0; i < values.length; i++) {
			parsers[i] = values[i].create();
		}
	}

	public static void parse(Tile tile, UUID world, RegionMCAFile region) throws IOException {

		for (OverlayDataParser parser : parsers) {

			// check if we already have this data cached

			File cacheFile = new File(Config.getCacheDirForWorldUUID(world), parser.name() + "/" + FileHelper.createDATFileName(region.getLocation()));
			if (cacheFile.exists()) {
				continue;
			}

			long[] data = new long[1024];

			for (int i = 0; i < 1024; i++) {
				RegionChunk chunk = region.getChunk(i);
				if (chunk == null) {
					continue;
				}
				data[i] = parser.parseValue(new ChunkData(chunk, null, null));
			}

			try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(cacheFile)))) {

			}
		}
	}
}
