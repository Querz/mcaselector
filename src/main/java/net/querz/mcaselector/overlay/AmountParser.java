package net.querz.mcaselector.overlay;

public abstract class AmountParser extends Overlay {

	public AmountParser(OverlayType type) {
		super(type);
	}

	@Override
	public boolean setMin(String raw) {
		setRawMin(raw);
		if (raw == null || raw.isEmpty()) {
			return setMinInt(null);
		}
		try {
			return setMinInt(Integer.parseInt(raw));
		} catch (NumberFormatException ex) {
			return setMinInt(null);
		}
	}

	@Override
	public boolean setMax(String raw) {
		setRawMax(raw);
		if (raw == null || raw.isEmpty()) {
			return setMaxInt(null);
		}
		try {
			return setMaxInt(Integer.parseInt(raw));
		} catch (NumberFormatException ex) {
			return setMaxInt(null);
		}
	}
}
