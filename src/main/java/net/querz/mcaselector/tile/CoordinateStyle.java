package net.querz.mcaselector.tile;

import net.querz.mcaselector.text.Translation;

public enum CoordinateStyle {

	DYNAMIC(Translation.MENU_VIEW_COORDINATE_STYLE_DYNAMIC),
	EDGE(Translation.MENU_VIEW_COORDINATE_STYLE_EDGE),
	GRID(Translation.MENU_VIEW_COORDINATE_STYLE_GRID);

	private final Translation translation;

	CoordinateStyle(Translation translation) {
		this.translation = translation;
	}

	public Translation getTranslation() {
		return translation;
	}
}