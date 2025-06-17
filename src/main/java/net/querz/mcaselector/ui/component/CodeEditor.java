package net.querz.mcaselector.ui.component;

import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import net.querz.mcaselector.config.Config;
import net.querz.mcaselector.config.GlobalConfig;
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.text.Translation;
import net.querz.mcaselector.ui.DialogHelper;
import net.querz.mcaselector.ui.UIFactory;
import net.querz.mcaselector.ui.dialog.ConfirmationDialog;
import net.querz.mcaselector.ui.dialog.ErrorDialog;
import net.querz.mcaselector.util.property.DataProperty;
import org.fxmisc.flowless.VirtualizedScrollPane;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public class CodeEditor extends StackPane {

	private static final String newFileName = "script.groovy";

	private final GroovyCodeArea codeArea = new GroovyCodeArea(true);
	private final Menu recentFilesMenu;
	private final DataProperty<Boolean> initTextEval = new DataProperty<>(true);
	private boolean saved = true;
	private Consumer<File> onSave;
	private File sourceFile;
	private Window owner;
	private Config.RecentFiles recentFiles;

	Label fileNameLabel = new Label("* " + newFileName);

	public CodeEditor(String initText) {
		codeArea.setChangeListener(() -> {
			if (!initTextEval.get()) {
				saved = false;
				updateFileNameLabel();
			} else {
				initTextEval.set(false);
			}
		});
		codeArea.setText(initText);

		getStylesheets().add(Objects.requireNonNull(CodeEditor.class.getClassLoader().getResource("style/component/code-editor.css")).toExternalForm());

		getStyleClass().add("code-editor-script-box");
		StackPane scriptPane = new StackPane(new VirtualizedScrollPane<>(codeArea));
		scriptPane.getStyleClass().add("script-pane");
		Label errorLabel = new Label();
		errorLabel.getStyleClass().add("script-error-label");
		errorLabel.textProperty().bind(codeArea.errorProperty());
		VBox.setVgrow(scriptPane, Priority.ALWAYS);

		MenuBar menuBar = new MenuBar();
		Menu fileMenu = UIFactory.menu(Translation.MENU_FILE);

		MenuItem newMenu = UIFactory.menuItem(Translation.MENU_FILE_NEW);
		newMenu.setAccelerator(new KeyCodeCombination(KeyCode.N, KeyCodeCombination.SHORTCUT_DOWN));
		newMenu.setOnAction(e -> unsavedChangesConfirmation(() -> {
			sourceFile = null;
			initTextEval.set(true);
			codeArea.setText(initText);
			updateFileNameLabel();
			if (recentFiles != null) {
				recentFiles.addRecentFile(sourceFile);
				setRecentFiles(recentFiles);
			}
		}));

		MenuItem openMenu = UIFactory.menuItem(Translation.MENU_FILE_OPEN);
		openMenu.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCodeCombination.SHORTCUT_DOWN));
		openMenu.setOnAction(e -> unsavedChangesConfirmation(() -> openFile(null)));

		MenuItem saveAsMenu = UIFactory.menuItem(Translation.MENU_FILE_SAVE_AS);
		saveAsMenu.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCodeCombination.SHORTCUT_DOWN, KeyCodeCombination.SHIFT_DOWN));
		saveAsMenu.setOnAction(e -> save(true));

		MenuItem saveMenu = UIFactory.menuItem(Translation.MENU_FILE_SAVE);
		saveMenu.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCodeCombination.SHORTCUT_DOWN));
		saveMenu.setOnAction(e -> save(false));

		recentFilesMenu = UIFactory.menu(Translation.MENU_FILE_OPEN_RECENT);

		menuBar.getMenus().addAll(fileMenu);
		fileMenu.getItems().addAll(newMenu, UIFactory.separator(), openMenu, recentFilesMenu, UIFactory.separator(), saveMenu, saveAsMenu);

		fileNameLabel.getStyleClass().add("file-name-label");

		BorderPane menuPane = new BorderPane();
		menuPane.setLeft(menuBar);
		menuPane.setRight(fileNameLabel);

		VBox box = new VBox();
		box.getChildren().addAll(menuPane, scriptPane, errorLabel);
		getChildren().add(box);
	}

	private void openFile(File file) {
		if (file == null) {
			file = DialogHelper.createFileChooser(FileHelper.getLastOpenedDirectory("code_editor", null),
					new FileChooser.ExtensionFilter("*.groovy Files", "*.groovy")).showOpenDialog(owner);
		}
		if (file != null) {
			try {
				String s = Files.readString(file.toPath());
				initTextEval.set(true);
				codeArea.setText(s);
				FileHelper.setLastOpenedDirectory("code_editor", file.getParent());
				sourceFile = file;
				saved = true;
				updateFileNameLabel();
				if (recentFiles != null) {
					recentFiles.addRecentFile(file);
					setRecentFiles(recentFiles);
				}
			} catch (IOException ex) {
				new ErrorDialog(owner, ex);
			}
		}
	}

	private void unsavedChangesConfirmation(Runnable action) {
		if (!saved) {
			Optional<ButtonType> result = new ConfirmationDialog(owner,
					Translation.DIALOG_UNSAVED_SCRIPT_TITLE, Translation.DIALOG_UNSAVED_SCRIPT_HEADER,
					"unsaved-changes").showAndWait();
			result.ifPresent(r -> {
				if (r == ButtonType.OK) {
					action.run();
				}
			});
		} else {
			action.run();
		}
	}

	private boolean save(File file) {
		try {
			Files.writeString(file.toPath(), codeArea.getText(), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
			FileHelper.setLastOpenedDirectory("code_editor", file.getParent());
			sourceFile = file;
			saved = true;
			updateFileNameLabel();
			if (recentFiles != null) {
				recentFiles.addRecentFile(file);
				setRecentFiles(recentFiles);
			}
			if (onSave != null) {
				onSave.accept(file);
			}
			return true;
		} catch (IOException ex) {
			new ErrorDialog(owner, ex);
			return false;
		}
	}

	public boolean save(boolean newSave) {
		if (newSave || sourceFile == null) {
			File file = DialogHelper.createFileChooser(FileHelper.getLastOpenedDirectory("code_editor", null),
					new FileChooser.ExtensionFilter("*.groovy Files", "*.groovy")).showSaveDialog(owner);
			if (file != null) {
				return save(file);
			}
			return false;
		} else {
			return save(sourceFile);
		}
	}

	public boolean isSaved() {
		return saved;
	}

	public void setOnSave(Consumer<File> onSave) {
		this.onSave = onSave;
	}

	private void updateFileNameLabel() {
		if (saved) {
			fileNameLabel.setText(sourceFile == null ? "* " + newFileName : sourceFile.getName());
		} else if (sourceFile == null) {
			fileNameLabel.setText("* " + newFileName);
		} else {
			fileNameLabel.setText("* " + sourceFile.getName());
		}
	}

	public void setOwner(Window owner) {
		if (this.owner == null) {
			List<String> t = getStylesheets();
			// insert owner's style sheets before the last element to maintain hierarchical order
			owner.getScene().getStylesheets().forEach(s -> getStylesheets().add(t.size() - 1, s));
		}
		this.owner = owner;
	}

	public void setRecentFiles(Config.RecentFiles recentFiles) {
		this.recentFiles = recentFiles;
		if (!recentFiles.isEmpty()) {
			recentFilesMenu.getItems().clear();
			recentFiles.descendingMap().forEach((k, v) -> {
				if (!v.equals(sourceFile)) {
					MenuItem openRecentItem = new MenuItem(v.toString());
					openRecentItem.setMnemonicParsing(false);
					openRecentItem.setOnAction(e -> unsavedChangesConfirmation(() -> openFile(v)));
					recentFilesMenu.getItems().add(openRecentItem);
				}
			});
			recentFilesMenu.getItems().add(UIFactory.separator());
			MenuItem clear = UIFactory.menuItem(Translation.MENU_FILE_OPEN_RECENT_CLEAR);
			clear.setOnAction(e -> {
				recentFilesMenu.getItems().clear();
				recentFiles.clear();
				recentFilesMenu.setDisable(true);
			});
			recentFilesMenu.getItems().add(clear);
		}
		recentFilesMenu.setDisable(recentFilesMenu.getItems().size() <= 2);
	}

	public void setSource(GlobalConfig.TempScript tempScript) {
		if (tempScript.text() == null || tempScript.text().isEmpty()) {
			return;
		}
		boolean exists = tempScript.file() == null || tempScript.file().exists();
		sourceFile = exists ? tempScript.file() : null;
		codeArea.setText(tempScript.text());
		this.saved = exists && tempScript.saved();
		updateFileNameLabel();
	}

	public GlobalConfig.TempScript getSource() {
		return new GlobalConfig.TempScript(sourceFile, saved, getText());
	}

	public String getText() {
		return codeArea.getText();
	}
}
