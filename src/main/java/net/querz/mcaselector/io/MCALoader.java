package net.querz.mcaselector.io;

import net.querz.mcaselector.Config;
import net.querz.mcaselector.ui.ProgressTask;
import net.querz.mcaselector.filter.GroupFilter;
import net.querz.mcaselector.util.Debug;
import net.querz.mcaselector.util.Helper;
import net.querz.mcaselector.util.Point2i;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MCALoader {

	private MCALoader() {}

	public static MCAFile read(File file, ByteArrayPointer ptr) {
		try {
			MCAFile mcaFile = new MCAFile(file);
			mcaFile.readHeader(ptr);
			return mcaFile;
		} catch (Exception ex) {
			Debug.error(ex);
		}
		return null;
	}

	public static void deleteChunks(Map<Point2i, Set<Point2i>> chunksToBeDeleted, ProgressTask progressChannel) {
		deleteChunks(chunksToBeDeleted, progressChannel, Config.getWorldDir(), false);
	}

	public static void deleteChunks(Map<Point2i, Set<Point2i>> chunksToBeDeleted, ProgressTask progressChannel, File dir, boolean backup) {
		int regionCount = chunksToBeDeleted.size();
		int index = -1;
		for (Map.Entry<Point2i, Set<Point2i>> entry : chunksToBeDeleted.entrySet()) {
			index++;
			File file = new File(dir, Helper.createMCAFileName(entry.getKey()));

			progressChannel.updateProgress(file.getName(), index, regionCount);

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
					mcaFile = MCALoader.read(new File(dir, Helper.createMCAFileName(entry.getKey())), new ByteArrayPointer(null));

					if (mcaFile == null) {
						Debug.error("error reading " + file + ", skipping");
						continue;
					}

					mcaFile.deleteChunkIndices(entry.getValue());
					File tmpFile = mcaFile.deFragment(raf);
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
					if (backup && mcaFile != null) {
						restore(mcaFile);
					}
				}
			}
		}
		progressChannel.updateProgress("Done", 1, regionCount);
	}

	public static void deleteChunks(GroupFilter filter, ProgressTask progressChannel) {
		deleteChunks(filter, progressChannel, Config.getWorldDir(), false);
	}

	public static void deleteChunks(GroupFilter filter, ProgressTask progressChannel, File dir, boolean backup) {
		File[] files = dir.listFiles((d, n) -> n.matches("^r\\.-?\\d+\\.-?\\d+\\.mca$"));
		if (files == null) {
			return;
		}
		int filesCount = files.length;
		for (int i = 0; i < files.length; i++) {
			File file = files[i];

			progressChannel.updateProgress(file.getName(), i, filesCount);

			Pattern p = Pattern.compile("^r\\.(?<regionX>-?\\d+)\\.(?<regionZ>-?\\d+)\\.mca$");
			Matcher m = p.matcher(file.getName());
			if (m.find()) {
				int regionX = Integer.parseInt(m.group("regionX"));
				int regionZ = Integer.parseInt(m.group("regionZ"));

				if (!filter.appliesToRegion(new Point2i(regionX, regionZ))) {
					Debug.dump("filter does not apply to file " + file);
					continue;
				}

				deleteChunks(filter, file, backup);

			} else {
				Debug.dump("skipping " + file + ", could not parse file name");
			}
		}
		progressChannel.updateProgress("Done", 1, filesCount);
	}

	public static void deleteChunks(GroupFilter filter, File file, boolean backup) {
		if (backup) {
			Debug.dump("creating backup of " + file);
			backup(file);
		}

		MCAFile mcaFile = null;

		try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
			mcaFile = MCALoader.read(file, new ByteArrayPointer(null));

			if (mcaFile == null) {
				Debug.error("error reading " + file + ", skipping");
				return;
			}

			mcaFile.deleteChunkIndices(filter, raf);
			File tmpFile = mcaFile.deFragment(raf);
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
			if (backup && mcaFile != null) {
				restore(mcaFile);
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
