package net.querz.mcaselector.version.java_1_14;

import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.version.MCVersionImplementation;
import net.querz.mcaselector.version.java_1_13.ChunkFilter_1_13_PRE3;
import net.querz.mcaselector.version.mapping.generator.HeightmapConfig;

public class ChunkFilter_18w43a {

	@MCVersionImplementation(1901)
	public static class Heightmap extends ChunkFilter_1_13_PRE3.Heightmap {

		@Override
		protected void loadCfg() {
			cfg = FileHelper.loadFromResource("mapping/java_1_14/heightmaps_18w43a.json", HeightmapConfig::load);
		}
	}
}
