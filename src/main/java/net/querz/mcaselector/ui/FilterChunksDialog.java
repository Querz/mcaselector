package net.querz.mcaselector.ui;

import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.querz.mcaselector.filter.Comparator;
import net.querz.mcaselector.filter.GroupFilter;
import net.querz.mcaselector.filter.Operator;
import net.querz.mcaselector.filter.XPosFilter;
import net.querz.mcaselector.filter.ZPosFilter;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.text.Translation;

public class FilterChunksDialog extends Dialog<FilterChunksDialog.Result> {

	private static GroupFilter gf = new GroupFilter();
	static {
		gf.addFilter(new XPosFilter(Operator.AND, Comparator.SEQ, 100));
		gf.addFilter(new XPosFilter(Operator.AND, Comparator.LEQ, -100));
		gf.addFilter(new ZPosFilter(Operator.AND, Comparator.SEQ, 100));
		gf.addFilter(new ZPosFilter(Operator.AND, Comparator.LEQ, -100));
	}

	private GroupFilter value = gf;
	private GroupFilterBox groupFilterBox = new GroupFilterBox(null, value, true);
	private ToggleGroup toggleGroup = new ToggleGroup();
	private RadioButton select = UIFactory.radio(Translation.DIALOG_FILTER_CHUNKS_SELECT);
	private RadioButton export = UIFactory.radio(Translation.DIALOG_FILTER_CHUNKS_EXPORT);
	private RadioButton delete = UIFactory.radio(Translation.DIALOG_FILTER_CHUNKS_DELETE);
	private CheckBox selectionOnly = UIFactory.checkbox(Translation.DIALOG_FILTER_CHUNKS_SELECTION_ONLY);
	private TextField selectionRadius = new TextField();

	private int radius;

	public FilterChunksDialog(Stage primaryStage) {
		titleProperty().bind(Translation.DIALOG_FILTER_CHUNKS_TITLE.getProperty());

		initStyle(StageStyle.UTILITY);

		getDialogPane().getStyleClass().add("filter-dialog-pane");

		setResultConverter(p -> p == ButtonType.OK ? new Result(value, getHandleType(), selectionOnly.isSelected(), radius) : null);

		//apply same stylesheets to this dialog
		getDialogPane().getStylesheets().addAll(primaryStage.getScene().getStylesheets());

		getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

		select.setTooltip(UIFactory.tooltip(Translation.DIALOG_FILTER_CHUNKS_SELECT_TOOLTIP));
		export.setTooltip(UIFactory.tooltip(Translation.DIALOG_FILTER_CHUNKS_EXPORT_TOOLTIP));
		delete.setTooltip(UIFactory.tooltip(Translation.DIALOG_FILTER_CHUNKS_DELETE_TOOLTIP));

		toggleGroup.getToggles().addAll(select, export, delete);
		toggleGroup.selectedToggleProperty().addListener(l -> selectionOnly.setDisable(select.isSelected()));
		select.fire();

		setResizable(true);

		ScrollPane scrollPane = new ScrollPane();
		scrollPane.setContent(groupFilterBox);
		groupFilterBox.prefWidthProperty().bind(scrollPane.prefWidthProperty());

		groupFilterBox.setOnUpdate(f -> {
			getDialogPane().lookupButton(ButtonType.OK).setDisable(!value.isValid());
			if (value.isValid()) {
				Debug.dump(value);
			}
		});

		selectionRadius.textProperty().addListener((a, o, n) -> onSelectionRadiusInput(o, n));

		VBox actionBox = new VBox();
		actionBox.getChildren().addAll(select, export, delete);

		HBox selectionRadiusBox = new HBox();
		selectionRadiusBox.getStyleClass().add("filter-dialog-selection-radius-box");
		selectionRadiusBox.getChildren().add(UIFactory.label(Translation.DIALOG_FILTER_CHUNKS_SELECTION_RADIUS));
		selectionRadiusBox.getChildren().add(selectionRadius);

		VBox optionBox =  new VBox();
		optionBox.getStyleClass().add("filter-dialog-option-box");
		selectionOnly.setTooltip(UIFactory.tooltip(Translation.DIALOG_FILTER_CHUNKS_SELECTION_ONLY_TOOLTIP));
		optionBox.getChildren().addAll(selectionOnly, selectionRadiusBox);

		HBox selectionBox = new HBox();
		selectionBox.getChildren().addAll(actionBox, optionBox);

		VBox box = new VBox();
		box.getChildren().addAll(scrollPane, new Separator(), selectionBox);
		getDialogPane().setContent(box);
	}

	private void onSelectionRadiusInput(String oldValue, String newValue) {
		if (newValue.isEmpty()) {
			radius = 0;
		} else {
			if (!newValue.matches("[0-9]+")) {
				selectionRadius.setText(oldValue);
				return;
			}
			radius = Integer.parseInt(newValue);
		}
	}

	private HandleType getHandleType() {
		if (select.isSelected()) {
			return HandleType.SELECT;
		} else if (export.isSelected()) {
			return HandleType.EXPORT;
		} else if (delete.isSelected()) {
			return HandleType.DELETE;
		}
		return null;
	}

	public static class Result {

		private HandleType type;
		private boolean selectionOnly;
		private GroupFilter filter;
		private int radius;

		public Result(GroupFilter filter, HandleType type, boolean selectionOnly, int radius) {
			this.filter = filter;
			this.type = type;
			this.selectionOnly = selectionOnly;
			this.radius = radius;
		}

		public HandleType getType() {
			return type;
		}

		public GroupFilter getFilter() {
			return filter;
		}

		public boolean isSelectionOnly() {
			return selectionOnly;
		}

		public int getRadius() {
			return radius;
		}
	}

	public enum HandleType {
		SELECT, EXPORT, DELETE
	}
}
