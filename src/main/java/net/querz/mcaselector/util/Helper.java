package net.querz.mcaselector.util;

import javafx.scene.control.ButtonType;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import net.querz.mcaselector.Config;
import net.querz.mcaselector.DeleteConfirmationDialog;
import net.querz.mcaselector.GotoDialog;
import net.querz.mcaselector.io.MCALoader;
import net.querz.mcaselector.io.SelectionExporter;
import net.querz.mcaselector.tiles.TileMap;
import java.io.File;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class Helper {

	public static Point2i blockToRegion(Point2i i) {
		return i.shiftRight(9);
	}

	public static Point2i regionToBlock(Point2i i) {
		return i.shiftLeft(9);
	}

	public static Point2i blockToChunk(Point2i i) {
		return i.shiftRight(4);
	}

	public static Point2i chunkToBlock(Point2i i) {
		return i.shiftLeft(4);
	}

	public static Integer parseInt(String s, int radix) {
		try {
			return Integer.parseInt(s, radix);
		} catch (NumberFormatException ex) {
			return null;
		}
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
		return new File(Config.getWorldDir(), String.format("r.%d.%d.mca", r.getX(), r.getY()));
	}

	public static File createPNGFilePath(Point2i r) {
		return new File(Config.getCacheDir(), String.format("r.%d.%d.png", r.getX(), r.getY()));
	}

	public static void openWorld(TileMap tileMap, Stage primaryStage) {
		String savesDir = Helper.getMCSavesDir();
		File file = createDirectoryChooser(savesDir).showDialog(primaryStage);
		if (file != null && file.isDirectory()) {
			File[] files = file.listFiles((dir, name) -> name.matches("^r\\.-?\\d+\\.-?\\d+\\.mca"));
			if (files != null && files.length > 0) {
				System.out.println("setting world dir to " + file.getAbsolutePath());
				Config.setWorldDir(file);
				tileMap.clear();
				tileMap.update();
			}
		}
	}

	public static void importSelection(TileMap tileMap, Stage primaryStage) {
		File file = createFileChooser(null,
				new FileChooser.ExtensionFilter("*.csv Files", "*.csv")).showOpenDialog(primaryStage);
		if (file != null) {
			Map<Point2i, Set<Point2i>> chunks = SelectionExporter.importSelection(file);
			tileMap.setMarkedChunks(chunks);
			tileMap.update();
		}
	}

	public static void exportSelection(TileMap tileMap, Stage primaryStage) {
		File file = createFileChooser(null,
				new FileChooser.ExtensionFilter("*.csv Files", "*.csv")).showSaveDialog(primaryStage);
		if (file != null) {
			SelectionExporter.exportSelection(tileMap.getMarkedChunks(), file);
			tileMap.update();
		}
	}

	private static DirectoryChooser createDirectoryChooser(String initialDirectory) {
		DirectoryChooser directoryChooser = new DirectoryChooser();
		directoryChooser.setInitialDirectory(new File(initialDirectory));
		return directoryChooser;
	}

	private static FileChooser createFileChooser(String initialDirectory, FileChooser.ExtensionFilter filter) {
		FileChooser fileChooser = new FileChooser();
		if (filter != null) {
			fileChooser.getExtensionFilters().add(filter);
//			fileChooser.setSelectedExtensionFilter(filter);
		}
		if (initialDirectory != null) {
			fileChooser.setInitialDirectory(new File(initialDirectory));
		}
		return fileChooser;
	}

	public static void clearAllCache(TileMap tileMap) {
		File[] files = Config.getCacheDir().listFiles((dir, name) -> name.matches("^r\\.-?\\d+\\.-?\\d+\\.png"));
		if (files != null) {
			for (File file : files) {
				if (!file.isDirectory()) {
					System.out.println("deleting " + file);
					if (!file.delete()) {
						System.out.println("could not delete file " + file);
					}
				}
			}
		}
		tileMap.clear();
		tileMap.update();
	}

	public static void clearViewCache(TileMap tileMap) {
		for (Point2i regionBlock : tileMap.getVisibleRegions()) {
			File file = Helper.createPNGFilePath(regionBlock);
			if (file.exists()) {
				if (!file.delete()) {
					System.out.println("could not delete file " + file);
				}
			}
		}
		tileMap.clear();
		tileMap.update();
	}

	public static void clearSelectionCache(TileMap tileMap) {
		for (Map.Entry<Point2i, Set<Point2i>> entry : tileMap.getMarkedChunks().entrySet()) {
			File file = Helper.createPNGFilePath(entry.getKey());
			if (file.exists()) {
				if (!file.delete()) {
					System.out.println("could not delete file " + file);
				}
			}
		}
		tileMap.clear();
		tileMap.update();
	}

	public static void deleteSelection(TileMap tileMap) {
		Optional<ButtonType> result = new DeleteConfirmationDialog(tileMap).showAndWait();
		result.ifPresent(r -> {
			if (r == ButtonType.OK) {
				MCALoader.deleteChunks(tileMap.getMarkedChunks());
				clearSelectionCache(tileMap);
			}
		});
	}

	public static void gotoCoordinate(TileMap tileMap) {
		Optional<Point2i> result = new GotoDialog().showAndWait();
		result.ifPresent(r -> tileMap.goTo(r.getX(), r.getY()));
	}
}
