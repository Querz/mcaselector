package net.querz.mcaselector.tiles.overlay;

import net.querz.mcaselector.io.mca.ChunkData;

public abstract class OverlayDataParser {

	private final OverlayType type;
	private boolean active;
	private int min;
	private int max;

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

	public int min() {
		return min;
	}

	public int max() {
		return max;
	}

	protected boolean setMin(int min) {
		if (min > max) {
			return false;
		} else {
			this.min = min;
			return true;
		}
	}

	protected boolean setMax(int max) {
		if (max < min) {
			return false;
		} else {
			this.max = max;
			return true;
		}
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
}
