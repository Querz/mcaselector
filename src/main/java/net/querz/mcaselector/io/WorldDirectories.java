package net.querz.mcaselector.io;

import java.io.File;

public class WorldDirectories {

	private File region;
	private File poi;
	private File entities;

	public WorldDirectories() {}

	public WorldDirectories(File region, File poi, File entities) {
		this.region = region;
		this.poi = poi;
		this.entities = entities;
	}

	public void setRegion(File region) {
		this.region = region;
	}

	public void setPoi(File poi) {
		this.poi = poi;
	}

	public void setEntities(File entities) {
		this.entities = entities;
	}

	public File getRegion() {
		return region;
	}

	public File getPoi() {
		return poi;
	}

	public File getEntities() {
		return entities;
	}

	@Override
	public String toString() {
		return "<region=" + region + ", poi=" + poi + ", entities=" + entities + ">";
	}
}