package net.querz.mcaselector.version.anvil115;

import net.querz.mcaselector.version.anvil113.Anvil113ChunkFilter;
import net.querz.nbt.CompoundTag;
import java.util.Arrays;

public class Anvil115ChunkFilter extends Anvil113ChunkFilter {

	@Override
	public void forceBiome(CompoundTag data, int id) {
		if (data.containsKey("Level")) {
			int[] biomes = new int[1024];
			Arrays.fill(biomes, id);
			data.getCompoundTag("Level").putIntArray("Biomes", biomes);
		}
	}
}
