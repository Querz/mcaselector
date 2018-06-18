package net.querz.mcaselector.filter;

import net.querz.nbt.CompoundTag;

public class FilterData {

	private int lastUpdated;
	private CompoundTag chunk;

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
