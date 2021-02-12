package net.querz.mcaselector.version;

import net.querz.nbt.tag.CompoundTag;
import java.util.Map;

public interface ChunkFilter {

	boolean matchBlockNames(CompoundTag data, String... names);

	boolean matchBiomeIDs(CompoundTag data, int... ids);

	void changeBiome(CompoundTag data, int id);

	void forceBiome(CompoundTag data, int id);

	void replaceBlocks(CompoundTag data, Map<String, String> replace);
}
