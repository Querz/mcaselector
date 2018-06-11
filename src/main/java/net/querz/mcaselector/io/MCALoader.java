package net.querz.mcaselector.io;

import net.querz.mcaselector.Config;
import net.querz.mcaselector.util.Debug;
import net.querz.mcaselector.util.Helper;
import net.querz.mcaselector.util.Point2i;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Set;

public class MCALoader {

	private MCALoader() {}

	public static MCAFile read(File file, RandomAccessFile raf) {
		try {
			MCAFile mcaFile = new MCAFile(file);
			mcaFile.read(raf);
			return mcaFile;
		} catch (Exception ex) {
			Debug.error(ex);
		}
		return null;
	}

	public static void deleteChunks(Map<Point2i, Set<Point2i>> chunksToBeDeleted) {
		deleteChunks(chunksToBeDeleted, Config.getWorldDir(), false);
	}

	public static void deleteChunks(Map<Point2i, Set<Point2i>> chunksToBeDeleted, File dir, boolean backup) {
		for (Map.Entry<Point2i, Set<Point2i>> entry : chunksToBeDeleted.entrySet()) {
			File file = new File(dir, Helper.createMCAFileName(entry.getKey()));
			//delete region

			if (backup) {
				Debug.dump("creating backup of " + file);
				backup(file);
			}

			if (entry.getValue() == null) {
				try {
					Files.deleteIfExists(file.toPath());
				} catch (IOException ex) {
					Debug.error(ex);
				}
			} else {
				MCAFile mcaFile = null;

				try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
					mcaFile = MCALoader.read(new File(dir, Helper.createMCAFileName(entry.getKey())), raf);

					if (mcaFile == null) {
						Debug.error("error reading " + file + ", skipping");
						continue;
					}

					mcaFile.deleteChunkIndices(entry.getValue());
					File tmpFile = mcaFile.deFragment(raf);
					raf.close();
					Files.move(tmpFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
				} catch (Exception ex) {
					Debug.error(ex);
					if (backup && mcaFile != null) {
						restore(mcaFile);
					}
				}
			}
		}
	}

	private static void backup(File mcaFile) {
		//rename original file to r.x.x.mca_old
		Path oldFile = mcaFile.toPath();
		Path backupFile = oldFile.resolveSibling(mcaFile.getName() + "_old");
		if (backupFile.toFile().exists()) {
			Debug.dump("backup file " + backupFile + " already exists");
			return;
		}
		try {
			Files.copy(oldFile, backupFile);
		} catch (IOException ex) {
			Debug.error("error trying to create backup file for " + mcaFile, ex);
		}
	}

	private static void restore(MCAFile mcaFile) {
		//rename backup file to r.x.x.mca
		Path oldFile = mcaFile.getFile().toPath();
		Path backupFile = oldFile.resolveSibling(mcaFile.getFile().getName() + "_old");
		try {
			Files.move(backupFile, oldFile);
		} catch (IOException ex) {
			Debug.error("error trying to restore backup file for " + mcaFile.getFile(), ex);
		}
	}
}
