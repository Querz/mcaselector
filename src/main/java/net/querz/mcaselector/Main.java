package net.querz.mcaselector;

import java.io.File;
import java.io.RandomAccessFile;

public class Main {

	public static void main(String[] args) throws Exception {
		MCALoader loader = new MCALoader();
		MCAFile file = loader.read(new File("src/main/resources/r.-1.0.mca"));

		try (
			RandomAccessFile raf = new RandomAccessFile(file.getFile(), "r")
		) {
			for (int i = 0; i < 1024; i++) {

				MCAChunkData data = file.getChunkData(i);

				data.readHeader(raf);
				System.out.println(data.getLength() + " " + data.getCompressionType());

				try {
					data.loadData(raf);
				} catch (Exception ex) {
					System.out.println(ex.getMessage());
				}
			}
		}
	}
}
