package net.querz.mcaselector.version.java_1_21;

import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.version.MCVersionImplementation;
import net.querz.mcaselector.version.mapping.generator.HeightmapConfig;

public class ChunkFilter_25w32a {

	@MCVersionImplementation(4536)
	public static class Heightmap extends ChunkFilter_24w18a.Heightmap {

		@Override
		protected void loadCfg() {
			cfg = FileHelper.loadFromResource("mapping/java_1_21/heightmaps_25w32a.json", HeightmapConfig::load);
		}
	}
}
