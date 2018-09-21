package net.querz.mcaselector.version;

import net.querz.nbt.CompoundTag;

public interface ChunkFilter {

	boolean matchBlockNames(CompoundTag data, String... names);
}
