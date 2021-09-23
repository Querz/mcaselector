package net.querz.mcaselector.io.mca;

import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.range.Range;
import net.querz.nbt.tag.CompoundTag;
import java.io.File;
import java.util.List;
import java.util.Set;

public class RegionMCAFile extends MCAFile<RegionChunk> {

	public RegionMCAFile(File file) {
		super(file, RegionChunk::new);
		super.chunks = new RegionChunk[1024];
	}

	private RegionMCAFile(Point2i location) {
		super(location);
	}

	static RegionChunk newEmptyChunk(Point2i absoluteLocation, int dataVersion) {
		RegionChunk chunk = new RegionChunk(absoluteLocation);
		CompoundTag root = new CompoundTag();
		CompoundTag level = new CompoundTag();
		level.putInt("xPos", absoluteLocation.getX());
		level.putInt("zPos", absoluteLocation.getZ());
		level.putString("Status", "full");
		root.put("Level", level);
		root.putInt("DataVersion", dataVersion);
		chunk.data = root;
		chunk.compressionType = CompressionType.ZLIB;
		return chunk;
	}

	@Override
	public void mergeChunksInto(MCAFile<RegionChunk> destination, Point2i offset, boolean overwrite, Set<Point2i> sourceChunks, Set<Point2i> selection, List<Range> ranges) {
		mergeChunksInto(destination, offset, overwrite, sourceChunks, selection, ranges, RegionMCAFile::newEmptyChunk);
	}

	public RegionMCAFile minimizeForRendering() {
		RegionMCAFile min = new RegionMCAFile(getLocation());
		min.setFile(getFile());
		min.chunks = new RegionChunk[1024];

		for (int index = 0; index < 1024; index++) {
			RegionChunk chunk = getChunk(index);
			if (chunk == null || chunk.data == null) {
				continue;
			}

			try {
				CompoundTag minData = new CompoundTag();
				minData.put("DataVersion", chunk.data.get("DataVersion").clone());
				CompoundTag level = new CompoundTag();
				minData.put("Level", level);
				level.put("Biomes", chunk.data.getCompoundTag("Level").get("Biomes").clone());
				level.put("Sections", chunk.data.getCompoundTag("Level").get("Sections").clone());
				level.put("Status", chunk.data.getCompoundTag("Level").get("Status").clone());

				RegionChunk minChunk = new RegionChunk(chunk.absoluteLocation.clone());
				minChunk.data = minData;

				min.chunks[index] = minChunk;
			} catch (Exception ex) {
				min.chunks[index] = chunk;
			}
		}
		return min;
	}
}
