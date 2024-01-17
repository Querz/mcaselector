package net.querz.mcaselector.ui.dialog;

import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.querz.mcaselector.changer.ChangeParser;
import net.querz.mcaselector.changer.Field;
import net.querz.mcaselector.changer.FieldType;
import net.querz.mcaselector.changer.fields.ScriptField;
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.io.mca.RegionChunk;
import net.querz.mcaselector.io.mca.RegionMCAFile;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.property.DataProperty;
import net.querz.mcaselector.selection.Selection;
import net.querz.mcaselector.text.Translation;
import net.querz.mcaselector.tile.TileMap;
import net.querz.mcaselector.ui.UIFactory;
import net.querz.mcaselector.ui.component.GroovyCodeArea;
import net.querz.mcaselector.validation.BeforeAfterCallback;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fxmisc.flowless.VirtualizedScrollPane;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChangeNBTDialog extends Dialog<ChangeNBTDialog.Result> {

	private static final Logger LOGGER = LogManager.getLogger(ChangeNBTDialog.class);

	private static final String initScript = """
			import net.querz.mcaselector.io.mca.ChunkData;
			import net.querz.nbt.*;

			void before() {
			\t
			}
			
			void apply(ChunkData data) {
			\t
			}
			
			void after() {
			\t
			}""";

	private final List<Field<?>> fields = new ArrayList<>();
	private final TabPane tabs = new TabPane();
	private final TextField changeQuery = new TextField();
	private final RadioButton change = UIFactory.radio(Translation.DIALOG_CHANGE_NBT_CHANGE);
	private final RadioButton force = UIFactory.radio(Translation.DIALOG_CHANGE_NBT_FORCE);
	private final CheckBox selectionOnly = UIFactory.checkbox(Translation.DIALOG_CHANGE_NBT_SELECTION_ONLY);
	private static final GroovyCodeArea codeArea = new GroovyCodeArea(true);

	static {
		codeArea.setText(initScript);
	}

	public ChangeNBTDialog(TileMap tileMap, Stage primaryStage) {
		titleProperty().bind(Translation.DIALOG_CHANGE_NBT_TITLE.getProperty());

		initStyle(StageStyle.UTILITY);

		setResizable(true);

		getDialogPane().getStyleClass().add("change-nbt-dialog-pane");

		setResultConverter(p -> {
			if (p != ButtonType.OK) {
				return null;
			}
			if (tabs.getSelectionModel().isSelected(0)) {
				for (int i = 0; i < fields.size(); i++) {
					if (!fields.get(i).needsChange()) {
						fields.remove(i);
						i--;
					}
				}
				if (fields.isEmpty()) {
					return null;
				}
				return new Result(fields, force.isSelected(), selectionOnly.isSelected(), null);
			} else {
				ScriptField scriptField = new ScriptField();
				scriptField.parseNewValue(codeArea.getText());
				if (!scriptField.needsChange()) {
					return null;
				}
				return new Result(Collections.singletonList(scriptField), force.isSelected(), selectionOnly.isSelected(), scriptField);
			}
		});

		// apply same stylesheets to this dialog
		getDialogPane().getStylesheets().addAll(primaryStage.getScene().getStylesheets());
		getDialogPane().getStylesheets().add(ChangeNBTDialog.class.getClassLoader().getResource("style/component/change-nbt-dialog.css").toExternalForm());

		getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

		FieldView fieldView = new FieldView();
		for (FieldType fieldType : FieldType.uiValues()) {
			Field<?> field = fieldType.newInstance();
			fieldView.addField(field);
			fields.add(field);
		}

		if (tileMap.getSelectedChunks() == 1) {
			readSingleChunkAsync(tileMap, fieldView);
		}

		ToggleGroup toggleGroup = new ToggleGroup();
		toggleGroup.getToggles().addAll(change, force);
		change.fire();

		ScrollPane scrollPane = new ScrollPane();
		scrollPane.setContent(fieldView);
		fieldView.prefWidthProperty().bind(scrollPane.widthProperty());

		VBox actionBox = new VBox();
		change.setTooltip(UIFactory.tooltip(Translation.DIALOG_CHANGE_NBT_CHANGE_TOOLTIP));
		force.setTooltip(UIFactory.tooltip(Translation.DIALOG_CHANGE_NBT_FORCE_TOOLTIP));
		actionBox.getChildren().addAll(change, force);

		VBox optionBox = new VBox();
		selectionOnly.setTooltip(UIFactory.tooltip(Translation.DIALOG_CHANGE_NBT_SELECTION_ONLY_TOOLTIP));
		selectionOnly.setSelected(true);
		optionBox.getChildren().add(selectionOnly);

		changeQuery.setOnAction(e -> {
			String text = changeQuery.getText();
			int caret = changeQuery.getCaretPosition();
			ChangeParser cp = new ChangeParser(text);
			try {
				List<Field<?>> f = cp.parse();
				fieldView.updateFields(f);
			} catch (Exception ex) {
				LOGGER.warn("failed to parse change query from: {}, error: {}", changeQuery.getText(), ex.getMessage());
				fieldView.updateFields(Collections.emptyList());
			}
			changeQuery.setText(text);
			changeQuery.positionCaret(caret);
		});

		HBox selectionBox = new HBox();
		selectionBox.getChildren().addAll(actionBox, optionBox);

		VBox mainBox = new VBox();
		mainBox.getChildren().addAll(scrollPane, new Separator(), changeQuery);

		VBox scriptBox = new VBox();
		StackPane scriptPane = new StackPane(new VirtualizedScrollPane<>(codeArea));
		Label errorLabel = new Label();
		errorLabel.getStyleClass().add("script-error-label");
		errorLabel.textProperty().bind(codeArea.errorProperty());
		VBox.setVgrow(scriptPane, Priority.ALWAYS);
		scriptBox.getChildren().addAll(scriptPane, errorLabel);

		Tab mainTab = UIFactory.tab(Translation.DIALOG_CHANGE_NBT_TAB_QUERY);
		mainTab.setContent(mainBox);
		Tab scriptTab = UIFactory.tab(Translation.DIALOG_CHANGE_NBT_TAB_SCRIPT);
		scriptTab.setContent(scriptBox);

		tabs.getTabs().addAll(mainTab, scriptTab);

		VBox panelBox = new VBox();
		panelBox.getChildren().addAll(tabs, new Separator(), selectionBox);
		getDialogPane().setContent(panelBox);
	}

	private void readSingleChunkAsync(TileMap tileMap, FieldView fieldView) {
		new Thread(() -> {
			Selection selection = tileMap.getSelection();
			DataProperty<Point2i> region = new DataProperty<>();
			DataProperty<Point2i> chunk = new DataProperty<>();
			selection.forEach(e -> {
				region.set(new Point2i(e.getLongKey()));
				e.getValue().forEach(c -> chunk.set(new Point2i(c)));
			});
			File file = FileHelper.createMCAFilePath(region.get());
			LOGGER.debug("attempting to read single chunk from file: {}", chunk.get());
			if (file.exists()) {
				try {
					// only load region for now, there is no field in entities or poi that we could display
					RegionChunk regionChunk = new RegionMCAFile(file).loadSingleChunk(chunk.get());
					ChunkData chunkData = new ChunkData(regionChunk, null, null, true);

					fieldView.getChildren().forEach(child -> {
						FieldCell cell = (FieldCell) child;
						Object oldValue = cell.value.getOldValue(chunkData);
						String promptText = oldValue == null ? "" : oldValue.toString();
						Platform.runLater(() -> cell.textField.setPromptText(promptText));
					});
				} catch (IOException ex) {
					LOGGER.warn("failed to load single chunk", ex);
				}
			}
		}).start();
	}

	private class FieldView extends VBox {

		public FieldView() {
			getStyleClass().add("field-view");
		}

		public void addField(Field<?> field) {
			FieldCell fieldCell = new FieldCell(field, getChildren().size());
			fieldCell.prefWidthProperty().bind(widthProperty());
			getChildren().add(fieldCell);
		}

		public void updateFields(List<Field<?>> fields) {
			childLoop:
			for (Node child : getChildren()) {
				if (child instanceof FieldCell fieldCell) {
					for (Field<?> newField : fields) {
						if (newField.getType() == fieldCell.value.getType()) {
							fieldCell.value.setNewValueRaw(newField.getNewValue());
							fieldCell.textField.setText(newField.valueToString());
							fieldCell.textField.positionCaret(fieldCell.textField.getText().length());
							continue childLoop;
						}
					}
					fieldCell.textField.setText("");
				}
			}
		}
	}

	private class FieldCell extends HBox {

		private final Field<?> value;
		private final TextField textField;

		private static final PseudoClass valid = PseudoClass.getPseudoClass("valid");
		private static final PseudoClass even = PseudoClass.getPseudoClass("even");
		private static final PseudoClass odd = PseudoClass.getPseudoClass("odd");

		public FieldCell(Field<?> value, int index) {
			getStyleClass().add("field-cell");
			if (index % 2 == 0) {
				pseudoClassStateChanged(even, true);
			} else {
				pseudoClassStateChanged(odd, true);
			}
			this.value = value;
			textField = new TextField();
			textField.getStyleClass().add("field-cell-text");
			getChildren().addAll(new Label(value.getType().toString()), textField);
			textField.textProperty().addListener((a, o, n) -> onInput(n));
			textField.setAlignment(Pos.CENTER);
			textField.prefWidthProperty().bind(prefWidthProperty());
		}

		private void onInput(String newValue) {
			boolean result = value.parseNewValue(newValue);
			textField.pseudoClassStateChanged(valid, result);

			StringBuilder sb = new StringBuilder();
			boolean first = true;
			for (Field<?> field : fields) {
				if (field.needsChange()) {
					sb.append(first ? "" : ", ").append(field);
					first = false;
				}
			}
			if (sb.length() > 0) {
				if (result) {
					LOGGER.debug(sb);
				}
				changeQuery.setText(sb.toString());
			} else {
				changeQuery.setText(null);
			}
		}
	}

	public record Result(List<Field<?>> fields, boolean force, boolean selectionOnly, ScriptField script) implements BeforeAfterCallback {
		public boolean requiresClearCache() {
				for (Field<?> field : fields) {
					if (field.getType().requiresClearCache()) {
						return true;
					}
				}
				return false;
			}

		@Override
		public void before() {
			script.before();
		}

		@Override
		public void after() {
			script.after();
		}

		@Override
		public boolean valid() {
			return script != null;
		}
	}
}
