package net.querz.mcaselector.version.anvil114;

import net.querz.mcaselector.version.Helper;
import net.querz.mcaselector.version.anvil113.Anvil113ChunkFilter;
import net.querz.nbt.*;

public class Anvil114ChunkFilter extends Anvil113ChunkFilter {

	@Override
	public ByteTag getLightPopulated(CompoundTag data) {
		return Helper.tagFromLevelFromRoot(data, "isLightOn");
	}

	@Override
	public void setLightPopulated(CompoundTag data, byte lightPopulated) {
		CompoundTag level = Helper.levelFromRoot(data);
		if (level != null) {
			level.putLong("isLightOn", lightPopulated);
		}
	}
}
