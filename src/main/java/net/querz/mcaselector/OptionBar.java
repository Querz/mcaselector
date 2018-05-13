package net.querz.mcaselector;

import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import net.querz.mcaselector.io.MCAChunkData;
import net.querz.mcaselector.io.MCAFile;
import net.querz.mcaselector.io.MCALoader;
import net.querz.mcaselector.tiles.TileMap;
import net.querz.mcaselector.util.Helper;
import net.querz.mcaselector.util.Point2i;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class OptionBar extends MenuBar {
	/*
	* File		View				Selection
	* - Open	- Chunk Grid		- Clear
	* - Quit	- Region Grid		- Delete
	*			- Goto
	* */

	private Menu file = new Menu("File");
	private Menu view = new Menu("View");
	private Menu selection = new Menu("Selection");

	private MenuItem open = new MenuItem("Open");
	private MenuItem quit = new MenuItem("Quit");
	private CheckMenuItem chunkGrid = new CheckMenuItem("Chunk Grid");
	private CheckMenuItem regionGrid = new CheckMenuItem("Region Grid");
	private MenuItem goTo = new MenuItem("Goto");
	private MenuItem clear = new MenuItem("Clear");
	private MenuItem delete = new MenuItem("Delete");

	private static final SeparatorMenuItem separator = new SeparatorMenuItem();

	public OptionBar(TileMap tileMap, Stage primaryStage) {
		getMenus().addAll(file, view, selection);
		file.getItems().addAll(open, quit);
		view.getItems().addAll(chunkGrid, regionGrid, separator, goTo);
		selection.getItems().addAll(clear, delete);

		open.setOnAction(e -> {
			String appdata = Helper.getAppdataDir();
			if (appdata != null) {
				File file = createDirectoryChooser(appdata + "/.minecraft/saves").showDialog(primaryStage);
				if (file != null && file.isDirectory()) {
					File[] files = file.listFiles((dir, name) -> name.matches("^r\\.-?\\d+\\.-?\\d+\\.mca"));
					if (files != null && files.length > 0) {
						System.out.println("setting world dir to " + file.getAbsolutePath());
						Config.setWorldDir(file);
						tileMap.clear();
					}
				}
			}
		});

		quit.setOnAction(e -> System.exit(0));

		chunkGrid.setSelected(true);
		chunkGrid.setOnAction(e -> tileMap.setShowChunkGrid(chunkGrid.isSelected()));

		regionGrid.setSelected(true);
		regionGrid.setOnAction(e -> tileMap.setShowRegionGrid(regionGrid.isSelected()));

		goTo.setOnAction(e -> {
			Optional<Point2i> result = new GotoDialog().showAndWait();
			result.ifPresent(r -> tileMap.goTo(r.getX(), r.getY()));
		});

		clear.setOnAction(e -> tileMap.clearSelection());

		delete.setOnAction(e -> {
			Optional<ButtonType> result = new DeleteConfirmationDialog(tileMap).showAndWait();
			result.ifPresent(r -> {
				if (r == ButtonType.OK) {
					for (Map.Entry<Point2i, Set<Point2i>> entry : tileMap.getMarkedChunks().entrySet()) {
						File file = createMCAFilePath(entry.getKey());
						if (entry.getValue() == null) {
							try {
								Files.deleteIfExists(file.toPath());
							} catch (IOException e1) {
								e1.printStackTrace();
							}
						} else {
							//read MCAFile
							MCAFile mcaFile = null;
							MCAChunkData[] mcaChunkDataArray = null;

							try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
								mcaFile = MCALoader.read(createMCAFilePath(entry.getKey()), raf);
								if (mcaFile == null) {
									System.out.println("error reading " + file + ", skipping");
									continue;
								}

								mcaChunkDataArray = new MCAChunkData[1024];

								for (int i = 0; i < 1024; i++) {
									MCAChunkData mcaChunkData = mcaFile.getChunkData(i);
									if (!mcaChunkData.isEmpty() && !entry.getValue().contains(getChunkCoordinate(entry.getKey(), i))) {
										mcaChunkData.readHeader(raf);
										mcaChunkData.loadRawData(raf);
										mcaChunkDataArray[i] = mcaChunkData;
									}
								}
							} catch (Exception ex) {
								ex.printStackTrace();
								continue;
							}

							//need to close raf before reopening
							MCALoader.backup(mcaFile);

							boolean restore = false;
							try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
								MCALoader.write(mcaFile, mcaChunkDataArray, raf);
							} catch (Exception ex) {
								ex.printStackTrace();
								restore = true;
							}

							if (restore){
								MCALoader.restore(mcaFile);
							}
						}
					}
				}
			});
		});
	}

	private DirectoryChooser createDirectoryChooser(String initialDirectory) {
		DirectoryChooser directoryChooser = new DirectoryChooser();
		directoryChooser.setInitialDirectory(new File(initialDirectory));
		return directoryChooser;
	}

	private File createMCAFilePath(Point2i r) {
		return new File(Config.getWorldDir(), String.format("r.%d.%d.mca", r.getX(), r.getY()));
	}

	private int getChunkIndex(Point2i chunkCoordinate) {
		return (chunkCoordinate.getX() & 31) + (chunkCoordinate.getY() & 31) * 32;
	}

	private static Point2i getChunkCoordinate(Point2i region, int index) {
		return new Point2i(
				region.getX() * 32 + index % 32,
				region.getY() * 32 + index / 32
		);
	}
}
