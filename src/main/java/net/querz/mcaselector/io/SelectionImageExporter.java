package net.querz.mcaselector.io;

import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.progress.Progress;
import java.util.Map;
import java.util.Set;

public class SelectionImageExporter {

	private SelectionImageExporter() {}

	public static void exportSelectionImage(SelectionData selection, Progress progressChannel) {
		if (selection.getSelection().isEmpty() && !selection.isInverted()) {
			progressChannel.done("no selection");
		}

		MCAFilePipe.clearQueues();

		Map<Point2i, Set<Point2i>> sel = SelectionHelper.getTrueSelection(selection);

		progressChannel.setMax(sel.size());
		Point2i first = sel.entrySet().iterator().next().getKey();
		progressChannel.updateProgress(FileHelper.createMCAFileName(first), 0);


	}
}
