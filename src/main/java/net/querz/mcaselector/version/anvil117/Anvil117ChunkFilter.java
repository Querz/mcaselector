package net.querz.mcaselector.version.anvil117;

import net.querz.mcaselector.io.registry.BiomeRegistry;
import net.querz.mcaselector.version.NbtHelper;
import net.querz.mcaselector.version.anvil116.Anvil116ChunkFilter;
import net.querz.nbt.CompoundTag;
import java.util.Arrays;

public class Anvil117ChunkFilter extends Anvil116ChunkFilter {

	@Override
	public void forceBiome(CompoundTag data, BiomeRegistry.BiomeIdentifier biome) {
		CompoundTag level = NbtHelper.levelFromRoot(data);
		if (level != null) {
			int[] biomes = NbtHelper.intArrayFromCompound(level, "Biomes");
			if (biomes != null && (biomes.length == 1024 || biomes.length == 1536)) {
				biomes = new int[biomes.length];
			} else {
				biomes = new int[1024];
			}
			Arrays.fill(biomes, biome.getID());
			level.putIntArray("Biomes", biomes);
		}
	}
}
