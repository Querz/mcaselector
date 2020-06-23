package net.querz.mcaselector.filter;

import net.querz.nbt.tag.CompoundTag;

public class FilterData {

	private final int lastUpdated;
	private final CompoundTag chunk;

	public FilterData(int lastUpdated, CompoundTag chunk) {
		this.lastUpdated = lastUpdated;
		this.chunk = chunk;
	}

	public int getLastUpdated() {
		return lastUpdated;
	}

	public CompoundTag getChunk() {
		return chunk;
	}
}
