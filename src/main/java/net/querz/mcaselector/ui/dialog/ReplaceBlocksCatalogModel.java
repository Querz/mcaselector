package net.querz.mcaselector.ui.dialog;

import net.querz.mcaselector.version.mapping.blockstate.BlockStateCatalog;
import java.util.Comparator;
import java.util.List;

final class ReplaceBlocksCatalogModel {

	private final List<BlockStateCatalog> catalogs;
	private BlockStateCatalog selected;

	ReplaceBlocksCatalogModel(List<BlockStateCatalog> catalogs) {
		this.catalogs = catalogs.stream()
				.sorted(Comparator.comparingInt(BlockStateCatalog::dataVersion))
				.toList();
		this.selected = this.catalogs.isEmpty() ? BlockStateCatalog.latestJava() : this.catalogs.getLast();
	}

	List<BlockStateCatalog> catalogs() {
		return catalogs;
	}

	BlockStateCatalog selected() {
		return selected;
	}

	boolean select(String version) {
		for (BlockStateCatalog catalog : catalogs) {
			if (catalog.version().equals(version)) {
				selected = catalog;
				return true;
			}
		}
		return false;
	}

	Compatibility compatibility(String name, boolean source) {
		if (selected.containsBlock(name)) {
			return Compatibility.KNOWN;
		}
		return source ? Compatibility.UNKNOWN_SOURCE : Compatibility.UNKNOWN_TARGET;
	}

	String label(BlockStateCatalog catalog) {
		return "Java " + catalog.version() + " (DataVersion " + catalog.dataVersion() + ")";
	}

	String shortLabel(BlockStateCatalog catalog) {
		return "Java " + catalog.version();
	}

	enum Compatibility {
		KNOWN,
		UNKNOWN_SOURCE,
		UNKNOWN_TARGET
	}
}
