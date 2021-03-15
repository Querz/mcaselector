package net.querz.mcaselector.tiles.overlay;

public abstract class AmountParser extends OverlayDataParser {

	public AmountParser(OverlayType type) {
		super(type);
	}

	@Override
	public boolean setMin(String raw) {
		if (raw.isEmpty()) {
			return setMin(0);
		}
		try {
			return setMin(Integer.parseInt(raw));
		} catch (NumberFormatException ex) {
			return false;
		}
	}

	@Override
	public boolean setMax(String raw) {
		if (raw.isEmpty()) {
			return setMax(0);
		}
		try {
			return setMax(Integer.parseInt(raw));
		} catch (NumberFormatException ex) {
			return false;
		}
	}
}
