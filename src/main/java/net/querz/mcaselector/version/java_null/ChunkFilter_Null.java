package net.querz.mcaselector.version.java_null;

import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.util.point.Point2i;
import net.querz.mcaselector.util.point.Point3i;
import net.querz.mcaselector.util.range.Range;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.MCVersionImplementation;
import net.querz.mcaselector.version.mapping.registry.BiomeRegistry;
import net.querz.mcaselector.version.mapping.registry.StatusRegistry;
import net.querz.nbt.*;
import java.util.*;

public class ChunkFilter_Null {

	@MCVersionImplementation(0)
	public static class Biomes implements ChunkFilter.Biomes {

		@Override
		public boolean matchBiomes(ChunkData data, Collection<BiomeRegistry.BiomeIdentifier> biomes) {
			return false;
		}

		@Override
		public boolean matchAnyBiome(ChunkData data, Collection<BiomeRegistry.BiomeIdentifier> biomes) {
			return false;
		}

		@Override
		public void changeBiome(ChunkData data, BiomeRegistry.BiomeIdentifier biome) {}

		@Override
		public void forceBiome(ChunkData data, BiomeRegistry.BiomeIdentifier biome) {}
	}

	@MCVersionImplementation(0)
	public static class Blocks implements ChunkFilter.Blocks {

		@Override
		public boolean matchBlockNames(ChunkData data, Collection<String> names) {
			return false;
		}

		@Override
		public boolean matchAnyBlockName(ChunkData data, Collection<String> names) {
			return false;
		}

		@Override
		public void replaceBlocks(ChunkData data, Map<String, ChunkFilter.BlockReplaceData> replace) {}

		@Override
		public int getBlockAmount(ChunkData data, String[] blocks) {
			return 0;
		}

		@Override
		public int getAverageHeight(ChunkData data) {
			return 0;
		}
	}

	@MCVersionImplementation(0)
	public static class Palette implements ChunkFilter.Palette {

		@Override
		public boolean paletteEquals(ChunkData data, Collection<String> names) {
			return false;
		}
	}

	@MCVersionImplementation(0)
	public static class TileEntities implements ChunkFilter.TileEntities {

		@Override
		public ListTag getTileEntities(ChunkData data) {
			return null;
		}
	}

	@MCVersionImplementation(0)
	public static class Sections implements ChunkFilter.Sections {

		@Override
		public ListTag getSections(ChunkData data) {
			return null;
		}

		@Override
		public void deleteSections(ChunkData data, List<Range> ranges) {}
	}

	@MCVersionImplementation(0)
	public static class InhabitedTime implements ChunkFilter.InhabitedTime {

		@Override
		public LongTag getInhabitedTime(ChunkData data) {
			return null;
		}

		@Override
		public void setInhabitedTime(ChunkData data, long inhabitedTime) {}
	}

	@MCVersionImplementation(0)
	public static class Status implements ChunkFilter.Status {

		@Override
		public StringTag getStatus(ChunkData data) {
			return null;
		}

		@Override
		public void setStatus(ChunkData data, StatusRegistry.StatusIdentifier status) {}

		@Override
		public boolean matchStatus(ChunkData data, StatusRegistry.StatusIdentifier status) {
			return false;
		}
	}

	@MCVersionImplementation(0)
	public static class LastUpdate implements ChunkFilter.LastUpdate {

		@Override
		public LongTag getLastUpdate(ChunkData data) {
			return null;
		}

		@Override
		public void setLastUpdate(ChunkData data, long lastUpdate) {}
	}

	@MCVersionImplementation(0)
	public static class Pos implements ChunkFilter.Pos {

		@Override
		public IntTag getXPos(ChunkData data) {
			return null;
		}

		@Override
		public IntTag getYPos(ChunkData data) {
			return null;
		}

		@Override
		public IntTag getZPos(ChunkData data) {
			return null;
		}
	}

	@MCVersionImplementation(0)
	public static class LightPopulated implements ChunkFilter.LightPopulated {

		@Override
		public ByteTag getLightPopulated(ChunkData data) {
			return null;
		}

		@Override
		public void setLightPopulated(ChunkData data, byte lightPopulated) {}
	}

	@MCVersionImplementation(0)
	public static class Structures implements ChunkFilter.Structures {

		@Override
		public CompoundTag getStructureReferences(ChunkData data) {
			return null;
		}

		@Override
		public CompoundTag getStructureStarts(ChunkData data) {
			return null;
		}
	}

	@MCVersionImplementation(0)
	public static class Blending implements ChunkFilter.Blending {

		@Override
		public void forceBlending(ChunkData data) {}
	}

	@MCVersionImplementation(0)
	public static class Relocate implements ChunkFilter.Relocate {

		@Override
		public boolean relocate(CompoundTag root, Point3i offset) {
			return false;
		}
	}

	@MCVersionImplementation(0)
	public static class Merge implements ChunkFilter.Merge {

		@Override
		public void mergeChunks(CompoundTag source, CompoundTag destination, List<Range> ranges, int yOffset) {}

		@Override
		public CompoundTag newEmptyChunk(Point2i absoluteLocation, int dataVersion) {
			return null;
		}
	}

	@MCVersionImplementation(0)
	public static class Entities implements ChunkFilter.Entities {

		@Override
		public void deleteEntities(ChunkData data, List<Range> ranges) {}

		@Override
		public ListTag getEntities(ChunkData data) {
			return null;
		}
	}

	@MCVersionImplementation(0)
	public static class MergeEntities implements ChunkFilter.MergeEntities {

		@Override
		public void mergeChunks(CompoundTag source, CompoundTag destination, List<Range> ranges, int yOffset) {}

		@Override
		public CompoundTag newEmptyChunk(Point2i absoluteLocation, int dataVersion) {
			return null;
		}
	}

	@MCVersionImplementation(0)
	public static class MergePOI implements ChunkFilter.MergePOI {

		@Override
		public void mergeChunks(CompoundTag source, CompoundTag destination, List<Range> ranges, int yOffset) {
		}

		@Override
		public CompoundTag newEmptyChunk(Point2i absoluteLocation, int dataVersion) {
			return null;
		}
	}

	@MCVersionImplementation(0)
	public static class RelocateEntities implements ChunkFilter.RelocateEntities {

		@Override
		public boolean relocate(CompoundTag root, Point3i offset) {
			return true;
		}
	}

	@MCVersionImplementation(0)
	public static class RelocatePOI implements ChunkFilter.RelocatePOI {

		@Override
		public boolean relocate(CompoundTag root, Point3i offset) {
			return true;
		}
	}

	@MCVersionImplementation(0)
	public static class Heightmap implements ChunkFilter.Heightmap {

		@Override
		public void worldSurface(ChunkData data) {}

		@Override
		public void oceanFloor(ChunkData data) {}

		@Override
		public void motionBlocking(ChunkData data) {}

		@Override
		public void motionBlockingNoLeaves(ChunkData data) {}
	}
}
