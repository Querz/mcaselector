package net.querz.mcaselector.io;

import javafx.scene.image.Image;
import net.querz.mcaselector.Config;
import net.querz.mcaselector.point.Point2i;
import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FileHelper {

	public static final String MCA_FILE_PATTERN = "^r\\.-?\\d+\\.-?\\d+\\.mca$";
	public static final Pattern REGION_GROUP_PATTERN = Pattern.compile("^r\\.(?<regionX>-?\\d+)\\.(?<regionZ>-?\\d+)\\.mca$");

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

	public static Point2i parseMCAFileName(String name) {
		Matcher m = FileHelper.REGION_GROUP_PATTERN.matcher(name);
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

	public static String getLastOpenedDirectory(String key) {
		return lastOpenedDirectoryMap.get(key);
	}

	public static void setLastOpenedDirectory(String key, String lastOpenedDirectory) {
		FileHelper.lastOpenedDirectoryMap.put(key, lastOpenedDirectory);
	}

	public static File createMCAFilePath(Point2i r) {
		return new File(Config.getWorldDir(), createMCAFileName(r));
	}

	public static File createPNGFilePath(File cacheDir, Point2i r) {
		return new File(cacheDir, createPNGFileName(r));
	}

	public static File createPNGFilePath(File cacheDir, int zoomLevel, Point2i r) {
		return new File(cacheDir, zoomLevel + "/" + createPNGFileName(r));
	}

	public static String createMCAFileName(Point2i r) {
		return String.format("r.%d.%d.mca", r.getX(), r.getY());
	}

	public static String createPNGFileName(Point2i r) {
		return String.format("r.%d.%d.png", r.getX(), r.getY());
	}

	public static Attributes getManifestAttributes() throws IOException {
		String className = FileHelper.class.getSimpleName() + ".class";
		String classPath = FileHelper.class.getResource(className).toString();
		if (!classPath.startsWith("jar")) {
			throw new IOException("application not running in jar file");
		}
		URL url = new URL(classPath);
		JarURLConnection jarConnection = (JarURLConnection) url.openConnection();
		Manifest manifest = jarConnection.getManifest();
		return manifest.getMainAttributes();
	}

	public static void clearFolder(File dir) throws IOException {
		Files.walkFileTree(dir.toPath(), new SimpleFileVisitor<Path>() {
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
}
