package net.querz.mcaselector.version.java_1_17;

import net.querz.mcaselector.version.MCVersionImplementation;
import net.querz.mcaselector.version.java_1_15.ChunkFilter_19w36a;
import net.querz.mcaselector.version.java_1_16.ChunkFilter_20w17a;

public class ChunkFilter_21w15a {

	// reverting to old implementations, because Mojang reverted the new world height introduced in 21w06a again in 21w15a
	// when the caves and cliffs update was split into 1.17 and 1.18

	@MCVersionImplementation(2709)
	public static class Biomes extends ChunkFilter_19w36a.Biomes {}

	@MCVersionImplementation(2709)
	public static class Merge extends ChunkFilter_20w45a.Merge {}

	@MCVersionImplementation(2709)
	public static class Relocate extends ChunkFilter_20w45a.Relocate {}

	@MCVersionImplementation(2709)
	public static class Heightmap extends ChunkFilter_20w45a.Heightmap {}

	@MCVersionImplementation(2709)
	public static class Blocks extends ChunkFilter_20w17a.Blocks {}
}
