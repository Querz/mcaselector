package net.querz.mcaselector.util;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import net.querz.mcaselector.*;
import net.querz.mcaselector.io.*;
import net.querz.mcaselector.tiles.Tile;
import net.querz.mcaselector.tiles.TileMap;
import net.querz.mcaselector.ui.AboutDialog;
import net.querz.mcaselector.ui.ChangeFieldsConfirmationDialog;
import net.querz.mcaselector.ui.ChangeNBTDialog;
import net.querz.mcaselector.ui.DeleteConfirmationDialog;
import net.querz.mcaselector.ui.ExportConfirmationDialog;
import net.querz.mcaselector.ui.FilterChunksDialog;
import net.querz.mcaselector.ui.GotoDialog;
import net.querz.mcaselector.ui.ImportConfirmationDialog;
import net.querz.mcaselector.ui.OptionBar;
import net.querz.mcaselector.ui.ProgressDialog;
import net.querz.mcaselector.ui.SettingsDialog;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Helper {

	public static final String MCA_FILE_PATTERN = "^r\\.-?\\d+\\.-?\\d+\\.mca$";
	public static final Pattern REGION_GROUP_PATTERN = Pattern.compile("^r\\.(?<regionX>-?\\d+)\\.(?<regionZ>-?\\d+)\\.mca$");

	public static Point2i blockToRegion(Point2i i) {
		return i.shiftRight(9);
	}

	public static Point2i regionToBlock(Point2i i) {
		return i.shiftLeft(9);
	}

	public static Point2i regionToChunk(Point2i i) {
		return i.shiftLeft(5);
	}

	public static Point2i blockToChunk(Point2i i) {
		return i.shiftRight(4);
	}

	public static Point2i chunkToBlock(Point2i i) {
		return i.shiftLeft(4);
	}

	public static Point2i chunkToRegion(Point2i i) {
		return i.shiftRight(5);
	}

	public static Integer parseInt(String s, int radix) {
		try {
			return Integer.parseInt(s, radix);
		} catch (NumberFormatException ex) {
			return null;
		}
	}

	public static Image getIconFromResources(String name) {
		return new Image(Helper.class.getClassLoader().getResourceAsStream(name + ".png"));
	}

	public static String getMCDir() {
		String os = System.getProperty("os.name").toLowerCase();
		String appdataDir = null;
		if (os.contains("win")) {
			String env = System.getenv("AppData");
			File file = new File(env == null ? "" : env, ".minecraft");
			if (file.exists()) {
				appdataDir = file.getAbsolutePath();
			}
		} else {
			appdataDir = getHomeDir();
			appdataDir += "/Library/Application Support/minecraft";
		}
		return appdataDir;
	}

	public static String getHomeDir() {
		return System.getProperty("user.home");
	}

	public static String getWorkingDir() {
		return System.getProperty("user.dir");
	}

	public static String getMCSavesDir() {
		String appData = getMCDir();
		File saves;
		if (appData == null || !(saves = new File(appData, "saves")).exists()) {
			return getHomeDir();
		}
		return saves.getAbsolutePath();
	}

	public static File createMCAFilePath(Point2i r) {
		return new File(Config.getWorldDir(), createMCAFileName(r));
	}

	public static File createPNGFilePath(File cacheDir, Point2i r) {
		return new File(cacheDir, createPNGFileName(r));
	}

	public static String createMCAFileName(Point2i r) {
		return String.format("r.%d.%d.mca", r.getX(), r.getY());
	}

	public static String createPNGFileName(Point2i r) {
		return String.format("r.%d.%d.png", r.getX(), r.getY());
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

	public static int getZoomLevel(float scale) {
		int b = 1;
		while (b <= scale) {
			b = b << 1;
		}
		return (int) Math.ceil(b / 2.0);
	}

	public static int getMaxZoomLevel() {
		return getZoomLevel(TileMap.MAX_SCALE);
	}

	public static int getMinZoomLevel() {
		return getZoomLevel(TileMap.MIN_SCALE);
	}

	public static float getZoomLevelImageSize(float scale) {
		int zoomLevel = getZoomLevel(scale);
		return Tile.SIZE / ((scale - zoomLevel) / (zoomLevel * 2 - zoomLevel) + 1);
	}

	public static Point2f getRegionGridMin(Point2f offset, float scale) {
		Point2i min = Helper.blockToRegion(offset.toPoint2i());
		Point2i regionOffset = Helper.regionToBlock(min).sub((int) offset.getX(), (int) offset.getY());
		return new Point2f(regionOffset.getX() / scale, regionOffset.getY() / scale);
	}

	public static Point2f getChunkGridMin(Point2f offset, float scale) {
		Point2i min = Helper.blockToChunk(offset.toPoint2i());
		Point2i chunkOffset = Helper.chunkToBlock(min).sub((int) offset.getX(), (int) offset.getY());
		return new Point2f(chunkOffset.getX() / scale, chunkOffset.getY() / scale);
	}

	//-----------------------------------------------------------------

	public static File createPNGFilePath(Point2i r, int zoomLevel) {
		File cacheDir = new File(Config.getCacheDir(), zoomLevel + "");
		//r is a region location
		Point2i scaledRegion = getZoomLevelOrigin(r, zoomLevel);
		return new File(cacheDir, createPNGFileName(scaledRegion));
	}

	public static Point2i getPointInCachedImage(Point2i region, int zoomLevel) {
		Point2i result = region.mod(zoomLevel).mul(Tile.SIZE / zoomLevel);
		if (result.getX() < 0) {
			result.setX(Tile.SIZE + result.getX());
		}
		if (result.getY() < 0) {
			result.setY(Tile.SIZE + result.getY());
		}
		return result;
	}

	public static Point2i getZoomLevelOrigin(Point2i region, int zoomLevel) {
		Point2i result = region.sub(region.mod(zoomLevel));
		if (region.getX() < 0) {
			int absX = -region.getX() - 1;
			result.setX(-(absX - absX % zoomLevel) - zoomLevel);
		}
		if (region.getY() < 0) {
			int absY = -region.getY() - 1;
			result.setY(-(absY - absY % zoomLevel) - zoomLevel);
		}
		return result;
	}

	public static Image loadCachedImage(Point2i origin, int zoomLevel) {
		try (FileInputStream fis = new FileInputStream(Helper.createPNGFilePath(origin, zoomLevel))) {
			return new Image(fis);
		} catch (IOException e) {
			Debug.errorf("error reading cached file %s: %s", Helper.createPNGFilePath(origin, zoomLevel), e.getMessage());
		}
		return null;
	}

	public static Image generateImage(File file, byte[] rawData) {
		Timer t = new Timer();

		ByteArrayPointer ptr = new ByteArrayPointer(rawData);

		MCAFile mcaFile = MCAFile.readHeader(file, ptr);
		if (mcaFile == null) {
			Debug.error("error reading mca file " + file);
			//mark as loaded, we won't try to load this again
			return null;
		}
		Debug.dumpf("took %s to read mca file header of %s", t, file.getName());

		t.reset();

		Image image = mcaFile.createImage(ptr);

		Debug.dumpf("took %s to generate image of %s", t, file.getName());

		return image;
	}

	public static void mergeImageToPNGCacheFiles(BufferedImage image, Point2i r) throws IOException {
		// loop through all zoom levels
		for (int z = getMinZoomLevel(); z <= getMaxZoomLevel(); z *= 2) {
			// get origin png file
			File cacheFile = createPNGFilePath(r, z);

			Point2i pointInCachedImage = getPointInCachedImage(r, z);

			BufferedImage scaledImage = scaleImage(image, (float) Tile.SIZE / (float) z);

			BufferedImage cachedImage;

			if (cacheFile.exists()) {
				cachedImage = ImageIO.read(cacheFile);
			} else {
				cacheFile.getParentFile().mkdirs();
				cachedImage = new BufferedImage(Tile.SIZE, Tile.SIZE, image.getType());
			}

			Graphics graphics = cachedImage.getGraphics();

			Debug.dumpf("drawing %s in %d/%s at %s", r, z, cacheFile.getName(), pointInCachedImage);

			graphics.drawImage(scaledImage, pointInCachedImage.getX(), pointInCachedImage.getY(), null);

			ImageIO.write(cachedImage, "png", cacheFile);
		}
	}

	// from is already scaled
	public static void mergeImages(BufferedImage from, BufferedImage to, Point2i offset) {
		Graphics graphics = to.getGraphics();
		graphics.drawImage(from, offset.getX(), offset.getY(), null);
	}

//-----------------------------------------------------------------

	public static void openWorld(TileMap tileMap, Stage primaryStage, OptionBar optionBar) {
		String savesDir = Helper.getMCSavesDir();
		File file = createDirectoryChooser(savesDir).showDialog(primaryStage);
		if (file != null && file.isDirectory()) {
			File[] files = file.listFiles((dir, name) -> name.matches("^r\\.-?\\d+\\.-?\\d+\\.mca$"));
			if (files != null && files.length > 0) {
				Debug.dump("setting world dir to " + file.getAbsolutePath());
				Config.setWorldDir(file);
				tileMap.clear();
				tileMap.update();
				optionBar.setWorldDependentMenuItemsEnabled(true);
			}
		}
	}

	public static void importSelection(TileMap tileMap, Stage primaryStage) {
		File file = createFileChooser(null,
				new FileChooser.ExtensionFilter("*.csv Files", "*.csv")).showOpenDialog(primaryStage);
		if (file != null) {
			Map<Point2i, Set<Point2i>> chunks = SelectionUtil.importSelection(file);
			tileMap.setMarkedChunks(chunks);
			tileMap.update();
		}
	}

	public static void exportSelection(TileMap tileMap, Stage primaryStage) {
		File file = createFileChooser(null,
				new FileChooser.ExtensionFilter("*.csv Files", "*.csv")).showSaveDialog(primaryStage);
		if (file != null) {
			SelectionUtil.exportSelection(tileMap.getMarkedChunks(), file);
			tileMap.update();
		}
	}

	private static DirectoryChooser createDirectoryChooser(String initialDirectory) {
		DirectoryChooser directoryChooser = new DirectoryChooser();
		if (initialDirectory != null) {
			directoryChooser.setInitialDirectory(new File(initialDirectory));
		}
		return directoryChooser;
	}

	private static FileChooser createFileChooser(String initialDirectory, FileChooser.ExtensionFilter filter) {
		FileChooser fileChooser = new FileChooser();
		if (filter != null) {
			fileChooser.getExtensionFilters().add(filter);
		}
		if (initialDirectory != null) {
			fileChooser.setInitialDirectory(new File(initialDirectory));
		}
		return fileChooser;
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
				File file = Helper.createPNGFilePath(cacheDir, regionBlock);
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
				File file = Helper.createPNGFilePath(cacheDir, entry.getKey());
				if (file.exists()) {
					if (!file.delete()) {
						Debug.error("could not delete file " + file);
					}
				}
				tileMap.clearTile(Helper.regionToBlock(entry.getKey()));
			}
		}
		tileMap.update();
	}

	public static void deleteSelection(TileMap tileMap, Stage primaryStage) {
		Optional<ButtonType> result = new DeleteConfirmationDialog(tileMap, primaryStage).showAndWait();
		result.ifPresent(r -> {
			if (r == ButtonType.OK) {
				new ProgressDialog(Translation.DIALOG_PROGRESS_TITLE_DELETING_SELECTION, primaryStage)
						.showProgressBar(t -> SelectionDeleter.deleteSelection(tileMap.getMarkedChunks(), t));
				clearSelectionCache(tileMap);
			}
		});
	}

	public static void exportSelectedChunks(TileMap tileMap, Stage primaryStage) {
		File dir = createDirectoryChooser(null).showDialog(primaryStage);
		if (dir != null) {
			Optional<ButtonType> result = new ExportConfirmationDialog(tileMap, primaryStage).showAndWait();
			result.ifPresent(r -> {
				if (r == ButtonType.OK) {
					new ProgressDialog(Translation.DIALOG_PROGRESS_TITLE_EXPORTING_SELECTION, primaryStage)
							.showProgressBar(t -> SelectionExporter.exportSelection(tileMap.getMarkedChunks(), dir, t));
				}
			});
		}
	}

	public static void importChunks(TileMap tileMap, Stage primaryStage) {
		File dir = createDirectoryChooser(null).showDialog(primaryStage);
		SimpleBooleanProperty overwriteProperty = new SimpleBooleanProperty();
		if (dir != null) {
			Optional<ButtonType> result = new ImportConfirmationDialog(primaryStage, overwriteProperty::set).showAndWait();
			result.ifPresent(r -> {
				if (r == ButtonType.OK) {
					new ProgressDialog(Translation.DIALOG_PROGRESS_TITLE_IMPORTING_CHUNKS, primaryStage)
							.showProgressBar(t -> ChunkImporter.importChunks(dir, t, overwriteProperty.get()));
					clearAllCache(tileMap);
				}
			});
		}
	}

	public static void gotoCoordinate(TileMap tileMap, Stage primaryStage) {
		Optional<Point2i> result = new GotoDialog(primaryStage).showAndWait();
		result.ifPresent(r -> tileMap.goTo(r.getX(), r.getY()));
	}

	public static void filterChunks(TileMap tileMap, Stage primaryStage) {
		Optional<FilterChunksDialog.Result> result = new FilterChunksDialog(primaryStage).showAndWait();
		result.ifPresent(r -> {
			Debug.dump("chunk filter query: " + r.getFilter());
			if (r.getFilter().isEmpty()) {
				Debug.dump("filter is empty, won't delete everything");
				return;
			}

			switch (r.getType()) {
			case DELETE:
				Optional<ButtonType> confRes = new DeleteConfirmationDialog(null, primaryStage).showAndWait();
				confRes.ifPresent(confR -> {
					if (confR == ButtonType.OK) {
						new ProgressDialog(Translation.DIALOG_PROGRESS_TITLE_DELETING_FILTERED_CHUNKS, primaryStage)
								.showProgressBar(t -> ChunkFilterDeleter.deleteFilter(
										r.getFilter(),
										r.isSelectionOnly() ? tileMap.getMarkedChunks() : null,
										t
								));
						clearAllCache(tileMap);
					}
				});
				break;
			case EXPORT:
				File dir = createDirectoryChooser(null).showDialog(primaryStage);
				if (dir != null) {
					confRes = new ExportConfirmationDialog(null, primaryStage).showAndWait();
					confRes.ifPresent(confR -> {
						if (confR == ButtonType.OK) {
							Debug.dump("exporting chunks to " + dir);
							new ProgressDialog(Translation.DIALOG_PROGRESS_TITLE_EXPORTING_FILTERED_CHUNKS, primaryStage)
									.showProgressBar(t -> ChunkFilterExporter.exportFilter(
											r.getFilter(),
											r.isSelectionOnly() ? tileMap.getMarkedChunks() : null,
											dir,
											t
									));
						}
					});
				} else {
					Debug.dump("cancelled exporting chunks, no valid destination directory");
				}
				break;
			case SELECT:
				tileMap.clearSelection();
				new ProgressDialog(Translation.DIALOG_PROGRESS_TITLE_SELECTING_FILTERED_CHUNKS, primaryStage)
					.showProgressBar(t -> ChunkFilterSelector.selectFilter(r.getFilter(), tileMap, t));
				break;
			default:
				Debug.dump("i have no idea how you got no selection there...");
			}
		});
	}

	public static void changeFields(TileMap tileMap, Stage primaryStage) {
		Optional<ChangeNBTDialog.Result> result = new ChangeNBTDialog(primaryStage).showAndWait();
		result.ifPresent(r -> {
			Optional<ButtonType> confRes = new ChangeFieldsConfirmationDialog(null, primaryStage).showAndWait();
			confRes.ifPresent(confR -> {
				if (confR == ButtonType.OK) {
					new ProgressDialog(Translation.DIALOG_PROGRESS_TITLE_CHANGING_NBT_DATA, primaryStage)
							.showProgressBar(t -> FieldChanger.changeNBTFields(
									r.getFields(),
									r.isForce(),
									r.isSelectionOnly() ? tileMap.getMarkedChunks() : null,
									t
							));
				}
			});
		});
	}

	public static void editSettings(TileMap tileMap, Stage primaryStage) {
		Optional<SettingsDialog.Result> result = new SettingsDialog(primaryStage).showAndWait();
		result.ifPresent(r -> {
			if (Config.getLoadThreads() != r.getReadThreads()
					|| Config.getProcessThreads() != r.getProcessThreads()
					|| Config.getWriteThreads() != r.getWriteThreads()
					|| Config.getMaxLoadedFiles() != r.getMaxLoadedFiles()) {
				MCAFilePipe.init(r.getReadThreads(), r.getProcessThreads(), r.getWriteThreads(), r.getMaxLoadedFiles());
			}

			if (!Config.getLocale().equals(r.getLocale())) {
				Config.setLocale(r.getLocale());
				Locale.setDefault(Config.getLocale());
				Translation.load(Config.getLocale());
			}
			Config.setLoadThreads(r.getReadThreads());
			Config.setProcessThreads(r.getProcessThreads());
			Config.setWriteThreads(r.getWriteThreads());
			Config.setMaxLoadedFiles(r.getMaxLoadedFiles());
			Config.setRegionSelectionColor(r.getRegionColor());
			Config.setChunkSelectionColor(r.getChunkColor());
			Config.setDebug(r.getDebug());
			tileMap.update();
		});
	}

	public static void showAboutDialog(TileMap tileMap, Stage primaryStage) {
		new AboutDialog(primaryStage).showAndWait();
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

	private static final Map<Pattern, Long> DURATION_REGEXP = new HashMap<>();

	static {
		DURATION_REGEXP.put(Pattern.compile("(?<data>\\d+)\\W*(?:years?|y)"), 31536000L);
		DURATION_REGEXP.put(Pattern.compile("(?<data>\\d+)\\W*(?:months?)"), 2628000L);
		DURATION_REGEXP.put(Pattern.compile("(?<data>\\d+)\\W*(?:days?|d)"), 90000L);
		DURATION_REGEXP.put(Pattern.compile("(?<data>\\d+)\\W*(?:hours?|h)"), 3600L);
		DURATION_REGEXP.put(Pattern.compile("(?<data>\\d+)\\W*(?:minutes?|mins?)"), 60L);
		DURATION_REGEXP.put(Pattern.compile("(?<data>\\d+)\\W*(?:seconds?|secs?|s)"), 1L);
	}

	public static long parseDuration(String d) {
		boolean result = false;
		int duration = 0;
		for (Map.Entry<Pattern, Long> entry : DURATION_REGEXP.entrySet()) {
			Matcher m = entry.getKey().matcher(d);
			if (m.find()) {
				duration += Long.parseLong(m.group("data")) * entry.getValue();
				result = true;
			}
		}
		if (!result) {
			throw new IllegalArgumentException("could not parse anything from duration string");
		}
		return duration;
	}

	private static final DateTimeFormatter TIMESTAMP_FORMAT =
			new DateTimeFormatterBuilder().appendPattern("yyyy-MM-dd[ [HH][:mm][:ss]]")
					.parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
					.parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
					.parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
					.toFormatter();

	private static final ZoneId ZONE_ID = ZoneId.systemDefault();

	public static int parseTimestamp(String t) {
		String trim = t.trim();
		try {
			LocalDateTime date = LocalDateTime.parse(trim, TIMESTAMP_FORMAT);
			ZonedDateTime zdt = ZonedDateTime.of(date, ZONE_ID);
			return (int) zdt.toInstant().getEpochSecond();
		} catch (DateTimeParseException e) {
			Debug.dump(e.getMessage());
		}
		throw new IllegalArgumentException("could not parse date time");
	}

	public static Attributes getManifestAttributes() throws IOException {
		String className = Helper.class.getSimpleName() + ".class";
		String classPath = Helper.class.getResource(className).toString();
		if (!classPath.startsWith("jar")) {
			throw new IOException("application not running in jar file");
		}
		URL url = new URL(classPath);
		JarURLConnection jarConnection = (JarURLConnection) url.openConnection();
		Manifest manifest = jarConnection.getManifest();
		return manifest.getMainAttributes();
	}
}
