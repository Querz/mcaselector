package net.querz.mcaselector.io;

import net.querz.mcaselector.tiles.Tile;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.progress.Progress;
import net.querz.mcaselector.progress.Timer;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SelectionExporter {

	private SelectionExporter() {}

	public static void exportSelection(SelectionData selection, File destination, Progress progressChannel) {
		if (selection.getSelection().isEmpty() && !selection.isInverted()) {
			progressChannel.done("no selection");
			return;
		}

		MCAFilePipe.clearQueues();

		Map<Point2i, Set<Point2i>> sel = SelectionHelper.getTrueSelection(selection);

		progressChannel.setMax(sel.size());
		Point2i first = sel.entrySet().iterator().next().getKey();
		progressChannel.updateProgress(FileHelper.createMCAFileName(first), 0);

		for (Map.Entry<Point2i, Set<Point2i>> entry : sel.entrySet()) {
			MCAFilePipe.addJob(new MCADeleteSelectionLoadJob(
					FileHelper.createMCAFilePath(entry.getKey()),
					entry.getValue(),
					new File(destination, FileHelper.createMCAFileName(entry.getKey())),
					progressChannel));
		}
	}

	private static class MCADeleteSelectionLoadJob extends LoadDataJob {

		private final Set<Point2i> chunksToBeExported;
		private final File destination;
		private final Progress progressChannel;

		private MCADeleteSelectionLoadJob(File file, Set<Point2i> chunksToBeExported, File destination, Progress progressChannel) {
			super(file);
			this.chunksToBeExported = chunksToBeExported;
			this.destination = destination;
			this.progressChannel = progressChannel;
		}

		@Override
		public void execute() {
			if (chunksToBeExported == null) {
				try {
					Files.copy(getFile().toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
					Debug.dumpf("moved file %s", getFile().getName());
				} catch (Exception ex) {
					Debug.dumpException("error moving file " + getFile().getName(), ex);
				}
				progressChannel.incrementProgress(getFile().getName());
				return;
			}
			byte[] data = load(MCAFile.SECTION_SIZE * 2); //load header only
			if (data != null) {
				MCAFilePipe.executeProcessData(new MCADeleteSelectionProcessJob(getFile(), data, chunksToBeExported, destination, progressChannel));
			} else {
				Debug.errorf("error loading mca file %s", getFile().getName());
				progressChannel.incrementProgress(getFile().getName());
			}
		}
	}

	private static class MCADeleteSelectionProcessJob extends ProcessDataJob {

		private final Progress progressChannel;
		private final Set<Point2i> chunksToBeExported;
		private final File destination;

		private MCADeleteSelectionProcessJob(File file, byte[] data, Set<Point2i> chunksToBeExported, File destination, Progress progressChannel) {
			super(file, data);
			this.chunksToBeExported = chunksToBeExported;
			this.destination = destination;
			this.progressChannel = progressChannel;
		}

		@Override
		public void execute() {
			//load MCAFile
			Timer t = new Timer();
			try {
				MCAFile mca = MCAFile.readHeader(getFile(), new ByteArrayPointer(getData()));
				if (mca != null) {

					Set<Point2i> inverted = new HashSet<>(Tile.CHUNKS - chunksToBeExported.size());
					Point2i origin = chunksToBeExported.iterator().next().chunkToRegion().regionToChunk();
					for (int x = origin.getX(); x < origin.getX() + Tile.SIZE_IN_CHUNKS; x++) {
						for (int z = origin.getZ(); z < origin.getZ() + Tile.SIZE_IN_CHUNKS; z++) {
							Point2i cp = new Point2i(x, z);
							if (!chunksToBeExported.contains(cp)) {
								inverted.add(cp);
							}
						}
					}

					mca.deleteChunkIndices(inverted);
					Debug.dumpf("took %s to delete chunk indices in %s", t, getFile().getName());
					MCAFilePipe.executeSaveData(new MCADeleteSelectionSaveJob(getFile(), mca, destination, progressChannel));
				}
			} catch (Exception ex) {
				Debug.dumpException("error deleting chunk indices in " + getFile().getName(), ex);
				progressChannel.incrementProgress(getFile().getName());

			}
		}
	}

	private static class MCADeleteSelectionSaveJob extends SaveDataJob<MCAFile> {

		private final Progress progressChannel;
		private final File destination;

		private MCADeleteSelectionSaveJob(File file, MCAFile data, File destination, Progress progressChannel) {
			super(file, data);
			this.destination = destination;
			this.progressChannel = progressChannel;
		}

		@Override
		public void execute() {
			Timer t = new Timer();
			try {
				File tmpFile;
				try (RandomAccessFile raf = new RandomAccessFile(getFile(), "r")) {
					tmpFile = getData().deFragment(raf);
				}

				if (tmpFile != null) {
					Files.move(tmpFile.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
				}
			} catch (Exception ex) {
				Debug.dumpException("failed to export chunks for " + getFile().getName(), ex);
			}
			progressChannel.incrementProgress(getFile().getName());
			Debug.dumpf("took %s to save data to %s", t, getFile().getName());
		}
	}
}
