package net.querz.mcaselector.version.java_1_21;

import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.version.MCVersionImplementation;
import net.querz.mcaselector.version.java_1_20.ChunkFilter_23w12a;
import net.querz.mcaselector.version.mapping.generator.HeightmapConfig;

public class ChunkFilter_24w18a {

	@MCVersionImplementation(3940)
	public static class Heightmap extends ChunkFilter_23w12a.Heightmap {

		@Override
		protected void loadCfg() {
			cfg = FileHelper.loadFromResource("mapping/java_1_21/heightmaps_24w18a.json", HeightmapConfig::load);
		}
	}
}
