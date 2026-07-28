package net.querz.mcaselector.ui.dialog;

import net.querz.mcaselector.io.job.ReplaceBlocksPreviewer;
import net.querz.mcaselector.text.Translation;

final class ReplaceBlocksPreviewFormatter {

	private ReplaceBlocksPreviewFormatter() {}

	static String format(ReplaceBlocksPreviewer.Result result) {
		StringBuilder builder = new StringBuilder(Translation.DIALOG_REPLACE_BLOCKS_PREVIEW_RESULT.format(
				result.getScannedRegions(), result.getScannedChunks(), result.getAffectedChunks(),
				result.getAffectedSections(), result.getMatchedBlocks(), result.getTileEntityAdditions(),
				result.getTileEntityRemovals(), result.getTileEntityUpdates()));
		appendRules(builder, result);
		if (result.getOverlappingBlocks() > 0) {
			builder.append("\n\n").append(Translation.DIALOG_REPLACE_BLOCKS_PREVIEW_WARNING_OVERLAP.format(result.getOverlappingBlocks()));
		}
		if (result.replacesAir()) {
			builder.append("\n\n").append(Translation.DIALOG_REPLACE_BLOCKS_PREVIEW_WARNING_AIR.format(result.getCompletedAirSections()));
		}
		if (result.replacesWithTileEntity() || result.getTileEntityAdditions() > 0
				|| result.getTileEntityRemovals() > 0 || result.getTileEntityUpdates() > 0) {
			builder.append("\n\n").append(Translation.DIALOG_REPLACE_BLOCKS_PREVIEW_WARNING_TILE.toString());
		}
		if (result.getLightSections() > 0) {
			builder.append("\n\n").append(Translation.DIALOG_REPLACE_BLOCKS_PREVIEW_WARNING_LIGHT.format(result.getLightSections()));
		}
		if (result.getPotentialAdjacentRelightChunks() > 0) {
			builder.append("\n\n").append(Translation.DIALOG_REPLACE_BLOCKS_PREVIEW_WARNING_ADJACENT_RELIGHT.format(
					result.getPotentialAdjacentRelightChunks()));
		}
		if (result.getAffectedChunks() > 0) {
			builder.append("\n\n").append(Translation.DIALOG_REPLACE_BLOCKS_PREVIEW_WARNING_HEIGHTMAPS.format(result.getAffectedChunks()));
		}
		if (result.getUnsupportedChunks() > 0) {
			builder.append("\n\n").append(Translation.DIALOG_REPLACE_BLOCKS_PREVIEW_WARNING_UNSUPPORTED.format(result.getUnsupportedChunks()));
		}
		if (result.getErrorChunks() > 0 || result.getErrorRegions() > 0) {
			builder.append("\n\n").append(Translation.DIALOG_REPLACE_BLOCKS_PREVIEW_WARNING_ERRORS.format(
					result.getErrorChunks(), result.getErrorRegions()));
			for (String error : result.getErrors()) {
				builder.append("\n").append(error);
			}
		}
		return builder.toString();
	}

	private static void appendRules(StringBuilder builder, ReplaceBlocksPreviewer.Result result) {
		if (result.getRules().isEmpty()) {
			return;
		}
		builder.append("\n\n").append(Translation.DIALOG_REPLACE_BLOCKS_PREVIEW_RULES.toString());
		for (ReplaceBlocksPreviewer.RulePreview rule : result.getRules()) {
			builder.append("\n").append(Translation.DIALOG_REPLACE_BLOCKS_PREVIEW_RULE.format(
					rule.getIndex(), rule.getSourceMode(), rule.getSourceText(), rule.getTargetText(), rule.getBlocks()));
		}
	}
}
