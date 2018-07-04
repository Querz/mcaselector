package net.querz.mcaselector.version;

import net.querz.mcaselector.version.anvil112.Anvil112ChunkDataProcessor;
import net.querz.mcaselector.version.anvil112.Anvil112ColorMapping;
import net.querz.mcaselector.version.anvil113.Anvil113ChunkDataProcessor;
import net.querz.mcaselector.version.anvil113.Anvil113ColorMapping;

public class VersionController {

	private VersionController() {}

	public static ChunkDataProcessor getChunkDataProcessor(int dataVersion) {
		return Mapping.match(dataVersion).getChunkDataProcessor();
	}

	public static ColorMapping getColorMapping(int dataVersion) {
		return Mapping.match(dataVersion).getColorMapping();
	}

	private enum Mapping {

		ANVIL_1_12(0, 1343, Anvil112ChunkDataProcessor.class, Anvil112ColorMapping.class),
		ANVIL_1_13(1344, Integer.MAX_VALUE, Anvil113ChunkDataProcessor.class, Anvil113ColorMapping.class);

		private int from, to;
		private Class<? extends ChunkDataProcessor> cdp;
		private Class<? extends ColorMapping> cm;
		private ChunkDataProcessor cdpInstance;
		private ColorMapping cmInstance;

		Mapping(int from, int to, Class<? extends ChunkDataProcessor> cdp, Class<? extends ColorMapping> cm) {
			this.from = from;
			this.to = to;
			this.cdp = cdp;
			this.cm = cm;
		}

		ChunkDataProcessor getChunkDataProcessor() {
			try {
				return cdpInstance == null ? cdpInstance = cdp.newInstance() : cdpInstance;
			} catch (InstantiationException | IllegalAccessException e) {
				e.printStackTrace();
			}
			return null;
		}

		ColorMapping getColorMapping() {
			try {
				return cmInstance == null ? cmInstance = cm.newInstance() : cmInstance;
			} catch (InstantiationException | IllegalAccessException e) {
				e.printStackTrace();
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
