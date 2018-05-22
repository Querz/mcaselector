package net.querz.mcaselector.version;

import net.querz.mcaselector.ChunkDataProcessor;
import net.querz.mcaselector.ColorMapping;
import net.querz.mcaselector.version.anvil112.Anvil112ChunkDataProcessor;
import net.querz.mcaselector.version.anvil112.Anvil112ColorMapping;

public class VersionController {

	private VersionController() {}

	public static ChunkDataProcessor getChunkDataProcessor(int dataVersion) {
		return Mapping.match(dataVersion).cdp;
	}

	public static ColorMapping getColorMapping(int dataVersion) {
		return Mapping.match(dataVersion).cm;
	}

	private enum Mapping {
		ANVIL_1_12(0, 1343, new Anvil112ChunkDataProcessor(), new Anvil112ColorMapping()),
		ANVIL_1_13(1344, Integer.MAX_VALUE, null, null); //TODO

		private int from, to;
		private ChunkDataProcessor cdp;
		private ColorMapping cm;

		Mapping(int from, int to, ChunkDataProcessor cdp, ColorMapping cm) {
			this.from = from;
			this.to = to;
			this.cdp = cdp;
			this.cm = cm;
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
