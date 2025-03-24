package net.querz.mcaselector.version.java_1_19;

import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.version.MCVersionImplementation;
import net.querz.mcaselector.version.java_1_18.ChunkFilter_21w43a;
import net.querz.mcaselector.version.mapping.generator.HeightmapConfig;

public class ChunkFilter_22w11a {

	@MCVersionImplementation(3080)
	public static class Heightmap extends ChunkFilter_21w43a.Heightmap {

		@Override
		protected void loadCfg() {
			cfg = FileHelper.loadFromResource("mapping/java_1_19/heightmaps_22w11a.json", HeightmapConfig::load);
		}
	}
}
