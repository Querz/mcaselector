package net.querz.mcaselector.version.java_1_13;

import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.version.ColorMapping;
import net.querz.mcaselector.version.Helper;
import net.querz.mcaselector.version.MCVersionImplementation;
import net.querz.mcaselector.version.mapping.generator.ColorConfig;
import net.querz.nbt.CompoundTag;

@MCVersionImplementation(1451)
public class ColorMapping_17w47a implements ColorMapping<CompoundTag, Integer> {

	private static final ColorConfig cfg = FileHelper.loadFromResource(
			"mapping/java_1_13/colors_17w47a.json",
			ColorConfig::loadLegacy);

	@Override
	public int getRGB(CompoundTag o, Integer biome) {
		String name = Helper.stringFromCompound(o, "Name");
		CompoundTag properties = Helper.tagFromCompound(o, "Properties");
		return cfg.getLegacyColor(name, biome, properties) | 0xFF000000;
	}

	@Override
	public boolean isFoliage(CompoundTag o) {
		return ColorConfig.colorProperties.foliage().contains(o.getStringOrDefault("Name", ""));
	}

	@Override
	public boolean isTransparent(CompoundTag o) {
		return ColorConfig.colorProperties.transparent().contains(o.getStringOrDefault("Name", ""));
	}

	@Override
	public boolean isWater(CompoundTag o) {
		return ColorConfig.colorProperties.water().contains(o.getStringOrDefault("Name", ""));
	}

	@Override
	public boolean isWaterlogged(CompoundTag o) {
		return cfg.states.isWaterlogged(o.getCompoundOrDefault("Properties", null));
	}
}
