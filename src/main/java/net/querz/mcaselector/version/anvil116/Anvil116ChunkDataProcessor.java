package net.querz.mcaselector.version.anvil116;

import net.querz.mcaselector.version.anvil115.Anvil115ChunkDataProcessor;
import net.querz.nbt.tag.CompoundTag;

import static net.querz.mcaselector.validation.ValidationHelper.withDefault;

public class Anvil116ChunkDataProcessor extends Anvil115ChunkDataProcessor {

	@Override
	protected int getPaletteIndex(int index, long[] blockStates, int bits, int clean) {
		int indicesPerLong = (int) (64D / bits);
		int blockStatesIndex = index / indicesPerLong;
		int startBit = (index % indicesPerLong) * bits;
		return (int) (blockStates[blockStatesIndex] >> startBit) & clean;
	}

	@Override
	protected boolean isIgnoredInNether(int biome, CompoundTag blockData, int height) {
		// all nether biomes: nether/nether_wastes, soul_sand_valley, crimson_forest, warped_forest, basalt_deltas
		if (biome == 8 || biome == 170 || biome == 171 || biome == 172 || biome == 173) {
			switch (withDefault(() -> blockData.getString("Name"), "")) {
				case "minecraft:bedrock":
				case "minecraft:flowing_lava":
				case "minecraft:lava":
				case "minecraft:netherrack":
				case "minecraft:nether_quartz_ore":
				case "minecraft:basalt":
				case "minecraft:soul_sand":
				case "minecraft:nether_gold_ore":
				case "minecraft:netherite_block":
				case "minecraft:ancient_debris":
				case "minecraft:crimson_nylium":
				case "minecraft:warped_nylium":
					return height > 75;
			}
		}
		return false;
	}
}