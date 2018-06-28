package net.querz.mcaselector;

import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import net.querz.mcaselector.filter.GroupFilterBox;
import net.querz.mcaselector.filter.structure.Comparator;
import net.querz.mcaselector.filter.structure.DataVersionFilter;
import net.querz.mcaselector.filter.structure.GroupFilter;
import net.querz.mcaselector.filter.structure.InhabitedTimeFilter;
import net.querz.mcaselector.filter.structure.LastUpdateFilter;
import net.querz.mcaselector.filter.structure.Operator;
import net.querz.mcaselector.filter.structure.XPosFilter;
import net.querz.mcaselector.filter.structure.ZPosFilter;

public class FilterChunksDialog extends Dialog<GroupFilter> {

		static GroupFilter gf = new GroupFilter();
		static {
			gf.addFilter(new DataVersionFilter(Operator.AND, net.querz.mcaselector.filter.structure.Comparator.LT, 1343));
			GroupFilter inner = new GroupFilter();
			inner.addFilter(new XPosFilter(Operator.AND, net.querz.mcaselector.filter.structure.Comparator.ST, 100));
			inner.addFilter(new ZPosFilter(Operator.AND, net.querz.mcaselector.filter.structure.Comparator.LT, -100));
			inner.addFilter(new InhabitedTimeFilter(Operator.OR, Comparator.EQ, 100));
			gf.addFilter(inner);

			GroupFilter innerInner = new GroupFilter();
			innerInner.addFilter(new LastUpdateFilter());
			innerInner.addFilter(new LastUpdateFilter(Operator.OR, net.querz.mcaselector.filter.structure.Comparator.EQ, 10));

			inner.addFilter(innerInner);
		}


	private GroupFilter value = gf;
	private GroupFilterBox groupFilterBox = new GroupFilterBox(null, value, true);
	private ToggleGroup toggleGroup = new ToggleGroup();
	private RadioButton delete = new RadioButton("Delete");
	private RadioButton export = new RadioButton("Export");

	public FilterChunksDialog(Stage primaryStage) {
		setTitle("Filter chunks");

		getDialogPane().getStyleClass().add("filter-dialog-pane");

		setResultConverter(p -> p == ButtonType.OK ? value : null);

		//apply same stylesheets to this dialog
		getDialogPane().getStylesheets().addAll(primaryStage.getScene().getStylesheets());

		getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

		setResizable(true);
		getDialogPane().setMinWidth(300);


		toggleGroup.getToggles().addAll(delete, export);
		export.fire();

		ScrollPane scrollPane = new ScrollPane();
		scrollPane.setContent(groupFilterBox);
		groupFilterBox.prefWidthProperty().bind(scrollPane.prefWidthProperty());
		scrollPane.setPrefHeight(300);
		scrollPane.setMaxHeight(300);

		Separator separator = new Separator();

		VBox box = new VBox();
		box.getChildren().addAll(scrollPane, separator, delete, export);
		getDialogPane().setContent(box);
	}
}
