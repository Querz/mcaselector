package net.querz.mcaselector.ui.dialog;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.querz.mcaselector.filter.*;
import net.querz.mcaselector.exception.ParseException;
import net.querz.mcaselector.filter.filters.GroupFilter;
import net.querz.mcaselector.filter.filters.InhabitedTimeFilter;
import net.querz.mcaselector.filter.filters.ScriptFilter;
import net.querz.mcaselector.text.Translation;
import net.querz.mcaselector.ui.component.GroovyCodeArea;
import net.querz.mcaselector.ui.component.filter.GroupFilterBox;
import net.querz.mcaselector.ui.UIFactory;
import net.querz.mcaselector.validation.BeforeAfterCallback;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.fxmisc.flowless.VirtualizedScrollPane;

public class FilterChunksDialog extends Dialog<FilterChunksDialog.Result> {

	private static final Logger LOGGER = LogManager.getLogger(FilterChunksDialog.class);

	private static GroupFilter gf = new GroupFilter();
	static {
		InhabitedTimeFilter fiveMins = new InhabitedTimeFilter(Operator.AND, Comparator.SMALLER, 6000);
		fiveMins.setFilterValue("5 minutes");
		gf.addFilter(fiveMins);
	}

	private static final String initScript = """
			import net.querz.mcaselector.io.mca.ChunkData;
			import net.querz.nbt.*;

			void before() {
			\t
			}
			
			boolean filter(ChunkData data) {
			\t
			}
			
			void after() {
			\t
			}""";

	private GroupFilter value = gf;
	private final TabPane tabs = new TabPane();
	private final TextField filterQuery = new TextField();
	private final GroupFilterBox groupFilterBox = new GroupFilterBox(null, value, true);
	private final RadioButton select = UIFactory.radio(Translation.DIALOG_FILTER_CHUNKS_SELECT);
	private final RadioButton export = UIFactory.radio(Translation.DIALOG_FILTER_CHUNKS_EXPORT);
	private final RadioButton delete = UIFactory.radio(Translation.DIALOG_FILTER_CHUNKS_DELETE);
	private final Label selectionOnlyLabel = UIFactory.label(Translation.DIALOG_FILTER_CHUNKS_SELECTION_ONLY);
	private final Label overwriteSelectionLabel = UIFactory.label(Translation.DIALOG_FILTER_CHUNKS_OVERWRITE_SELECTION);
	private final Label selectionRadiusLabel = UIFactory.label(Translation.DIALOG_FILTER_CHUNKS_SELECTION_RADIUS);
	private final CheckBox selectionOnly = new CheckBox();
	private final CheckBox overwriteSelection = new CheckBox();
	private final TextField selectionRadius = new TextField();
	private static final GroovyCodeArea codeArea = new GroovyCodeArea(true);

	static {
		codeArea.setText(initScript);
	}

	private static int radius;
	private static boolean applyToSelectionOnly;
	private static boolean applyOverwriteSelection = true;

	public FilterChunksDialog(Stage primaryStage) {
		titleProperty().bind(Translation.DIALOG_FILTER_CHUNKS_TITLE.getProperty());

		initStyle(StageStyle.UTILITY);

		getDialogPane().getStyleClass().add("filter-dialog-pane");

		setResultConverter(p -> {
			if (p != ButtonType.OK) {
				return null;
			}
			GroupFilter filter;
			ScriptFilter scriptFilter = null;
			if (tabs.getSelectionModel().isSelected(0)) {
				filter = value;
			} else {
				filter = new GroupFilter();
				scriptFilter = new ScriptFilter();
				scriptFilter.setFilterValue(codeArea.getText());
				filter.addFilter(scriptFilter);
			}
			return new Result(filter, getHandleType(), applyToSelectionOnly, applyOverwriteSelection, radius, scriptFilter);
		});

		// apply same stylesheets to this dialog
		getDialogPane().getStylesheets().addAll(primaryStage.getScene().getStylesheets());
		getDialogPane().getStylesheets().add(FilterChunksDialog.class.getClassLoader().getResource("style/component/filter-chunks-dialog.css").toExternalForm());

		getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

		select.setTooltip(UIFactory.tooltip(Translation.DIALOG_FILTER_CHUNKS_SELECT_TOOLTIP));
		export.setTooltip(UIFactory.tooltip(Translation.DIALOG_FILTER_CHUNKS_EXPORT_TOOLTIP));
		delete.setTooltip(UIFactory.tooltip(Translation.DIALOG_FILTER_CHUNKS_DELETE_TOOLTIP));

		ToggleGroup toggleGroup = new ToggleGroup();
		toggleGroup.getToggles().addAll(select, export, delete);
		select.fire();

		selectionOnly.setSelected(applyToSelectionOnly);
		selectionOnly.setOnAction(e -> applyToSelectionOnly = selectionOnly.isSelected());

		overwriteSelection.setSelected(applyOverwriteSelection);
		overwriteSelection.setOnAction(e -> applyOverwriteSelection = overwriteSelection.isSelected());

		EventHandler<ActionEvent> selectEnable = e -> {
			selectionRadius.setDisable(!select.isSelected());
			overwriteSelection.setDisable(!select.isSelected());
		};

		select.setOnAction(selectEnable);
		export.setOnAction(selectEnable);
		delete.setOnAction(selectEnable);

		setResizable(true);

		ScrollPane scrollPane = new ScrollPane();
		scrollPane.setContent(groupFilterBox);
		groupFilterBox.prefWidthProperty().bind(scrollPane.prefWidthProperty());

		groupFilterBox.setOnUpdate(f -> {
			getDialogPane().lookupButton(ButtonType.OK).setDisable(!value.isValid());
			if (value.isValid()) {
				filterQuery.setText(value.toString());
			}

			if (value.selectionOnly()) {
				if (delete.isSelected()) {
					select.fire();
				}
				delete.setDisable(true);
			} else {
				delete.setDisable(false);
			}
		});

		filterQuery.setText(gf.toString());

		filterQuery.setOnAction(e -> {
			FilterParser fp = new FilterParser(filterQuery.getText());
			try {
				gf = fp.parse();
				gf = FilterParser.unwrap(gf);
				LOGGER.debug("parsed filter query from: {}, to: {}", filterQuery.getText(), gf);
				value = gf;
				groupFilterBox.setFilter(gf);
			} catch (ParseException ex) {
				LOGGER.warn("failed to parse filter query from: {}, error: {}", filterQuery.getText(), ex.getMessage());
			}
		});

		selectionRadius.setText(radius == 0 ? "" : ("" + radius));
		selectionRadius.textProperty().addListener((a, o, n) -> onSelectionRadiusInput(o, n));

		VBox actionBox = new VBox();
		actionBox.getChildren().addAll(select, export, delete);

		GridPane optionBox = new GridPane();
		optionBox.getStyleClass().add("filter-dialog-option-box");
		optionBox.add(selectionOnlyLabel, 0, 0, 1, 1);
		optionBox.add(withStackPane(selectionOnly), 1, 0, 1, 1);
		optionBox.add(overwriteSelectionLabel, 0, 1, 1, 1);
		optionBox.add(withStackPane(overwriteSelection), 1, 1, 1, 1);
		optionBox.add(selectionRadiusLabel, 0, 2, 1, 1);
		optionBox.add(withStackPane(selectionRadius), 1, 2, 1, 1);

		HBox selectionBox = new HBox();
		selectionBox.getChildren().addAll(actionBox, optionBox);

//		setOnCloseRequest(e -> codeArea.close());

		VBox mainBox = new VBox();
		mainBox.getChildren().addAll(scrollPane, new Separator(), filterQuery);

		VBox scriptBox = new VBox();
		StackPane scriptPane = new StackPane(new VirtualizedScrollPane<>(codeArea));
		Label errorLabel = new Label();
		errorLabel.getStyleClass().add("script-error-label");
		errorLabel.textProperty().bind(codeArea.errorProperty());
		VBox.setVgrow(scriptPane, Priority.ALWAYS);
		scriptBox.getChildren().addAll(scriptPane, errorLabel);

		Tab mainTab = UIFactory.tab(Translation.DIALOG_FILTER_CHUNKS_TAB_QUERY);
		mainTab.setContent(mainBox);
		Tab scriptTab = UIFactory.tab(Translation.DIALOG_FILTER_CHUNKS_TAB_SCRIPT);
		scriptTab.setContent(scriptBox);

		tabs.getTabs().addAll(mainTab, scriptTab);

		VBox panelBox = new VBox();
		panelBox.getChildren().addAll(tabs, new Separator(), selectionBox);
		getDialogPane().setContent(panelBox);
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

	public record Result(GroupFilter filter, HandleType type, boolean selectionOnly, boolean overwriteSelection, int radius, ScriptFilter script) implements BeforeAfterCallback {

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

	public enum HandleType {
		SELECT, EXPORT, DELETE
	}
}
