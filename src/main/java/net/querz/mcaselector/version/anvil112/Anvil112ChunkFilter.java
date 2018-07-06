package net.querz.mcaselector.version.anvil112;

import net.querz.mcaselector.version.ChunkFilter;
import net.querz.nbt.CompoundTag;

public class Anvil112ChunkFilter implements ChunkFilter {

	@Override
	public boolean matchBlockNames(CompoundTag data, String... names) {
		//implementing this sucks. we'll need a complete 1.13 blockstate names to 1.12 block id mapping.
		//that's approximately 400-500 blocks if we take relevant variations like colors into account...
		//argh.
		return false;
	}
}
