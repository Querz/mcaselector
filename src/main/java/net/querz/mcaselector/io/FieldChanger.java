package net.querz.mcaselector.io;

import net.querz.mcaselector.Config;
import net.querz.mcaselector.changer.Field;
import net.querz.mcaselector.ui.ProgressTask;
import net.querz.mcaselector.util.Debug;
import net.querz.mcaselector.util.Point2i;
import net.querz.mcaselector.util.Timer;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FieldChanger {

	private static final Pattern regionGroupPattern = Pattern.compile("^r\\.(?<regionX>-?\\d+)\\.(?<regionZ>-?\\d+)\\.mca$");

	private FieldChanger() {}

	public static void changeNBTFields(List<Field> fields, boolean force, Map<Point2i, Set<Point2i>> selection, ProgressTask progressChannel) {
		File[] files = Config.getWorldDir().listFiles((d, n) -> n.matches("^r\\.-?\\d+\\.-?\\d+\\.mca$"));
		if (files == null || files.length == 0) {
			return;
		}

		MCAFilePipe.clearQueues();

		progressChannel.setMax(files.length);
		progressChannel.updateProgress(files[0].getName(), 0);

		for (File file : files) {
			MCAFilePipe.addJob(new MCAFieldChangeLoadJob(file, fields, force, selection, progressChannel));
		}
	}

	public static class MCAFieldChangeLoadJob extends LoadDataJob {

		private ProgressTask progressChannel;
		private List<Field> fields;
		private boolean force;
		private Map<Point2i, Set<Point2i>> selection;

		MCAFieldChangeLoadJob(File file, List<Field> fields, boolean force, Map<Point2i, Set<Point2i>> selection, ProgressTask progressChannel) {
			super(file);
			this.fields = fields;
			this.force = force;
			this.selection = selection;
			this.progressChannel = progressChannel;
		}

		@Override
		public void execute() {
			byte[] data = load();
			if (data != null) {
				Set<Point2i> chunks = null;
				if (selection != null) {
					Matcher m = regionGroupPattern.matcher(getFile().getName());
					if (m.find()) {
						int regionX = Integer.parseInt(m.group("regionX"));
						int regionZ = Integer.parseInt(m.group("regionZ"));
						Point2i location = new Point2i(regionX, regionZ);
						if (!selection.containsKey(location)) {
							Debug.dumpf("will not apply nbt changes to %s", getFile().getName());
							progressChannel.incrementProgress(getFile().getName());
							return;
						}
						chunks = selection.get(location);
					}
				}
				MCAFilePipe.executeProcessData(new MCAFieldChangeProcessJob(getFile(), data, fields, force, chunks, progressChannel));
			} else {
				Debug.errorf("error loading mca file %s", getFile().getName());
				progressChannel.incrementProgress(getFile().getName() + ": error");
			}
		}
	}

	public static class MCAFieldChangeProcessJob extends ProcessDataJob {

		private ProgressTask progressChannel;
		private List<Field> fields;
		private boolean force;
		private Set<Point2i> selection;

		MCAFieldChangeProcessJob(File file, byte[] data, List<Field> fields, boolean force, Set<Point2i> selection, ProgressTask progressChannel) {
			super(file, data);
			this.fields = fields;
			this.force = force;
			this.selection = selection;
			this.progressChannel = progressChannel;
		}

		@Override
		public void execute() {
			//load MCAFile
			Timer t = new Timer();
			try {
				MCAFile mca = MCAFile.readAll(getFile(), new ByteArrayPointer(getData()));
				mca.applyFieldChanges(fields, force, selection);
				Debug.dumpf("took %s to apply field changes to %s", t, getFile().getName());
				MCAFilePipe.executeSaveData(new MCAFieldChangeSaveJob(getFile(), mca, progressChannel));
			} catch (Exception ex) {
				progressChannel.incrementProgress(getFile().getName());
				Debug.errorf("error changing fields in %s", getFile().getName());
			}
		}
	}

	public static class MCAFieldChangeSaveJob extends SaveDataJob<MCAFile> {

		private ProgressTask progressChannel;

		MCAFieldChangeSaveJob(File file, MCAFile data, ProgressTask progressChannel) {
			super(file, data);
			this.progressChannel = progressChannel;
		}

		@Override
		public void execute() {
			Timer t = new Timer();
			try {
				File tmpFile = File.createTempFile(getFile().getName(), null, null);

				boolean empty;

				try (RandomAccessFile raf = new RandomAccessFile(tmpFile, "rw")) {
					empty = !getData().saveAll(raf);
				}

				if (empty) {
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
