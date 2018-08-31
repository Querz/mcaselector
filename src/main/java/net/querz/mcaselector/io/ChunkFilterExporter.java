package net.querz.mcaselector.io;

import net.querz.mcaselector.Config;
import net.querz.mcaselector.filter.GroupFilter;
import net.querz.mcaselector.ui.ProgressTask;
import net.querz.mcaselector.util.Debug;
import net.querz.mcaselector.util.Point2i;
import net.querz.mcaselector.util.Timer;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChunkFilterExporter {

	private static final Pattern regionGroupPattern = Pattern.compile("^r\\.(?<regionX>-?\\d+)\\.(?<regionZ>-?\\d+)\\.mca$");

	private ChunkFilterExporter() {}

	public static void exportFilter(GroupFilter filter, File destination, ProgressTask progressChannel) {
		File[] files = Config.getWorldDir().listFiles((d, n) -> n.matches("^r\\.-?\\d+\\.-?\\d+\\.mca$"));
		if (files == null || files.length == 0) {
			return;
		}

		progressChannel.setMax(files.length);
		progressChannel.updateProgress(files[0].getName(), 0);

		GroupFilter cloneFilter = filter.clone();
		cloneFilter.setInverted(true);

		for (File file : files) {
			MCAFilePipe.addJob(new MCAExportFilterLoadJob(file, cloneFilter, destination, progressChannel));
		}
	}

	public static class MCAExportFilterLoadJob extends LoadDataJob {

		private GroupFilter filter;
		private ProgressTask progressChannel;
		private File destination;

		MCAExportFilterLoadJob(File file, GroupFilter filter, File destination, ProgressTask progressChannel) {
			super(file);
			this.filter = filter;
			this.destination = destination;
			this.progressChannel = progressChannel;
		}

		@Override
		public void execute() {
			Matcher m = regionGroupPattern.matcher(getFile().getName());
			if (m.find()) {
				int regionX = Integer.parseInt(m.group("regionX"));
				int regionZ = Integer.parseInt(m.group("regionZ"));

				if (!filter.appliesToRegion(new Point2i(regionX, regionZ))) {
					Debug.dump("filter does not apply to file " + getFile().getName());
					progressChannel.incrementProgress(getFile().getName());
					return;
				}

				//copy file to new directory
				File to = new File(destination, getFile().getName());
				if (to.exists()) {
					Debug.dump(to.getAbsolutePath() + " exists, not overwriting");
					progressChannel.incrementProgress(getFile().getName());
					return;
				}

				byte[] data = load();
				if (data != null) {
					MCAFilePipe.executeProcessData(new MCAExportFilterProcessJob(getFile(), data, filter, to, progressChannel));
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

	public static class MCAExportFilterProcessJob extends ProcessDataJob {

		private ProgressTask progressChannel;
		private GroupFilter filter;
		private File destination;

		MCAExportFilterProcessJob(File file, byte[] data, GroupFilter filter, File destination, ProgressTask progressChannel) {
			super(file, data);
			this.filter = filter;
			this.destination = destination;
			this.progressChannel = progressChannel;
		}

		@Override
		public void execute() {
			//load MCAFile
			Timer t = new Timer();
			try {
				MCAFile mca = MCAFile.readAll(getFile(), new ByteArrayPointer(getData()));
				if (mca != null) {
					mca.deleteChunkIndices(filter);
					Debug.dumpf("took %s to delete chunk indices in %s", t, getFile().getName());
					MCAFilePipe.executeSaveData(new MCAExportFilterSaveJob(getFile(), mca, destination, progressChannel));
				}
			} catch (Exception ex) {
				progressChannel.incrementProgress(getFile().getName());
				Debug.errorf("error deleting chunk indices in %s", getFile().getName());
			}
		}
	}

	public static class MCAExportFilterSaveJob extends SaveDataJob<MCAFile> {

		private File destination;
		private ProgressTask progressChannel;

		MCAExportFilterSaveJob(File file, MCAFile data, File destination, ProgressTask progressChannel) {
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
