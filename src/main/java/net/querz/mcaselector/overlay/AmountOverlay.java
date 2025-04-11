package net.querz.mcaselector.overlay;

import java.util.function.Function;

public abstract class AmountOverlay extends Overlay {

	private final int limitMin;
	private final int limitMax;

	public AmountOverlay(OverlayType type) {
		this(type, Integer.MIN_VALUE, Integer.MAX_VALUE);
	}

	public AmountOverlay(OverlayType type, int limitMin, int limitMax) {
		super(type);
		this.limitMin = limitMin;
		this.limitMax = limitMax;
	}

	@Override
	public boolean setMin(String raw) {
		setRawMin(raw);
		return parseRaw(raw, this::setMinInt);
	}

	@Override
	public boolean setMax(String raw) {
		setRawMax(raw);
		return parseRaw(raw, this::setMaxInt);
	}

	private boolean parseRaw(String raw, Function<Integer, Boolean> func) {
		if (raw == null || raw.isEmpty()) {
			return func.apply(null);
		}
		try {
			int value = Integer.parseInt(raw);
			if (value < limitMin || value > limitMax) {
				return func.apply(null);
			}
			return func.apply(value);
		} catch (NumberFormatException ex) {
			return func.apply(null);
		}
	}
}
