package net.querz.mcaselector.tiles.overlay;

public abstract class AmountParser extends OverlayDataParser {

	public AmountParser(OverlayType type) {
		super(type);
	}

	@Override
	public boolean setMin(String raw) {
		try {
			return setMin(Integer.parseInt(raw));
		} catch (NumberFormatException ex) {
			return false;
		}
	}

	@Override
	public boolean setMax(String raw) {
		try {
			return setMax(Integer.parseInt(raw));
		} catch (NumberFormatException ex) {
			return false;
		}
	}
}
