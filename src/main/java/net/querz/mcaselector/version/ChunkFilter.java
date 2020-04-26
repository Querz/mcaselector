package net.querz.mcaselector.version;

import net.querz.nbt.tag.CompoundTag;

public interface ChunkFilter {

	boolean matchBlockNames(CompoundTag data, String... names);

	boolean matchBiomeIDs(CompoundTag data, int... ids);

	void changeBiome(CompoundTag data, int id);

	void forceBiome(CompoundTag data, int id);
}
