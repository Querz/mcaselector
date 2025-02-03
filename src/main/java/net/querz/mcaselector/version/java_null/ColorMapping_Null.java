package net.querz.mcaselector.version.java_null;

import net.querz.mcaselector.version.ColorMapping;
import net.querz.mcaselector.version.MCVersionImplementation;

@MCVersionImplementation(0)
public class ColorMapping_Null implements ColorMapping<Void, Void> {

	@Override
	public int getRGB(Void o, Void unused) {
		return 0;
	}

	@Override
	public boolean isFoliage(Void o) {
		return false;
	}

	@Override
	public boolean isTransparent(Void o) {
		return false;
	}

	@Override
	public boolean isWater(Void o) {
		return false;
	}

	@Override
	public boolean isWaterlogged(Void o) {
		return false;
	}
}
