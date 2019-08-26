package net.querz.mcaselector.io;

import net.querz.mcaselector.util.Debug;
import net.querz.mcaselector.util.FileHelper;
import net.querz.mcaselector.util.Helper;
import net.querz.mcaselector.util.Point2i;
import net.querz.mcaselector.util.Progress;
import net.querz.mcaselector.util.Timer;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Set;

public class SelectionDeleter {

	private SelectionDeleter() {}

	public static void deleteSelection(Map<Point2i, Set<Point2i>> chunksToBeDeleted, Progress progressChannel) {
		if (chunksToBeDeleted.isEmpty()) {
			progressChannel.done("no selection");
			return;
		}

		MCAFilePipe.clearQueues();

		progressChannel.setMax(chunksToBeDeleted.size());
		Point2i first = chunksToBeDeleted.entrySet().iterator().next().getKey();
		progressChannel.updateProgress(FileHelper.createMCAFileName(first), 0);

		for (Map.Entry<Point2i, Set<Point2i>> entry : chunksToBeDeleted.entrySet()) {
			MCAFilePipe.addJob(new MCADeleteSelectionLoadJob(FileHelper.createMCAFilePath(entry.getKey()), entry.getValue(), progressChannel));
		}
	}

	public static class MCADeleteSelectionLoadJob extends LoadDataJob {

		private Set<Point2i> chunksToBeDeleted;
		private Progress progressChannel;

		MCADeleteSelectionLoadJob(File file, Set<Point2i> chunksToBeDeleted, Progress progressChannel) {
			super(file);
			this.chunksToBeDeleted = chunksToBeDeleted;
			this.progressChannel = progressChannel;
		}

		@Override
		public void execute() {
			if (chunksToBeDeleted == null) {
				if (getFile().delete()) {
					Debug.dumpf("deleted file %s", getFile().getName());
				} else {
					Debug.errorf("could not delete file %s", getFile().getName());
				}
				progressChannel.incrementProgress(getFile().getName());
				return;
			}
			byte[] data = load(MCAFile.SECTION_SIZE * 2); //load header only
			if (data != null) {
				MCAFilePipe.executeProcessData(new MCADeleteSelectionProcessJob(getFile(), data, chunksToBeDeleted, progressChannel));
			} else {
				Debug.errorf("error loading mca file %s", getFile().getName());
				progressChannel.incrementProgress(getFile().getName());
			}
		}
	}

	public static class MCADeleteSelectionProcessJob extends ProcessDataJob {

		private Progress progressChannel;
		private Set<Point2i> chunksToBeDeleted;

		MCADeleteSelectionProcessJob(File file, byte[] data, Set<Point2i> chunksToBeDeleted, Progress progressChannel) {
			super(file, data);
			this.chunksToBeDeleted = chunksToBeDeleted;
			this.progressChannel = progressChannel;
		}

		@Override
		public void execute() {
			//load MCAFile
			Timer t = new Timer();
			try {
				MCAFile mca = MCAFile.readHeader(getFile(), new ByteArrayPointer(getData()));
				if (mca != null) {
					mca.deleteChunkIndices(chunksToBeDeleted);
					Debug.dumpf("took %s to delete chunk indices in %s", t, getFile().getName());
					MCAFilePipe.executeSaveData(new MCADeleteSelectionSaveJob(getFile(), mca, progressChannel));
				}
			} catch (Exception ex) {
				progressChannel.incrementProgress(getFile().getName());
				Debug.errorf("error deleting chunk indices in %s", getFile().getName());
			}
		}
	}

	public static class MCADeleteSelectionSaveJob extends SaveDataJob<MCAFile> {

		private Progress progressChannel;

		MCADeleteSelectionSaveJob(File file, MCAFile data, Progress progressChannel) {
			super(file, data);
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

				if (tmpFile == null) {
					if (getFile().delete()) {
						Debug.dumpf("deleted empty region file %s", getFile().getAbsolutePath());
					} else {
						Debug.dumpf("could not delete empty region file %s", getFile().getAbsolutePath());
					}
				} else {
					Files.move(tmpFile.toPath(), getFile().toPath(), StandardCopyOption.REPLACE_EXISTING);
				}
			} catch (Exception ex) {
				Debug.error(ex);
			}
			progressChannel.incrementProgress(getFile().getName());
			Debug.dumpf("took %s to save data to %s", t, getFile().getName());
		}
	}
}
