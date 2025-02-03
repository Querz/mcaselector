package net.querz.mcaselector.version;

public interface ColorMapping<BLOCK, BIOME> {

	// default tints from plains biome
	int DEFAULT_GRASS_TINT = 0x91bd59;
	int DEFAULT_FOLIAGE_TINT = 0x77ab2f;
	int DEFAULT_WATER_TINT = 0x3f76e4;

	// returns a color based on the block data given as the parameter
	int getRGB(BLOCK o, BIOME biome);

	boolean isFoliage(BLOCK o);

	boolean isTransparent(BLOCK o);

	boolean isWater(BLOCK o);

	boolean isWaterlogged(BLOCK o);
}
