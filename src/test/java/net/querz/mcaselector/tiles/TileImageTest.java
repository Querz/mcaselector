package net.querz.mcaselector.tiles;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import net.querz.mcaselector.Config;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.io.ByteArrayPointer;
import net.querz.mcaselector.io.mca.RegionMCAFile;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.progress.Timer;
import org.junit.Test;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.UUID;
import static org.junit.Assert.*;
import static net.querz.mcaselector.MCASelectorTestCase.*;

public class TileImageTest {

	@Test
	public void testGenerateImage112() throws IOException {
		Config.setShade(true);
		Config.setShadeWater(true);
		RegionMCAFile data = loadRegionMCAFileFromResource("anvil112/r.0.0.mca");
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

	@Test
	public void testGenerateImage113() throws IOException {
		Config.setShade(true);
		Config.setShadeWater(true);
		RegionMCAFile data = loadRegionMCAFileFromResource("anvil113/r.0.0.mca");
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

	@Test
	public void testGenerateImage114() throws IOException {
		Config.setShade(true);
		Config.setShadeWater(true);
		RegionMCAFile data = loadRegionMCAFileFromResource("anvil114/r.0.0.mca");
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

	@Test
	public void testGenerateImage115() throws IOException {
		Config.setShade(true);
		Config.setShadeWater(true);
		RegionMCAFile data = loadRegionMCAFileFromResource("anvil115/r.0.0.mca");
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

	@Test
	public void testGenerateImage116() throws IOException {
		Config.setShade(true);
		Config.setShadeWater(true);
		RegionMCAFile data = loadRegionMCAFileFromResource("anvil116/r.0.0.mca");
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

	private void writeImage(Image image, File file) throws IOException {
		if (!file.getParentFile().exists()) {
			file.getParentFile().mkdirs();
		}
		BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);
		ImageIO.write(bufferedImage, "png", file);
	}

	private void assertImageEquals(String expected, Image actual) throws IOException {
//		writeImage(actual, new File("actual/" + expected));
		Image expectedImage = SwingFXUtils.toFXImage(ImageIO.read(getResourceFile(expected)), null);
		assertArrayEquals(getImageData(expectedImage), getImageData(actual));
	}

	private int[] getImageData(Image image) {
		int width = (int) image.getWidth();
		int height = (int) image.getHeight();
		int[] data = new int[width * height];
		image.getPixelReader().getPixels(0, 0, width, height, PixelFormat.getIntArgbPreInstance(), data, 0, width);
		return data;
	}
}
