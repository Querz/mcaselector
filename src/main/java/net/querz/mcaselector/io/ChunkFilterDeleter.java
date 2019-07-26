package net.querz.mcaselector.io;

import net.querz.mcaselector.Config;
import net.querz.mcaselector.filter.GroupFilter;
import net.querz.mcaselector.util.Debug;
import net.querz.mcaselector.util.Helper;
import net.querz.mcaselector.util.Point2i;
import net.querz.mcaselector.util.Progress;
import net.querz.mcaselector.util.Timer;
import net.querz.mcaselector.util.Translation;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

public class ChunkFilterDeleter {

	private ChunkFilterDeleter() {}

	public static void deleteFilter(GroupFilter filter, Map<Point2i, Set<Point2i>> selection, Progress progressChannel) {
		File[] files = Config.getWorldDir().listFiles((d, n) -> n.matches(Helper.MCA_FILE_PATTERN));
		if (files == null || files.length == 0) {
			progressChannel.done(Translation.DIALOG_PROGRESS_NO_FILES.toString());
			return;
		}

		MCAFilePipe.clearQueues();

		progressChannel.setMax(files.length);
		progressChannel.updateProgress(files[0].getName(), 0);

		for (File file : files) {
			MCAFilePipe.addJob(new MCADeleteFilterLoadJob(file, filter, selection, progressChannel));
		}
	}

	public static class MCADeleteFilterLoadJob extends LoadDataJob {

		private GroupFilter filter;
		private Map<Point2i, Set<Point2i>> selection;
		private Progress progressChannel;

		MCADeleteFilterLoadJob(File file, GroupFilter filter, Map<Point2i, Set<Point2i>> selection, Progress progressChannel) {
			super(file);
			this.filter = filter;
			this.selection = selection;
			this.progressChannel = progressChannel;
		}

		@Override
		public void execute() {
			Matcher m = Helper.REGION_GROUP_PATTERN.matcher(getFile().getName());
			if (m.find()) {
				int regionX = Integer.parseInt(m.group("regionX"));
				int regionZ = Integer.parseInt(m.group("regionZ"));
				Point2i location = new Point2i(regionX, regionZ);

				if (!filter.appliesToRegion(location) || selection != null && !selection.containsKey(location)) {
					Debug.dump("filter does not apply to file " + getFile().getName());
					progressChannel.incrementProgress(getFile().getName());
					return;
				}

				byte[] data = load();
				if (data != null) {
					MCAFilePipe.executeProcessData(new MCADeleteFilterProcessJob(getFile(), data, filter, selection != null ? selection.get(location) : null, progressChannel));
				} else {
					Debug.errorf("error loading mca file %s", getFile().getName());
					progressChannel.incrementProgress(getFile().getName());
				}
			} else {
				Debug.dump("wtf, how did we get here??");
				progressChannel.incrementProgress(getFile().getName());
			}
		}
	}

	public static class MCADeleteFilterProcessJob extends ProcessDataJob {

		private Progress progressChannel;
		private GroupFilter filter;
		private Set<Point2i> selection;

		MCADeleteFilterProcessJob(File file, byte[] data, GroupFilter filter, Set<Point2i> selection, Progress progressChannel) {
			super(file, data);
			this.filter = filter;
			this.selection = selection;
			this.progressChannel = progressChannel;
		}

		@Override
		public void execute() {
			//load MCAFile
			Timer t = new Timer();
			try {
				MCAFile mca = MCAFile.readAll(getFile(), new ByteArrayPointer(getData()));
				if (mca != null) {
					mca.deleteChunkIndices(filter, selection);
					Debug.dumpf("took %s to delete chunk indices in %s", t, getFile().getName());
					MCAFilePipe.executeSaveData(new MCADeleteFilterSaveJob(getFile(), mca, progressChannel));
				}
			} catch (Exception ex) {
				progressChannel.incrementProgress(getFile().getName());
				Debug.errorf("error deleting chunk indices in %s", getFile().getName());
			}
		}
	}

	public static class MCADeleteFilterSaveJob extends SaveDataJob<MCAFile> {

		private Progress progressChannel;

		MCADeleteFilterSaveJob(File file, MCAFile data, Progress progressChannel) {
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
