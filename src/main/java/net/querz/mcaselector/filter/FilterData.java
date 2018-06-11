package net.querz.mcaselector.filter;

import net.querz.nbt.CompoundTag;

public class FilterData {

	private CompoundTag chunkData;
	private int timestamp;

	public FilterData(int timestamp, CompoundTag chunkData) {
		this.timestamp = timestamp;
		this.chunkData = chunkData;
	}

	public CompoundTag getChunkData() {
		return chunkData;
	}

	public int getTimestamp() {
		return timestamp;
	}
}
