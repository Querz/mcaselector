package net.querz.mcaselector.version;

import net.querz.mcaselector.version.anvil112.*;
import net.querz.mcaselector.version.anvil113.*;
import net.querz.mcaselector.version.anvil114.*;
import net.querz.mcaselector.version.anvil115.*;
import net.querz.mcaselector.version.anvil116.*;
import net.querz.mcaselector.version.anvil117.*;
import net.querz.mcaselector.version.anvil118.*;
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

	public static ChunkRelocator getChunkRelocator(int dataVersion) {
		return Mapping.match(dataVersion).getChunkRelocator();
	}

	public static ChunkRenderer getChunkRenderer(int dataVersion) {
		return Mapping.match(dataVersion).getChunkRenderer();
	}

	public static ColorMapping getColorMapping(int dataVersion) {
		return Mapping.match(dataVersion).getColorMapping();
	}

	public static PoiRelocator getPoiRelocator(int dataVersion) {
		return Mapping.match(dataVersion).getPOIRelocator();
	}

	public static EntityRelocator getEntityRelocator(int dataVersion) {
		return Mapping.match(dataVersion).getEntityRelocator();
	}

	public static EntityFilter getEntityFilter(int dataVersion) {
		return Mapping.match(dataVersion).getEntityFilter();
	}

	private static final Map<Supplier<? extends ChunkFilter>, ChunkFilter> chunkFilterInstances = new ConcurrentHashMap<>();
	private static final Map<Supplier<? extends ChunkMerger>, ChunkMerger> chunkMergerInstances = new ConcurrentHashMap<>();
	private static final Map<Supplier<? extends ChunkRelocator>, ChunkRelocator> chunkRelocatorInstances = new ConcurrentHashMap<>();
	private static final Map<Supplier<? extends PoiRelocator>, PoiRelocator> poiRelocatorInstances = new ConcurrentHashMap<>();
	private static final Map<Supplier<? extends EntityRelocator>, EntityRelocator> entityRelocatorInstances = new ConcurrentHashMap<>();
	private static final Map<Supplier<? extends EntityFilter>, EntityFilter> entityFilterInstances = new ConcurrentHashMap<>();
	private static final Map<Supplier<? extends ChunkRenderer>, ChunkRenderer> chunkRendererInstances = new ConcurrentHashMap<>();
	private static final Map<Supplier<? extends ColorMapping>, ColorMapping> colorMappingInstances = new ConcurrentHashMap<>();

	private enum Mapping {

		ANVIL112(0, 1343, Anvil112ChunkFilter::new, Anvil112ChunkMerger::new, Anvil112ChunkRelocator::new, Anvil112PoiRelocator::new, Anvil112EntityRelocator::new, Anvil112EntityFilter::new, Anvil112ChunkRenderer::new, Anvil112ColorMapping::new),
		ANVIL113(1344, 1631, Anvil113ChunkFilter::new, Anvil113ChunkMerger::new, Anvil113ChunkRelocator::new, Anvil112PoiRelocator::new, Anvil112EntityRelocator::new, Anvil112EntityFilter::new, Anvil113ChunkRenderer::new, Anvil113ColorMapping::new),
		ANVIL114(1632, 2201, Anvil113ChunkFilter::new, Anvil113ChunkMerger::new, Anvil114ChunkRelocator::new, Anvil114PoiRelocator::new, Anvil112EntityRelocator::new, Anvil112EntityFilter::new, Anvil113ChunkRenderer::new, Anvil114ColorMapping::new),
		ANVIL115(2202, 2526, Anvil115ChunkFilter::new, Anvil115ChunkMerger::new, Anvil115ChunkRelocator::new, Anvil114PoiRelocator::new, Anvil112EntityRelocator::new, Anvil112EntityFilter::new, Anvil115ChunkRenderer::new, Anvil115ColorMapping::new),
		ANVIL116(2527, 2686, Anvil116ChunkFilter::new, Anvil115ChunkMerger::new, Anvil116ChunkRelocator::new, Anvil114PoiRelocator::new, Anvil112EntityRelocator::new, Anvil112EntityFilter::new, Anvil116ChunkRenderer::new, Anvil116ColorMapping::new),
		ANVIL117(2687, 2824, Anvil117ChunkFilter::new, Anvil115ChunkMerger::new, Anvil117ChunkRelocator::new, Anvil114PoiRelocator::new, Anvil117EntityRelocator::new, Anvil117EntityFilter::new, Anvil117ChunkRenderer::new, Anvil117ColorMapping::new),
		ANVIL118(2825, Integer.MAX_VALUE, Anvil118ChunkFilter::new, Anvil115ChunkMerger::new, Anvil117ChunkRelocator::new, Anvil114PoiRelocator::new, Anvil117EntityRelocator::new, Anvil117EntityFilter::new, Anvil118ChunkRenderer::new, Anvil118ColorMapping::new);


		private final int minVersion, maxVersion;
		private final Supplier<? extends ChunkFilter> chunkFilter;
		private final Supplier<? extends ChunkMerger> chunkMerger;
		private final Supplier<? extends ChunkRelocator> chunkRelocator;
		private final Supplier<? extends PoiRelocator> poiRelocator;
		private final Supplier<? extends EntityRelocator> entityRelocator;
		private final Supplier<? extends EntityFilter> entityFilter;

		private final Supplier<? extends ChunkRenderer> chunkRenderer;
		private final Supplier<? extends ColorMapping> colorMapping;

		private static final Map<Integer, Mapping> mappingCache = new HashMap<>();

		Mapping(
				int minVersion,
				int maxVersion,
				Supplier<? extends ChunkFilter> chunkFilter,
				Supplier<? extends ChunkMerger> chunkMerger,
				Supplier<? extends ChunkRelocator> chunkRelocator,
				Supplier<? extends PoiRelocator> poiRelocator,
				Supplier<? extends EntityRelocator> entityRelocator,
				Supplier<? extends EntityFilter> entityFilter,
				Supplier<? extends ChunkRenderer> chunkRenderer,
				Supplier<? extends ColorMapping> colorMapping) {
			this.minVersion = minVersion;
			this.maxVersion = maxVersion;
			this.chunkFilter = chunkFilter;
			this.chunkMerger = chunkMerger;
			this.chunkRelocator = chunkRelocator;
			this.poiRelocator = poiRelocator;
			this.entityRelocator = entityRelocator;
			this.entityFilter = entityFilter;
			this.chunkRenderer = chunkRenderer;
			this.colorMapping = colorMapping;
		}

		ChunkFilter getChunkFilter() {
			return chunkFilterInstances.computeIfAbsent(chunkFilter, Supplier::get);
		}

		ChunkMerger getChunkMerger() {
			return chunkMergerInstances.computeIfAbsent(chunkMerger, Supplier::get);
		}

		ChunkRelocator getChunkRelocator() {
			return chunkRelocatorInstances.computeIfAbsent(chunkRelocator, Supplier::get);
		}

		PoiRelocator getPOIRelocator() {
			return poiRelocatorInstances.computeIfAbsent(poiRelocator, Supplier::get);
		}

		EntityRelocator getEntityRelocator() {
			return entityRelocatorInstances.computeIfAbsent(entityRelocator, Supplier::get);
		}

		EntityFilter getEntityFilter() {
			return entityFilterInstances.computeIfAbsent(entityFilter, Supplier::get);
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
