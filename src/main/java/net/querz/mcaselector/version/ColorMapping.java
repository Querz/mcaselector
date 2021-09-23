package net.querz.mcaselector.version;

public interface ColorMapping {

	// default tints from plains biome
	int DEFAULT_GRASS_TINT = 0x91bd59;
	int DEFAULT_FOLIAGE_TINT = 0x77ab2f;
	int DEFAULT_WATER_TINT = 0x3f76e4;

	// returns a color based on the block data given as the parameter
	int getRGB(Object o, int biome);

	int getRGB(Object o, String biome);

	boolean isFoliage(Object o);

	default int applyTint(int color, int tint) {
		int nr = (tint >> 16 & 0xFF) * (color >> 16 & 0xFF) >> 8;
		int ng = (tint >> 8 & 0xFF) * (color >> 8 & 0xFF) >> 8;
		int nb = (tint & 0xFF) * (color & 0xFF) >> 8;
		return color & 0xFF000000 | nr << 16 | ng << 8 | nb;
	}
}
