package net.querz.mcaselector.version.anvil117;

import net.querz.mcaselector.validation.ValidationHelper;
import net.querz.mcaselector.version.anvil113.Anvil113ChunkFilter;
import net.querz.nbt.tag.CompoundTag;
import java.util.Arrays;

public class Anvil117ChunkFilter extends Anvil113ChunkFilter {

	@Override
	public void forceBiome(CompoundTag data, int id) {
		if (data.containsKey("Level")) {
			int[] biomes = ValidationHelper.withDefault(() -> data.getCompoundTag("Level").getIntArray("Biomes"), null);
			if (biomes != null && (biomes.length == 1024 || biomes.length == 1536)) {
				biomes = new int[biomes.length];
			} else {
				biomes = new int[1536];
			}
			Arrays.fill(biomes, id);
			data.getCompoundTag("Level").putIntArray("Biomes", biomes);
		}
	}
}
