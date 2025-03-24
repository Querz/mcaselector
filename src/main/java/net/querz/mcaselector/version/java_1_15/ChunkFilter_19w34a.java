package net.querz.mcaselector.version.java_1_15;

import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.version.MCVersionImplementation;
import net.querz.mcaselector.version.java_1_14.ChunkFilter_18w45a;
import net.querz.mcaselector.version.mapping.generator.HeightmapConfig;

public class ChunkFilter_19w34a {

	@MCVersionImplementation(2200)
	public static class Heightmap extends ChunkFilter_18w45a.Heightmap {

		@Override
		protected void loadCfg() {
			cfg = FileHelper.loadFromResource("mapping/java_1_15/heightmaps_19w34a.json", HeightmapConfig::load);
		}
	}
}
