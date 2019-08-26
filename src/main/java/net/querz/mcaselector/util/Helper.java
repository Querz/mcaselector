package net.querz.mcaselector.util;

import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import net.querz.mcaselector.*;
import net.querz.mcaselector.io.*;
import net.querz.mcaselector.tiles.Tile;
import net.querz.mcaselector.tiles.TileMap;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

public class Helper {

	public static Integer parseInt(String s, int radix) {
		try {
			return Integer.parseInt(s, radix);
		} catch (NumberFormatException ex) {
			return null;
		}
	}

	public static BufferedImage scaleImage(BufferedImage before, double newSize) {
		double w = before.getWidth();
		double h = before.getHeight();
		BufferedImage after = new BufferedImage((int) newSize, (int) newSize, BufferedImage.TYPE_INT_ARGB);
		AffineTransform at = new AffineTransform();
		at.scale(newSize / w, newSize / h);
		AffineTransformOp scaleOp = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
		return scaleOp.filter(before, after);
	}

	public static Point2f getRegionGridMin(Point2f offset, float scale) {
		Point2i min = offset.toPoint2i().blockToRegion();
		Point2i regionOffset = min.regionToBlock().sub((int) offset.getX(), (int) offset.getY());
		return new Point2f(regionOffset.getX() / scale, regionOffset.getY() / scale);
	}

	public static Point2f getChunkGridMin(Point2f offset, float scale) {
		Point2i min = offset.toPoint2i().blockToChunk();
		Point2i chunkOffset = min.chunkToBlock().sub((int) offset.getX(), (int) offset.getY());
		return new Point2f(chunkOffset.getX() / scale, chunkOffset.getY() / scale);
	}

	public static void forceGenerateCache(Integer zoomLevel, Progress progressChannel) {
		File[] files = Config.getWorldDir().listFiles((d, n) -> n.matches(FileHelper.MCA_FILE_PATTERN));
		if (files == null || files.length == 0) {
			return;
		}

		progressChannel.setMax(files.length);
		progressChannel.updateProgress(files[0].getName(), 0);

		for (File file : files) {
			Matcher m = FileHelper.REGION_GROUP_PATTERN.matcher(file.getName());
			if (m.find()) {
				int x = Integer.parseInt(m.group("regionX"));
				int z = Integer.parseInt(m.group("regionZ"));
				boolean scaleOnly = zoomLevel != null;
				float zoomLevelSupplier = scaleOnly ? zoomLevel : 1;
				RegionImageGenerator.generate(new Tile(new Point2i(x, z)), () -> {}, () -> zoomLevelSupplier, true, scaleOnly, progressChannel);
			}
		}
	}

	public static void clearAllCache(TileMap tileMap) {
		for (File cacheDir : Config.getCacheDirs()) {
			File[] files = cacheDir.listFiles((dir, name) -> name.matches("^r\\.-?\\d+\\.-?\\d+\\.png$"));
			if (files != null) {
				for (File file : files) {
					if (!file.isDirectory()) {
						Debug.dump("deleting " + file);
						if (!file.delete()) {
							Debug.error("could not delete file " + file);
						}
					}
				}
			}
		}


		tileMap.clear();
		tileMap.update();
	}

	public static void clearViewCache(TileMap tileMap) {
		for (Point2i regionBlock : tileMap.getVisibleRegions()) {
			for (File cacheDir : Config.getCacheDirs()) {
				File file = FileHelper.createPNGFilePath(cacheDir, regionBlock);
				if (file.exists()) {
					if (!file.delete()) {
						Debug.error("could not delete file " + file);
					}
				}
			}
		}
		tileMap.clear();
		tileMap.update();
	}

	public static void clearSelectionCache(TileMap tileMap) {
		for (Map.Entry<Point2i, Set<Point2i>> entry : tileMap.getMarkedChunks().entrySet()) {
			for (File cacheDir : Config.getCacheDirs()) {
				File file = FileHelper.createPNGFilePath(cacheDir, entry.getKey());
				if (file.exists()) {
					if (!file.delete()) {
						Debug.error("could not delete file " + file);
					}
				}
				tileMap.clearTile(entry.getKey());
			}
		}
		tileMap.update();
	}

	public static TextField attachTextFieldToSlider(Slider slider) {
		TextField sliderValue = new TextField();
		sliderValue.getStyleClass().add("slider-value-field");
		sliderValue.textProperty().addListener((l, o, n) -> {
			if (!n.matches("\\d*")) {
				sliderValue.setText(n.replaceAll("[^\\d]", ""));
			} else if ("".equals(n)) {
				slider.setValue(slider.getMin());
			} else {
				slider.setValue(Integer.parseInt(n));
			}
		});
		sliderValue.focusedProperty().addListener((l, o, n) -> {
			if (!n) {
				sliderValue.setText((int) slider.getValue() + "");
			}
		});
		slider.valueProperty().addListener((l, o, n) -> {
			if (n.intValue() != slider.getMin() || slider.isFocused()) {
				sliderValue.setText(n.intValue() + "");
			}
		});
		sliderValue.setText((int) slider.getValue() + "");
		return sliderValue;
	}

	public static String byteToBinaryString(byte b) {
		StringBuilder s = new StringBuilder(Integer.toBinaryString(b & 0xFF));
		for (int i = s.length(); i < 8; i++) {
			s.insert(0, "0");
		}
		return s.toString();
	}

	public static String intToBinaryString(int n) {
		StringBuilder s = new StringBuilder(Integer.toBinaryString(n));
		for (int i = s.length(); i < 32; i++) {
			s.insert(0, "0");
		}
		return s.toString();
	}

	public static int getZoomLevel(float scale) {
		int b = 1;
		while (b <= scale) {
			b = b << 1;
		}
		return (int) Math.ceil(b / 2.0);
	}

	public static int getMaxZoomLevel() {
		return getZoomLevel(Config.MAX_SCALE);
	}

	public static int getMinZoomLevel() {
		return getZoomLevel(Config.MIN_SCALE);
	}
}
