package net.querz.mcaselector.io.mca;

import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.point.Point3i;
import net.querz.mcaselector.range.Range;
import net.querz.mcaselector.selection.ChunkSet;
import net.querz.mcaselector.version.ChunkMerger;
import net.querz.mcaselector.version.ChunkRenderer;
import net.querz.mcaselector.version.HeightmapCalculator;
import net.querz.mcaselector.version.VersionController;
import net.querz.nbt.CompoundTag;
import java.io.File;
import java.util.List;

public class RegionMCAFile extends MCAFile<RegionChunk> implements Cloneable {

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

		// recalculate heightmaps if we merged chunks or moved sections up / down
		if (ranges != null || offset.getY() != 0) {
			Point2i relativeOffset = location.regionToChunk().add(offset.toPoint2i()).sub(destination.location.regionToChunk());
			int startX = relativeOffset.getX() > 0 ? 0 : 32 - (32 + relativeOffset.getX());
			int limitX = relativeOffset.getX() > 0 ? (32 - relativeOffset.getX()) : 32;
			int startZ = relativeOffset.getZ() > 0 ? 0 : 32 - (32 + relativeOffset.getZ());
			int limitZ = relativeOffset.getZ() > 0 ? (32 - relativeOffset.getZ()) : 32;

			for (int x = startX; x < limitX; x++) {
				for (int z = startZ; z < limitZ; z++) {
					int sourceIndex = z * 32 + x;
					// skip all chunks that were not merged because the source chunk was not selected
					if (sourceChunks != null && !sourceChunks.get(sourceIndex)) {
						continue;
					}
					int destX = relativeOffset.getX() > 0 ? relativeOffset.getX() + x : x - startX;
					int destZ = relativeOffset.getZ() > 0 ? relativeOffset.getZ() + z : z - startZ;
					int destIndex = destZ * 32 + destX;
					// skip all chunks that were not merged because the target chunk was not selected
					if (targetChunks != null && !targetChunks.get(destIndex)) {
						continue;
					}

					// skip all chunks that are empty
					RegionChunk destinationChunk = destination.chunks[destIndex];
					if (destinationChunk == null || destinationChunk.isEmpty()) {
						continue;
					}

					HeightmapCalculator heightmapCalculator = VersionController.getHeightmapCalculator(destinationChunk.getData().getIntOrDefault("DataVersion", 0));
					heightmapCalculator.worldSurface(destinationChunk.getData());
					heightmapCalculator.oceanFloor(destinationChunk.getData());
					heightmapCalculator.motionBlocking(destinationChunk.getData());
					heightmapCalculator.motionBlockingNoLeaves(destinationChunk.getData());
				}
			}
		}
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
				ChunkRenderer chunkRenderer = VersionController.getChunkRenderer(chunk.data.getIntOrDefault("DataVersion", 0));
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
