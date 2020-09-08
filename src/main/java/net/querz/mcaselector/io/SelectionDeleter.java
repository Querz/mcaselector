package net.querz.mcaselector.io;

import net.querz.mcaselector.Config;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.progress.Progress;
import net.querz.mcaselector.progress.Timer;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Set;

public class SelectionDeleter {

	private SelectionDeleter() {}

	public static void deleteSelection(SelectionData selection, Progress progressChannel) {
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
			MCAFilePipe.addJob(new MCADeleteSelectionLoadJob(FileHelper.createMCAFilePath(entry.getKey()), entry.getValue(), progressChannel));
		}
	}

	private static class MCADeleteSelectionLoadJob extends LoadDataJob {

		private final Set<Point2i> selection;
		private final Progress progressChannel;

		private MCADeleteSelectionLoadJob(File file, Set<Point2i> selection, Progress progressChannel) {
			super(file);
			this.selection = selection;
			this.progressChannel = progressChannel;
		}

		@Override
		public void execute() {
			if (selection == null) {
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
				MCAFilePipe.executeProcessData(new MCADeleteSelectionProcessJob(getFile(), data, selection, progressChannel));
			} else {
				Debug.errorf("error loading mca file %s", getFile().getName());
				progressChannel.incrementProgress(getFile().getName());
			}
		}
	}

	private static class MCADeleteSelectionProcessJob extends ProcessDataJob {

		private final Progress progressChannel;
		private final Set<Point2i> selection;

		private MCADeleteSelectionProcessJob(File file, byte[] data, Set<Point2i> selection, Progress progressChannel) {
			super(file, data);
			this.selection = selection;
			this.progressChannel = progressChannel;
		}

		@Override
		public void execute() {
			//load MCAFile
			Timer t = new Timer();
			try {
				MCAFile mca = MCAFile.readHeader(getFile(), new ByteArrayPointer(getData()));
				if (mca != null) {
					mca.deleteChunkIndices(selection);
					Debug.dumpf("took %s to delete chunk indices in %s", t, getFile().getName());
					MCAFilePipe.executeSaveData(new MCADeleteSelectionSaveJob(getFile(), mca, progressChannel));
				}
			} catch (Exception ex) {
				progressChannel.incrementProgress(getFile().getName());
				Debug.errorf("error deleting chunk indices in %s", getFile().getName());
			}
		}
	}

	private static class MCADeleteSelectionSaveJob extends SaveDataJob<MCAFile> {

		private final Progress progressChannel;

		private MCADeleteSelectionSaveJob(File file, MCAFile data, Progress progressChannel) {
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
				Debug.dumpException("failed to delete selected chunk from " + getFile().getName(), ex);
			}
			progressChannel.incrementProgress(getFile().getName());
			Debug.dumpf("took %s to save data to %s", t, getFile().getName());
		}
	}
}
