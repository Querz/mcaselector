package net.querz.mcaselector.ui;

import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.querz.mcaselector.filter.Comparator;
import net.querz.mcaselector.filter.GroupFilter;
import net.querz.mcaselector.filter.Operator;
import net.querz.mcaselector.filter.XPosFilter;
import net.querz.mcaselector.filter.ZPosFilter;
import net.querz.mcaselector.util.Debug;

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
	private RadioButton select = new RadioButton("Select");
	private RadioButton export = new RadioButton("Export");
	private RadioButton delete = new RadioButton("Delete");
	private CheckBox selectionOnly = new CheckBox("Apply to selection only");

	public FilterChunksDialog(Stage primaryStage) {
		setTitle("Filter chunks");

		initStyle(StageStyle.UTILITY);

		getDialogPane().getStyleClass().add("filter-dialog-pane");

		setResultConverter(p -> p == ButtonType.OK ? new Result(value, getHandleType(), selectionOnly.isSelected()) : null);

		//apply same stylesheets to this dialog
		getDialogPane().getStylesheets().addAll(primaryStage.getScene().getStylesheets());

		getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

		select.setTooltip(new Tooltip("Creates a selection of the affected chunks."));
		export.setTooltip(new Tooltip("Exports all affected chunks into a new directory."));
		delete.setTooltip(new Tooltip("Deletes all affected chunk from the current world."));

		toggleGroup.getToggles().addAll(select, export, delete);
		toggleGroup.selectedToggleProperty().addListener(l -> selectionOnly.setDisable(select.isSelected()));
		select.fire();

		setResizable(true);

		ScrollPane scrollPane = new ScrollPane();
		scrollPane.setContent(groupFilterBox);
		groupFilterBox.prefWidthProperty().bind(scrollPane.prefWidthProperty());

		groupFilterBox.setOnUpdate(f -> {
			getDialogPane().lookupButton(ButtonType.OK).setDisable(!value.isValid());
			Debug.dump(value);
		});

		VBox actionBox = new VBox();
		actionBox.getChildren().addAll(select, export, delete);

		VBox optionBox =  new VBox();
		selectionOnly.setTooltip(new Tooltip("If this filter should only apply to the current selection."));
		optionBox.getChildren().add(selectionOnly);

		HBox selectionBox = new HBox();
		selectionBox.getChildren().addAll(actionBox, optionBox);

		VBox box = new VBox();
		box.getChildren().addAll(scrollPane, new Separator(), selectionBox);
		getDialogPane().setContent(box);
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

	public class Result {

		private HandleType type;
		private boolean selectionOnly;
		private GroupFilter filter;

		public Result(GroupFilter filter, HandleType type, boolean selectionOnly) {
			this.filter = filter;
			this.type = type;
			this.selectionOnly = selectionOnly;
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
	}

	public enum HandleType {
		SELECT, EXPORT, DELETE
	}
}
