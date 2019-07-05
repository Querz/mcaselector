package net.querz.mcaselector.ui;

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
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.querz.mcaselector.changer.Field;
import net.querz.mcaselector.changer.FieldType;
import net.querz.mcaselector.util.Translation;
import net.querz.mcaselector.util.UIFactory;

import java.util.ArrayList;
import java.util.List;

public class ChangeNBTDialog extends Dialog<ChangeNBTDialog.Result> {

	 /*
	 * List of fields that can be changed
	 * () change () force
	 * change --> only set if the value existed before
	 * force --> set value even if it didn't exist.
	 * */

	private List<Field> value = new ArrayList<>();
	private ToggleGroup toggleGroup = new ToggleGroup();
	private RadioButton change = UIFactory.radio(Translation.DIALOG_CHANGE_NBT_CHANGE);
	private RadioButton force = UIFactory.radio(Translation.DIALOG_CHANGE_NBT_FORCE);
	private CheckBox selectionOnly = UIFactory.checkbox(Translation.DIALOG_CHANGE_NBT_SELECTION_ONLY);

	public ChangeNBTDialog(Stage primaryStage) {
		titleProperty().bind(Translation.DIALOG_CHANGE_NBT_TITLE.getProperty());

		initStyle(StageStyle.UTILITY);

		getDialogPane().getStyleClass().add("change-nbt-dialog-pane");

		setResultConverter(p -> {
			if (p == ButtonType.OK) {
				for (int i = 0; i < value.size(); i++) {
					if (!value.get(i).needsChange()) {
						value.remove(i);
						i--;
					}
				}
				return value.isEmpty() ? null : new Result(value, force.isSelected(), selectionOnly.isSelected());
			}
			return null;
		});

		//apply same stylesheets to this dialog
		getDialogPane().getStylesheets().addAll(primaryStage.getScene().getStylesheets());

		getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

		FieldView fw = new FieldView();
		for (FieldType ft : FieldType.values()) {
			Field f = ft.newInstance();
			fw.addField(f);
			value.add(f);
		}

		toggleGroup.getToggles().addAll(change, force);
		change.fire();

		ScrollPane scrollPane = new ScrollPane();
		scrollPane.setContent(fw);

		VBox actionBox = new VBox();
		change.setTooltip(UIFactory.tooltip(Translation.DIALOG_CHANGE_NBT_CHANGE_TOOLTIP));
		force.setTooltip(UIFactory.tooltip(Translation.DIALOG_CHANGE_NBT_FORCE_TOOLTIP));
		actionBox.getChildren().addAll(change, force);

		VBox optionBox = new VBox();
		selectionOnly.setTooltip(UIFactory.tooltip(Translation.DIALOG_CHANGE_NBT_SELECTION_ONLY_TOOLTIP));
		optionBox.getChildren().add(selectionOnly);

		HBox selectionBox = new HBox();
		selectionBox.getChildren().addAll(actionBox, optionBox);

		VBox box = new VBox();
		box.getChildren().addAll(scrollPane, new Separator(), selectionBox);
		getDialogPane().setContent(box);
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
			getChildren().addAll(new Label(value.toString()), textField);
			textField.textProperty().addListener((a, o, n) -> onInput(n));
			textField.setAlignment(Pos.CENTER);
		}

		private void onInput(String newValue) {
			Boolean result = value.parseNewValue(newValue);
			if (result) {
				if (!textField.getStyleClass().contains("field-cell-valid")) {
					textField.getStyleClass().add("field-cell-valid");
				}
			} else {
				textField.getStyleClass().remove("field-cell-valid");
			}
		}
	}

	public class Result {

		private boolean force;
		private List<Field> fields;
		private boolean selectionOnly;

		public Result(List<Field> fields, boolean force, boolean selectionOnly) {
			this.force = force;
			this.fields = fields;
			this.selectionOnly = selectionOnly;
		}

		public boolean isForce() {
			return force;
		}

		public List<Field> getFields() {
			return fields;
		}

		public boolean isSelectionOnly() {
			return selectionOnly;
		}
	}
}
