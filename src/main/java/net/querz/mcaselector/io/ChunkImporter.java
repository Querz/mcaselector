package net.querz.mcaselector.io;

import net.querz.mcaselector.Config;
import net.querz.mcaselector.ui.ProgressTask;
import net.querz.mcaselector.util.Debug;
import net.querz.mcaselector.util.Helper;
import net.querz.mcaselector.util.Timer;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class ChunkImporter {

	private ChunkImporter() {}

	public static void importChunks(File importDir, ProgressTask progressChannel) {
		File[] importFiles = importDir.listFiles((dir, name) -> name.matches(Helper.MCA_FILE_PATTERN));
		if (importFiles == null || importFiles.length == 0) {
			progressChannel.done("no files");
			return;
		}

		System.out.println("going to import chunks from " + importFiles.length + " files");

		MCAFilePipe.clearQueues();

		progressChannel.setMax(importFiles.length);
		progressChannel.updateProgress(importFiles[0].getName(), 0);

		for (File file : importFiles) {
			MCAFilePipe.addJob(new MCAChunkImporterLoadJob(file, progressChannel));
		}
	}

	public static class MCAChunkImporterLoadJob extends LoadDataJob {

		private ProgressTask progressChannel;

		MCAChunkImporterLoadJob(File file, ProgressTask progressChannel) {
			super(file);
			this.progressChannel = progressChannel;
		}

		@Override
		public void execute() {
			File dest = new File(Config.getWorldDir(), getFile().getName());

			if (!dest.exists()) {
				//if the entire mca file doesn't exist, just copy it over
				try {
					Files.copy(getFile().toPath(), dest.toPath());
				} catch (IOException ex) {
					Debug.errorf("failed to copy file %s to %s: %s", getFile(), dest, ex.getMessage());
				}
				progressChannel.incrementProgress(getFile().getName());
				return;
			}

			// load both files
			byte[] sourceData = load();
			byte[] destData = load(dest);

			if (sourceData == null) {
				Debug.errorf("error loading source mca file %s", getFile().getName());
				progressChannel.incrementProgress(getFile().getName());
				return;
			}

			if (destData == null) {
				Debug.errorf("error loading destination mca file %s", getFile().getName());
				progressChannel.incrementProgress(getFile().getName());
				return;
			}

			MCAFilePipe.executeProcessData(new MCAChunkImporterProcessJob(getFile(), dest, sourceData, destData, progressChannel));
		}
	}

	public static class MCAChunkImporterProcessJob extends ProcessDataJob {

		private File destFile;
		private byte[] destData;
		private ProgressTask progressChannel;

		MCAChunkImporterProcessJob(File sourceFile, File destFile, byte[] sourceData, byte[] destData, ProgressTask progressChannel) {
			super(sourceFile, sourceData);
			this.destFile = destFile;
			this.destData = destData;
			this.progressChannel = progressChannel;
		}

		@Override
		public void execute() {
			//load MCAFiles
			Timer t = new Timer();
			try {
				MCAFile source = MCAFile.readAll(getFile(), new ByteArrayPointer(getData()));
				if (source == null) {
					progressChannel.incrementProgress(getFile().getName());
					Debug.errorf("failed to load source MCAFile %s", getFile().getName());
					return;
				}

				MCAFile dest = MCAFile.readAll(destFile, new ByteArrayPointer(destData));
				if (dest == null) {
					progressChannel.incrementProgress(destFile.getName());
					Debug.errorf("failed to load destination MCAFile %s", destFile.getName());
					return;
				}

				source.mergeChunksInto(dest, true); // TODO: overwrite

				MCAFilePipe.executeSaveData(new MCAChunkImporterSaveJob(destFile, dest, progressChannel));

			} catch (Exception ex) {
				Debug.errorf("error merging chunks from %s into %s: %s", getFile(), destFile, ex.getMessage());
				progressChannel.incrementProgress(getFile().getName());
			}
			Debug.dumpf("took %s to merge %s into %s", t, getFile(), destFile.getName());
		}
	}

	public static class MCAChunkImporterSaveJob extends SaveDataJob<MCAFile> {

		private ProgressTask progressChannel;

		MCAChunkImporterSaveJob(File file, MCAFile data, ProgressTask progressChannel) {
			super(file, data);
			this.progressChannel = progressChannel;
		}

		@Override
		public void execute() {
			Timer t = new Timer();
			try {
				File tmpFile = File.createTempFile(getFile().getName(), null, null);
				try (RandomAccessFile raf = new RandomAccessFile(tmpFile, "rw")) {
					getData().saveAll(raf);
				}
				Files.move(tmpFile.toPath(), getFile().toPath(), StandardCopyOption.REPLACE_EXISTING);
			} catch (Exception ex) {
				Debug.error(ex);
			}
			progressChannel.incrementProgress(getFile().getName());
			Debug.dumpf("took %s to save data to %s", t, getFile().getName());
		}
	}
}
