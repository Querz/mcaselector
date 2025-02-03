package net.querz.mcaselector.version.java_1_16;

import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.version.MCVersionImplementation;
import net.querz.mcaselector.version.java_1_15.ChunkFilter_19w34a;
import net.querz.mcaselector.version.mapping.generator.HeightmapConfig;

public class ChunkFilter_20w06a {

	@MCVersionImplementation(2504)
	public static class Heightmap extends ChunkFilter_19w34a.Heightmap {

		@Override
		protected void loadCfg() {
			cfg = FileHelper.loadFromResource("mapping/java_1_16/heightmaps_20w06a.json", HeightmapConfig::load);
		}
	}
}
