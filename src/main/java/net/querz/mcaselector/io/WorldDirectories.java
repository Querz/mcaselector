package net.querz.mcaselector.io;

import net.querz.mcaselector.point.Point2i;
import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class WorldDirectories implements Serializable, Cloneable {

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

	public RegionDirectories makeRegionDirectories(Point2i region) {
		RegionDirectories rd = new RegionDirectories();
		rd.setRegion(new File(this.region, FileHelper.createMCAFileName(region)));
		rd.setPoi(new File(this.poi, FileHelper.createMCAFileName(region)));
		rd.setEntities(new File(this.entities, FileHelper.createMCAFileName(region)));
		return rd;
	}

	public RegionDirectories[] listRegions(SelectionData selection) {
		Map<Point2i, RegionDirectories> regionDirectories = new HashMap<>();
		File[] r = this.region.listFiles((d, n) -> n.matches(FileHelper.MCA_FILE_PATTERN));
		if (r != null) {
			for (File f : r) {
				Point2i l = FileHelper.parseMCAFileName(f);
				if (selection == null || selection.isRegionSelected(l)) {
					regionDirectories.put(l, new RegionDirectories(l, f, null, null));
				}
			}
		}

		if (this.entities != null) {
			File[] p = this.entities.listFiles((d, n) -> n.matches(FileHelper.MCA_FILE_PATTERN));
			if (p != null) {
				for (File f : p) {
					Point2i l = FileHelper.parseMCAFileName(f);
					if (selection == null || selection.isRegionSelected(l)) {
						if (regionDirectories.containsKey(l)) {
							regionDirectories.get(l).setEntities(f);
						} else {
							regionDirectories.put(l, new RegionDirectories(l, null, f, null));
						}
					}
				}
			}
		}

		if (this.poi != null) {
			File[] e = this.poi.listFiles((d, n) -> n.matches(FileHelper.MCA_FILE_PATTERN));
			if (e != null) {
				for (File f : e) {
					Point2i l = FileHelper.parseMCAFileName(f);
					if (selection == null || selection.isRegionSelected(l)) {
						if (regionDirectories.containsKey(l)) {
							regionDirectories.get(l).setPoi(f);
						} else {
							regionDirectories.put(l, new RegionDirectories(l, null, null, f));
						}
					}
				}
			}
		}

		return regionDirectories.values().toArray(new RegionDirectories[0]);
	}

	@Override
	public WorldDirectories clone() {
		try {
			return (WorldDirectories) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public String toString() {
		return "<region=" + region + ", poi=" + poi + ", entities=" + entities + ">";
	}

	public boolean sharesDirectories(WorldDirectories other) {
		return region != null && region.equals(other.region) ||
				poi != null && poi.equals(other.poi) ||
				entities != null && entities.equals(other.entities);
	}
}