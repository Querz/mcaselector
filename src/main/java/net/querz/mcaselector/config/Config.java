package net.querz.mcaselector.config;

import com.google.gson.Gson;
import net.querz.mcaselector.logging.Logging;
import net.querz.mcaselector.math.Bits;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public abstract class Config {

	// defaults
	public static final File BASE_DIR;
	public static final File BASE_CACHE_DIR;
	public static final File BASE_LOG_DIR;
	public static final File BASE_CONFIG_FILE;
	public static final File BASE_OVERLAYS_FILE;

	static {
		// find jar file directory
		String jarPath;
		String path = ".";
		try {
			jarPath = Config.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
			if (!jarPath.endsWith(".jar")) { // no jar
				path = jarPath.replaceAll("build/classes/java/main/$", "");
			} else {
				File jarFile = new File(jarPath);
				path = jarFile.getParent();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		BASE_DIR = new File(path);

		String osName = System.getProperty("os.name").toLowerCase();
		if (osName.contains("mac")) {
			BASE_CACHE_DIR = new File(System.getProperty("user.home"), "Library/Caches/mcaselector");
			BASE_LOG_DIR = new File(System.getProperty("user.home"), "Library/Logs/mcaselector");
			BASE_CONFIG_FILE = new File(System.getProperty("user.home"), "Library/Application Support/mcaselector/settings.json");
			BASE_OVERLAYS_FILE = new File(System.getProperty("user.home"), "Library/Application Support/mcaselector/overlays.json");
		} else if (osName.contains("windows")) {
			BASE_CACHE_DIR = getEnvFilesWithDefault(BASE_DIR.getAbsolutePath(), "mcaselector/cache", ';', "LOCALAPPDATA");
			BASE_LOG_DIR = getEnvFilesWithDefault(BASE_DIR.getAbsolutePath(), "mcaselector/log", ';', "LOCALAPPDATA");
			BASE_CONFIG_FILE = getEnvFilesWithDefault(BASE_DIR.getAbsolutePath(), "mcaselector/settings.json", ';', "LOCALAPPDATA");
			BASE_OVERLAYS_FILE = getEnvFilesWithDefault(BASE_DIR.getAbsolutePath(), "mcaselector/overlays.json", ';', "LOCALAPPDATA");
		} else {
			BASE_CACHE_DIR = getEnvFilesWithDefault("~/.cache", "mcaselector", ':', "XDG_CACHE_HOME", "XDG_CACHE_DIRS");
			BASE_LOG_DIR = getEnvFilesWithDefault("~/.local/share", "mcaselector/log", ':', "XDG_DATA_HOME", "XDG_DATA_DIRS");
			BASE_CONFIG_FILE = getEnvFilesWithDefault("~/.mcaselector", "mcaselector/settings.json", ':', "XDG_CONFIG_HOME", "XDG_CONFIG_DIRS");
			BASE_OVERLAYS_FILE = getEnvFilesWithDefault("~/.mcaselector", "mcaselector/overlays.json", ':', "XDG_CONFIG_HOME", "XDG_CONFIG_DIRS");
		}

		if (!BASE_CACHE_DIR.exists()) {
			BASE_CACHE_DIR.mkdirs();
		}
		if (!BASE_LOG_DIR.exists()) {
			BASE_LOG_DIR.mkdirs();
		}
		if (!BASE_OVERLAYS_FILE.getParentFile().exists()) {
			BASE_OVERLAYS_FILE.getParentFile().mkdirs();
		}
		Logging.setLogDir(BASE_LOG_DIR);
	}

	private static File getEnvFilesWithDefault(String def, String suffix, char divider, String... envs) {
		File file;
		for (String env : envs) {
			String value = System.getenv(env);
			if (value != null && !value.isEmpty()) {
				String[] split = value.split("" + divider);
				if (split.length > 1) {
					for (String part : split) {
						File f = new File(resolveHome(part), suffix);
						if (f.exists()) {
							return f;
						}
					}
					file = new File(resolveHome(split[0]), suffix);
				} else {
					file = new File(resolveHome(value), suffix);
				}
				if (attemptCreateDirectory(file)) {
					return file;
				}
			}
		}
		file = new File(resolveHome(def), suffix);
		if (attemptCreateDirectory(file)) {
			return file;
		}
		throw new RuntimeException("failed to create directories for " + suffix + ", please check permissions for " + resolveHome(def));
	}

	private static boolean attemptCreateDirectory(File file) {
		File parent = file.getParentFile();
		if (parent == null) {
			return false;
		}
		try {
			Files.createDirectories(parent.getCanonicalFile().toPath());
			return true;
		} catch (IOException ex) {
//			LOGGER.warn("failed to create directory {}", parent, ex);
			return false;
		}
	}

	private static String resolveHome(String dir) {
		if (dir.startsWith("~/")) {
			return System.getProperty("user.home") + dir.substring(1);
		}
		return dir;
	}

	// static values
	public static final float MAX_SCALE = 15.9999f;
	public static final float MIN_SCALE = 0.05f;
	public static final int MIN_ZOOM_LEVEL = Bits.getMsb((int) MIN_SCALE);
	public static final int MAX_ZOOM_LEVEL = Bits.getMsb((int) MAX_SCALE);
	public static final double IMAGE_POOL_SIZE = 2.5;

	private static final Logger LOGGER = LogManager.getLogger(Config.class);

	public abstract void save();

	protected String save(Gson gson) {
		return gson.toJson(this);
	}

	protected void save(Gson gson, File file) {
		try {
			Files.writeString(file.toPath(), save(gson));
		} catch (IOException ex) {
			LOGGER.warn("error writing config file " + file, ex);
		}
	}

	protected static String loadString(File file) {
		try {
			return Files.readString(file.toPath());
		} catch (IOException ex) {
			LOGGER.warn("error reading config file " + file, ex);
		}
		return null;
	}
}
