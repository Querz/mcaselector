package net.querz.mcaselector.io.mca;

import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.range.Range;
import net.querz.mcaselector.tiles.overlay.OverlayDataParser;
import net.querz.nbt.tag.CompoundTag;
import java.io.File;
import java.util.List;
import java.util.Set;

public class RegionMCAFile extends MCAFile<RegionChunk> {

	public RegionMCAFile(File file) {
		super(file, RegionChunk::new);
		super.chunks = new RegionChunk[1024];
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

	public long[] parseData(OverlayDataParser parser) {
		long[] data = new long[1024];
		for (int i = 0; i < 1024; i++) {
			RegionChunk chunk = getChunk(i);
			if (chunk != null) {
				try {
					data[i] = parser.parseValue(new ChunkData(chunk, null, null));
				} catch (Exception ex) {
					Point2i chunkLocation = getLocation().regionToChunk().add(i % 32, (i - (i % 32)) / 32);
					Debug.dumpException(String.format("failed to parse %s overlay value in chunk %s", parser.name(), chunkLocation), ex);
				}
			}
		}
		return data;
	}
}
