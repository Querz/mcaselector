package net.querz.mcaselector.version.java_1_21;

import net.querz.mcaselector.version.MCVersionImplementation;
import net.querz.mcaselector.version.java_1_18.ChunkFilter_21w43a;

public class ChunkFilter_1_21_5_RC2 {

	// 1.21.5-rc2 fixes the blending bug, so we revert to the previous implementation
	@MCVersionImplementation(4324)
	public static class Blending extends ChunkFilter_21w43a.Blending {}
}
