package net.querz.mcaselector.io;

import net.querz.mcaselector.Config;
import net.querz.mcaselector.changer.Field;
import net.querz.mcaselector.ui.ProgressTask;
import net.querz.mcaselector.util.Debug;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class FieldChanger {

	public static void changeNBTFields(List<Field> fields, boolean force, ProgressTask progressChannel) {
		File[] files = Config.getWorldDir().listFiles((d, n) -> n.matches("^r\\.-?\\d+\\.-?\\d+\\.mca$"));
		if (files == null) {
			return;
		}
		double filesCount = files.length;
		for (int i = 0; i < files.length; i++) {
			File file = files[i];

			progressChannel.updateProgress(file.getName(), i, filesCount);


			MCAFile mcaFile;

			try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
				mcaFile = MCALoader.read(file, raf);

				if (mcaFile == null) {
					Debug.error("error reading " + file + ", skipping");
					return;
				}

				File tmpFile = mcaFile.applyFieldChanges(fields, force, raf);
				raf.close();

				//delete region file if it's empty, otherwise replace it with tmpFile
				if (tmpFile == null) {
					if (file.delete()) {
						Debug.dump("deleted empty region file " + file);
					} else {
						Debug.dump("could not delete empty region file " + file);
					}
				} else {
					Files.move(tmpFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
				}

			} catch (Exception ex) {
				Debug.error(ex);
			}
		}
		progressChannel.updateProgress("Done", 1, filesCount);
	}
}
