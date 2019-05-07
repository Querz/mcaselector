package net.querz.mcaselector.io;

import net.querz.mcaselector.ui.ProgressTask;
import net.querz.mcaselector.util.Helper;
import net.querz.mcaselector.util.Point2i;

import java.io.File;

public class ChunkImporter {

	private ChunkImporter() {}

	public static void importChunks(File importDir, ProgressTask progressChannel) {
		MCAFilePipe.clearQueues();

		File[] files = importDir.listFiles((dir, name) -> name.matches(Helper.MCA_FILE_PATTERN));
		if (files == null || files.length == 0) {
			return;
		}

		progressChannel.setMax(files.length);
		File first = files[0];

	}
}
