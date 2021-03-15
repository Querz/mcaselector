package net.querz.mcaselector.tiles.overlay;

import net.querz.mcaselector.io.mca.ChunkData;

public abstract class OverlayDataParser {

	private final OverlayType type;
	private boolean active;
	private Integer min;
	private Integer max;

	public OverlayDataParser(OverlayType type) {
		this.type = type;
	}

	public OverlayType getType() {
		return type;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public boolean isActive() {
		return active;
	}

	public Integer min() {
		return min;
	}

	public Integer max() {
		return max;
	}

	public String minString() {
		return min == null ? "" : min + "";
	}

	public String maxString() {
		return max == null ? "" : max + "";
	}

	public boolean isValid() {
		return min != null && max != null && min < max;
	}

	protected boolean setMin(Integer min) {
		this.min = min;
		return isValid();
	}

	protected boolean setMax(Integer max) {
		this.max = max;
		return isValid();
	}

	public abstract int parseValue(ChunkData chunkData);

	public abstract String name();

	public abstract boolean setMin(String raw);

	public abstract boolean setMax(String raw);

	// can be overwritten to set additional data points for a single overlay
	public boolean setMultiValues(String raw) {
		return true;
	}

	// can be overwritten to supply additional data points for a single overlay
	public String[] multiValues() {
		return null;
	}

	@Override
	public String toString() {
		return type + "{min=" + minString() + ", max=" + maxString() + ", active=" + active + "}";
	}
}
