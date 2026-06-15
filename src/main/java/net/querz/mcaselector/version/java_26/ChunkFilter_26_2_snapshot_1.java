package net.querz.mcaselector.version.java_26;

import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.version.MCVersionImplementation;
import net.querz.mcaselector.version.mapping.generator.HeightmapConfig;

public class ChunkFilter_26_2_snapshot_1 {

	@MCVersionImplementation(4883)
	public static class Heightmap extends ChunkFilter_26_1_snapshot_1.Heightmap {

		@Override
		protected void loadCfg() {
			cfg = FileHelper.loadFromResource("mapping/java_26/heightmaps_26.2-snapshot-1.json", HeightmapConfig::load);
		}
	}
}
