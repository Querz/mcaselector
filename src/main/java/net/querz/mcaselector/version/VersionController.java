package net.querz.mcaselector.version;

import net.querz.mcaselector.version.anvil112.*;
import net.querz.mcaselector.version.anvil113.*;
import net.querz.mcaselector.version.anvil114.*;
import net.querz.mcaselector.version.anvil115.*;
import net.querz.mcaselector.version.anvil116.*;
import net.querz.mcaselector.version.anvil117.*;
import net.querz.mcaselector.version.anvil118.*;
import net.querz.mcaselector.version.anvil119.*;
import net.querz.mcaselector.version.anvil120.*;
import net.querz.mcaselector.version.anvil121.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class VersionController {

	private VersionController() {}

	public static ChunkFilter getChunkFilter(int dataVersion) {
		return Mapping.match(dataVersion).getChunkFilter();
	}

	public static ChunkMerger getChunkMerger(int dataVersion) {
		return Mapping.match(dataVersion).getChunkMerger();
	}

	public static ChunkMerger getPoiMerger(int dataVersion) {
		return Mapping.match(dataVersion).getPoiMerger();
	}

	public static ChunkMerger getEntityMerger(int dataVersion) {
		return Mapping.match(dataVersion).getEntityMerger();
	}

	public static ChunkRelocator getChunkRelocator(int dataVersion) {
		return Mapping.match(dataVersion).getChunkRelocator();
	}

	public static ChunkRenderer getChunkRenderer(int dataVersion) {
		return Mapping.match(dataVersion).getChunkRenderer();
	}

	public static ColorMapping getColorMapping(int dataVersion) {
		return Mapping.match(dataVersion).getColorMapping();
	}

	public static ChunkRelocator getPoiRelocator(int dataVersion) {
		return Mapping.match(dataVersion).getPOIRelocator();
	}

	public static ChunkRelocator getEntityRelocator(int dataVersion) {
		return Mapping.match(dataVersion).getEntityRelocator();
	}

	public static EntityFilter getEntityFilter(int dataVersion) {
		return Mapping.match(dataVersion).getEntityFilter();
	}

	public static HeightmapCalculator getHeightmapCalculator(int dataVersion) {
		return Mapping.match(dataVersion).getHeightmapCalculator();
	}

	private static final Map<Supplier<? extends ChunkFilter>, ChunkFilter> chunkFilterInstances = new ConcurrentHashMap<>();
	private static final Map<Supplier<? extends ChunkMerger>, ChunkMerger> chunkMergerInstances = new ConcurrentHashMap<>();
	private static final Map<Supplier<? extends ChunkMerger>, ChunkMerger> poiMergerInstances = new ConcurrentHashMap<>();
	private static final Map<Supplier<? extends ChunkMerger>, ChunkMerger> entityMergerInstances = new ConcurrentHashMap<>();
	private static final Map<Supplier<? extends ChunkRelocator>, ChunkRelocator> chunkRelocatorInstances = new ConcurrentHashMap<>();
	private static final Map<Supplier<? extends ChunkRelocator>, ChunkRelocator> poiRelocatorInstances = new ConcurrentHashMap<>();
	private static final Map<Supplier<? extends ChunkRelocator>, ChunkRelocator> entityRelocatorInstances = new ConcurrentHashMap<>();
	private static final Map<Supplier<? extends EntityFilter>, EntityFilter> entityFilterInstances = new ConcurrentHashMap<>();
	private static final Map<Supplier<? extends ChunkRenderer>, ChunkRenderer> chunkRendererInstances = new ConcurrentHashMap<>();
	private static final Map<Supplier<? extends ColorMapping>, ColorMapping> colorMappingInstances = new ConcurrentHashMap<>();
	private static final Map<Supplier<? extends HeightmapCalculator>, HeightmapCalculator> heightmapCalculatorInstances = new ConcurrentHashMap<>();

	private enum Mapping {

		ANVIL112(0,    1343, Anvil112ChunkFilter::new, Anvil112ChunkMerger::new, Anvil112PoiMerger::new, Anvil112EntityMerger::new, Anvil112ChunkRelocator::new, Anvil112PoiRelocator::new, Anvil112EntityRelocator::new, Anvil112EntityFilter::new, Anvil112HeightmapCalculator::new, Anvil112ChunkRenderer::new, Anvil112ColorMapping::new),
		ANVIL113(1344, 1631, Anvil113ChunkFilter::new, Anvil113ChunkMerger::new, Anvil112PoiMerger::new, Anvil112EntityMerger::new, Anvil113ChunkRelocator::new, Anvil112PoiRelocator::new, Anvil112EntityRelocator::new, Anvil112EntityFilter::new, Anvil113HeightmapCalculator::new, Anvil113ChunkRenderer::new, Anvil113ColorMapping::new),
		ANVIL114(1632, 2201, Anvil113ChunkFilter::new, Anvil114ChunkMerger::new, Anvil114PoiMerger::new, Anvil112EntityMerger::new, Anvil114ChunkRelocator::new, Anvil114PoiRelocator::new, Anvil112EntityRelocator::new, Anvil112EntityFilter::new, Anvil113HeightmapCalculator::new, Anvil113ChunkRenderer::new, Anvil114ColorMapping::new),
		ANVIL115(2202, 2526, Anvil115ChunkFilter::new, Anvil115ChunkMerger::new, Anvil114PoiMerger::new, Anvil112EntityMerger::new, Anvil115ChunkRelocator::new, Anvil114PoiRelocator::new, Anvil112EntityRelocator::new, Anvil112EntityFilter::new, Anvil113HeightmapCalculator::new, Anvil115ChunkRenderer::new, Anvil115ColorMapping::new),
		ANVIL116(2527, 2686, Anvil116ChunkFilter::new, Anvil115ChunkMerger::new, Anvil114PoiMerger::new, Anvil112EntityMerger::new, Anvil116ChunkRelocator::new, Anvil114PoiRelocator::new, Anvil112EntityRelocator::new, Anvil112EntityFilter::new, Anvil116HeightmapCalculator::new, Anvil116ChunkRenderer::new, Anvil116ColorMapping::new),
		ANVIL117(2687, 2824, Anvil117ChunkFilter::new, Anvil117ChunkMerger::new, Anvil114PoiMerger::new, Anvil117EntityMerger::new, Anvil117ChunkRelocator::new, Anvil114PoiRelocator::new, Anvil117EntityRelocator::new, Anvil117EntityFilter::new, Anvil117HeightmapCalculator::new, Anvil117ChunkRenderer::new, Anvil117ColorMapping::new),
		ANVIL118(2825, 3065, Anvil118ChunkFilter::new, Anvil118ChunkMerger::new, Anvil114PoiMerger::new, Anvil117EntityMerger::new, Anvil118ChunkRelocator::new, Anvil114PoiRelocator::new, Anvil118EntityRelocator::new, Anvil118EntityFilter::new, Anvil118HeightmapCalculator::new, Anvil118ChunkRenderer::new, Anvil118ColorMapping::new),
		ANVIL119(3066, 3441, Anvil119ChunkFilter::new, Anvil119ChunkMerger::new, Anvil114PoiMerger::new, Anvil117EntityMerger::new, Anvil119ChunkRelocator::new, Anvil114PoiRelocator::new, Anvil118EntityRelocator::new, Anvil118EntityFilter::new, Anvil119HeightmapCalculator::new, Anvil119ChunkRenderer::new, Anvil119ColorMapping::new),
		ANVIL120(3442, 3939, Anvil119ChunkFilter::new, Anvil119ChunkMerger::new, Anvil114PoiMerger::new, Anvil117EntityMerger::new, Anvil120ChunkRelocator::new, Anvil114PoiRelocator::new, Anvil118EntityRelocator::new, Anvil118EntityFilter::new, Anvil119HeightmapCalculator::new, Anvil119ChunkRenderer::new, Anvil120ColorMapping::new),
		ANVIL121(3940, Integer.MAX_VALUE, Anvil119ChunkFilter::new, Anvil119ChunkMerger::new, Anvil114PoiMerger::new, Anvil117EntityMerger::new, Anvil121ChunkRelocator::new, Anvil114PoiRelocator::new, Anvil118EntityRelocator::new, Anvil118EntityFilter::new, Anvil119HeightmapCalculator::new, Anvil119ChunkRenderer::new, Anvil121ColorMapping::new);



		private final int minVersion, maxVersion;
		private final Supplier<? extends ChunkFilter> chunkFilter;
		private final Supplier<? extends ChunkMerger> chunkMerger;
		private final Supplier<? extends ChunkMerger> poiMerger;
		private final Supplier<? extends ChunkMerger> entityMerger;
		private final Supplier<? extends ChunkRelocator> chunkRelocator;
		private final Supplier<? extends ChunkRelocator> poiRelocator;
		private final Supplier<? extends ChunkRelocator> entityRelocator;
		private final Supplier<? extends EntityFilter> entityFilter;
		private final Supplier<? extends HeightmapCalculator> heightmapCalculator;

		private final Supplier<? extends ChunkRenderer> chunkRenderer;
		private final Supplier<? extends ColorMapping> colorMapping;

		private static final Map<Integer, Mapping> mappingCache = new HashMap<>();

		Mapping(
				int minVersion,
				int maxVersion,
				Supplier<? extends ChunkFilter> chunkFilter,
				Supplier<? extends ChunkMerger> chunkMerger,
				Supplier<? extends ChunkMerger> poiMerger,
				Supplier<? extends ChunkMerger> entityMerger,
				Supplier<? extends ChunkRelocator> chunkRelocator,
				Supplier<? extends ChunkRelocator> poiRelocator,
				Supplier<? extends ChunkRelocator> entityRelocator,
				Supplier<? extends EntityFilter> entityFilter,
				Supplier<? extends HeightmapCalculator> heightmapCalculator,
				Supplier<? extends ChunkRenderer> chunkRenderer,
				Supplier<? extends ColorMapping> colorMapping) {
			this.minVersion = minVersion;
			this.maxVersion = maxVersion;
			this.chunkFilter = chunkFilter;
			this.chunkMerger = chunkMerger;
			this.poiMerger = poiMerger;
			this.entityMerger = entityMerger;
			this.chunkRelocator = chunkRelocator;
			this.poiRelocator = poiRelocator;
			this.entityRelocator = entityRelocator;
			this.entityFilter = entityFilter;
			this.heightmapCalculator = heightmapCalculator;
			this.chunkRenderer = chunkRenderer;
			this.colorMapping = colorMapping;
		}

		ChunkFilter getChunkFilter() {
			return chunkFilterInstances.computeIfAbsent(chunkFilter, Supplier::get);
		}

		ChunkMerger getChunkMerger() {
			return chunkMergerInstances.computeIfAbsent(chunkMerger, Supplier::get);
		}

		ChunkMerger getPoiMerger() {
			return poiMergerInstances.computeIfAbsent(poiMerger, Supplier::get);
		}

		ChunkMerger getEntityMerger() {
			return entityMergerInstances.computeIfAbsent(entityMerger, Supplier::get);
		}

		ChunkRelocator getChunkRelocator() {
			return chunkRelocatorInstances.computeIfAbsent(chunkRelocator, Supplier::get);
		}

		ChunkRelocator getPOIRelocator() {
			return poiRelocatorInstances.computeIfAbsent(poiRelocator, Supplier::get);
		}

		ChunkRelocator getEntityRelocator() {
			return entityRelocatorInstances.computeIfAbsent(entityRelocator, Supplier::get);
		}

		EntityFilter getEntityFilter() {
			return entityFilterInstances.computeIfAbsent(entityFilter, Supplier::get);
		}

		HeightmapCalculator getHeightmapCalculator() {
			return heightmapCalculatorInstances.computeIfAbsent(heightmapCalculator, Supplier::get);
		}

		ChunkRenderer getChunkRenderer() {
			return chunkRendererInstances.computeIfAbsent(chunkRenderer, Supplier::get);
		}

		ColorMapping getColorMapping() {
			return colorMappingInstances.computeIfAbsent(colorMapping, Supplier::get);
		}

		static Mapping match(int dataVersion) {
			Mapping mapping = mappingCache.get(dataVersion);
			if (mapping == null) {
				for (Mapping m : Mapping.values()) {
					if (m.minVersion <= dataVersion && m.maxVersion >= dataVersion) {
						mappingCache.put(dataVersion, m);
						return m;
					}
				}
				throw new IllegalArgumentException("invalid DataVersion: " + dataVersion);
			}
			return mapping;
		}
	}
}
