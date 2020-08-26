package net.querz.mcaselector.tiles;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import junit.framework.TestCase;
import net.querz.mcaselector.Config;
import net.querz.mcaselector.point.Point2i;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.UUID;
import static org.junit.Assert.*;

public class TileImageTest extends TestCase {

	public void testGenerateImage112() throws IOException {
		Config.setShade(true);
		Config.setShadeWater(true);
		byte[] data = loadDataFromResource("anvil112/r.0.0.mca");
		Image image = TileImage.generateImage(
				new Tile(new Point2i(0, 0)),
				UUID.randomUUID(),
				(i, u) -> {},
				() -> 1.0f,
				data
		);
		assertImageEquals("anvil112/r.0.0.png", image);

		Config.setShadeWater(false);
		image = TileImage.generateImage(
				new Tile(new Point2i(0, 0)),
				UUID.randomUUID(),
				(i, u) -> {},
				() -> 1.0f,
				data
		);
		assertImageEquals("anvil112/r.0.0-no_water_shade.png", image);

		Config.setShade(false);
		image = TileImage.generateImage(
				new Tile(new Point2i(0, 0)),
				UUID.randomUUID(),
				(i, u) -> {},
				() -> 1.0f,
				data
		);
		assertImageEquals("anvil112/r.0.0-no_shade.png", image);
	}

	public void testGenerateImage113() throws IOException {
		Config.setShade(true);
		Config.setShadeWater(true);
		byte[] data = loadDataFromResource("anvil113/r.0.0.mca");
		Image image = TileImage.generateImage(
				new Tile(new Point2i(0, 0)),
				UUID.randomUUID(),
				(i, u) -> {},
				() -> 1.0f,
				data
		);
		assertImageEquals("anvil113/r.0.0.png", image);

		Config.setShadeWater(false);
		image = TileImage.generateImage(
				new Tile(new Point2i(0, 0)),
				UUID.randomUUID(),
				(i, u) -> {},
				() -> 1.0f,
				data
		);
		assertImageEquals("anvil113/r.0.0-no_water_shade.png", image);

		Config.setShade(false);
		image = TileImage.generateImage(
				new Tile(new Point2i(0, 0)),
				UUID.randomUUID(),
				(i, u) -> {},
				() -> 1.0f,
				data
		);
		assertImageEquals("anvil113/r.0.0-no_shade.png", image);
	}

	public void testGenerateImage114() throws IOException {
		Config.setShade(true);
		Config.setShadeWater(true);
		byte[] data = loadDataFromResource("anvil114/r.0.0.mca");
		Image image = TileImage.generateImage(
				new Tile(new Point2i(0, 0)),
				UUID.randomUUID(),
				(i, u) -> {},
				() -> 1.0f,
				data
		);
		assertImageEquals("anvil114/r.0.0.png", image);

		Config.setShadeWater(false);
		image = TileImage.generateImage(
				new Tile(new Point2i(0, 0)),
				UUID.randomUUID(),
				(i, u) -> {},
				() -> 1.0f,
				data
		);
		assertImageEquals("anvil114/r.0.0-no_water_shade.png", image);

		Config.setShade(false);
		image = TileImage.generateImage(
				new Tile(new Point2i(0, 0)),
				UUID.randomUUID(),
				(i, u) -> {},
				() -> 1.0f,
				data
		);
		assertImageEquals("anvil114/r.0.0-no_shade.png", image);
	}

	public void testGenerateImage115() throws IOException {
		Config.setShade(true);
		Config.setShadeWater(true);
		byte[] data = loadDataFromResource("anvil115/r.0.0.mca");
		Image image = TileImage.generateImage(
				new Tile(new Point2i(0, 0)),
				UUID.randomUUID(),
				(i, u) -> {},
				() -> 1.0f,
				data
		);
		assertImageEquals("anvil115/r.0.0.png", image);

		Config.setShadeWater(false);
		image = TileImage.generateImage(
				new Tile(new Point2i(0, 0)),
				UUID.randomUUID(),
				(i, u) -> {},
				() -> 1.0f,
				data
		);
		assertImageEquals("anvil115/r.0.0-no_water_shade.png", image);

		Config.setShade(false);
		image = TileImage.generateImage(
				new Tile(new Point2i(0, 0)),
				UUID.randomUUID(),
				(i, u) -> {},
				() -> 1.0f,
				data
		);
		assertImageEquals("anvil115/r.0.0-no_shade.png", image);
	}

	public void testGenerateImage116() throws IOException {
		Config.setShade(true);
		Config.setShadeWater(true);
		byte[] data = loadDataFromResource("anvil116/r.0.0.mca");
		Image image = TileImage.generateImage(
				new Tile(new Point2i(0, 0)),
				UUID.randomUUID(),
				(i, u) -> {},
				() -> 1.0f,
				data
		);
		assertImageEquals("anvil116/r.0.0.png", image);

		Config.setShadeWater(false);
		image = TileImage.generateImage(
				new Tile(new Point2i(0, 0)),
				UUID.randomUUID(),
				(i, u) -> {},
				() -> 1.0f,
				data
		);
		assertImageEquals("anvil116/r.0.0-no_water_shade.png", image);

		Config.setShade(false);
		image = TileImage.generateImage(
				new Tile(new Point2i(0, 0)),
				UUID.randomUUID(),
				(i, u) -> {},
				() -> 1.0f,
				data
		);
		assertImageEquals("anvil116/r.0.0-no_shade.png", image);
	}

	private byte[] loadDataFromResource(String resource) throws IOException {
		return Files.readAllBytes(getResourceFile(resource).toPath());
	}

	private void writeImage(Image image, File file) throws IOException {
		if (!file.getParentFile().exists()) {
			file.getParentFile().mkdirs();
		}
		BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);
		ImageIO.write(bufferedImage, "png", file);
	}

	private void assertImageEquals(String expected, Image actual) throws IOException {
		Image expectedImage = SwingFXUtils.toFXImage(ImageIO.read(getResourceFile(expected)), null);
		assertArrayEquals(getImageData(expectedImage), getImageData(actual));
	}

	private File getResourceFile(String resource) {
		URL resFileURL = getClass().getClassLoader().getResource(resource);
		assertNotNull(resFileURL);
		return new File(resFileURL.getFile());
	}

	private int[] getImageData(Image image) {
		int width = (int) image.getWidth();
		int height = (int) image.getHeight();
		int[] data = new int[width * height];
		image.getPixelReader().getPixels(0, 0, width, height, PixelFormat.getIntArgbPreInstance(), data, 0, width);
		return data;
	}
}
