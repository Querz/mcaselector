package net.querz.mcaselector.io;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MCALoader {


	/*
	* header
	* 0-4095: locations (1024 int)
	* 4096-8191: timestamps (1024 int)
	*
	* */

	public static MCAFile read(File file, RandomAccessFile raf) {
		try {
			MCAFile mcaFile = new MCAFile(file);
			mcaFile.read(raf);
			System.out.println(mcaFile);

			return mcaFile;

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	public static void write(MCAFile mcaFile, MCAChunkData[] mcaChunkData, RandomAccessFile raf) throws Exception {
		mcaFile.write(mcaChunkData, raf);
	}

	public static void backup(MCAFile mcaFile) {
		//rename original file to r.x.x.mca_old
		Path oldFile = mcaFile.getFile().toPath();
		Path backupFile = oldFile.resolveSibling(mcaFile.getFile().getName() + "_old");
		try {
			Files.move(oldFile, backupFile);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	public static void restore(MCAFile mcaFile) {
		//rename backup file to r.x.x.mca
		Path oldFile = mcaFile.getFile().toPath();
		Path backupFile = oldFile.resolveSibling(mcaFile.getFile().getName() + "_old");
		try {
			Files.move(backupFile, oldFile);
		} catch (IOException ex) {
			ex.printStackTrace();
			System.out.println("error trying to restore backup file for " + mcaFile.getFile());
		}
	}
}
