package net.querz.mcaselector;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;

public class MCALoader {


	/*
	* header
	* 0-4095: locations (1024 int)
	* 4096-8191: timestamps (1024 int)
	*
	* */

	public MCAFile read(File file) {
		try (
			FileInputStream fis = new FileInputStream(file);
			DataInputStream dis = new DataInputStream(fis)
		) {
			MCAFile mcaFile = new MCAFile(file);
			mcaFile.read(dis);
			System.out.println(mcaFile);

			return mcaFile;

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}
}
