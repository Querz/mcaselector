package net.querz.mcaselector.io;

import net.querz.mcaselector.point.Point2i;
import java.io.File;

public class RegionDirectories {

	private Point2i location;
	private File region;
	private File poi;
	private File entities;

	private String locationAsFileName;

	public RegionDirectories() {}

	public RegionDirectories(Point2i location, File region, File poi, File entities) {
		this.location = location;
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

	public Point2i getLocation() {
		return location;
	}

	public String getLocationAsFileName() {
		if (locationAsFileName == null) {
			return locationAsFileName = FileHelper.createMCAFileName(location);
		}
		return locationAsFileName;
	}

	@Override
	public String toString() {
		return "<region=" + region + ", poi=" + poi + ", entities=" + entities + ">";
	}
}
