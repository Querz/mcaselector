package net.querz.mcaselector.filter;

import net.querz.mcaselector.io.mca.Chunk;

public class FilterData {

	private final int lastUpdated;
	private final Chunk region;
	private final Chunk entities;
	private final Chunk poi;

	public FilterData(int lastUpdated, Chunk region, Chunk entities, Chunk poi) {
		this.lastUpdated = lastUpdated;
		this.region = region;
		this.entities = entities;
		this.poi = poi;
	}

	public int getLastUpdated() {
		return lastUpdated;
	}

	public Chunk getRegion() {
		return region;
	}

	public Chunk getEntities() {
		return entities;
	}

	public Chunk getPOI() {
		return poi;
	}
}
