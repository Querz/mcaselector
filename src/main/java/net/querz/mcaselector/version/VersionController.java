package net.querz.mcaselector.version;

import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.version.anvil112.Anvil112ChunkDataProcessor;
import net.querz.mcaselector.version.anvil112.Anvil112ChunkFilter;
import net.querz.mcaselector.version.anvil112.Anvil112ColorMapping;
import net.querz.mcaselector.version.anvil113.Anvil113ChunkDataProcessor;
import net.querz.mcaselector.version.anvil113.Anvil113ChunkFilter;
import net.querz.mcaselector.version.anvil113.Anvil113ColorMapping;
import net.querz.mcaselector.version.anvil115.Anvil115ChunkDataProcessor;
import net.querz.mcaselector.version.anvil115.Anvil115ChunkFilter;
import net.querz.mcaselector.version.anvil116.Anvil116ChunkDataProcessor;

public class VersionController {

	private VersionController() {}

	public static ChunkDataProcessor getChunkDataProcessor(int dataVersion) {
		return Mapping.match(dataVersion).getChunkDataProcessor();
	}

	public static ColorMapping getColorMapping(int dataVersion) {
		return Mapping.match(dataVersion).getColorMapping();
	}

	public static ChunkFilter getChunkFilter(int dataVersion) {
		return Mapping.match(dataVersion).getChunkFilter();
	}

	private enum Mapping {

		// see https://minecraft-de.gamepedia.com/Versionen

		ANVIL_1_12(0, 1343, Anvil112ChunkDataProcessor.class, Anvil112ColorMapping.class, Anvil112ChunkFilter.class),
		ANVIL_1_13(1344, 2201, Anvil113ChunkDataProcessor.class, Anvil113ColorMapping.class, Anvil113ChunkFilter.class),
		ANVIL_1_15(2202, 2526, Anvil115ChunkDataProcessor.class, Anvil113ColorMapping.class, Anvil115ChunkFilter.class),
		ANVIL_1_16(2527, Integer.MAX_VALUE, Anvil116ChunkDataProcessor.class, Anvil113ColorMapping.class, Anvil115ChunkFilter.class);

		private final int minVersion, maxVersion;
		private final Class<? extends ChunkDataProcessor> chunkDataProcessor;
		private final Class<? extends ColorMapping> colorMapping;
		private final Class<? extends ChunkFilter> chunkFilter;
		private ChunkDataProcessor cdpInstance;
		private ColorMapping cmInstance;
		private ChunkFilter cfInstance;

		Mapping(int minVersion, int maxVersion, Class<? extends ChunkDataProcessor> chunkDataProcessor, Class<? extends ColorMapping> colorMapping, Class<? extends ChunkFilter> chunkFilter) {
			this.minVersion = minVersion;
			this.maxVersion = maxVersion;
			this.chunkDataProcessor = chunkDataProcessor;
			this.colorMapping = colorMapping;
			this.chunkFilter = chunkFilter;
		}

		ChunkDataProcessor getChunkDataProcessor() {
			try {
				return cdpInstance == null ? cdpInstance = chunkDataProcessor.newInstance() : cdpInstance;
			} catch (InstantiationException | IllegalAccessException ex) {
				Debug.dumpException(String.format("failed to create new instance of ChunkDataProcessor for %d-%d", minVersion, maxVersion), ex);
			}
			return null;
		}

		ColorMapping getColorMapping() {
			try {
				return cmInstance == null ? cmInstance = colorMapping.newInstance() : cmInstance;
			} catch (InstantiationException | IllegalAccessException ex) {
				Debug.dumpException(String.format("failed to create new instance of ColorMapping for %d-%d", minVersion, maxVersion), ex);
			}
			return null;
		}

		ChunkFilter getChunkFilter() {
			try {
				return cfInstance == null ? cfInstance = chunkFilter.newInstance() : cfInstance;
			} catch (InstantiationException | IllegalAccessException ex) {
				Debug.dumpException(String.format("faield to create new instance of ChunkFilter for %d-%d", minVersion, maxVersion), ex);
			}
			return null;
		}

		//wohooo! Runtime optimization!
		static Mapping match(int dataVersion) {
			for (Mapping m : Mapping.values()) {
				if (m.minVersion <= dataVersion && m.maxVersion >= dataVersion) {
					return m;
				}
			}
			throw new IllegalArgumentException("invalid DataVersion: " + dataVersion);
		}
	}
}
