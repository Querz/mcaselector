package net.querz.mcaselector.version.anvil115;

import net.querz.mcaselector.range.Range;
import net.querz.mcaselector.version.anvil113.Anvil113ChunkDataProcessor;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.ListTag;
import java.util.Arrays;
import java.util.List;
import static net.querz.mcaselector.validation.ValidationHelper.*;

public class Anvil115ChunkDataProcessor extends Anvil113ChunkDataProcessor {

	@Override
	public void mergeChunks(CompoundTag source, CompoundTag destination, List<Range> ranges) {
		super.mergeChunks(source, destination, ranges);

		mergeBiomes(source, destination, ranges);
	}

	protected void mergeBiomes(CompoundTag source, CompoundTag destination, List<Range> ranges) {
		int[] sourceBiomes = withDefault(() -> source.getCompoundTag("Level").getIntArray("Biomes"), null);
		int[] destinationBiomes = withDefault(() -> destination.getCompoundTag("Level").getIntArray("Biomes"), null);

		if (destinationBiomes == null) {
			// if there is no destination, we will let minecraft set the biome
			destinationBiomes = new int[1024];
			Arrays.fill(destinationBiomes, -1);
		}

		if (sourceBiomes == null) {
			// if there is no source biome, we set the biome to -1
			// merge biomes
			for (Range range : ranges) {
				int m = Math.min(range.getTo(), 15);
				for (int i = Math.max(range.getFrom(), 0); i <= m; i++) {
					setSectionBiomes(-1, destinationBiomes, i);
				}
			}
		} else {
			for (Range range : ranges) {
				int m = Math.min(range.getTo(), 15);
				for (int i = Math.max(range.getFrom(), 0); i <= m; i++) {
					copySectionBiomes(sourceBiomes, destinationBiomes, i);
				}
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
		for (int y = 0; y < 4; y++) {
			int biomeY = sectionY * 4 + y;
			for (int x = 0; x < 4; x++) {
				for (int z = 0; z < 4; z++) {
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

	@Override
	protected void fixEntityUUID(CompoundTag entity) {
		if (entity.containsKey("UUID")) {
			int[] uuid = entity.getIntArray("UUID");
			if (uuid.length == 4) {
				for (int i = 0; i < 4; i++) {
					uuid[i] = random.nextInt();
				}
			}
		}
		if (entity.containsKey("Passengers")) {
			ListTag<CompoundTag> passengers = withDefault(() -> entity.getListTag("Passengers").asCompoundTagList(), null);
			if (passengers != null) {
				passengers.forEach(this::fixEntityUUID);
			}
		}
	}
}
