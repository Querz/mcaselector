package net.querz.mcaselector.version.java_26;

import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.version.ColorMapping;
import net.querz.mcaselector.version.Helper;
import net.querz.mcaselector.version.MCVersionImplementation;
import net.querz.mcaselector.version.mapping.color.BlockColor;
import net.querz.mcaselector.version.mapping.generator.ColorConfig;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.Tag;

@MCVersionImplementation(5009)
public class ColorMapping_26_3_snapshot_7 implements ColorMapping<Tag, String> {

	private static final ColorConfig cfg = FileHelper.loadFromResource(
			"mapping/java_26/colors_26.3-snapshot-1.json",
			ColorConfig::load);

	@Override
	public int getRGB(Tag o, String biome) {
		String name = Helper.getBlockID(o, null);
		CompoundTag properties = Helper.tagFromCompound(o, "properties");
		BlockColor color = cfg.getColor(name, biome, properties);
		return color.color | 0xFF000000;
	}

	@Override
	public boolean isFoliage(Tag o) {
		return ColorConfig.colorProperties.foliage().contains(Helper.getBlockID(o, ""));
	}

	@Override
	public boolean isTransparent(Tag o) {
		return ColorConfig.colorProperties.transparent().contains(Helper.getBlockID(o, ""));
	}

	@Override
	public boolean isWater(Tag o) {
		return ColorConfig.colorProperties.water().contains(Helper.getBlockID(o, ""));
	}

	@Override
	public boolean isWaterlogged(Tag o) {
		return cfg.states.isWaterlogged((CompoundTag) Helper.tagFromCompound(o, "properties", null));
	}
}
