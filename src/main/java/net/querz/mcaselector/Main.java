package net.querz.mcaselector;

import javafx.application.Application;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.RandomAccessFile;
import java.util.Arrays;

public class Main {

	public static void main(String[] args) throws Exception {
//		MCALoader loader = new MCALoader();
//		MCAFile file = loader.read(new File("src/main/resources/r.-4.-1.mca"));
//
//		BufferedImage image = file.createImage(new Anvil112ChunkDataProcessor(), new Anvil112ColorMapping());
//
//		ImageIO.write(image, "png", new File("src/main/resources/out/test.png"));

		Window.launch(Window.class, args);
	}
}
