package net.querz.mcaselector.version.java_1_21;

import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.version.ColorMapping;
import net.querz.mcaselector.version.Helper;
import net.querz.mcaselector.version.MCVersionImplementation;
import net.querz.mcaselector.version.mapping.color.BlockColor;
import net.querz.mcaselector.version.mapping.generator.ColorConfig;
import net.querz.nbt.CompoundTag;

@MCVersionImplementation(4536)
public class ColorMapping_25w32a implements ColorMapping<CompoundTag, String> {

	private static final ColorConfig cfg = FileHelper.loadFromResource(
			"mapping/java_1_21/colors_25w32a.json",
			ColorConfig::load);

	@Override
	public int getRGB(CompoundTag o, String biome) {
		String name = Helper.stringFromCompound(o, "Name");
		CompoundTag properties = Helper.tagFromCompound(o, "Properties");
		BlockColor color = cfg.getColor(name, biome, properties);
		return color.color | 0xFF000000;
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
