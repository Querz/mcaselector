package net.querz.mcaselector.io.mca;

import net.querz.mcaselector.changer.Field;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.filter.Filter;
import net.querz.mcaselector.io.ByteArrayPointer;
import net.querz.mcaselector.io.RegionDirectories;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.range.Range;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// holds data for chunks, poi and entities
public class Region {

	private MCAFile region;
	private MCAFile poi;
	private MCAFile entities;

	public static Region loadRegion(RegionDirectories dirs, byte[] regionData, byte[] poiData, byte[] entitiesData) throws IOException {
		Region r = new Region();
		if (dirs.getRegion() != null && regionData != null) {
			r.loadRegion(dirs.getRegion(), new ByteArrayPointer(regionData));
		}
		if (dirs.getPoi() != null && poiData != null) {
			r.loadPOI(dirs.getPoi(), new ByteArrayPointer(poiData));
		}
		if (dirs.getEntities() != null && entitiesData != null) {
			r.loadEntities(dirs.getEntities(), new ByteArrayPointer(entitiesData));
		}
		return r;
	}

	public void loadRegion(File src) throws IOException {
		region = new MCAFile(src);
		region.load();
	}

	public void loadRegion(File src, ByteArrayPointer ptr) throws IOException {
		region = new MCAFile(src);
		region.load(ptr);
	}

	public void loadPOI(File src) throws IOException {
		poi = new MCAFile(src);
		poi.load();
	}

	public void loadPOI(File src, ByteArrayPointer ptr) throws IOException {
		poi = new MCAFile(src);
		poi.load(ptr);
	}

	public void loadEntities(File src) throws IOException {
		entities = new MCAFile(src);
		entities.load();
	}

	public void loadEntities(File src, ByteArrayPointer ptr) throws IOException {
		entities = new MCAFile(src);
		entities.load(ptr);
	}

	public MCAFile getRegion() {
		return region;
	}

	public MCAFile getPoi() {
		return poi;
	}

	public MCAFile getEntities() {
		return entities;
	}

	public void setRegion(MCAFile region) {
		this.region = region;
	}

	public void setPoi(MCAFile poi) {
		this.poi = poi;
	}

	public void setEntities(MCAFile entities) {
		this.entities = entities;
	}

	public void save() throws IOException {
		if (region != null) {
			region.save();
		}
		if (poi != null) {
			poi.save();
		}
		if (entities != null) {
			entities.save();
		}
	}

	public void saveWithTempFiles() throws IOException {
		if (region != null) {
			region.saveWithTempFile();
		}
		if (poi != null) {
			poi.saveWithTempFile();
		}
		if (entities != null) {
			entities.saveWithTempFile();
		}
	}

	public void saveWithTempFiles(RegionDirectories dest) throws IOException {
		if (region != null) {
			region.saveWithTempFile(dest.getRegion());
		}
		if (poi != null) {
			poi.saveWithTempFile(dest.getPoi());
		}
		if (entities != null) {
			entities.saveWithTempFile(dest.getEntities());
		}
	}

	public void deleteChunks(Set<Point2i> selection) {
		if (region != null) {
			region.deleteChunks(selection);
		}
		if (poi != null) {
			poi.deleteChunks(selection);
		}
		if (entities != null) {
			entities.deleteChunks(selection);
		}
	}

	public void deleteChunks(Filter<?> filter, Set<Point2i> selection) {
		for (int i = 0; i < 1024; i++) {
			Chunk region = this.region.getChunk(i);
			Chunk entities = this.entities == null ? null : this.entities.getChunk(i);
			Chunk poi = this.poi == null ? null : this.poi.getChunk(i);

			if (region == null || region.isEmpty() || selection != null && !selection.contains(region.getAbsoluteLocation())) {
				continue;
			}

			ChunkData filterData = new ChunkData(this.region.getTimestamp(i), region, entities, poi);

			if (filter.matches(filterData)) {
				deleteChunkIndex(i);
			}
		}
	}

	public void keepChunks(Filter<?> filter, Set<Point2i> selection) {
		for (int i = 0; i < 1024; i++) {
			Chunk region = this.region.getChunk(i);
			Chunk entities = this.entities == null ? null : this.entities.getChunk(i);
			Chunk poi = this.poi == null ? null : this.poi.getChunk(i);

			if (region == null || region.isEmpty()) {
				continue;
			}

			ChunkData filterData = new ChunkData(this.region.getTimestamp(i), region, entities, poi);

			// keep chunk if filter AND selection applies
			// ignore selection if it's null
			if (!filter.matches(filterData) || selection != null && !selection.contains(region.getAbsoluteLocation())) {
				deleteChunkIndex(i);
			}
		}
	}

	private void deleteChunkIndex(int index) {
		if (this.region != null) {
			this.region.setChunk(index, null);
			this.region.setTimestamp(index, 0);
		}
		if (this.entities != null) {
			this.entities.setChunk(index, null);
			this.entities.setTimestamp(index, 0);
		}
		if (this.poi != null) {
			this.poi.setChunk(index, null);
			this.poi.setTimestamp(index, 0);
		}
	}

	public Set<Point2i> getFilteredChunks(Filter<?> filter) {
		Set<Point2i> chunks = new HashSet<>();

		for (int i = 0; i < 1024; i++) {
			Chunk region = this.region.getChunk(i);
			Chunk entities = this.entities == null ? null : this.entities.getChunk(i);
			Chunk poi = this.poi == null ? null : this.poi.getChunk(i);

			if (region == null || region.isEmpty()) {
				continue;
			}

			ChunkData filterData = new ChunkData(this.region.getTimestamp(i), region, entities, poi);

			Point2i location = region.getAbsoluteLocation();
			try {
				if (filter.matches(filterData)) {
					if (location == null) {
						continue;
					}
					chunks.add(location);
				}
			} catch (Exception ex) {
				Debug.dumpException(String.format("failed to select chunk %s", location), ex);
			}
		}
		return chunks;
	}

	public void applyFieldChanges(List<Field<?>> fields, boolean force, Set<Point2i> selection) {
		if (region != null) {
			List<Field<?>> regionFields = new ArrayList<>(fields.size());
			for (Field<?> field : fields) {
				if (field.getType().getRegionType() == RegionType.REGION) {
					regionFields.add(field);
				}
			}
			region.applyFieldChanges(regionFields, force, selection);
		}
		if (entities != null) {
			List<Field<?>> entitiesFields = new ArrayList<>(1);
			for (Field<?> field : fields) {
				if (field.getType().getRegionType() == RegionType.ENTITIES) {
					entitiesFields.add(field);
				}
			}
			entities.applyFieldChanges(entitiesFields, force, selection);
		}
		if (poi != null) {
			List<Field<?>> poiFields = new ArrayList<>(1);
			for (Field<?> field : fields) {
				if (field.getType().getRegionType() == RegionType.POI) {
					poiFields.add(field);
				}
			}
			poi.applyFieldChanges(poiFields, force, selection);
		}
	}

	public void mergeInto(Region region, Point2i offset, boolean overwrite, Set<Point2i> sourceChunks, Set<Point2i> selection, List<Range> ranges) {
		if (this.region != null) {
			this.region.mergeChunksInto(region.region, offset, overwrite, sourceChunks, selection, ranges);
		}
		if (this.poi != null) {
			this.poi.mergeChunksInto(region.poi, offset, overwrite, sourceChunks, selection, ranges);
		}
		if (this.entities != null) {
			this.entities.mergeChunksInto(region.entities, offset, overwrite, sourceChunks, selection, ranges);
		}
	}
}
