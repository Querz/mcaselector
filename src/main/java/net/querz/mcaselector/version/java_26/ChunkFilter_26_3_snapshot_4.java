package net.querz.mcaselector.version.java_26;

import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.version.MCVersionImplementation;
import net.querz.mcaselector.version.mapping.generator.HeightmapConfig;

public class ChunkFilter_26_3_snapshot_4 {

	@MCVersionImplementation(5003)
	public static class Heightmap extends ChunkFilter_26_3_snapshot_1.Heightmap {

		@Override
		protected void loadCfg() {
			cfg = FileHelper.loadFromResource("mapping/java_26/heightmaps_26.3-snapshot-4.json", HeightmapConfig::load);
		}
	}
}
