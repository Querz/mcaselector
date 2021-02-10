package net.querz.mcaselector.ui.dialog;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.filter.Comparator;
import net.querz.mcaselector.filter.GroupFilter;
import net.querz.mcaselector.filter.Operator;
import net.querz.mcaselector.filter.XPosFilter;
import net.querz.mcaselector.filter.ZPosFilter;
import net.querz.mcaselector.filter.FilterParser;
import net.querz.mcaselector.exception.ParseException;
import net.querz.mcaselector.text.Translation;
import net.querz.mcaselector.ui.GroupFilterBox;
import net.querz.mcaselector.ui.UIFactory;

public class FilterChunksDialog extends Dialog<FilterChunksDialog.Result> {

	private static GroupFilter gf = new GroupFilter();
	static {
		gf.addFilter(new XPosFilter(Operator.AND, Comparator.SMALLER_EQUAL, 100));
		gf.addFilter(new XPosFilter(Operator.AND, Comparator.LARGER_EQUAL, -100));
		gf.addFilter(new ZPosFilter(Operator.AND, Comparator.SMALLER_EQUAL, 100));
		gf.addFilter(new ZPosFilter(Operator.AND, Comparator.LARGER_EQUAL, -100));
	}

	private GroupFilter value = gf;
	private final TextField filterQuery = new TextField();
	private final GroupFilterBox groupFilterBox = new GroupFilterBox(null, value, true);
	private final ToggleGroup toggleGroup = new ToggleGroup();
	private final RadioButton select = UIFactory.radio(Translation.DIALOG_FILTER_CHUNKS_SELECT);
	private final RadioButton export = UIFactory.radio(Translation.DIALOG_FILTER_CHUNKS_EXPORT);
	private final RadioButton delete = UIFactory.radio(Translation.DIALOG_FILTER_CHUNKS_DELETE);
	private final Label selectionOnlyLabel = UIFactory.label(Translation.DIALOG_FILTER_CHUNKS_SELECTION_ONLY);
	private final Label selectionRadiusLabel = UIFactory.label(Translation.DIALOG_FILTER_CHUNKS_SELECTION_RADIUS);
	private final CheckBox selectionOnly = new CheckBox();
	private final TextField selectionRadius = new TextField();

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
		toggleGroup.selectedToggleProperty().addListener(l -> {
			selectionOnly.setDisable(select.isSelected());
			selectionOnlyLabel.setDisable(select.isSelected());
			selectionRadius.setDisable(!select.isSelected());
			selectionRadiusLabel.setDisable(!select.isSelected());
		});
		select.fire();

		setResizable(true);

		ScrollPane scrollPane = new ScrollPane();
		scrollPane.setContent(groupFilterBox);
		groupFilterBox.prefWidthProperty().bind(scrollPane.prefWidthProperty());

		groupFilterBox.setOnUpdate(f -> {
			getDialogPane().lookupButton(ButtonType.OK).setDisable(!value.isValid());
			if (value.isValid()) {
				filterQuery.setText(value.toString());
			}
		});

		filterQuery.setText(gf.toString());

		filterQuery.setOnAction(e -> {
			FilterParser fp = new FilterParser(filterQuery.getText());
			try {
				gf = fp.parse();
				gf = FilterParser.unwrap(gf);
				Debug.dumpf("parsed filter query from: %s, to: %s", filterQuery.getText(), gf);
				value = gf;
				groupFilterBox.setFilter(gf);
			} catch (ParseException ex) {
				Debug.dumpf("failed to parse filter query from: %s, error: %s", filterQuery.getText(), ex.getMessage());
			}
		});

		selectionRadius.textProperty().addListener((a, o, n) -> onSelectionRadiusInput(o, n));

		VBox actionBox = new VBox();
		actionBox.getChildren().addAll(select, export, delete);

		GridPane optionBox = new GridPane();
		optionBox.getStyleClass().add("filter-dialog-option-box");
		optionBox.add(selectionOnlyLabel, 0, 0, 1, 1);
		optionBox.add(withStackPane(selectionOnly), 1, 0, 1, 1);
		optionBox.add(selectionRadiusLabel, 0, 1, 1, 1);
		optionBox.add(withStackPane(selectionRadius), 1, 1, 1, 1);

		HBox selectionBox = new HBox();
		selectionBox.getChildren().addAll(actionBox, optionBox);

		VBox box = new VBox();
		box.getChildren().addAll(scrollPane, new Separator(), filterQuery, new Separator(), selectionBox);
		getDialogPane().setContent(box);
	}

	private StackPane withStackPane(Node n) {
		StackPane stack = new StackPane();
		stack.getStyleClass().add("filter-dialog-stack-pane");
		stack.getChildren().add(n);
		StackPane.setAlignment(n, Pos.CENTER);
		return stack;
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

		private final HandleType type;
		private final boolean selectionOnly;
		private final GroupFilter filter;
		private final int radius;

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
