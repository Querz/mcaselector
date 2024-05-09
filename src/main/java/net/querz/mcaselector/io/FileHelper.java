package net.querz.mcaselector.io;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import net.querz.mcaselector.config.ConfigProvider;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.property.DataProperty;
import net.querz.mcaselector.ui.dialog.SelectWorldDialog;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FileHelper {

	private static final Logger LOGGER = LogManager.getLogger(FileHelper.class);

	public static final int HEADER_SIZE = 8192;
	public static final Pattern MCA_FILE_PATTERN = Pattern.compile("^r\\.-?\\d+\\.-?\\d+\\.mca$");
	public static final Pattern REGION_GROUP_PATTERN = Pattern.compile("^r\\.(?<regionX>-?\\d+)\\.(?<regionZ>-?\\d+)\\.mca$");
	public static final Pattern DAT_REGION_GROUP_PATTERN = Pattern.compile("^r\\.(?<regionX>-?\\d+)\\.(?<regionZ>-?\\d+)\\.dat$");
	public static final Pattern CACHE_REGION_GROUP_PATTERN = Pattern.compile("^r\\.(?<regionX>-?\\d+)\\.(?<regionZ>-?\\d+)\\.png$");

	private static final Map<String, String> lastOpenedDirectoryMap = new HashMap<>();

	private FileHelper() {}

	public static Image getIconFromResources(String name) {
		return new Image(Objects.requireNonNull(FileHelper.class.getClassLoader().getResourceAsStream(name + ".png")));
	}

	private static String getMCDir() {
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

	private static String getHomeDir() {
		return System.getProperty("user.home");
	}

	public static Point2i parseMCAFileName(File file) {
		return parseMCAFileName(file.getName());
	}

	public static Point2i parseDATFileName(File file) {
		return parseMCAFileName(file.getName());
	}

	public static Point2i parseDATFileName(String name) {
		Matcher m = FileHelper.DAT_REGION_GROUP_PATTERN.matcher(name);
		if (m.find()) {
			int x = Integer.parseInt(m.group("regionX"));
			int z = Integer.parseInt(m.group("regionZ"));
			return new Point2i(x, z);
		}
		return null;
	}

	public static Point2i parseMCAFileName(String name) {
		Matcher m = FileHelper.REGION_GROUP_PATTERN.matcher(name);
		if (m.find()) {
			int x = Integer.parseInt(m.group("regionX"));
			int z = Integer.parseInt(m.group("regionZ"));
			return new Point2i(x, z);
		}
		return null;
	}

	public static Point2i parseCacheFileName(File file) {
		return parseCacheFileName(file.getName());
	}

	public static Point2i parseCacheFileName(String name) {
		Matcher m = FileHelper.CACHE_REGION_GROUP_PATTERN.matcher(name);
		if (m.find()) {
			int x = Integer.parseInt(m.group("regionX"));
			int z = Integer.parseInt(m.group("regionZ"));
			return new Point2i(x, z);
		}
		return null;
	}

	public static String getMCSavesDir() {
		String appData = getMCDir();
		File saves;
		if (appData == null || !(saves = new File(appData, "saves")).exists()) {
			return getHomeDir();
		}
		return saves.getAbsolutePath();
	}

	public static String getLastOpenedDirectory(String key, String def) {
		String value = lastOpenedDirectoryMap.getOrDefault(key, def);
		if (value != null && new File(value).exists()) {
			return value;
		}
		if (def != null && new File(def).exists()) {
			return def;
		}
		return System.getProperty("user.home");
	}

	public static void setLastOpenedDirectory(String key, String lastOpenedDirectory) {
		FileHelper.lastOpenedDirectoryMap.put(key, lastOpenedDirectory);
	}

	public static File createRegionMCAFilePath(Point2i r) {
		return new File(ConfigProvider.WORLD.getWorldDirs().getRegion(), createMCAFileName(r));
	}

	public static File createPoiMCAFilePath(Point2i r) {
		return new File(ConfigProvider.WORLD.getWorldDirs().getPoi(), createMCAFileName(r));
	}

	public static File createEntitiesMCAFilePath(Point2i r) {
		return new File(ConfigProvider.WORLD.getWorldDirs().getEntities(), createMCAFileName(r));
	}

	public static File createRegionMCCFilePath(Point2i c) {
		return new File(ConfigProvider.WORLD.getWorldDirs().getRegion(), createMCCFileName(c));
	}

	public static File createPoiMCCFilePath(Point2i c) {
		return new File(ConfigProvider.WORLD.getWorldDirs().getPoi(), createMCCFileName(c));
	}

	public static File createEntitiesMCCFilePath(Point2i c) {
		return new File(ConfigProvider.WORLD.getWorldDirs().getEntities(), createMCCFileName(c));
	}

	public static WorldDirectories validateWorldDirectories(File dir) {
		File region = new File(dir, "region");
		File poi = new File(dir, "poi");
		File entities = new File(dir, "entities");
		if (!region.exists()) {
			return null;
		}
		return new WorldDirectories(region, poi.exists() ? poi : null, entities.exists() ? entities : null);
	}

	public static RegionDirectories createRegionDirectories(Point2i r) {
		File region = createRegionMCAFilePath(r);
		File poi = createPoiMCAFilePath(r);
		File entities = createEntitiesMCAFilePath(r);
		return new RegionDirectories(r, region, poi, entities);
	}

	public static File createMCAFilePath(Point2i r) {
		return new File(ConfigProvider.WORLD.getRegionDir(), createMCAFileName(r));
	}

	public static File createMCCFilePath(Point2i c) {
		return new File(ConfigProvider.WORLD.getRegionDir(), createMCCFileName(c));
	}

	public static File createPNGFilePath(File cacheDir, Point2i r) {
		return new File(cacheDir, createPNGFileName(r));
	}

	public static File createPNGFilePath(File cacheDir, int zoomLevel, Point2i r) {
		return new File(cacheDir, zoomLevel + "/" + createPNGFileName(r));
	}

	public static String createMCAFileName(Point2i r) {
		return String.format("r.%d.%d.mca", r.getX(), r.getZ());
	}

	public static String createMCCFileName(Point2i c) {
		return String.format("c.%d.%d.mcc", c.getX(), c.getZ());
	}

	public static String createPNGFileName(Point2i r) {
		return String.format("r.%d.%d.png", r.getX(), r.getZ());
	}

	public static Attributes getManifestAttributes() throws IOException {
		String className = FileHelper.class.getSimpleName() + ".class";
		String classPath = Objects.requireNonNull(FileHelper.class.getResource(className)).toString();
		if (!classPath.startsWith("jar")) {
			throw new IOException("application not running in jar file");
		}
		URL url = new URL(classPath);
		JarURLConnection jarConnection = (JarURLConnection) url.openConnection();
		Manifest manifest = jarConnection.getManifest();
		return manifest.getMainAttributes();
	}

	public static void clearFolder(File dir) throws IOException {
		Files.walkFileTree(dir.toPath(), new SimpleFileVisitor<>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.deleteIfExists(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException ex) throws IOException {
				if (ex == null) {
					Files.deleteIfExists(dir);
					return FileVisitResult.CONTINUE;
				}
				throw ex;
			}
		});
	}

	public static LongOpenHashSet parseAllMCAFileNames(File directory) {
		if (directory == null) {
			return LongOpenHashSet.of();
		}
		File[] files = directory.listFiles((dir, name) -> MCA_FILE_PATTERN.matcher(name).matches());
		if (files == null) {
			return LongOpenHashSet.of();
		}
		LongOpenHashSet regions = new LongOpenHashSet(files.length);
		Arrays.stream(files).forEach(f -> regions.add(FileHelper.parseMCAFileName(f).asLong()));
		return regions;
	}

	public static WorldDirectories createWorldDirectories(File file) {
		// make sure that target directories exist
		try {
			createDirectoryOrThrowException(file, "region");
			createDirectoryOrThrowException(file, "poi");
			createDirectoryOrThrowException(file, "entities");
			return new WorldDirectories(new File(file, "region"), new File(file, "poi"), new File(file, "entities"));
		} catch (IOException ex) {
			LOGGER.warn("failed to create directories", ex);
			return null;
		}
	}

	private static void createDirectoryOrThrowException(File dir, String folder) throws IOException {
		File d = new File(dir, folder);
		if (!d.exists() && !d.mkdirs()) {
			throw new IOException("failed to create directory " + d);
		}
	}

	public static boolean deleteDirectory(File dir) {
		File[] files = dir.listFiles();
		if (files != null) {
			for (File file : files) {
				deleteDirectory(file);
			}
		}
		return dir.delete();
	}


	// takes a directory and detects all world directories
	public static List<File> detectDimensionDirectories(File dir) {
		List<File> result = new ArrayList<>();

		// detect overworld
		if (isValidDimension(dir)) {
			result.add(dir);
		}

		// detect nether folder and end folder first to have them at the beginning of the list
		File nether = new File(dir, "DIM-1");
		if (isValidDimension(nether)) {
			result.add(nether);
		}
		File end = new File(dir, "DIM1");
		if (isValidDimension(end)) {
			result.add(end);
		}

		// detect custom dimensions
		File[] customDimensions = dir.listFiles((d, name) -> !name.equals("DIM-1") && !name.equals("DIM1") && name.matches("^DIM-?\\d+$"));
		if (customDimensions != null && customDimensions.length > 0) {
			for (File customDimension : customDimensions) {
				if (isValidDimension(customDimension)) {
					result.add(customDimension);
				}
			}
		}

		return result;
	}

	public static boolean isValidDimension(File dir) {
		File region = new File(dir, "region");
		return region.exists() && hasMCAFiles(region);
	}

	public static boolean hasMCAFiles(File dir) {
		File[] files = dir.listFiles((d, name) -> MCA_FILE_PATTERN.matcher(name).matches());
		return files != null && files.length > 0;
	}

	public static WorldDirectories detectWorldDirectories(File dir) {
		File region = new File(dir, "region");
		File poi = new File(dir, "poi");
		File entities = new File(dir, "entities");

		WorldDirectories worldDirectories = new WorldDirectories();

		if (region.exists() && FileHelper.hasMCAFiles(region)) {
			worldDirectories.setRegion(region);
		}
		if (poi.exists()) {
			worldDirectories.setPoi(poi);
		}
		if (entities.exists()) {
			worldDirectories.setEntities(entities);
		}

		return worldDirectories;
	}

	public static File testFilesInSameDirectory(List<File> files) {
		File dir = null;
		for (File file : files) {
			if (file.isDirectory()) {
				if (dir == null) {
					dir = file;
				} else if (!file.equals(dir)) {
					return null;
				}
			} else {
				if (dir == null) {
					dir = file.getParentFile();
				} else if (!file.getParentFile().equals(dir)) {
					return null;
				}
			}
		}
		return dir;
	}

	public static WorldDirectories testWorldDirectoriesValid(List<File> files, Stage primaryStage) {
		if (files.size() == 0) {
			return null;
		}

		// test if we have one or multiple files from the same directory
		File dir;
		if ((dir = testFilesInSameDirectory(files)) != null) {
			if (hasMCAFiles(dir)) {
				return new WorldDirectories(dir, null, null);
			}
		}

		// test if those are "region", "poi" and "entities" folders
		WorldDirectories wd = new WorldDirectories();
		fileLoop:
		for (File file : files) {
			if (!file.isDirectory()) {
				wd = null;
				break;
			}
			switch (file.getName()) {
				case "region" -> {
					if (wd.getRegion() != null) {
						wd = null;
						break fileLoop;
					}
					wd.setRegion(file);
				}
				case "poi" -> {
					if (wd.getPoi() != null) {
						wd = null;
						break fileLoop;
					}
					wd.setPoi(file);
				}
				case "entities" -> {
					if (wd.getEntities() != null) {
						wd = null;
						break fileLoop;
					}
					wd.setEntities(file);
				}
			}
		}
		if (wd != null && wd.getRegion() != null) {
			return wd;
		}

		// detect dimensions
		if (files.size() != 1) {
			return null;
		}
		File file = files.get(0);
		if (file.isFile()) {
			file = file.getParentFile();
		}
		List<File> dimensionDirs = detectDimensionDirectories(file);
		if (dimensionDirs.size() == 1) {
			return detectWorldDirectories(dimensionDirs.get(0));
		} else if (dimensionDirs.size() > 1) {
			if (primaryStage != null) {
				Optional<File> result = new SelectWorldDialog(dimensionDirs, primaryStage).showAndWait();
				DataProperty<WorldDirectories> dimensionProperty = new DataProperty<>();
				result.ifPresent(dim -> dimensionProperty.set(FileHelper.detectWorldDirectories(dim)));
				if (dimensionProperty.get() != null) {
					return dimensionProperty.get();
				}
			} else {
				return new WorldDirectories();
			}
		}

		return null;
	}
}
