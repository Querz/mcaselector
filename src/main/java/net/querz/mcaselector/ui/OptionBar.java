package net.querz.mcaselector.ui;

import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.stage.Stage;
import net.querz.mcaselector.tiles.TileMap;
import net.querz.mcaselector.io.CacheHelper;
import net.querz.mcaselector.text.Translation;
import net.querz.mcaselector.tiles.TileMapSelection;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;

public class OptionBar extends MenuBar {
	/*
	* File			View				Selection					Tools				About
	* - Open		- Chunk Grid		- Clear selection			- Import chunks
	* - Settings	- Region Grid		- Copy chunks				- Filter chunks
	* - Quit		- Goto				- Paste chunks				- Change fields
	*				- Clear cache		- Export selected chunks	- Edit NBT
	*				- Clear all cache	- Delete selected chunks	- Swap chunks
	*									- Import selection		
	*									- Export selection		
	*									- Clear cache
	* */

	private final Menu file = UIFactory.menu(Translation.MENU_FILE);
	private final Menu view = UIFactory.menu(Translation.MENU_VIEW);
	private final Menu selection = UIFactory.menu(Translation.MENU_SELECTION);
	private final Menu tools = UIFactory.menu(Translation.MENU_TOOLS);
	private final Label about = UIFactory.label(Translation.MENU_ABOUT);

	private final MenuItem open = UIFactory.menuItem(Translation.MENU_FILE_OPEN);
	private final MenuItem settings = UIFactory.menuItem(Translation.MENU_FILE_SETTINGS);
	private final MenuItem quit = UIFactory.menuItem(Translation.MENU_FILE_QUIT);
	private final CheckMenuItem chunkGrid = UIFactory.checkMenuItem(Translation.MENU_VIEW_CHUNK_GRID, true);
	private final CheckMenuItem regionGrid = UIFactory.checkMenuItem(Translation.MENU_VIEW_REGION_GRID, true);
	private final MenuItem goTo = UIFactory.menuItem(Translation.MENU_VIEW_GOTO);
	private final MenuItem clearViewCache = UIFactory.menuItem(Translation.MENU_VIEW_CLEAR_CACHE);
	private final MenuItem clearAllCache = UIFactory.menuItem(Translation.MENU_VIEW_CLEAR_ALL_CACHE);
	private final MenuItem clear = UIFactory.menuItem(Translation.MENU_SELECTION_CLEAR);
	private final MenuItem copy = UIFactory.menuItem(Translation.MENU_SELECTION_COPY_CHUNKS);
	private final MenuItem paste = UIFactory.menuItem(Translation.MENU_SELECTION_PASTE_CHUNKS);
	private final MenuItem exportChunks = UIFactory.menuItem(Translation.MENU_SELECTION_EXPORT_CHUNKS);
	private final MenuItem importChunks = UIFactory.menuItem(Translation.MENU_TOOLS_IMPORT_CHUNKS);
	private final MenuItem delete = UIFactory.menuItem(Translation.MENU_SELECTION_DELETE_CHUNKS);
	private final MenuItem importSelection = UIFactory.menuItem(Translation.MENU_SELECTION_IMPORT_SELECTION);
	private final MenuItem exportSelection = UIFactory.menuItem(Translation.MENU_SELECTION_EXPORT_SELECTION);
	private final MenuItem clearSelectionCache = UIFactory.menuItem(Translation.MENU_SELECTION_CLEAR_CACHE);
	private final MenuItem filterChunks = UIFactory.menuItem(Translation.MENU_TOOLS_FILTER_CHUNKS);
	private final MenuItem changeFields = UIFactory.menuItem(Translation.MENU_TOOLS_CHANGE_FIELDS);
	private final MenuItem editNBT = UIFactory.menuItem(Translation.MENU_TOOLS_EDIT_NBT);
	private final MenuItem swapChunks = UIFactory.menuItem(Translation.MENU_TOOLS_SWAP_CHUNKS);

	private int previousSelectedChunks = 0;

	public OptionBar(TileMap tileMap, Stage primaryStage) {
		getStyleClass().add("option-bar");

		tileMap.setOnUpdate(this::onUpdate);

		file.getItems().addAll(open, settings, UIFactory.separator(), quit);
		view.getItems().addAll(
				chunkGrid, regionGrid, UIFactory.separator(),
				goTo, UIFactory.separator(),
				clearViewCache, clearAllCache);
		selection.getItems().addAll(
				clear, UIFactory.separator(),
				copy, paste, UIFactory.separator(),
				exportChunks, delete, UIFactory.separator(),
				importSelection, exportSelection, UIFactory.separator(),
				clearSelectionCache);
		tools.getItems().addAll(importChunks, filterChunks, changeFields, editNBT, UIFactory.separator(), swapChunks);
		about.setOnMouseClicked(e -> DialogHelper.showAboutDialog(primaryStage));
		Menu aboutMenu = new Menu();
		aboutMenu.setGraphic(about);

		getMenus().addAll(file, view, selection, tools, aboutMenu);

		open.setOnAction(e -> DialogHelper.openWorld(tileMap, primaryStage, this));
		settings.setOnAction(e -> DialogHelper.editSettings(tileMap, primaryStage));
		quit.setOnAction(e -> System.exit(0));
		chunkGrid.setOnAction(e -> tileMap.setShowChunkGrid(chunkGrid.isSelected()));
		regionGrid.setOnAction(e -> tileMap.setShowRegionGrid(regionGrid.isSelected()));
		goTo.setOnAction(e -> DialogHelper.gotoCoordinate(tileMap, primaryStage));
		clearAllCache.setOnAction(e -> CacheHelper.clearAllCache(tileMap));
		clearViewCache.setOnAction(e -> CacheHelper.clearViewCache(tileMap));
		clear.setOnAction(e -> tileMap.clearSelection());
		copy.setOnAction(e -> DialogHelper.copySelectedChunks(tileMap));
		paste.setOnAction(e -> DialogHelper.pasteSelectedChunks(tileMap, primaryStage));
		exportChunks.setOnAction(e -> DialogHelper.exportSelectedChunks(tileMap, primaryStage));
		importChunks.setOnAction(e -> DialogHelper.importChunks(tileMap, primaryStage));
		delete.setOnAction(e -> DialogHelper.deleteSelection(tileMap, primaryStage));
		importSelection.setOnAction(e -> DialogHelper.importSelection(tileMap, primaryStage));
		exportSelection.setOnAction(e -> DialogHelper.exportSelection(tileMap, primaryStage));
		clearSelectionCache.setOnAction(e -> CacheHelper.clearSelectionCache(tileMap));
		filterChunks.setOnAction(e -> DialogHelper.filterChunks(tileMap, primaryStage));
		changeFields.setOnAction(e -> DialogHelper.changeFields(tileMap, primaryStage));
		editNBT.setOnAction(e -> DialogHelper.editNBT(tileMap, primaryStage));
		swapChunks.setOnAction(e -> DialogHelper.swapChunks(tileMap, primaryStage));


		open.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCodeCombination.SHORTCUT_DOWN));
		quit.setAccelerator(new KeyCodeCombination(KeyCode.Q, KeyCodeCombination.SHORTCUT_DOWN));
		chunkGrid.setAccelerator(new KeyCodeCombination(KeyCode.T, KeyCodeCombination.SHORTCUT_DOWN));
		regionGrid.setAccelerator(new KeyCodeCombination(KeyCode.R, KeyCodeCombination.SHORTCUT_DOWN));
		goTo.setAccelerator(new KeyCodeCombination(KeyCode.G, KeyCodeCombination.SHORTCUT_DOWN));
		clearAllCache.setAccelerator(new KeyCodeCombination(KeyCode.K, KeyCodeCombination.SHORTCUT_DOWN, KeyCodeCombination.SHIFT_DOWN));
		clearViewCache.setAccelerator(new KeyCodeCombination(KeyCode.K, KeyCodeCombination.SHORTCUT_DOWN));
		clear.setAccelerator(new KeyCodeCombination(KeyCode.L, KeyCodeCombination.SHORTCUT_DOWN));
		copy.setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCodeCombination.SHORTCUT_DOWN));
		paste.setAccelerator(new KeyCodeCombination(KeyCode.V, KeyCodeCombination.SHORTCUT_DOWN));
		exportChunks.setAccelerator(new KeyCodeCombination(KeyCode.E, KeyCodeCombination.SHORTCUT_DOWN, KeyCodeCombination.SHIFT_DOWN));
		importChunks.setAccelerator(new KeyCodeCombination(KeyCode.I, KeyCodeCombination.SHORTCUT_DOWN, KeyCodeCombination.SHIFT_DOWN));
		delete.setAccelerator(new KeyCodeCombination(KeyCode.D, KeyCodeCombination.SHORTCUT_DOWN));
		importSelection.setAccelerator(new KeyCodeCombination(KeyCode.I, KeyCodeCombination.SHORTCUT_DOWN));
		exportSelection.setAccelerator(new KeyCodeCombination(KeyCode.E, KeyCodeCombination.SHORTCUT_DOWN));
		clearSelectionCache.setAccelerator(new KeyCodeCombination(KeyCode.J, KeyCodeCombination.SHORTCUT_DOWN));
		filterChunks.setAccelerator(new KeyCodeCombination(KeyCode.F, KeyCodeCombination.SHORTCUT_DOWN));
		changeFields.setAccelerator(new KeyCodeCombination(KeyCode.N, KeyCodeCombination.SHORTCUT_DOWN));
		editNBT.setAccelerator(new KeyCodeCombination(KeyCode.B, KeyCodeCombination.SHORTCUT_DOWN));
		swapChunks.setAccelerator(new KeyCodeCombination(KeyCode.M, KeyCodeCombination.SHORTCUT_DOWN));

		setSelectionDependentMenuItemsEnabled(tileMap.getSelectedChunks());
		setWorldDependentMenuItemsEnabled(false, tileMap);

		Toolkit.getDefaultToolkit().getSystemClipboard().addFlavorListener(e -> paste.setDisable(!hasValidClipboardContent(tileMap) || tileMap.getDisabled()));
	}

	private void onUpdate(TileMap tileMap) {
		int selectedChunks = tileMap.getSelectedChunks();
		if (previousSelectedChunks != selectedChunks) {
			setSelectionDependentMenuItemsEnabled(selectedChunks);
		}
		previousSelectedChunks = selectedChunks;
	}

	public void setWorldDependentMenuItemsEnabled(boolean enabled, TileMap tileMap) {
		filterChunks.setDisable(!enabled);
		changeFields.setDisable(!enabled);
		importChunks.setDisable(!enabled);
		copy.setDisable(!enabled);
		paste.setDisable(!enabled || !hasValidClipboardContent(tileMap));
	}

	private void setSelectionDependentMenuItemsEnabled(int selected) {
		clear.setDisable(selected == 0);
		exportChunks.setDisable(selected == 0);
		exportSelection.setDisable(selected == 0);
		delete.setDisable(selected == 0);
		clearSelectionCache.setDisable(selected == 0);
		editNBT.setDisable(selected != 1);
		swapChunks.setDisable(selected != 2);
		copy.setDisable(selected == 0);
	}

	private boolean hasValidClipboardContent(TileMap tileMap) {
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		Transferable content = clipboard.getContents(tileMap);
		DataFlavor[] flavors = content.getTransferDataFlavors();
		return flavors.length == 1 && flavors[0].equals(TileMapSelection.SELECTION_DATA_FLAVOR);
	}
}
