package net.querz.mcaselector.version.anvil115;

import net.querz.mcaselector.io.registry.BiomeRegistry;
import net.querz.mcaselector.version.Helper;
import net.querz.mcaselector.version.anvil113.Anvil113ChunkFilter;
import net.querz.nbt.CompoundTag;
import java.util.Arrays;

public class Anvil115ChunkFilter extends Anvil113ChunkFilter {

	@Override
	public void forceBiome(CompoundTag data, BiomeRegistry.BiomeIdentifier biome) {
		CompoundTag level = Helper.levelFromRoot(data);
		if (level != null) {
			int[] biomes = new int[1024];
			Arrays.fill(biomes, (byte) biome.getID());
			level.putIntArray("Biomes", biomes);
		}
	}
}
