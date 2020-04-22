package net.querz.mcaselector.ui;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.querz.mcaselector.changer.Field;
import net.querz.mcaselector.changer.FieldType;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.io.MCAChunkData;
import net.querz.mcaselector.io.MCAFile;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.property.DataProperty;
import net.querz.mcaselector.text.Translation;
import net.querz.mcaselector.tiles.TileMap;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ChangeNBTDialog extends Dialog<ChangeNBTDialog.Result> {

	private List<Field<?>> fields = new ArrayList<>();
	private ToggleGroup toggleGroup = new ToggleGroup();
	private RadioButton change = UIFactory.radio(Translation.DIALOG_CHANGE_NBT_CHANGE);
	private RadioButton force = UIFactory.radio(Translation.DIALOG_CHANGE_NBT_FORCE);
	private CheckBox selectionOnly = UIFactory.checkbox(Translation.DIALOG_CHANGE_NBT_SELECTION_ONLY);

	public ChangeNBTDialog(TileMap tileMap, Stage primaryStage) {
		titleProperty().bind(Translation.DIALOG_CHANGE_NBT_TITLE.getProperty());

		initStyle(StageStyle.UTILITY);

		getDialogPane().getStyleClass().add("change-nbt-dialog-pane");

		setResultConverter(p -> {
			if (p == ButtonType.OK) {
				for (int i = 0; i < fields.size(); i++) {
					if (!fields.get(i).needsChange()) {
						fields.remove(i);
						i--;
					}
				}
				return fields.isEmpty() ? null : new Result(fields, force.isSelected(), selectionOnly.isSelected());
			}
			return null;
		});

		//apply same stylesheets to this dialog
		getDialogPane().getStylesheets().addAll(primaryStage.getScene().getStylesheets());

		getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

		FieldView fieldView = new FieldView();
		for (FieldType fieldType : FieldType.uiValues()) {
			Field field = fieldType.newInstance();
			fieldView.addField(field);
			fields.add(field);
		}

		if (tileMap.getSelectedChunks() == 1) {
			readSingleChunkAsync(tileMap, fieldView);
		}

		toggleGroup.getToggles().addAll(change, force);
		change.fire();

		ScrollPane scrollPane = new ScrollPane();
		scrollPane.setContent(fieldView);

		VBox actionBox = new VBox();
		change.setTooltip(UIFactory.tooltip(Translation.DIALOG_CHANGE_NBT_CHANGE_TOOLTIP));
		force.setTooltip(UIFactory.tooltip(Translation.DIALOG_CHANGE_NBT_FORCE_TOOLTIP));
		actionBox.getChildren().addAll(change, force);

		VBox optionBox = new VBox();
		selectionOnly.setTooltip(UIFactory.tooltip(Translation.DIALOG_CHANGE_NBT_SELECTION_ONLY_TOOLTIP));
		selectionOnly.setSelected(true);
		optionBox.getChildren().add(selectionOnly);

		HBox selectionBox = new HBox();
		selectionBox.getChildren().addAll(actionBox, optionBox);

		VBox box = new VBox();
		box.getChildren().addAll(scrollPane, new Separator(), selectionBox);
		getDialogPane().setContent(box);
	}

	private void readSingleChunkAsync(TileMap tileMap, FieldView fieldView) {
		new Thread(() -> {
			Map<Point2i, Set<Point2i>> selection = tileMap.getMarkedChunks();
			DataProperty<Point2i> region = new DataProperty<>();
			DataProperty<Point2i> chunk = new DataProperty<>();
			selection.forEach((k, v) -> {
				region.set(k);
				v.forEach(chunk::set);
			});
			File file = FileHelper.createMCAFilePath(region.get());
			Debug.dumpf("attempting to read single chunk from file: %s", chunk.get());
			if (file.exists()) {
				MCAChunkData chunkData = MCAFile.readSingleChunk(file, chunk.get());
				if (chunkData == null) {
					return;
				}
				fieldView.getChildren().forEach(child -> {
					FieldCell cell = (FieldCell) child;
					Object oldValue = cell.value.getOldValue(chunkData.getData());
					String promptText = oldValue == null ? "" : oldValue.toString();
					Platform.runLater(() -> cell.textField.setPromptText(promptText));
				});
			}
		}).start();
	}

	private class FieldView extends VBox {

		public FieldView() {
			getStyleClass().add("field-view");
		}

		public void addField(Field field) {
			getChildren().add(new FieldCell(field, getChildren().size()));
		}
	}

	private class FieldCell extends HBox {

		private Field value;
		private TextField textField;

		public FieldCell(Field value, int index) {
			getStyleClass().add("field-cell");
			getStyleClass().add("field-cell-" + (index % 2 == 0 ? "even" : "odd"));
			this.value = value;
			textField = new TextField();
			getChildren().addAll(new Label(value.getType().toString()), textField);
			textField.textProperty().addListener((a, o, n) -> onInput(n));
			textField.setAlignment(Pos.CENTER);
		}

		private void onInput(String newValue) {
			boolean result = value.parseNewValue(newValue);
			if (result) {
				if (!textField.getStyleClass().contains("field-cell-valid")) {
					textField.getStyleClass().add("field-cell-valid");
				}

				StringBuilder sb = new StringBuilder();
				boolean first = true;
				for (Field field : fields) {
					if (field.needsChange()) {
						sb.append(first ? "" : ", ").append(field);
						first = false;
					}
				}
				if (sb.length() > 0) {
					Debug.dump(sb);
				}
			} else {
				textField.getStyleClass().remove("field-cell-valid");
			}
		}
	}

	public static class Result {

		private boolean force;
		private List<Field<?>> fields;
		private boolean selectionOnly;

		public Result(List<Field<?>> fields, boolean force, boolean selectionOnly) {
			this.force = force;
			this.fields = fields;
			this.selectionOnly = selectionOnly;
		}

		public boolean isForce() {
			return force;
		}

		public List<Field<?>> getFields() {
			return fields;
		}

		public boolean isSelectionOnly() {
			return selectionOnly;
		}
	}
}
