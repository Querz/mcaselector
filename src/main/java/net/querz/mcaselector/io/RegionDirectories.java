package net.querz.mcaselector.io;

import net.querz.mcaselector.point.Point2i;
import java.io.File;

public class RegionDirectories implements Cloneable {

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
		if (location == null) {
			location = FileHelper.parseMCAFileName(region);
		}
		this.region = region;
	}

	public void setPoi(File poi) {
		if (location == null) {
			location = FileHelper.parseMCAFileName(poi);
		}
		this.poi = poi;
	}

	public void setEntities(File entities) {
		if (location == null) {
			location = FileHelper.parseMCAFileName(entities);
		}
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

	public static RegionDirectories fromWorldDirectories(WorldDirectories wd, Point2i location) {
		String fileName = FileHelper.createMCAFileName(location);
		return new RegionDirectories(
				location,
				new File(wd.getRegion(), fileName),
				new File(wd.getPoi(), fileName),
				new File(wd.getEntities(), fileName)
		);
	}

	public boolean exists() {
		return region.exists() || poi.exists() || entities.exists();
	}

	@Override
	public String toString() {
		return "<region=" + region + ", poi=" + poi + ", entities=" + entities + ">";
	}

	@Override
	public RegionDirectories clone() throws CloneNotSupportedException {
		RegionDirectories clone = (RegionDirectories) super.clone();
		clone.location = location.clone();
		return clone;
	}
}
