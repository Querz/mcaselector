package net.querz.mcaselector.util;

import javafx.scene.image.Image;
import net.querz.mcaselector.Config;
import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileHelper {

	public static final String MCA_FILE_PATTERN = "^r\\.-?\\d+\\.-?\\d+\\.mca$";
	public static final Pattern REGION_GROUP_PATTERN = Pattern.compile("^r\\.(?<regionX>-?\\d+)\\.(?<regionZ>-?\\d+)\\.mca$");

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

	public static Point2i parseMCAFileName(File file) {
		Matcher m = FileHelper.REGION_GROUP_PATTERN.matcher(file.getName());
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
