package net.querz.mcaselector.io.mca;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.point.Point3i;
import net.querz.mcaselector.range.Range;
import net.querz.mcaselector.selection.ChunkSet;
import net.querz.mcaselector.version.ChunkMerger;
import net.querz.mcaselector.version.ChunkRenderer;
import net.querz.mcaselector.version.VersionController;
import net.querz.nbt.CompoundTag;
import java.io.File;
import java.util.List;

public class RegionMCAFile extends MCAFile<RegionChunk> {

	public RegionMCAFile(File file) {
		super(file, RegionChunk::new);
		super.chunks = new RegionChunk[1024];
	}

	private RegionMCAFile(Point2i location) {
		super(location);
	}

	static RegionChunk newEmptyChunk(Point2i absoluteLocation, int dataVersion) {
		ChunkMerger chunkMerger = VersionController.getChunkMerger(dataVersion);
		CompoundTag root = chunkMerger.newEmptyChunk(absoluteLocation, dataVersion);
		RegionChunk chunk = new RegionChunk(absoluteLocation);
		chunk.data = root;
		chunk.compressionType = CompressionType.ZLIB;
		return chunk;
	}

	@Override
	public void mergeChunksInto(MCAFile<RegionChunk> destination, Point3i offset, boolean overwrite, ChunkSet sourceChunks, ChunkSet targetChunks, List<Range> ranges) {
		mergeChunksInto(destination, offset, overwrite, sourceChunks, targetChunks, ranges, RegionMCAFile::newEmptyChunk);
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
				ChunkRenderer chunkRenderer = VersionController.getChunkRenderer(chunk.getDataVersion());
				CompoundTag minData = chunkRenderer.minimizeChunk(chunk.data);

				RegionChunk minChunk = new RegionChunk(chunk.absoluteLocation.clone());
				minChunk.data = minData;

				min.chunks[index] = minChunk;
			} catch (Exception ex) {
				min.chunks[index] = chunk;
			}
		}
		return min;
	}

	public RegionMCAFile clone() {
		return clone(RegionMCAFile::new);
	}
}
