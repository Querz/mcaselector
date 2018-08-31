package net.querz.mcaselector.io;

import net.querz.mcaselector.ui.ProgressTask;
import net.querz.mcaselector.tiles.Tile;
import net.querz.mcaselector.util.Debug;
import net.querz.mcaselector.util.Helper;
import net.querz.mcaselector.util.Point2i;
import net.querz.mcaselector.util.Timer;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SelectionExporter {

	private SelectionExporter() {}

	public static void exportSelection(Map<Point2i, Set<Point2i>> chunksToBeExported, File destination, ProgressTask progressChannel) {
		if (chunksToBeExported.isEmpty()) {
			return;
		}

		progressChannel.setMax(chunksToBeExported.size());
		Point2i first = chunksToBeExported.entrySet().iterator().next().getKey();
		progressChannel.updateProgress(Helper.createMCAFileName(first), 0);

		for (Map.Entry<Point2i, Set<Point2i>> entry : chunksToBeExported.entrySet()) {
			MCAFilePipe.addJob(new MCADeleteSelectionLoadJob(
					Helper.createMCAFilePath(entry.getKey()),
					entry.getValue(),
					new File(destination,Helper.createMCAFileName(entry.getKey())),
					progressChannel));
		}
	}

	public static class MCADeleteSelectionLoadJob extends LoadDataJob {

		private Set<Point2i> chunksToBeExported;
		private File destination;
		private ProgressTask progressChannel;

		MCADeleteSelectionLoadJob(File file, Set<Point2i> chunksToBeExported, File destination, ProgressTask progressChannel) {
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
					Debug.errorf("error moving file: ", getFile().getName());
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

	public static class MCADeleteSelectionProcessJob extends ProcessDataJob {

		private ProgressTask progressChannel;
		private Set<Point2i> chunksToBeExported;
		private File destination;

		MCADeleteSelectionProcessJob(File file, byte[] data, Set<Point2i> chunksToBeExported, File destination, ProgressTask progressChannel) {
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
					Point2i origin = Helper.regionToChunk(Helper.chunkToRegion(chunksToBeExported.iterator().next()));
					System.out.println("origin: " + origin);
					for (int x = origin.getX(); x < origin.getX() + Tile.SIZE_IN_CHUNKS; x++) {
						for (int z = origin.getY(); z < origin.getY() + Tile.SIZE_IN_CHUNKS; z++) {
							Point2i cp = new Point2i(x, z);
							if (!chunksToBeExported.contains(cp)) {
								System.out.println("added inverted chunk: " + cp);
								inverted.add(cp);
							}
						}
					}

					mca.deleteChunkIndices(inverted);
					Debug.dumpf("took %s to delete chunk indices in %s", t, getFile().getName());
					MCAFilePipe.executeSaveData(new MCADeleteSelectionSaveJob(getFile(), mca, destination, progressChannel));
				}
			} catch (Exception ex) {
				ex.printStackTrace();
				progressChannel.incrementProgress(getFile().getName());
				Debug.errorf("error deleting chunk indices in %s: %s", getFile().getName(), ex.getMessage());
			}
		}
	}

	public static class MCADeleteSelectionSaveJob extends SaveDataJob<MCAFile> {

		private ProgressTask progressChannel;
		private File destination;

		MCADeleteSelectionSaveJob(File file, MCAFile data, File destination, ProgressTask progressChannel) {
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
				Debug.error(ex);
			}
			progressChannel.incrementProgress(getFile().getName());
			Debug.dumpf("took %s to save data to %s", t, getFile().getName());
		}
	}
}
