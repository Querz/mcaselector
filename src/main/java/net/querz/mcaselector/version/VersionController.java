package net.querz.mcaselector.version;

import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.version.anvil112.Anvil112ChunkDataProcessor;
import net.querz.mcaselector.version.anvil112.Anvil112ChunkFilter;
import net.querz.mcaselector.version.anvil112.Anvil112ColorMapping;
import net.querz.mcaselector.version.anvil113.Anvil113ChunkDataProcessor;
import net.querz.mcaselector.version.anvil113.Anvil113ChunkFilter;
import net.querz.mcaselector.version.anvil113.Anvil113ColorMapping;
import net.querz.mcaselector.version.anvil115.Anvil115ChunkFilter;

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
		ANVIL_1_15(2204, Integer.MAX_VALUE, Anvil113ChunkDataProcessor.class, Anvil113ColorMapping.class, Anvil115ChunkFilter.class);

		private int from, to;
		private Class<? extends ChunkDataProcessor> cdp;
		private Class<? extends ColorMapping> cm;
		private Class<? extends ChunkFilter> cf;
		private ChunkDataProcessor cdpInstance;
		private ColorMapping cmInstance;
		private ChunkFilter cfInstance;

		Mapping(int from, int to, Class<? extends ChunkDataProcessor> cdp, Class<? extends ColorMapping> cm, Class<? extends ChunkFilter> cf) {
			this.from = from;
			this.to = to;
			this.cdp = cdp;
			this.cm = cm;
			this.cf = cf;
		}

		ChunkDataProcessor getChunkDataProcessor() {
			try {
				return cdpInstance == null ? cdpInstance = cdp.newInstance() : cdpInstance;
			} catch (InstantiationException | IllegalAccessException e) {
				Debug.error(e);
			}
			return null;
		}

		ColorMapping getColorMapping() {
			try {
				return cmInstance == null ? cmInstance = cm.newInstance() : cmInstance;
			} catch (InstantiationException | IllegalAccessException e) {
				Debug.error(e);
			}
			return null;
		}

		ChunkFilter getChunkFilter() {
			try {
				return cfInstance == null ? cfInstance = cf.newInstance() : cfInstance;
			} catch (InstantiationException | IllegalAccessException e) {
				Debug.error(e);
			}
			return null;
		}

		//wohooo! Runtime optimization!
		static Mapping match(int dataVersion) {
			for (Mapping m : Mapping.values()) {
				if (m.from <= dataVersion && m.to >= dataVersion) {
					return m;
				}
			}
			throw new IllegalArgumentException("invalid DataVersion: " + dataVersion);
		}
	}
}
