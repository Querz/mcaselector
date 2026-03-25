package net.querz.mcaselector.version.java_26;

import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.version.MCVersionImplementation;
import net.querz.mcaselector.version.java_1_21.ChunkFilter_25w32a;
import net.querz.mcaselector.version.mapping.generator.HeightmapConfig;

public class ChunkFilter_26_1_snapshot_1 {

	@MCVersionImplementation(4764)
	public static class Heightmap extends ChunkFilter_25w32a.Heightmap {

		@Override
		protected void loadCfg() {
			cfg = FileHelper.loadFromResource("mapping/java_26/heightmaps_26.1-snapshot-1.json", HeightmapConfig::load);
		}
	}
}
