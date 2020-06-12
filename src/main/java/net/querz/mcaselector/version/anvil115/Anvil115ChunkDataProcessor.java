package net.querz.mcaselector.version.anvil115;

import net.querz.mcaselector.range.Range;
import net.querz.mcaselector.version.anvil113.Anvil113ChunkDataProcessor;
import net.querz.nbt.tag.CompoundTag;
import static net.querz.mcaselector.validation.ValidationHelper.*;
import java.util.Arrays;
import java.util.List;

public class Anvil115ChunkDataProcessor extends Anvil113ChunkDataProcessor {

	@Override
	public void mergeChunks(CompoundTag source, CompoundTag destination, List<Range> ranges) {
		super.mergeChunks(source, destination, ranges);

		int[] sourceBiomes = withDefault(() -> source.getCompoundTag("Level").getIntArray("Biomes"), null);
		int[] destinationBiomes = withDefault(() -> destination.getCompoundTag("Level").getIntArray("Biomes"), null);
		if (destinationBiomes == null) {
			// if there is no destination, we will let minecraft set the biome
			destinationBiomes = new int[1024];
			Arrays.fill(destinationBiomes, -1);
		}
		final int[] finalDestinationBiomes = destinationBiomes;
		if (sourceBiomes == null) {
			// if there is no source biome, we set the biome to -1
			// merge biomes
			for (Range range : ranges) {
				range.forEach(i -> setSectionBiomes(-1, finalDestinationBiomes, i), 0, 16);
			}
		} else {
			for (Range range : ranges) {
				range.forEach(i -> copySectionBiomes(sourceBiomes, finalDestinationBiomes, i), 0, 16);
			}
		}
	}

	private void copySectionBiomes(int[] sourceBiomes, int[] destinationBiomes, int sectionY) {
		for (int x = 0; x < 4; x++) {
			for (int y = 0; y < 4; y++) {
				for (int z = 0; z < 4; z++) {
					int biomeY = sectionY * 4 + y;
					setBiomeAt(destinationBiomes, x, biomeY, z, getBiomeAt(sourceBiomes, x, biomeY, z));
				}
			}
		}
	}

	private void setSectionBiomes(int biome, int[] destinationBiomes, int sectionY) {
		for (int x = 0; x < 4; x++) {
			for (int y = 0; y < 4; y++) {
				for (int z = 0; z < 4; z++) {
					int biomeY = sectionY * 4 + y;
					setBiomeAt(destinationBiomes, x, biomeY, z, biome);
				}
			}
		}
	}

	private int getBiomeAt(int[] biomes, int biomeX, int biomeY, int biomeZ) {
		if (biomes == null || biomes.length != 1024) {
			return -1;
		}
		return biomes[getBiomeIndex(biomeX, biomeY, biomeZ)];
	}

	private void setBiomeAt(int[] biomes, int biomeX, int biomeY, int biomeZ, int biomeID) {
		if (biomes == null || biomes.length != 1024) {
			biomes = new int[1024];
			Arrays.fill(biomes, -1);
		}
		biomes[getBiomeIndex(biomeX, biomeY, biomeZ)] = biomeID;
	}

	private int getBiomeIndex(int biomeX, int biomeY, int biomeZ) {
		return biomeY * 64 + biomeZ * 4 + biomeX;
	}
}
