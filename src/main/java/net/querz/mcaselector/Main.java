package net.querz.mcaselector;

import javafx.application.Application;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.RandomAccessFile;
import java.util.Arrays;

public class Main {

	public static void main(String[] args) throws Exception {
//		File[] files = new File("src/main/resources").listFiles((dir, name) -> name.endsWith(".mca"));
//		if (files == null) {
//			return;
//		}
//		for (File file : files) {
//			if (file.isDirectory()) {
//				continue;
//			}
//
//			MCALoader loader = new MCALoader();
//			MCAFile mcaFile = loader.read(file);
//
//			BufferedImage image = mcaFile.createImage(new Anvil112ChunkDataProcessor(), new Anvil112ColorMapping());
//
//			ImageIO.write(image, "png", new File("src/main/resources/out/" + file.getName().substring(0, file.getName().lastIndexOf('.')) + ".png"));
//		}
//

		Window.launch(Window.class, args);
	}
}
