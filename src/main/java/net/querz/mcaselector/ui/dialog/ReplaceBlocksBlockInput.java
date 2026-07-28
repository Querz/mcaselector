package net.querz.mcaselector.ui.dialog;

import javafx.scene.layout.VBox;
import net.querz.mcaselector.text.Translation;
import net.querz.mcaselector.version.mapping.blockstate.BlockStateCatalog;

abstract class ReplaceBlocksBlockInput extends VBox {

	protected final boolean source;
	private BlockStateCatalog catalog;

	protected ReplaceBlocksBlockInput(boolean source, BlockStateCatalog catalog) {
		this.source = source;
		this.catalog = catalog;
	}

	protected final void setCatalog(BlockStateCatalog catalog) {
		this.catalog = catalog;
	}

	protected final ReplaceBlocksDiagnostics.Diagnostic catalogDiagnostic(String text) {
		if (text.startsWith("{") || source && isSourceExpression(text)) {
			return ReplaceBlocksDiagnostics.none();
		}
		ReplaceBlocksDiagnostics.NameResult normalized = ReplaceBlocksDiagnostics.normalizeBuilderName(text, source);
		if (normalized.name() == null || catalog.containsBlock(normalized.name())) {
			return ReplaceBlocksDiagnostics.none();
		}
		Translation message = source
				? Translation.DIALOG_REPLACE_BLOCKS_BUILDER_CATALOG_SOURCE_UNKNOWN
				: Translation.DIALOG_REPLACE_BLOCKS_BUILDER_CATALOG_TARGET_UNKNOWN;
		return new ReplaceBlocksDiagnostics.Diagnostic(ReplaceBlocksDiagnostics.Severity.WARNING,
				message.format(normalized.name(), catalog.version()));
	}

	private boolean isSourceExpression(String value) {
		return value.startsWith("literal(") || value.startsWith("regex(") || value.startsWith("props(")
				|| value.startsWith("tile(") || value.startsWith("no_tile(")
				|| value.startsWith("y(") || value.startsWith("biome(");
	}
}
