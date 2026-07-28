package net.querz.mcaselector.ui.dialog;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import net.querz.mcaselector.changer.fields.ReplaceBlocksField;
import net.querz.mcaselector.config.ConfigProvider;
import net.querz.mcaselector.config.GlobalConfig;
import net.querz.mcaselector.io.job.ReplaceBlocksPreviewer;
import net.querz.mcaselector.text.Translation;
import net.querz.mcaselector.tile.TileMap;
import net.querz.mcaselector.ui.UIFactory;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.mapping.blockstate.BlockStateCatalog;
import net.querz.mcaselector.version.mapping.registry.BiomeRegistry;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.StringTag;
import net.querz.nbt.Tag;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ReplaceBlocksRuleBuilderDialog extends Dialog<String> {

	private static final PseudoClass error = PseudoClass.getPseudoClass("error");
	private static final PseudoClass warning = PseudoClass.getPseudoClass("warning");
	private static final PseudoClass success = PseudoClass.getPseudoClass("success");
	private static final PseudoClass autocompleteHighlighted = PseudoClass.getPseudoClass("autocomplete-highlighted");
	private static final String AUTOCOMPLETE_HIGHLIGHT_INDEX = "replace-blocks-autocomplete-highlight-index";
	private static final String COMBO_BOX_ROWS_TO_MEASURE_WIDTH = "comboBoxRowsToMeasureWidth";
	private static final int AUTOCOMPLETE_ROWS_TO_MEASURE_WIDTH = 32;
	private static final double EMPTY_RULES_HEIGHT = 160;
	private static final double EMPTY_RESULT_HEIGHT = 40;
	private static final double POPULATED_RESULT_MIN_HEIGHT = 52;
	private static final double POPULATED_RESULT_PREF_HEIGHT = 68;
	private static final double POPULATED_RESULT_MAX_HEIGHT = 92;
	private static final ButtonType HELP = new ButtonType("", ButtonBar.ButtonData.LEFT);
	private static final ButtonType PREVIEW = new ButtonType("", ButtonBar.ButtonData.LEFT);
	private static final Color PROPERTY_CHOICE_TEXT = Color.web("#f2f2f2");
	private static final Pattern RESOURCE_LOCATION = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

	private final Stage primaryStage;
	private final TileMap tileMap;
	private final boolean selectionOnly;
	private final EventHandler<WindowEvent> windowCloseFilter = this::handleWindowCloseRequest;
	private final ReplaceBlocksCatalogModel catalogModel = new ReplaceBlocksCatalogModel(BlockStateCatalog.available());
	private BlockStateCatalog catalog = catalogModel.selected();
	private final ObservableList<String> blockNames = FXCollections.observableArrayList(catalog.blockNames().stream().sorted().collect(Collectors.toList()));
	private final ObservableList<String> biomeCatalogNames = FXCollections.observableArrayList(BiomeRegistry.names().stream().sorted().collect(Collectors.toList()));
	private final ReplaceBlocksPresetRepository presetRepository = new ReplaceBlocksPresetRepository(
			ConfigProvider.GLOBAL.getReplaceBlocksUserPresets(), ConfigProvider.GLOBAL::saveWithResult);
	private final BlockInput from = new BlockInput(true);
	private final BlockInput to = new BlockInput(false);
	private final ComboBox<PresetItem> presets = new ComboBox<>();
	private final ObservableList<PresetItem> presetItems = FXCollections.observableArrayList();
	private final TableView<Rule> rules = new TableView<>();
	private final StackPane rulesArea = new StackPane();
	private final ObservableList<Rule> ruleItems = FXCollections.observableArrayList();
	private final TextArea result = new TextArea();
	private final Label validation = new Label();
	private final StackPane contentLayer = new StackPane();
	private final Rectangle ruleMarquee = new Rectangle();
	private BuilderContentState initialBuilderContent;
	private Node previewButton;
	private Button savePreset;
	private Button deletePreset;
	private boolean applyingPreset;
	private boolean allowNextDialogClose;
	private Window builderWindow;
	private boolean updatingCatalogSelector;
	private Point2D ruleMarqueeAnchor;
	private boolean ruleMarqueeAdditive;
	private boolean ruleMarqueeDragging;

	public ReplaceBlocksRuleBuilderDialog(Stage primaryStage, String initialValue) {
		this(primaryStage, null, true, initialValue);
	}

	public ReplaceBlocksRuleBuilderDialog(Stage primaryStage, TileMap tileMap, boolean selectionOnly, String initialValue) {
		this.primaryStage = primaryStage;
		this.tileMap = tileMap;
		this.selectionOnly = selectionOnly;
		titleProperty().bind(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_TITLE.getProperty());
		initStyle(StageStyle.UTILITY);
		setResizable(true);

		getDialogPane().getStylesheets().addAll(primaryStage.getScene().getStylesheets());
		getDialogPane().getStylesheets().add(Objects.requireNonNull(ChangeNBTDialog.class.getClassLoader().getResource("style/component/change-nbt-dialog.css")).toExternalForm());
		getDialogPane().getStyleClass().add("replace-blocks-builder-pane");
		getDialogPane().getButtonTypes().addAll(HELP, PREVIEW, ButtonType.OK, ButtonType.CANCEL);
		getDialogPane().setPrefSize(900, 650);
		Node help = getDialogPane().lookupButton(HELP);
		if (help instanceof Button button) {
			button.textProperty().bind(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_HELP_BUTTON.getProperty());
			button.addEventFilter(ActionEvent.ACTION, event -> {
				showHelp();
				event.consume();
			});
		}
		previewButton = getDialogPane().lookupButton(PREVIEW);
		if (previewButton instanceof Button button) {
			button.textProperty().bind(Translation.DIALOG_REPLACE_BLOCKS_PREVIEW_BUTTON.getProperty());
			button.addEventFilter(ActionEvent.ACTION, event -> {
				previewReplaceBlocks();
				event.consume();
			});
		}
		Node ok = getDialogPane().lookupButton(ButtonType.OK);
		ok.setDisable(true);
		ok.addEventFilter(ActionEvent.ACTION, event -> allowNextDialogClose = true);
		getDialogPane().lookupButton(ButtonType.CANCEL)
				.addEventFilter(ActionEvent.ACTION, this::handleCancelAction);
		setPreviewDisabled(true);

		setResultConverter(p -> p == ButtonType.OK ? result.getText() : null);
		setOnCloseRequest(event -> {
			if (allowNextDialogClose) {
				allowNextDialogClose = false;
			} else if (!requestDiscardBuilder()) {
				event.consume();
			}
		});
		setOnShown(event -> {
			builderWindow = getDialogPane().getScene().getWindow();
			builderWindow.addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, windowCloseFilter);
		});
		setOnHidden(event -> {
			if (builderWindow != null) {
				builderWindow.removeEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, windowCloseFilter);
				builderWindow = null;
			}
		});

		Button add = UIFactory.button(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_ADD_RULE);
		add.setOnAction(this::addRule);
		VBox addActions = new VBox();
		addActions.getStyleClass().add("replace-blocks-builder-add-actions");
		Label addSpacer = new Label(" ");
		addActions.getChildren().addAll(addSpacer, add);

		GridPane input = new GridPane();
		input.getStyleClass().add("replace-blocks-builder-input");
		input.setHgap(8);
		input.setVgap(6);
		input.getColumnConstraints().addAll(builderColumn(), builderColumn(), actionColumn());
		input.add(from, 0, 0);
		input.add(to, 1, 0);
		Node sourceConstraints = from.sourceConstraints();
		input.add(sourceConstraints, 0, 1, 2, 1);
		input.add(addActions, 2, 0);
		input.addEventFilter(KeyEvent.KEY_PRESSED, this::handleBuilderShortcut);
		GridPane.setHgrow(from, Priority.ALWAYS);
		GridPane.setHgrow(to, Priority.ALWAYS);
		GridPane.setHgrow(sourceConstraints, Priority.ALWAYS);
		GridPane.setValignment(addActions, VPos.TOP);

		presets.setItems(presetItems);
		configureBuilderComboBox(presets);
		presets.getStyleClass().add("replace-blocks-builder-preset-combo");
		presets.setCellFactory(v -> new PresetItemCell());
		presets.setButtonCell(new PresetItemCell());
		presets.setPrefWidth(360);
		presets.setMaxWidth(420);
		presets.promptTextProperty().bind(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_PRESET_PROMPT.getProperty());
		refreshPresetItems(null);
		Button applyPreset = UIFactory.button(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_PRESET_APPLY);
		applyPreset.getStyleClass().add("replace-blocks-builder-preset-action");
		applyPreset.disableProperty().bind(presets.valueProperty().isNull());
		applyPreset.setOnAction(this::applyPreset);
		savePreset = UIFactory.button(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_PRESET_SAVE);
		savePreset.getStyleClass().add("replace-blocks-builder-preset-action");
		savePreset.setOnAction(this::savePreset);
		deletePreset = UIFactory.button(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_PRESET_DELETE);
		deletePreset.getStyleClass().add("replace-blocks-builder-preset-action");
		deletePreset.setOnAction(this::deletePreset);
		updatePresetButtons();
		presets.valueProperty().addListener((a, o, n) -> updatePresetButtons());
		GridPane presetInput = new GridPane();
		presetInput.getStyleClass().add("replace-blocks-builder-presets");
		presetInput.setHgap(8);
		Label presetLabel = UIFactory.label(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_PRESET);
		presetInput.add(presetLabel, 0, 0);
		presetInput.add(presets, 1, 0);
		presetInput.add(applyPreset, 2, 0);
		presetInput.add(savePreset, 3, 0);
		presetInput.add(deletePreset, 4, 0);

		TableColumn<Rule, String> fromColumn = new TableColumn<>();
		fromColumn.textProperty().bind(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_FROM.getProperty());
		fromColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().from()));
		fromColumn.setCellFactory(c -> new RuleCell());
		fromColumn.setSortable(false);

		TableColumn<Rule, String> toColumn = new TableColumn<>();
		toColumn.textProperty().bind(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_TO.getProperty());
		toColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().to()));
		toColumn.setCellFactory(c -> new RuleCell());
		toColumn.setSortable(false);

		rules.setItems(ruleItems);
		rules.getStyleClass().add("replace-blocks-builder-rules-table");
		Label emptyRules = new Label(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_RULES_EMPTY.toString());
		emptyRules.setWrapText(true);
		emptyRules.setMaxWidth(Double.MAX_VALUE);
		emptyRules.setAlignment(Pos.CENTER);
		emptyRules.getStyleClass().add("replace-blocks-builder-empty-placeholder");
		rules.setPlaceholder(emptyRules);
		rules.getColumns().add(fromColumn);
		rules.getColumns().add(toColumn);
		rules.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		rules.getSelectionModel().getSelectedItems().addListener((ListChangeListener<Rule>) change -> updatePresetButtons());
		rules.addEventFilter(KeyEvent.KEY_PRESSED, this::handleRulesKeyPressed);
		rules.addEventFilter(MouseEvent.MOUSE_PRESSED, this::handleRulesMousePressed);
		rules.addEventFilter(MouseEvent.MOUSE_DRAGGED, this::handleRulesMouseDragged);
		rules.addEventFilter(MouseEvent.MOUSE_RELEASED, this::handleRulesMouseReleased);
		rules.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
		rules.setMinHeight(150);
		rules.setPrefHeight(220);
		ruleItems.addListener((ListChangeListener<Rule>) c -> updateRulesTableHeight());
		updateRulesTableHeight();

		ruleMarquee.getStyleClass().add("replace-blocks-builder-rule-marquee");
		ruleMarquee.setManaged(false);
		ruleMarquee.setMouseTransparent(true);
		ruleMarquee.setVisible(false);
		rulesArea.getChildren().setAll(rules);
		rulesArea.getStyleClass().add("replace-blocks-builder-rules-area");

		Button edit = UIFactory.button(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_EDIT_RULE);
		edit.disableProperty().bind(Bindings.size(rules.getSelectionModel().getSelectedItems()).isNotEqualTo(1));
		edit.setOnAction(this::editSelectedRule);

		Button delete = UIFactory.button(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_DELETE_RULE);
		delete.disableProperty().bind(Bindings.isEmpty(rules.getSelectionModel().getSelectedItems()));
		delete.setOnAction(e -> deleteSelectedRules());
		HBox ruleActions = new HBox(6, edit, delete);
		ruleActions.getStyleClass().add("replace-blocks-builder-rule-actions");

		result.setEditable(false);
		result.setFocusTraversable(false);
		result.setWrapText(true);
		result.getStyleClass().add("replace-blocks-builder-result");
		updateResultDisplayHeight();

		validation.getStyleClass().add("replace-blocks-builder-validation");
		validation.setText("");

		VBox content = new VBox();
		content.getStyleClass().add("replace-blocks-builder");
		content.setSpacing(10);
		// DialogPane already supplies the same inset to the content region and button bar.
		// Keep the Builder content at that baseline so the four native dialog buttons align
		// with the controls above instead of receiving a second horizontal inset.
		content.setPadding(new Insets(0));
		Label rulesLabel = UIFactory.label(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_RULES);
		Label resultLabel = UIFactory.label(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_RESULT);
		content.getChildren().addAll(createBuilderToolbar(presetInput), input, rulesLabel, rulesArea, ruleActions, resultLabel, result, validation);
		VBox.setMargin(rulesLabel, new Insets(8, 0, 0, 0));
		VBox.setVgrow(rulesArea, Priority.ALWAYS);
		contentLayer.getChildren().setAll(content, ruleMarquee);
		getDialogPane().setContent(contentLayer);

		loadSimpleRules(initialValue);
		updateResult();
		initialBuilderContent = builderContentState();
	}

	private static ColumnConstraints builderColumn() {
		ColumnConstraints constraints = new ColumnConstraints();
		constraints.setMinWidth(300);
		constraints.setPrefWidth(0);
		constraints.setHgrow(Priority.ALWAYS);
		constraints.setFillWidth(true);
		return constraints;
	}

	private static ColumnConstraints actionColumn() {
		ColumnConstraints constraints = new ColumnConstraints();
		constraints.setHgrow(Priority.NEVER);
		constraints.setFillWidth(false);
		return constraints;
	}

	private Node createCatalogControl() {
		Label label = new Label(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_CATALOG.toString());
		if (catalogModel.catalogs().size() <= 1) {
			Label version = new Label(catalogModel.shortLabel(catalog));
			version.setTooltip(new Tooltip(catalogModel.label(catalog)));
			version.getStyleClass().add("replace-blocks-builder-catalog-version");
			return wrapCatalogControl(new HBox(8, label, version));
		}
		ComboBox<BlockStateCatalog> selector = new ComboBox<>(FXCollections.observableArrayList(catalogModel.catalogs()));
		selector.setConverter(new javafx.util.StringConverter<>() {
			@Override public String toString(BlockStateCatalog value) {
				return value == null ? "" : catalogModel.label(value);
			}
			@Override public BlockStateCatalog fromString(String value) {
				return null;
			}
		});
		selector.setCellFactory(value -> new ListCell<>() {
			@Override
			protected void updateItem(BlockStateCatalog item, boolean empty) {
				super.updateItem(item, empty);
				setText(empty || item == null ? "" : catalogModel.label(item));
				setTooltip(empty || item == null ? null : new Tooltip(catalogModel.label(item)));
			}
		});
		selector.setButtonCell(new ListCell<>() {
			@Override
			protected void updateItem(BlockStateCatalog item, boolean empty) {
				super.updateItem(item, empty);
				setText(empty || item == null ? "" : catalogModel.shortLabel(item));
				setTooltip(empty || item == null ? null : new Tooltip(catalogModel.label(item)));
			}
		});
		selector.setValue(catalog);
		selector.setTooltip(new Tooltip(catalogModel.label(catalog)));
		configureBuilderComboBox(selector);
		selector.valueProperty().addListener((observable, oldCatalog, newCatalog) -> {
			requestCatalogSwitch(selector, oldCatalog, newCatalog);
			if (selector.getValue() != null) {
				selector.setTooltip(new Tooltip(catalogModel.label(selector.getValue())));
			}
		});
		return wrapCatalogControl(new HBox(8, label, selector));
	}

	private Node createBuilderToolbar(GridPane presetInput) {
		GridPane toolbar = new GridPane();
		toolbar.getStyleClass().add("replace-blocks-builder-toolbar");
		toolbar.setVgap(4);
		toolbar.add(presetInput, 0, 0);
		toolbar.add(createCatalogControl(), 0, 1);
		GridPane.setHgrow(presetInput, Priority.ALWAYS);
		return toolbar;
	}

	private void requestCatalogSwitch(ComboBox<BlockStateCatalog> selector,
			BlockStateCatalog oldCatalog, BlockStateCatalog newCatalog) {
		if (updatingCatalogSelector || newCatalog == null
				|| newCatalog.version().equals(catalog.version())) {
			return;
		}
		if (hasBuilderContent()) {
			setCatalogSelectorValue(selector, oldCatalog == null ? catalog : oldCatalog);
			if (!confirmCatalogSwitch(newCatalog)) {
				return;
			}
			setCatalogSelectorValue(selector, newCatalog);
		}
		if (!catalogModel.select(newCatalog.version())) {
			return;
		}
		catalog = catalogModel.selected();
		blockNames.setAll(catalog.blockNames().stream().sorted().toList());
		from.catalogChanged();
		to.catalogChanged();
		resetBuilder();
	}

	private void setCatalogSelectorValue(ComboBox<BlockStateCatalog> selector,
			BlockStateCatalog value) {
		updatingCatalogSelector = true;
		try {
			selector.setValue(value);
		} finally {
			updatingCatalogSelector = false;
		}
	}

	private boolean confirmCatalogSwitch(BlockStateCatalog newCatalog) {
		String message = Translation.DIALOG_REPLACE_BLOCKS_BUILDER_CATALOG_SWITCH_MESSAGE.format(
				catalogModel.label(catalog), catalogModel.label(newCatalog));
		Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.OK, ButtonType.CANCEL);
		alert.initOwner(getDialogPane().getScene().getWindow());
		alert.titleProperty().bind(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_CATALOG_SWITCH_TITLE.getProperty());
		alert.setHeaderText(null);
		alert.getDialogPane().getStylesheets().addAll(primaryStage.getScene().getStylesheets());
		return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
	}

	private void resetBuilder() {
		presets.hide();
		presets.getSelectionModel().clearSelection();
		rules.getSelectionModel().clearSelection();
		ruleItems.clear();
		from.clear();
		to.clear();
		updateResult();
	}

	private static Node wrapCatalogControl(Node control) {
		Label note = new Label(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_CATALOG_NOTE.toString());
		note.getStyleClass().add("replace-blocks-builder-catalog-note");
		note.setMaxWidth(Double.MAX_VALUE);
		note.setWrapText(true);
		return new VBox(2, control, note);
	}

	private void applyPreset(ActionEvent event) {
		PresetItem preset = presets.getValue();
		if (preset == null) {
			return;
		}
		applyingPreset = true;
		try {
			if (preset.custom()) {
				List<ParsedPresetRule> loaded = parsePresetRules(preset.value());
				if (loaded.isEmpty()) {
					showDiagnostic(ReplaceBlocksDiagnostics.builderInvalid());
					return;
				}
				List<ParsedRule> appended = new ArrayList<>();
				for (ParsedPresetRule presetRule : loaded) {
					Rule rule = presetRule.rule();
					if (!hasRule(rule.from(), rule.to())) {
						ruleItems.add(rule);
						appended.add(presetRule.parsed());
					}
				}
				updateResult();
				if (appended.isEmpty()) {
					showDiagnostic(new ReplaceBlocksDiagnostics.Diagnostic(
							ReplaceBlocksDiagnostics.Severity.ERROR,
							Translation.DIALOG_REPLACE_BLOCKS_BUILDER_PRESET_ALREADY_FILLED.toString()));
				} else {
					ReplaceBlocksDiagnostics.Diagnostic compatibilityWarning = presetCompatibilityWarning(appended);
					if (compatibilityWarning.isWarning()) {
						showDiagnostic(compatibilityWarning);
					}
				}
				return;
			}
			from.applyPreset(preset.source(), preset.tileMode());
			to.applyPreset(preset.target(), SourceTileMode.ANY);
			if (preset.warning() == null) {
				updateResult();
			} else {
				showDiagnostic(new ReplaceBlocksDiagnostics.Diagnostic(
						ReplaceBlocksDiagnostics.Severity.WARNING,
						preset.warning().toString()));
			}
			from.focusInput();
		} finally {
			applyingPreset = false;
		}
	}

	private ReplaceBlocksDiagnostics.Diagnostic presetCompatibilityWarning(List<ParsedRule> appended) {
		String firstUnknownTarget = null;
		String firstUnknownSource = null;
		for (ParsedRule parsed : appended) {
			String target = switch (parsed.target().getType()) {
				case NAME, STATE -> parsed.target().getName();
				default -> null;
			};
			if (firstUnknownTarget == null && target != null && !catalog.containsBlock(target)) {
				firstUnknownTarget = target;
			}
			String source = switch (parsed.source().getType()) {
				case LITERAL_NAME, SELECTED_PROPERTIES, EXACT_STATE -> parsed.source().getName();
				default -> null;
			};
			if (firstUnknownSource == null && source != null && !catalog.containsBlock(source)) {
				firstUnknownSource = source;
			}
		}
		if (firstUnknownTarget != null) {
			return presetCatalogWarning(
					Translation.DIALOG_REPLACE_BLOCKS_BUILDER_CATALOG_TARGET_UNKNOWN,
					firstUnknownTarget);
		}
		if (firstUnknownSource != null) {
			return presetCatalogWarning(
					Translation.DIALOG_REPLACE_BLOCKS_BUILDER_CATALOG_SOURCE_UNKNOWN,
					firstUnknownSource);
		}
		return ReplaceBlocksDiagnostics.none();
	}

	private ReplaceBlocksDiagnostics.Diagnostic presetCatalogWarning(Translation message, String blockName) {
		return new ReplaceBlocksDiagnostics.Diagnostic(
				ReplaceBlocksDiagnostics.Severity.WARNING,
				message.format(blockName, catalog.version()));
	}

	private void savePreset(ActionEvent event) {
		PresetValue presetValue = presetValue();
		if (presetValue.diagnostic().isError()) {
			showDiagnostic(presetValue.diagnostic());
			return;
		}
		String value = presetValue.value();
		TextInputDialog dialog = new TextInputDialog(suggestPresetName());
		dialog.initOwner(getDialogPane().getScene().getWindow());
		dialog.titleProperty().bind(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_PRESET_SAVE_TITLE.getProperty());
		dialog.headerTextProperty().bind(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_PRESET_SAVE_HEADER.getProperty());
		dialog.contentTextProperty().bind(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_PRESET_SAVE_NAME.getProperty());
		dialog.getDialogPane().getStylesheets().addAll(primaryStage.getScene().getStylesheets());
		Optional<String> answer = dialog.showAndWait();
		if (answer.isEmpty()) {
			return;
		}
		String name = answer.get().trim();
		if (name.isEmpty()) {
			showPresetMessage(Alert.AlertType.WARNING, Translation.DIALOG_REPLACE_BLOCKS_BUILDER_PRESET_NAME_EMPTY.toString());
			return;
		}
		if (isBuiltinPresetName(name)) {
			showPresetMessage(Alert.AlertType.WARNING, Translation.DIALOG_REPLACE_BLOCKS_BUILDER_PRESET_BUILTIN_LOCKED.toString());
			return;
		}
		int existing = findUserPreset(name);
		if (existing >= 0 && !confirmPresetOverwrite(name)) {
			return;
		}
		if (!presetRepository.save(name, value)) {
			showPresetMessage(Alert.AlertType.ERROR, Translation.DIALOG_REPLACE_BLOCKS_BUILDER_PRESET_PERSIST_FAILED.toString());
			return;
		}
		refreshPresetItems(name);
		showMessage(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_PRESET_SAVED.format(name));
	}

	private void deletePreset(ActionEvent event) {
		PresetItem preset = presets.getValue();
		if (preset == null || !preset.custom()) {
			return;
		}
		if (!confirmPresetDelete(preset.name())) {
			return;
		}
		if (!presetRepository.delete(preset.name())) {
			showPresetMessage(Alert.AlertType.ERROR, Translation.DIALOG_REPLACE_BLOCKS_BUILDER_PRESET_PERSIST_FAILED.toString());
			return;
		}
		refreshPresetItems(null);
		updatePresetButtons();
	}

	private void editSelectedRule(ActionEvent event) {
		if (rules.getSelectionModel().getSelectedItems().size() != 1) {
			return;
		}
		Rule selected = rules.getSelectionModel().getSelectedItem();
		if (selected == null) {
			return;
		}
		ruleItems.remove(selected);
		ParsedRule parsed = parseEditableRule(selected.from(), selected.to());
		if (parsed == null) {
			from.applyPreset(selected.from(), SourceTileMode.ANY);
			to.applyPreset(selected.to(), SourceTileMode.ANY);
		} else {
			from.applySource(parsed.source(), selected.from());
			to.applyTarget(parsed.target(), selected.to());
		}
		updateResult();
		clearPresetSelectionAfterEdit();
		from.focusInput();
	}

	private void refreshPresetItems(String selectName) {
		presetItems.clear();
		for (ReplaceBlocksPreset preset : ReplaceBlocksPreset.values()) {
			presetItems.add(PresetItem.builtin(preset));
		}
		for (GlobalConfig.ReplaceBlocksUserPreset preset : ConfigProvider.GLOBAL.getReplaceBlocksUserPresets()) {
			if (preset != null && preset.name() != null && preset.value() != null) {
				presetItems.add(PresetItem.custom(preset));
			}
		}
		if (selectName != null) {
			presetItems.stream()
					.filter(PresetItem::custom)
					.filter(item -> item.name().equals(selectName))
					.findFirst()
					.ifPresent(item -> presets.getSelectionModel().select(item));
		}
	}

	private void updatePresetButtons() {
		if (savePreset != null) {
			savePreset.setDisable(presetValue().diagnostic().isError());
		}
		if (deletePreset != null) {
			PresetItem selected = presets.getValue();
			deletePreset.setDisable(selected == null || !selected.custom());
		}
	}

	private PresetValue presetValue() {
		List<Rule> selected = List.copyOf(rules.getSelectionModel().getSelectedItems());
		if (!selected.isEmpty()) {
			return validatedPresetValue(formatRulesValue(selected));
		}

		String value;
		if (from.userInputPresentProperty().get() || to.userInputPresentProperty().get()) {
			ValueResult fromName = from.value();
			if (fromName.diagnostic().isError()) {
				return new PresetValue(null, fromName.diagnostic());
			}
			ValueResult toName = to.value();
			if (toName.diagnostic().isError()) {
				return new PresetValue(null, toName.diagnostic());
			}
			value = formatFrom(fromName.value()) + "=" + formatTo(toName.value());
		} else {
			value = result.getText() == null ? "" : result.getText().trim();
		}
		if (value == null || value.isBlank()) {
			return new PresetValue(null, ReplaceBlocksDiagnostics.builderInvalid());
		}
		return validatedPresetValue(value);
	}

	private PresetValue validatedPresetValue(String value) {
		String normalized = normalizeRulesValue(value);
		return normalized == null
				? new PresetValue(null, ReplaceBlocksDiagnostics.builderInvalid())
				: new PresetValue(normalized, ReplaceBlocksDiagnostics.none());
	}

	private String suggestPresetName() {
		PresetItem selected = presets.getValue();
		return selected != null && selected.custom() ? selected.name() : "";
	}

	private boolean isBuiltinPresetName(String name) {
		for (ReplaceBlocksPreset preset : ReplaceBlocksPreset.values()) {
			if (preset.toString().equals(name)) {
				return true;
			}
		}
		return false;
	}

	private int findUserPreset(String name) {
		List<GlobalConfig.ReplaceBlocksUserPreset> userPresets = ConfigProvider.GLOBAL.getReplaceBlocksUserPresets();
		for (int i = 0; i < userPresets.size(); i++) {
			GlobalConfig.ReplaceBlocksUserPreset preset = userPresets.get(i);
			if (preset != null && name.equals(preset.name())) {
				return i;
			}
		}
		return -1;
	}

	private boolean confirmPresetOverwrite(String name) {
		return confirmPresetAction(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_PRESET_OVERWRITE_CONFIRM.format(name));
	}

	private boolean confirmPresetDelete(String name) {
		return confirmPresetAction(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_PRESET_DELETE_CONFIRM.format(name));
	}

	private boolean confirmDiscardBuilder() {
		ButtonType discard = new ButtonType(
				Translation.DIALOG_REPLACE_BLOCKS_BUILDER_DISCARD_ACTION.toString(),
				ButtonBar.ButtonData.OK_DONE);
		ButtonType continueEditing = new ButtonType(
				Translation.DIALOG_REPLACE_BLOCKS_BUILDER_CONTINUE_EDITING_ACTION.toString(),
				ButtonBar.ButtonData.CANCEL_CLOSE);
		Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
				Translation.DIALOG_REPLACE_BLOCKS_BUILDER_DISCARD_HEADER.toString(),
				discard, continueEditing);
		alert.initOwner(getDialogPane().getScene().getWindow());
		alert.titleProperty().bind(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_DISCARD_TITLE.getProperty());
		alert.setHeaderText(null);
		alert.getDialogPane().getStylesheets().addAll(primaryStage.getScene().getStylesheets());
		return alert.showAndWait().orElse(continueEditing) == discard;
	}

	private boolean requestDiscardBuilder() {
		return !isBuilderDirty() || confirmDiscardBuilder();
	}

	private void handleCancelAction(ActionEvent event) {
		if (requestDiscardBuilder()) {
			allowNextDialogClose = true;
		} else {
			event.consume();
		}
	}

	private void handleWindowCloseRequest(WindowEvent event) {
		if (requestDiscardBuilder()) {
			allowNextDialogClose = true;
		} else {
			event.consume();
		}
	}

	private boolean confirmPresetAction(String message) {
		Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.OK, ButtonType.CANCEL);
		alert.initOwner(getDialogPane().getScene().getWindow());
		alert.titleProperty().bind(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_PRESET_TITLE.getProperty());
		alert.setHeaderText(null);
		alert.getDialogPane().getStylesheets().addAll(primaryStage.getScene().getStylesheets());
		return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
	}

	private void showPresetMessage(Alert.AlertType type, String message) {
		Alert alert = new Alert(type, message, ButtonType.OK);
		alert.initOwner(getDialogPane().getScene().getWindow());
		alert.titleProperty().bind(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_PRESET_TITLE.getProperty());
		alert.setHeaderText(null);
		alert.getDialogPane().getStylesheets().addAll(primaryStage.getScene().getStylesheets());
		alert.showAndWait();
	}

	private void showMessage(String message) {
		validation.textProperty().unbind();
		validation.pseudoClassStateChanged(error, false);
		validation.pseudoClassStateChanged(warning, false);
		validation.pseudoClassStateChanged(success, true);
		validation.setText(message);
	}

	private void clearPresetSelectionAfterEdit() {
		if (!applyingPreset) {
			presets.getSelectionModel().clearSelection();
		}
		updatePresetButtons();
	}

	private boolean hasBuilderContent() {
		return !ruleItems.isEmpty() || from.hasMeaningfulContent() || to.hasMeaningfulContent();
	}

	private boolean isBuilderDirty() {
		return initialBuilderContent != null && !initialBuilderContent.equals(builderContentState());
	}

	private BuilderContentState builderContentState() {
		return new BuilderContentState(List.copyOf(ruleItems), from.contentState(), to.contentState());
	}

	private static String valueOrEmpty(String value) {
		return value == null ? "" : value;
	}

	private boolean isFilledRuleRow(Object target) {
		Node node = target instanceof Node n ? n : null;
		while (node != null && node != rules) {
			if (node instanceof TableRow<?> row) {
				return !row.isEmpty();
			}
			node = node.getParent();
		}
		return false;
	}

	private boolean isRuleTableDragArea(Object target) {
		Node node = target instanceof Node n ? n : null;
		boolean excluded = false;
		while (node != null) {
			if (node == rules) {
				return canStartRuleMarquee(true, excluded);
			}
			if (node.getStyleClass().contains("column-header")
					|| node.getStyleClass().contains("column-header-background")
					|| node.getStyleClass().contains("scroll-bar")) {
				excluded = true;
			}
			node = node.getParent();
		}
		return canStartRuleMarquee(false, excluded);
	}

	private void handleBuilderShortcut(KeyEvent event) {
		if (isBuilderAddShortcut(event.getCode(), event.isControlDown())) {
			addRule(null);
			event.consume();
		}
	}

	private void handleRulesKeyPressed(KeyEvent event) {
		if (event.getCode() == KeyCode.DELETE && !rules.getSelectionModel().getSelectedItems().isEmpty()) {
			deleteSelectedRules();
			event.consume();
		} else if (event.getCode() == KeyCode.ESCAPE && !rules.getSelectionModel().getSelectedItems().isEmpty()) {
			rules.getSelectionModel().clearSelection();
			event.consume();
		}
	}

	private void handleRulesMousePressed(MouseEvent event) {
		if (event.getButton() != MouseButton.PRIMARY || !isRuleTableDragArea(event.getTarget())) {
			return;
		}
		rules.requestFocus();
		ruleMarqueeAdditive = event.isControlDown();
		ruleMarqueeDragging = false;
		if (!ruleMarqueeAdditive && !isFilledRuleRow(event.getTarget())) {
			rules.getSelectionModel().clearSelection();
		}
		ruleMarqueeAnchor = new Point2D(event.getX(), event.getY());
	}

	private void handleRulesMouseDragged(MouseEvent event) {
		if (ruleMarqueeAnchor == null) {
			return;
		}
		if (!ruleMarqueeDragging && !isRuleMarqueeDrag(
				event.getX() - ruleMarqueeAnchor.getX(), event.getY() - ruleMarqueeAnchor.getY())) {
			return;
		}
		if (!ruleMarqueeDragging) {
			ruleMarqueeDragging = true;
			if (!ruleMarqueeAdditive) {
				rules.getSelectionModel().clearSelection();
			}
		}
		updateRuleMarquee(event.getX(), event.getY());
		event.consume();
	}

	private void handleRulesMouseReleased(MouseEvent event) {
		if (ruleMarqueeAnchor == null) {
			return;
		}
		if (!ruleMarqueeDragging) {
			ruleMarqueeAnchor = null;
			return;
		}
		applyRuleMarquee();
		ruleMarquee.setVisible(false);
		ruleMarqueeAnchor = null;
		ruleMarqueeDragging = false;
		event.consume();
	}

	private void updateRuleMarquee(double x, double y) {
		Point2D anchor = rulesToContent(ruleMarqueeAnchor);
		Point2D current = rulesToContent(new Point2D(x, y));
		double minX = Math.min(anchor.getX(), current.getX());
		double minY = Math.min(anchor.getY(), current.getY());
		ruleMarquee.setX(minX);
		ruleMarquee.setY(minY);
		ruleMarquee.setWidth(Math.abs(current.getX() - anchor.getX()));
		ruleMarquee.setHeight(Math.abs(current.getY() - anchor.getY()));
		ruleMarquee.setVisible(ruleMarquee.getWidth() > 0 && ruleMarquee.getHeight() > 0);
	}

	private void applyRuleMarquee() {
		Point2D start = contentToRules(new Point2D(ruleMarquee.getX(), ruleMarquee.getY()));
		Point2D end = contentToRules(new Point2D(
			ruleMarquee.getX() + ruleMarquee.getWidth(), ruleMarquee.getY() + ruleMarquee.getHeight()));
		Rectangle2D marquee = new Rectangle2D(
			Math.min(start.getX(), end.getX()), Math.min(start.getY(), end.getY()),
			Math.abs(end.getX() - start.getX()), Math.abs(end.getY() - start.getY()));
		List<Integer> selected = intersectingRuleIndices(marquee, visibleRuleBounds());
		if (!ruleMarqueeAdditive) {
			rules.getSelectionModel().clearSelection();
		}
		selected.forEach(index -> rules.getSelectionModel().select(index));
	}

	private Point2D rulesToContent(Point2D point) {
		return contentLayer.sceneToLocal(rules.localToScene(point));
	}

	private Point2D contentToRules(Point2D point) {
		return rules.sceneToLocal(contentLayer.localToScene(point));
	}

	private List<VisibleRuleBounds> visibleRuleBounds() {
		return rules.lookupAll(".table-row-cell").stream()
				.filter(TableRow.class::isInstance)
				.map(node -> (TableRow<?>) node)
				.filter(row -> !row.isEmpty() && row.getIndex() >= 0)
				.map(row -> {
					Bounds bounds = row.localToScene(row.getBoundsInLocal());
					Point2D min = rules.sceneToLocal(bounds.getMinX(), bounds.getMinY());
					Point2D max = rules.sceneToLocal(bounds.getMaxX(), bounds.getMaxY());
					return new VisibleRuleBounds(row.getIndex(), new Rectangle2D(min.getX(), min.getY(), max.getX() - min.getX(), max.getY() - min.getY()));
				})
				.toList();
	}

	private void previewReplaceBlocks() {
		if (tileMap == null) {
			showPreviewMessage(Translation.DIALOG_REPLACE_BLOCKS_PREVIEW_NO_FIELD.toString());
			return;
		}
		ReplaceBlocksField field = new ReplaceBlocksField();
		if (!ReplaceBlocksDiagnostics.parseReplaceBlocksValue(field, result.getText())) {
			showPreviewMessage(Translation.DIALOG_REPLACE_BLOCKS_PREVIEW_NO_FIELD.toString());
			return;
		}
		AtomicReference<ReplaceBlocksPreviewer.Result> previewResult = new AtomicReference<>();
		CancellableProgressDialog progress = new CancellableProgressDialog(
				Translation.DIALOG_PROGRESS_TITLE_PREVIEW_REPLACE_BLOCKS,
				primaryStage,
				CancellableProgressDialog.CancellationScope.CURRENT_TASK);
		progress.showProgressBar(t -> previewResult.set(ReplaceBlocksPreviewer.preview(
				field,
				selectionOnly ? tileMap.getSelection() : null,
				t
		)));
		if (!progress.cancelled() && previewResult.get() != null) {
			showPreviewMessage(formatPreviewResult(previewResult.get()));
		}
	}

	private void setPreviewDisabled(boolean disabled) {
		if (previewButton != null) {
			previewButton.setDisable(disabled || tileMap == null);
		}
	}

	private void showPreviewMessage(String message) {
		Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
		alert.initOwner(getDialogPane().getScene().getWindow());
		alert.titleProperty().bind(Translation.DIALOG_REPLACE_BLOCKS_PREVIEW_TITLE.getProperty());
		alert.setHeaderText(null);
		alert.getDialogPane().getStylesheets().addAll(primaryStage.getScene().getStylesheets());
		alert.showAndWait();
	}

	private String formatPreviewResult(ReplaceBlocksPreviewer.Result result) {
		return ReplaceBlocksPreviewFormatter.format(result);
	}

	private void updateRulesTableHeight() {
		boolean empty = ruleItems.isEmpty();
		rules.setMaxHeight(empty ? EMPTY_RULES_HEIGHT : Double.MAX_VALUE);
		rules.setPrefHeight(empty ? EMPTY_RULES_HEIGHT : 220);
		VBox.setVgrow(rulesArea, empty ? Priority.NEVER : Priority.ALWAYS);
	}

	private void updateResultDisplayHeight() {
		if (ruleItems.isEmpty()) {
			result.setPrefRowCount(1);
			result.setMinHeight(EMPTY_RESULT_HEIGHT);
			result.setPrefHeight(EMPTY_RESULT_HEIGHT);
			result.setMaxHeight(EMPTY_RESULT_HEIGHT);
		} else {
			result.setPrefRowCount(2);
			result.setMinHeight(POPULATED_RESULT_MIN_HEIGHT);
			result.setPrefHeight(POPULATED_RESULT_PREF_HEIGHT);
			result.setMaxHeight(POPULATED_RESULT_MAX_HEIGHT);
		}
	}

	private void addRule(ActionEvent event) {
		ValueResult fromName = from.value();
		if (fromName.diagnostic().isError()) {
			showDiagnostic(fromName.diagnostic());
			return;
		}
		ValueResult toName = to.value();
		if (toName.diagnostic().isError()) {
			showDiagnostic(toName.diagnostic());
			return;
		}
		if (hasRule(fromName.value(), toName.value())) {
			showDiagnostic(ReplaceBlocksDiagnostics.builderDuplicate());
			return;
		}
		ReplaceBlocksDiagnostics.Diagnostic compatibilityWarning = toName.diagnostic().isWarning()
				? toName.diagnostic() : fromName.diagnostic();
		ruleItems.add(new Rule(fromName.value(), toName.value()));
		from.clear();
		to.clear();
		updateResult();
		if (compatibilityWarning.isWarning()) {
			showDiagnostic(compatibilityWarning);
		}
		clearPresetSelectionAfterEdit();
		from.focusInput();
	}

	private void deleteSelectedRules() {
		List<Rule> selected = List.copyOf(rules.getSelectionModel().getSelectedItems());
		if (selected.isEmpty()) {
			return;
		}
		ruleItems.removeAll(selected);
		updateResult();
		clearPresetSelectionAfterEdit();
	}

	private boolean hasRule(String from, String to) {
		ParsedRule candidate = parseEditableRule(from, to);
		return candidate != null && ruleItems.stream()
				.map(rule -> parseEditableRule(rule.from(), rule.to()))
				.filter(Objects::nonNull)
				.anyMatch(existing -> candidate.source().equals(existing.source()));
	}

	static String formatRulesValue(List<Rule> rules) {
		return rules.stream()
				.map(rule -> formatFrom(rule.from()) + "=" + formatTo(rule.to()))
				.collect(Collectors.joining(", "));
	}

	static boolean isBuilderAddShortcut(KeyCode code, boolean controlDown) {
		return controlDown && (code == KeyCode.ENTER || code == KeyCode.SEPARATOR);
	}

	static boolean canStartRuleMarquee(boolean insideRulesTable, boolean excludedControl) {
		return insideRulesTable && !excludedControl;
	}

	static boolean isRuleMarqueeDrag(double deltaX, double deltaY) {
		return Math.hypot(deltaX, deltaY) >= 3;
	}

	static boolean isAutocompletePopupKey(KeyCode code) {
		return ReplaceBlocksAutocomplete.isPopupKey(code);
	}

	static int popupSelectionTarget(KeyCode code, int selectedIndex, int itemCount, int visibleRows) {
		return ReplaceBlocksAutocomplete.selectionTarget(code, selectedIndex, itemCount, visibleRows);
	}

	static boolean focusPopupSuggestion(ComboBox<?> comboBox, int index) {
		return ReplaceBlocksAutocomplete.focusSuggestion(comboBox, index);
	}

	private static int popupVisibleRowCount(ComboBox<?> comboBox, int fallback) {
		return ReplaceBlocksAutocomplete.visibleRowCount(comboBox, fallback);
	}

	static boolean popupNeedsReveal(int target, int firstVisible, int lastVisible) {
		return ReplaceBlocksAutocomplete.needsReveal(target, firstVisible, lastVisible);
	}

	static int popupPageSize(int firstVisible, int lastVisible, int fallback) {
		return ReplaceBlocksAutocomplete.pageSize(firstVisible, lastVisible, fallback);
	}

	static boolean isPopupCellFullyVisible(Bounds cellBounds, Bounds viewportBounds) {
		return ReplaceBlocksAutocomplete.isCellFullyVisible(cellBounds, viewportBounds);
	}

	static List<String> suggestionsForQuery(List<String> names, String query, boolean allowEmpty) {
		return ReplaceBlocksAutocomplete.suggestions(names, query, allowEmpty);
	}

	static boolean isComboBoxArrowTarget(Object target) {
		return ReplaceBlocksAutocomplete.isArrowTarget(target);
	}

	static boolean isAutocompleteCellHighlighted(int cellIndex, int highlightIndex, boolean empty) {
		return ReplaceBlocksAutocomplete.isCellHighlighted(cellIndex, highlightIndex, empty);
	}

	static boolean shouldClearExplicitCatalog(boolean explicitCatalog, boolean showing, String editorText) {
		return ReplaceBlocksAutocomplete.shouldClearExplicitCatalog(explicitCatalog, showing, editorText);
	}

	static void clearEmptyExplicitCatalog(ComboBox<?> comboBox) {
		ReplaceBlocksAutocomplete.clearEmptyExplicitCatalog(comboBox);
	}

	private static void clearPopupSuggestionHighlight(ComboBox<?> comboBox) {
		ReplaceBlocksAutocomplete.clearHighlight(comboBox);
	}

	static void installAutocompletePopupKeyFilter(ComboBox<?> comboBox, EventHandler<KeyEvent> handler) {
		ReplaceBlocksAutocomplete.installPopupKeyFilter(comboBox, handler);
	}

	static void configureBuilderComboBox(ComboBox<?> comboBox) {
		ReplaceBlocksAutocomplete.configure(comboBox);
	}

	private static void configureAutocompleteComboBox(ComboBox<?> comboBox) {
		comboBox.getProperties().put(COMBO_BOX_ROWS_TO_MEASURE_WIDTH, AUTOCOMPLETE_ROWS_TO_MEASURE_WIDTH);
		configureBuilderComboBox(comboBox);
	}

	static List<Integer> intersectingRuleIndices(Rectangle2D marquee, List<VisibleRuleBounds> rows) {
		if (marquee.getWidth() <= 0 || marquee.getHeight() <= 0) {
			return List.of();
		}
		return rows.stream()
				.filter(row -> row.bounds().intersects(marquee))
				.map(VisibleRuleBounds::index)
				.toList();
	}

	static String normalizeRule(String from, String to) {
		return normalizeRulesValue(formatFrom(from) + "=" + formatTo(to));
	}

	static String normalizeRulesValue(String value) {
		return ReplaceBlocksRuleBuilderModel.normalizeRulesValue(value);
	}

	static ParsedRule parseEditableRule(String from, String to) {
		ParsedRule parsed = parseSingleRule(formatFrom(from) + "=" + formatTo(to));
		return parsed == null ? parseSingleRule(from + "=" + to) : parsed;
	}

	private static ParsedRule parseSingleRule(String value) {
		Map<ChunkFilter.BlockReplaceSource, ChunkFilter.BlockReplaceData> rules =
				ReplaceBlocksRuleBuilderModel.parseRules(value);
		if (rules.isEmpty()) {
			return null;
		}
		Map.Entry<ChunkFilter.BlockReplaceSource, ChunkFilter.BlockReplaceData> parsed = rules.entrySet().iterator().next();
		return new ParsedRule(parsed.getKey(), parsed.getValue());
	}

	static List<Rule> parseSimpleRules(String initialValue) {
		return parsePresetRules(initialValue).stream()
				.map(ParsedPresetRule::rule)
				.toList();
	}

	private static List<ParsedPresetRule> parsePresetRules(String initialValue) {
		if (initialValue == null || initialValue.isBlank()) {
			return List.of();
		}
		List<ParsedPresetRule> parsed = new ArrayList<>();
		Map<ChunkFilter.BlockReplaceSource, ChunkFilter.BlockReplaceData> rules =
				ReplaceBlocksRuleBuilderModel.parseRules(initialValue);
		if (rules.isEmpty()) {
			return List.of();
		}
		for (Map.Entry<ChunkFilter.BlockReplaceSource, ChunkFilter.BlockReplaceData> rule : rules.entrySet()) {
			parsed.add(new ParsedPresetRule(
					new Rule(rule.getKey().toString(), rule.getValue().toString()),
					new ParsedRule(rule.getKey(), rule.getValue())));
		}
		return List.copyOf(parsed);
	}

	private void loadSimpleRules(String initialValue) {
		ruleItems.addAll(parseSimpleRules(initialValue));
	}

	private void updateResult() {
		updateRulesTableHeight();
		updateResultDisplayHeight();
		if (ruleItems.isEmpty()) {
			result.clear();
			validation.textProperty().unbind();
			validation.setText("");
			validation.pseudoClassStateChanged(error, false);
			validation.pseudoClassStateChanged(warning, false);
			validation.pseudoClassStateChanged(success, false);
			getDialogPane().lookupButton(ButtonType.OK).setDisable(true);
			setPreviewDisabled(true);
			updatePresetButtons();
			return;
		}
		result.setText(formatRulesValue(ruleItems));
		ReplaceBlocksField field = new ReplaceBlocksField();
		boolean valid = ReplaceBlocksDiagnostics.parseReplaceBlocksValue(field, result.getText());
		getDialogPane().lookupButton(ButtonType.OK).setDisable(!valid);
		setPreviewDisabled(!valid);
		updatePresetButtons();
		showDiagnostic(ReplaceBlocksDiagnostics.diagnoseValue(result.getText(), valid));
	}

	private void showHelp() {
		Dialog<Void> help = new Dialog<>();
		help.initOwner(getDialogPane().getScene().getWindow());
		help.initStyle(StageStyle.UTILITY);
		help.setResizable(true);
		help.titleProperty().bind(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_HELP_TITLE.getProperty());
		help.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
		help.getDialogPane().getStylesheets().addAll(primaryStage.getScene().getStylesheets());
		help.getDialogPane().getStylesheets().add(Objects.requireNonNull(ChangeNBTDialog.class.getClassLoader().getResource("style/component/change-nbt-dialog.css")).toExternalForm());
		help.getDialogPane().getStyleClass().add("replace-blocks-builder-help-pane");
		help.getDialogPane().setPrefSize(560, 380);

		VBox content = new VBox(8);
		content.getStyleClass().add("replace-blocks-builder-help");
		content.setPadding(new Insets(8));
		content.getChildren().addAll(
				helpLabel(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_HELP_NBT_TITLE, "replace-blocks-builder-help-heading"),
				helpLabel(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_HELP_NBT_INTRO, "replace-blocks-builder-help-text"),
				helpLabel(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_HELP_NBT_ANY, "replace-blocks-builder-help-text"),
				helpLabel(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_HELP_NBT_PRESENT, "replace-blocks-builder-help-text"),
				helpLabel(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_HELP_NBT_ABSENT, "replace-blocks-builder-help-text"),
				helpLabel(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_HELP_NBT_NOTE, "replace-blocks-builder-help-text"),
				helpLabel(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_HELP_BIOME_TITLE, "replace-blocks-builder-help-heading"),
				helpLabel(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_HELP_BIOME_INTRO, "replace-blocks-builder-help-text"),
				helpLabel(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_HELP_BIOME_SYNTAX, "replace-blocks-builder-help-text"),
				helpLabel(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_ADVANCED, "replace-blocks-builder-help-text")
		);

		ScrollPane scroll = new ScrollPane(content);
		scroll.setFitToWidth(true);
		scroll.getStyleClass().add("replace-blocks-builder-help-scroll");
		help.getDialogPane().setContent(scroll);
		help.showAndWait();
	}

	private static Label helpLabel(Translation translation, String styleClass) {
		Label label = UIFactory.label(translation);
		label.setWrapText(true);
		label.getStyleClass().add(styleClass);
		return label;
	}

	private static String formatFrom(String name) {
		if (name.startsWith("{") || name.startsWith("'") || isSourceModeExpression(name)) {
			return name;
		}
		return "literal(" + formatWrapperArgument(name) + ")";
	}

	private static String formatTo(String name) {
		if (name.startsWith("{") || name.startsWith("'")) {
			return name;
		}
		if (name.startsWith("minecraft:")) {
			return name;
		}
		return "{Name:\"" + name + "\"}";
	}

	private static boolean isSourceModeExpression(String value) {
		return value.startsWith("literal(") || value.startsWith("regex(") || value.startsWith("props(")
				|| value.startsWith("tile(") || value.startsWith("no_tile(") || value.startsWith("y(") || value.startsWith("biome(");
	}

	private static boolean isTileEntityModeExpression(String value) {
		return value.startsWith("tile(") || value.startsWith("no_tile(");
	}

	private static boolean isYRangeExpression(String value) {
		return value.startsWith("y(");
	}

	private static boolean isBiomeExpression(String value) {
		return value.startsWith("biome(");
	}

	private static String formatWrapperArgument(String value) {
		if (!value.equals(value.trim()) || value.contains(")")) {
			return "'" + value + "'";
		}
		return value;
	}

	static String biomeToken(String text, int caretPosition) {
		if (text == null || text.isEmpty()) {
			return "";
		}
		int caret = Math.max(0, Math.min(caretPosition, text.length()));
		int start = text.lastIndexOf(';', Math.max(0, caret - 1)) + 1;
		int end = text.indexOf(';', caret);
		if (end < 0) {
			end = text.length();
		}
		return text.substring(start, end).trim();
	}

	private void showDiagnostic(ReplaceBlocksDiagnostics.Diagnostic diagnostic) {
		validation.textProperty().unbind();
		validation.pseudoClassStateChanged(error, false);
		validation.pseudoClassStateChanged(warning, false);
		validation.pseudoClassStateChanged(success, false);
		if (diagnostic.isNone()) {
			validation.setText("");
			return;
		}
		validation.setText(diagnostic.message());
		validation.pseudoClassStateChanged(diagnostic.isError() ? error : warning, true);
	}

	private static String blockStateSNBT(String name, Map<String, String> properties) {
		StringBuilder builder = new StringBuilder("{Name:\"").append(name).append("\"");
		if (!properties.isEmpty()) {
			builder.append(",Properties:{");
			boolean first = true;
			for (Map.Entry<String, String> property : properties.entrySet()) {
				if (!first) {
					builder.append(",");
				}
				builder.append(property.getKey()).append(":\"").append(property.getValue()).append("\"");
				first = false;
			}
			builder.append("}");
		}
		builder.append("}");
		return builder.toString();
	}

	static Map<String, String> editableStateProperties(CompoundTag state) {
		if (state == null || state.getString("Name") == null) {
			return null;
		}
		for (Map.Entry<String, Tag> entry : state) {
			if (!"Name".equals(entry.getKey()) && !"Properties".equals(entry.getKey())) {
				return null;
			}
		}
		CompoundTag properties = state.getCompoundTag("Properties");
		if (properties == null || properties.isEmpty()) {
			return Map.of();
		}
		Map<String, String> values = new LinkedHashMap<>();
		for (Map.Entry<String, Tag> property : properties) {
			if (!(property.getValue() instanceof StringTag value)) {
				return null;
			}
			values.put(property.getKey(), value.getValue());
		}
		return values;
	}

	private class BlockInput extends ReplaceBlocksBlockInput {

		private final ComboBox<String> block = new ComboBox<>();
		private final ObservableList<String> blockSuggestions = FXCollections.observableArrayList();
		private final ComboBox<SourceTileMode> tileEntityMode = new ComboBox<>();
		private final TextField minY = new TextField();
		private final TextField maxY = new TextField();
		private final ComboBox<String> biomeNames = new ComboBox<>();
		private final ObservableList<String> biomeSuggestions = FXCollections.observableArrayList();
		private final GridPane properties = new GridPane();
		private final GridPane sourceConstraints = new GridPane();
		private final Map<String, ComboBox<PropertyChoice>> propertyEditors = new LinkedHashMap<>();
		private final BooleanProperty userInputPresent = new SimpleBooleanProperty(false);
		private boolean updatingItems;
		private boolean updatingBiomeItems;
		private boolean suppressSuggestions;
		private boolean suppressBiomeSuggestions;
		private int blockSuggestionRevision;
		private int biomeSuggestionRevision;
		private int blockSuggestionHighlight = -1;
		private int biomeSuggestionHighlight = -1;
		private boolean blockExplicitCatalog;
		private boolean biomeExplicitCatalog;

		private BlockInput(boolean source) {
			super(source, catalog);
			getStyleClass().add("replace-blocks-builder-block");
			addEventFilter(KeyEvent.KEY_PRESSED, this::handleAutocompleteKeyPressed);
			setMaxWidth(Double.MAX_VALUE);
			setPrefWidth(0);
			setSpacing(4);

			Label label = UIFactory.label(source
					? Translation.DIALOG_REPLACE_BLOCKS_BUILDER_FROM
					: Translation.DIALOG_REPLACE_BLOCKS_BUILDER_TO);

			block.setEditable(true);
			block.setMinWidth(0);
			block.setMaxWidth(Double.MAX_VALUE);
			block.setVisibleRowCount(12);
			block.setItems(blockSuggestions);
			configureAutocompleteComboBox(block);
			block.setCellFactory(v -> new HighlightedBlockCell());
			installAutocompletePopupKeyFilter(block, this::handleKeyPressed);
			block.addEventHandler(ComboBoxBase.ON_SHOWN, event -> blockSuggestionHighlight = -1);
			block.addEventFilter(MouseEvent.MOUSE_PRESSED, this::handleBlockMousePressed);
			block.setOnHidden(event -> scheduleBlockExplicitCatalogCleanup());
			block.getEditor().setAlignment(Pos.CENTER);
			block.getEditor().setOnAction(ReplaceBlocksRuleBuilderDialog.this::addRule);
			block.getEditor().textProperty().addListener((a, o, n) -> {
				if (suppressSuggestions) {
					return;
				}
				updateUserInputPresent();
				clearPresetSelectionAfterEdit();
				if (!updatingItems) {
					rebuildProperties(n);
					scheduleBlockSuggestions(n);
				}
			});

			properties.getStyleClass().add("replace-blocks-builder-properties");
			properties.setHgap(6);
			properties.setVgap(4);
			setPropertiesVisible(false);

			getChildren().addAll(label, block, properties);
			VBox.setVgrow(properties, Priority.NEVER);
			if (source) {
				sourceConstraints.getStyleClass().add("replace-blocks-builder-source-constraints");
				sourceConstraints.setHgap(8);
				sourceConstraints.setVgap(4);
				Label constraintsLabel = UIFactory.label(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_SOURCE_CONSTRAINTS);
				constraintsLabel.getStyleClass().add("replace-blocks-builder-source-constraints-title");
				sourceConstraints.add(constraintsLabel, 0, 0, 3, 1);
				ColumnConstraints tileColumn = new ColumnConstraints();
				tileColumn.setMinWidth(180);
				tileColumn.setPrefWidth(220);
				ColumnConstraints yColumn = new ColumnConstraints();
				yColumn.setMinWidth(220);
				yColumn.setPrefWidth(250);
				ColumnConstraints biomeColumn = new ColumnConstraints();
				biomeColumn.setHgrow(Priority.ALWAYS);
				biomeColumn.setFillWidth(true);
				sourceConstraints.getColumnConstraints().addAll(tileColumn, yColumn, biomeColumn);
				tileEntityMode.setItems(FXCollections.observableArrayList(SourceTileMode.values()));
				configureBuilderComboBox(tileEntityMode);
				tileEntityMode.setValue(SourceTileMode.ANY);
				tileEntityMode.setMinWidth(0);
				tileEntityMode.setMaxWidth(Double.MAX_VALUE);
				tileEntityMode.valueProperty().addListener((a, o, n) -> clearPresetSelectionAfterEdit());
				GridPane yRange = new GridPane();
				yRange.getStyleClass().add("replace-blocks-builder-y-range");
				yRange.setHgap(6);
				Label minLabel = UIFactory.label(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_Y_MIN);
				Label maxLabel = UIFactory.label(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_Y_MAX);
				minLabel.setMinWidth(Region.USE_PREF_SIZE);
				maxLabel.setMinWidth(Region.USE_PREF_SIZE);
				minY.promptTextProperty().bind(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_Y_MIN.getProperty());
				maxY.promptTextProperty().bind(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_Y_MAX.getProperty());
				minY.setMinWidth(0);
				minY.setMaxWidth(Double.MAX_VALUE);
				maxY.setMinWidth(0);
				maxY.setMaxWidth(Double.MAX_VALUE);
				minY.textProperty().addListener((a, o, n) -> {
					updateUserInputPresent();
					clearPresetSelectionAfterEdit();
				});
				maxY.textProperty().addListener((a, o, n) -> {
					updateUserInputPresent();
					clearPresetSelectionAfterEdit();
				});
				yRange.add(minLabel, 0, 0);
				yRange.add(minY, 1, 0);
				yRange.add(maxLabel, 2, 0);
				yRange.add(maxY, 3, 0);
				GridPane.setHgrow(minY, Priority.ALWAYS);
				GridPane.setHgrow(maxY, Priority.ALWAYS);
				GridPane biomeFilter = new GridPane();
				biomeFilter.getStyleClass().add("replace-blocks-builder-biome");
				biomeFilter.setHgap(6);
				Label biomeLabel = UIFactory.label(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_BIOME);
				biomeNames.setEditable(true);
				biomeNames.setMinWidth(0);
				biomeNames.setMaxWidth(Double.MAX_VALUE);
				biomeNames.setVisibleRowCount(12);
				biomeNames.setItems(biomeSuggestions);
				configureAutocompleteComboBox(biomeNames);
				biomeNames.setCellFactory(v -> new HighlightedBiomeCell());
				installAutocompletePopupKeyFilter(biomeNames, this::handleBiomeKeyPressed);
				biomeNames.addEventHandler(ComboBoxBase.ON_SHOWN, event -> biomeSuggestionHighlight = -1);
				biomeNames.addEventFilter(MouseEvent.MOUSE_PRESSED, this::handleBiomeMousePressed);
				biomeNames.setOnHidden(event -> scheduleBiomeExplicitCatalogCleanup());
				biomeNames.getEditor().promptTextProperty().bind(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_BIOME_PROMPT.getProperty());
				biomeNames.getEditor().textProperty().addListener((a, o, n) -> {
					if (suppressBiomeSuggestions) {
						return;
					}
					updateUserInputPresent();
					clearPresetSelectionAfterEdit();
					if (!updatingBiomeItems) {
						scheduleBiomeSuggestions();
					}
				});
				biomeFilter.add(biomeLabel, 0, 0);
				biomeFilter.add(biomeNames, 1, 0);
				GridPane.setHgrow(biomeNames, Priority.ALWAYS);
				sourceConstraints.add(tileEntityMode, 0, 1);
				sourceConstraints.add(yRange, 1, 1);
				sourceConstraints.add(biomeFilter, 2, 1);
				GridPane.setHgrow(tileEntityMode, Priority.ALWAYS);
				GridPane.setHgrow(yRange, Priority.ALWAYS);
			}
			updateBlockSuggestions("");
			updateBiomeSuggestions();
			rebuildProperties("");
		}

		private BooleanProperty userInputPresentProperty() {
			return userInputPresent;
		}

		private Node sourceConstraints() {
			return sourceConstraints;
		}

		private ValueResult value() {
			String generated = generatedValue();
			if (generated == null) {
				return new ValueResult(null, ReplaceBlocksDiagnostics.builderInvalid());
			}
			ReplaceBlocksDiagnostics.Diagnostic catalogDiagnostic = catalogDiagnostic();
			if (!source && generated.contains(";")) {
				ReplaceBlocksField field = new ReplaceBlocksField();
				boolean valid = ReplaceBlocksDiagnostics.parseReplaceBlocksValue(field, "minecraft:stone=" + generated);
				return new ValueResult(generated, valid ? catalogDiagnostic : ReplaceBlocksDiagnostics.builderInvalid());
			}
			if (source && isSourceModeExpression(generated)) {
				ReplaceBlocksField field = new ReplaceBlocksField();
				boolean valid = ReplaceBlocksDiagnostics.parseReplaceBlocksValue(field, generated + "=minecraft:stone");
				return new ValueResult(generated, valid ? catalogDiagnostic : ReplaceBlocksDiagnostics.builderInvalid());
			}
			ReplaceBlocksDiagnostics.NameResult normalized = ReplaceBlocksDiagnostics.normalizeBuilderValue(generated, source);
			return new ValueResult(normalized.name(), normalized.diagnostic().isError()
					? normalized.diagnostic() : catalogDiagnostic);
		}

		private ReplaceBlocksDiagnostics.Diagnostic catalogDiagnostic() {
			return catalogDiagnostic(currentText());
		}

		private void catalogChanged() {
			setCatalog(catalog);
		}

		private boolean hasMeaningfulContent() {
			return !currentText().isEmpty()
					|| source && tileEntityMode.getValue() != SourceTileMode.ANY
					|| minY.getText() != null && !minY.getText().isBlank()
					|| maxY.getText() != null && !maxY.getText().isBlank()
					|| biomeNames.getEditor().getText() != null && !biomeNames.getEditor().getText().isBlank()
					|| propertyEditors.values().stream()
							.map(ComboBox::getValue)
							.anyMatch(choice -> choice != null && !choice.all());
		}

		private BlockInputState contentState() {
			List<PropertyState> propertyStates = propertyEditors.entrySet().stream()
					.map(entry -> new PropertyState(entry.getKey(), entry.getValue().getValue()))
					.toList();
			return new BlockInputState(
					valueOrEmpty(block.getEditor().getText()),
					source ? tileEntityMode.getValue() : null,
					source ? valueOrEmpty(minY.getText()) : "",
					source ? valueOrEmpty(maxY.getText()) : "",
					source ? valueOrEmpty(biomeNames.getEditor().getText()) : "",
					propertyStates);
		}

		private String generatedValue() {
			String text = currentText();
			if (text.isEmpty()) {
				return text;
			}
			if (text.startsWith("{") || source && isSourceModeExpression(text)) {
				return source ? applySourceConditions(text) : text;
			}
			ReplaceBlocksDiagnostics.NameResult normalized = ReplaceBlocksDiagnostics.normalizeBuilderName(text, source);
			if (normalized.diagnostic().isError()) {
				return text;
			}
			String name = normalized.name();
			if (propertyEditors.isEmpty()) {
				String value = source ? "literal(" + formatWrapperArgument(name) + ")" : name;
				return source ? applySourceConditions(value) : value;
			}
			Map<String, String> selectedProperties = new LinkedHashMap<>();
			for (Map.Entry<String, ComboBox<PropertyChoice>> property : propertyEditors.entrySet()) {
				PropertyChoice choice = property.getValue().getValue();
				if (choice == null) {
					return null;
				}
				if (choice.all()) {
					continue;
				}
				if (!catalog.isValidPropertyValue(name, property.getKey(), choice.value())) {
					return null;
				}
				selectedProperties.put(property.getKey(), choice.value());
			}
			String value;
			if (selectedProperties.isEmpty()) {
				value = source ? "literal(" + formatWrapperArgument(name) + ")" : name;
			} else {
				String blockState = blockStateSNBT(name, selectedProperties);
				value = source ? "props(" + blockState + ")" : blockState;
			}
			return source ? applySourceConditions(value) : value;
		}

		private String applySourceConditions(String value) {
			return applySourceBiome(applySourceYRange(applySourceTileMode(value)));
		}

		private String applySourceTileMode(String value) {
			if (!source || value == null || value.isEmpty() || isTileEntityModeExpression(value)) {
				return value;
			}
			return tileEntityMode.getValue().wrap(value);
		}

		private String applySourceYRange(String value) {
			if (!source || value == null || value.isEmpty() || isYRangeExpression(value)) {
				return value;
			}
			String range = yRangeText();
			if (range == null) {
				return value;
			}
			if (range.isEmpty()) {
				return null;
			}
			return "y(" + range + ", " + value + ")";
		}

		private String yRangeText() {
			String min = minY.getText() == null ? "" : minY.getText().trim();
			String max = maxY.getText() == null ? "" : maxY.getText().trim();
			if (min.isEmpty() && max.isEmpty()) {
				return null;
			}
			try {
				Integer minValue = min.isEmpty() ? null : Integer.parseInt(min);
				Integer maxValue = max.isEmpty() ? null : Integer.parseInt(max);
				if (minValue != null && maxValue != null && minValue > maxValue) {
					return "";
				}
				return min + ".." + max;
			} catch (NumberFormatException ex) {
				return "";
			}
		}

		private String applySourceBiome(String value) {
			if (!source || value == null || value.isEmpty() || isBiomeExpression(value)) {
				return value;
			}
			String biomes = biomeText();
			if (biomes == null) {
				return value;
			}
			if (biomes.isEmpty()) {
				return null;
			}
			return "biome(" + biomes + ", " + value + ")";
		}

		private String biomeText() {
			String raw = biomeNames.getEditor().getText() == null ? "" : biomeNames.getEditor().getText().trim();
			if (raw.isEmpty()) {
				return null;
			}
			String[] parts = raw.split(";", -1);
			List<String> normalized = new ArrayList<>(parts.length);
			for (String part : parts) {
				String biome = part.trim();
				if (biome.isEmpty()) {
					return "";
				}
				if (!biome.contains(":")) {
					biome = "minecraft:" + biome;
				}
				if (biome.startsWith("minecraft:") && !BiomeRegistry.isValidName(biome)
						|| !biome.startsWith("minecraft:") && !RESOURCE_LOCATION.matcher(biome).matches()) {
					return "";
				}
				normalized.add(biome);
			}
			return String.join(";", normalized);
		}

		private void clear() {
			suppressSuggestions = true;
			suppressBiomeSuggestions = true;
			try {
				blockSuggestionRevision++;
				biomeSuggestionRevision++;
				blockExplicitCatalog = false;
				biomeExplicitCatalog = false;
				blockSuggestionHighlight = -1;
				biomeSuggestionHighlight = -1;
				block.hide();
				biomeNames.hide();
				tileEntityMode.hide();
				propertyEditors.values().forEach(ComboBox::hide);
				clearPopupSuggestionHighlight(block);
				clearPopupSuggestionHighlight(biomeNames);
				block.setValue(null);
				block.getSelectionModel().clearSelection();
				block.getEditor().clear();
				minY.clear();
				maxY.clear();
				biomeNames.setValue(null);
				biomeNames.getSelectionModel().clearSelection();
				biomeNames.getEditor().clear();
				if (source) {
					tileEntityMode.setValue(SourceTileMode.ANY);
				}
				propertyEditors.clear();
				properties.getChildren().clear();
				setPropertiesVisible(false);
				updateBlockSuggestions("");
				updateBiomeSuggestions();
				userInputPresent.set(false);
			} finally {
				suppressSuggestions = false;
				suppressBiomeSuggestions = false;
			}
		}

		private void applyPreset(String text, SourceTileMode sourceTileMode) {
			suppressSuggestions = true;
			try {
				String value = text == null ? "" : text;
				block.setValue(null);
				block.getSelectionModel().clearSelection();
				block.getEditor().setText(value);
				block.hide();
				updateBlockSuggestions(value);
				rebuildProperties(value);
				collapseEditorCaretToEnd();
				if (source) {
					tileEntityMode.setValue(sourceTileMode == null ? SourceTileMode.ANY : sourceTileMode);
					minY.clear();
					maxY.clear();
					clearBiomeInput();
				}
				updateUserInputPresent();
			} finally {
				suppressSuggestions = false;
			}
		}

		private void applySource(ChunkFilter.BlockReplaceSource source, String fallback) {
			boolean applied = switch (source.getType()) {
				case LITERAL_NAME -> applyEditableBlock(source.getName(), Map.of());
				case SELECTED_PROPERTIES -> {
					Map<String, String> selectedProperties = editableStateProperties(source.getState());
					yield selectedProperties != null && applyEditableBlock(source.getName(), selectedProperties);
				}
				case REGEX_NAME -> {
					applyPreset("regex(" + formatWrapperArgument(source.getName()) + ")", SourceTileMode.fromTileEntityMode(source.getTileEntityMode()));
					yield true;
				}
				default -> false;
			};
			if (applied) {
				applySourceConditions(source);
			} else {
				applyPreset(fallback, SourceTileMode.ANY);
			}
		}

		private void applyTarget(ChunkFilter.BlockReplaceData target, String fallback) {
			boolean applied = switch (target.getType()) {
				case NAME -> applyEditableBlock(target.getName(), Map.of());
				case STATE -> {
					Map<String, String> selectedProperties = editableStateProperties(target.getState());
					yield selectedProperties != null && applyEditableBlock(target.getName(), selectedProperties);
				}
				default -> false;
			};
			if (!applied) {
				applyPreset(fallback, SourceTileMode.ANY);
			}
		}

		private boolean applyEditableBlock(String name, Map<String, String> selectedProperties) {
			String blockName = BlockStateCatalog.normalizeName(name);
			suppressSuggestions = true;
			try {
				block.setValue(null);
				block.getSelectionModel().clearSelection();
				block.getEditor().setText(blockName);
				block.hide();
				updateBlockSuggestions(blockName);
				rebuildProperties(blockName);
				if (!selectProperties(blockName, selectedProperties)) {
					return false;
				}
				collapseEditorCaretToEnd();
				updateUserInputPresent();
				return true;
			} finally {
				suppressSuggestions = false;
			}
		}

		private boolean selectProperties(String blockName, Map<String, String> selectedProperties) {
			for (Map.Entry<String, String> selected : selectedProperties.entrySet()) {
				if (!catalog.isValidPropertyValue(blockName, selected.getKey(), selected.getValue())) {
					return false;
				}
				ComboBox<PropertyChoice> editor = propertyEditors.get(selected.getKey());
				if (editor == null) {
					return false;
				}
				editor.setValue(PropertyChoice.value(selected.getValue()));
			}
			return true;
		}

		private void applySourceConditions(ChunkFilter.BlockReplaceSource source) {
			if (!this.source) {
				return;
			}
			tileEntityMode.setValue(SourceTileMode.fromTileEntityMode(source.getTileEntityMode()));
			minY.setText(source.getMinY() == null ? "" : Integer.toString(source.getMinY()));
			maxY.setText(source.getMaxY() == null ? "" : Integer.toString(source.getMaxY()));
			setBiomeText(source.getBiomes().stream().collect(Collectors.joining(";")));
			updateUserInputPresent();
		}

		private void focusInput() {
			block.requestFocus();
			block.getEditor().requestFocus();
		}

		private String currentText() {
			String text = block.getEditor().getText();
			return text == null ? "" : text.trim();
		}

		private void updateUserInputPresent() {
			userInputPresent.set(!currentText().isEmpty()
					|| minY.getText() != null && !minY.getText().isBlank()
					|| maxY.getText() != null && !maxY.getText().isBlank()
					|| biomeNames.getEditor().getText() != null && !biomeNames.getEditor().getText().isBlank());
			updatePresetButtons();
		}

		private void updateBlockSuggestions(String query) {
			blockExplicitCatalog = false;
			updateBlockSuggestions(query, false);
		}

		private void updateBlockSuggestions(String query, boolean allowEmpty) {
			String text = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
			blockSuggestionHighlight = -1;
			clearPopupSuggestionHighlight(block);
			updatingItems = true;
			try {
				block.hide();
				if (text.startsWith("{") || source && isSourceModeExpression(text)) {
					blockSuggestions.clear();
				} else {
					blockSuggestions.setAll(suggestionsForQuery(blockNames, text, allowEmpty));
				}
				block.setVisibleRowCount(Math.max(1, Math.min(12, blockSuggestions.size())));
			} finally {
				updatingItems = false;
			}
		}

		private void scheduleBlockSuggestions(String query) {
			int revision = ++blockSuggestionRevision;
			Platform.runLater(() -> {
				if (revision == blockSuggestionRevision && !suppressSuggestions) {
					updateBlockSuggestions(query);
					showBlockSuggestions(query);
				}
			});
		}

		private void showBlockSuggestions(String query) {
			if (suppressSuggestions) {
				return;
			}
			String text = query == null ? "" : query.trim();
			if (text.isEmpty() || text.startsWith("{") || source && isSourceModeExpression(text) || block.getItems().isEmpty()) {
				block.hide();
				return;
			}
			block.show();
		}

		private void updateBiomeSuggestions() {
			biomeExplicitCatalog = false;
			updateBiomeSuggestions(false);
		}

		private void updateBiomeSuggestions(boolean allowEmpty) {
			String query = currentBiomeQuery().toLowerCase(Locale.ROOT);
			biomeSuggestionHighlight = -1;
			clearPopupSuggestionHighlight(biomeNames);
			updatingBiomeItems = true;
			try {
				biomeNames.hide();
				biomeSuggestions.setAll(suggestionsForQuery(biomeCatalogNames, query, allowEmpty));
				biomeNames.setVisibleRowCount(Math.max(1, Math.min(12, biomeSuggestions.size())));
			} finally {
				updatingBiomeItems = false;
			}
		}

		private void scheduleBiomeSuggestions() {
			int revision = ++biomeSuggestionRevision;
			Platform.runLater(() -> {
				if (revision == biomeSuggestionRevision && !suppressBiomeSuggestions) {
					updateBiomeSuggestions();
					showBiomeSuggestions();
				}
			});
		}

		private void showBiomeSuggestions() {
			if (suppressBiomeSuggestions) {
				return;
			}
			if (currentBiomeQuery().isEmpty() || biomeNames.getItems().isEmpty()) {
				biomeNames.hide();
				return;
			}
			biomeNames.show();
		}

		private void handleAutocompleteKeyPressed(KeyEvent event) {
			if (!isAutocompletePopupKey(event.getCode())) {
				return;
			}
			if (block.isShowing()) {
				handleKeyPressed(event);
			} else if (source && biomeNames.isShowing()) {
				handleBiomeKeyPressed(event);
			}
		}

		private void handleBlockMousePressed(MouseEvent event) {
			if (event.getButton() == MouseButton.PRIMARY && !block.isShowing()
					&& currentText().isEmpty() && isComboBoxArrowTarget(event.getTarget())) {
				blockExplicitCatalog = true;
				updateBlockSuggestions("", true);
			}
		}

		private void handleBiomeMousePressed(MouseEvent event) {
			String text = biomeNames.getEditor().getText();
			if (event.getButton() == MouseButton.PRIMARY && !biomeNames.isShowing()
					&& (text == null || text.isBlank()) && isComboBoxArrowTarget(event.getTarget())) {
				biomeExplicitCatalog = true;
				updateBiomeSuggestions(true);
			}
		}

		private void scheduleBlockExplicitCatalogCleanup() {
			if (!blockExplicitCatalog) {
				return;
			}
			Platform.runLater(() -> {
				String text = block.getEditor().getText();
				if (shouldClearExplicitCatalog(blockExplicitCatalog, block.isShowing(), text)) {
					blockExplicitCatalog = false;
					blockSuggestionHighlight = -1;
					clearPopupSuggestionHighlight(block);
					clearEmptyExplicitCatalog(block);
					block.setVisibleRowCount(1);
				} else if (!block.isShowing() && text != null && !text.isBlank()) {
					blockExplicitCatalog = false;
				}
			});
		}

		private void scheduleBiomeExplicitCatalogCleanup() {
			if (!biomeExplicitCatalog) {
				return;
			}
			Platform.runLater(() -> {
				String text = biomeNames.getEditor().getText();
				if (shouldClearExplicitCatalog(biomeExplicitCatalog, biomeNames.isShowing(), text)) {
					biomeExplicitCatalog = false;
					biomeSuggestionHighlight = -1;
					clearPopupSuggestionHighlight(biomeNames);
					clearEmptyExplicitCatalog(biomeNames);
					biomeNames.setVisibleRowCount(1);
				} else if (!biomeNames.isShowing() && text != null && !text.isBlank()) {
					biomeExplicitCatalog = false;
				}
			});
		}

		private void handleBiomeKeyPressed(KeyEvent event) {
			if (biomeNames.isShowing()) {
				if (event.getCode() == KeyCode.ENTER) {
					String suggestion = selectedBiomeSuggestion();
					if (suggestion != null) {
						completeBiomeSuggestion(suggestion);
						event.consume();
					}
					return;
				}
				if (moveBiomeSuggestionHighlight(event)) {
					return;
				}
			}
			if (event.getCode() == KeyCode.TAB && biomeNames.isShowing()) {
				String suggestion = selectedBiomeSuggestion();
				if (suggestion != null) {
					completeBiomeSuggestion(suggestion);
					event.consume();
				}
			}
		}

		private String selectedBiomeSuggestion() {
			if (biomeSuggestionHighlight >= 0 && biomeSuggestionHighlight < biomeNames.getItems().size()) {
				return biomeNames.getItems().get(biomeSuggestionHighlight);
			}
			String selected = biomeNames.getSelectionModel().getSelectedItem();
			if (selected != null && biomeNames.getItems().contains(selected)) {
				return selected;
			}
			return biomeNames.getItems().isEmpty() ? null : biomeNames.getItems().get(0);
		}

		private boolean moveBiomeSuggestionHighlight(KeyEvent event) {
			int visibleRows = popupVisibleRowCount(biomeNames, biomeNames.getVisibleRowCount());
			int target = popupSelectionTarget(event.getCode(), biomeSuggestionHighlight,
					biomeNames.getItems().size(), visibleRows);
			if (target < 0) {
				return false;
			}
			biomeSuggestionHighlight = target;
			focusPopupSuggestion(biomeNames, target);
			event.consume();
			return true;
		}

		private void completeBiomeSuggestion(String suggestion) {
			TextField editor = biomeNames.getEditor();
			String text = editor.getText();
			if (text == null) {
				text = "";
			}
			int caret = Math.max(0, Math.min(editor.getCaretPosition(), text.length()));
			int start = text.lastIndexOf(';', Math.max(0, caret - 1)) + 1;
			int end = text.indexOf(';', caret);
			if (end < 0) {
				end = text.length();
			}
			String completed = text.substring(0, start) + suggestion + text.substring(end);
			int caretPosition = start + suggestion.length();

			suppressBiomeSuggestions = true;
			try {
				biomeNames.setValue(null);
				biomeNames.getSelectionModel().clearSelection();
				editor.setText(completed);
				editor.selectRange(caretPosition, caretPosition);
				updateUserInputPresent();
				updateBiomeSuggestions();
				biomeNames.hide();
			} finally {
				suppressBiomeSuggestions = false;
			}
		}

		private String currentBiomeQuery() {
			TextField editor = biomeNames.getEditor();
			return biomeToken(editor.getText(), editor.getCaretPosition());
		}

		private void clearBiomeInput() {
			suppressBiomeSuggestions = true;
			try {
				biomeNames.setValue(null);
				biomeNames.getSelectionModel().clearSelection();
				biomeNames.getEditor().clear();
				biomeNames.hide();
			} finally {
				suppressBiomeSuggestions = false;
			}
		}

		private void setBiomeText(String value) {
			suppressBiomeSuggestions = true;
			try {
				biomeNames.setValue(null);
				biomeNames.getSelectionModel().clearSelection();
				biomeNames.getEditor().setText(value == null ? "" : value);
				biomeNames.hide();
				updateBiomeSuggestions();
			} finally {
				suppressBiomeSuggestions = false;
			}
		}

		private void handleKeyPressed(KeyEvent event) {
			if (block.isShowing()) {
				if (event.getCode() == KeyCode.ENTER) {
					String suggestion = selectedSuggestion();
					if (suggestion != null) {
						completeSuggestion(suggestion);
						event.consume();
					}
					return;
				}
				if (moveBlockSuggestionHighlight(event)) {
					return;
				}
			}
			if (event.getCode() == KeyCode.TAB && block.isShowing()) {
				String suggestion = selectedSuggestion();
				if (suggestion != null) {
					completeSuggestion(suggestion);
					event.consume();
				}
			}
		}

		private String selectedSuggestion() {
			if (blockSuggestionHighlight >= 0 && blockSuggestionHighlight < block.getItems().size()) {
				return block.getItems().get(blockSuggestionHighlight);
			}
			String selected = block.getSelectionModel().getSelectedItem();
			if (selected != null && block.getItems().contains(selected)) {
				return selected;
			}
			return block.getItems().isEmpty() ? null : block.getItems().get(0);
		}

		private boolean moveBlockSuggestionHighlight(KeyEvent event) {
			int visibleRows = popupVisibleRowCount(block, block.getVisibleRowCount());
			int target = popupSelectionTarget(event.getCode(), blockSuggestionHighlight,
					block.getItems().size(), visibleRows);
			if (target < 0) {
				return false;
			}
			blockSuggestionHighlight = target;
			focusPopupSuggestion(block, target);
			event.consume();
			return true;
		}

		private void completeSuggestion(String suggestion) {
			commitEditorText(suggestion);
		}

		private void commitEditorText(String text) {
			suppressSuggestions = true;
			try {
				block.setValue(null);
				block.getSelectionModel().clearSelection();
				block.getEditor().setText(text);
				updateUserInputPresent();
				updateBlockSuggestions(text);
				rebuildProperties(text);
				block.hide();
				collapseEditorCaretToEnd();
			} finally {
				suppressSuggestions = false;
			}
		}

		private void collapseEditorCaretToEnd() {
			String text = block.getEditor().getText();
			if (text != null) {
				block.getEditor().selectRange(text.length(), text.length());
			}
		}

		private void rebuildProperties(String rawBlockName) {
			propertyEditors.clear();
			properties.getChildren().clear();
			setPropertiesVisible(false);

			String text = rawBlockName == null ? "" : rawBlockName.trim();
			if (text.isEmpty() || text.startsWith("{") || source && isSourceModeExpression(text)) {
				return;
			}

			String blockName = BlockStateCatalog.normalizeName(text);
			Map<String, List<String>> catalogProperties = catalog.properties(blockName);
			if (catalogProperties.isEmpty()) {
				return;
			}

			int row = 0;
			for (Map.Entry<String, List<String>> property : catalogProperties.entrySet()) {
				Label name = new Label(property.getKey());
				ObservableList<PropertyChoice> choices = FXCollections.observableArrayList();
				choices.add(PropertyChoice.allChoice());
				choices.addAll(property.getValue().stream().map(PropertyChoice::value).collect(Collectors.toList()));
				ComboBox<PropertyChoice> value = new ComboBox<>(choices);
				configureBuilderComboBox(value);
				value.setMinWidth(0);
				value.setMaxWidth(Double.MAX_VALUE);
				value.setCellFactory(v -> new PropertyChoiceCell());
				value.setButtonCell(new PropertyChoiceCell());
				value.setValue(PropertyChoice.allChoice());
				value.valueProperty().addListener((a, o, n) -> clearPresetSelectionAfterEdit());
				value.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
					if (!value.isShowing()) {
						Platform.runLater(value::show);
					}
				});
				propertyEditors.put(property.getKey(), value);
				properties.add(name, 0, row);
				properties.add(value, 1, row);
				GridPane.setHgrow(value, Priority.ALWAYS);
				row++;
			}
			setPropertiesVisible(true);
		}

		private void setPropertiesVisible(boolean visible) {
			properties.setVisible(visible);
			properties.setManaged(visible);
		}

		private class HighlightedBlockCell extends ListCell<String> {

			private HighlightedBlockCell() {
				addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
					if (!isEmpty() && getItem() != null) {
						completeSuggestion(getItem());
						event.consume();
					}
				});
			}

			@Override
			protected void updateItem(String item, boolean empty) {
				super.updateItem(item, empty);
				pseudoClassStateChanged(autocompleteHighlighted,
						isAutocompleteCellHighlighted(getIndex(), blockSuggestionHighlight, empty));
				if (empty || item == null) {
					setText(null);
					setGraphic(null);
					return;
				}
				String query = currentText();
				if (query.isEmpty()) {
					setText(item);
					setGraphic(null);
					return;
				}
				setText(null);
				setGraphic(highlightedText(item, query));
			}

			private TextFlow highlightedText(String item, String query) {
				TextFlow flow = new TextFlow();
				flow.getStyleClass().add("replace-blocks-builder-suggestion-text");
				String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
				if (needle.isEmpty()) {
					flow.getChildren().add(text(item, false));
					return flow;
				}

				String haystack = item.toLowerCase(Locale.ROOT);
				int offset = 0;
				int match;
				while ((match = haystack.indexOf(needle, offset)) >= 0) {
					if (match > offset) {
						flow.getChildren().add(text(item.substring(offset, match), false));
					}
					int end = match + needle.length();
					flow.getChildren().add(text(item.substring(match, end), true));
					offset = end;
				}
				if (offset < item.length()) {
					flow.getChildren().add(text(item.substring(offset), false));
				}
				if (flow.getChildren().isEmpty()) {
					flow.getChildren().add(text(item, false));
				}
				return flow;
			}

			private Text text(String value, boolean highlight) {
				Text text = new Text(value);
				text.getStyleClass().add(highlight ? "replace-blocks-builder-suggestion-highlight" : "replace-blocks-builder-suggestion-normal");
				return text;
			}
		}

		private class HighlightedBiomeCell extends ListCell<String> {

			private HighlightedBiomeCell() {
				addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
					if (!isEmpty() && getItem() != null) {
						completeBiomeSuggestion(getItem());
						event.consume();
					}
				});
			}

			@Override
			protected void updateItem(String item, boolean empty) {
				super.updateItem(item, empty);
				pseudoClassStateChanged(autocompleteHighlighted,
						isAutocompleteCellHighlighted(getIndex(), biomeSuggestionHighlight, empty));
				if (empty || item == null) {
					setText(null);
					setGraphic(null);
					return;
				}
				String query = currentBiomeQuery();
				if (query.isEmpty()) {
					setText(item);
					setGraphic(null);
					return;
				}
				setText(null);
				setGraphic(highlightedText(item, query));
			}

			private TextFlow highlightedText(String item, String query) {
				TextFlow flow = new TextFlow();
				flow.getStyleClass().add("replace-blocks-builder-suggestion-text");
				String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
				if (needle.isEmpty()) {
					flow.getChildren().add(text(item, false));
					return flow;
				}

				String haystack = item.toLowerCase(Locale.ROOT);
				int offset = 0;
				int match;
				while ((match = haystack.indexOf(needle, offset)) >= 0) {
					if (match > offset) {
						flow.getChildren().add(text(item.substring(offset, match), false));
					}
					int end = match + needle.length();
					flow.getChildren().add(text(item.substring(match, end), true));
					offset = end;
				}
				if (offset < item.length()) {
					flow.getChildren().add(text(item.substring(offset), false));
				}
				if (flow.getChildren().isEmpty()) {
					flow.getChildren().add(text(item, false));
				}
				return flow;
			}

			private Text text(String value, boolean highlight) {
				Text text = new Text(value);
				text.getStyleClass().add(highlight ? "replace-blocks-builder-suggestion-highlight" : "replace-blocks-builder-suggestion-normal");
				return text;
			}
		}
	}

	private record ValueResult(String value, ReplaceBlocksDiagnostics.Diagnostic diagnostic) {}

	private record PresetValue(String value, ReplaceBlocksDiagnostics.Diagnostic diagnostic) {}

	private record ParsedPresetRule(Rule rule, ParsedRule parsed) {}

	private record BuilderContentState(List<Rule> rules, BlockInputState from, BlockInputState to) {}

	private record BlockInputState(
			String text,
			SourceTileMode tileMode,
			String minY,
			String maxY,
			String biome,
			List<PropertyState> properties) {}

	private record PropertyState(String name, PropertyChoice choice) {}

	record ParsedRule(ChunkFilter.BlockReplaceSource source, ChunkFilter.BlockReplaceData target) {}

	record Rule(String from, String to) {}

	record VisibleRuleBounds(int index, Rectangle2D bounds) {}

	private static class RuleCell extends TableCell<Rule, String> {

		@Override
		protected void updateItem(String item, boolean empty) {
			super.updateItem(item, empty);
			if (empty || item == null || item.isEmpty()) {
				setText(null);
				setTooltip(null);
				return;
			}
			setText(item);
			setTooltip(new Tooltip(item));
		}
	}

	private static class PresetItemCell extends ListCell<PresetItem> {

		@Override
		protected void updateItem(PresetItem item, boolean empty) {
			super.updateItem(item, empty);
			setText(null);
			if (empty || item == null) {
				setGraphic(null);
				return;
			}
			Text text = new Text(item.toString());
			text.setFill(PROPERTY_CHOICE_TEXT);
			setGraphic(text);
		}
	}

	private record PresetItem(ReplaceBlocksPreset builtin, GlobalConfig.ReplaceBlocksUserPreset userPreset) {

		private static PresetItem builtin(ReplaceBlocksPreset preset) {
			return new PresetItem(preset, null);
		}

		private static PresetItem custom(GlobalConfig.ReplaceBlocksUserPreset preset) {
			return new PresetItem(null, preset);
		}

		private boolean custom() {
			return userPreset != null;
		}

		private String name() {
			return custom() ? userPreset.name() : builtin.toString();
		}

		private String value() {
			return userPreset == null ? null : userPreset.value();
		}

		private String source() {
			return builtin.source();
		}

		private String target() {
			return builtin.target();
		}

		private SourceTileMode tileMode() {
			return builtin.tileMode();
		}

		private Translation warning() {
			return builtin.warning();
		}

		@Override
		public String toString() {
			return name();
		}
	}

	private enum ReplaceBlocksPreset {
		AIR_TO_STONE(
				Translation.DIALOG_REPLACE_BLOCKS_BUILDER_PRESET_AIR_TO_STONE,
				"minecraft:air",
				"minecraft:stone",
				SourceTileMode.ANY,
				Translation.DIALOG_REPLACE_BLOCKS_BUILDER_PRESET_WARNING_AIR),
		FLUIDS_TO_AIR(
				Translation.DIALOG_REPLACE_BLOCKS_BUILDER_PRESET_FLUIDS_TO_AIR,
				"regex(minecraft:(water|lava))",
				"minecraft:air",
				SourceTileMode.ANY,
				null),
		LOGS_LEAVES_TO_AIR(
				Translation.DIALOG_REPLACE_BLOCKS_BUILDER_PRESET_LOGS_LEAVES_TO_AIR,
				"regex(minecraft:.*_(log|wood|stem|hyphae|leaves))",
				"minecraft:air",
				SourceTileMode.ANY,
				null),
		ORES_TO_STONE(
				Translation.DIALOG_REPLACE_BLOCKS_BUILDER_PRESET_ORES_TO_STONE,
				"regex(minecraft:.*_ore)",
				"minecraft:stone",
				SourceTileMode.ANY,
				null),
		CONTAINERS_TO_AIR(
				Translation.DIALOG_REPLACE_BLOCKS_BUILDER_PRESET_CONTAINERS_TO_AIR,
				"regex(minecraft:.*(chest|barrel|furnace|smoker|hopper|dispenser|dropper|shulker_box))",
				"minecraft:air",
				SourceTileMode.REQUIRE,
				Translation.DIALOG_REPLACE_BLOCKS_BUILDER_PRESET_WARNING_CONTAINERS);

		private final Translation label;
		private final String source;
		private final String target;
		private final SourceTileMode tileMode;
		private final Translation warning;

		ReplaceBlocksPreset(Translation label, String source, String target, SourceTileMode tileMode, Translation warning) {
			this.label = label;
			this.source = source;
			this.target = target;
			this.tileMode = tileMode;
			this.warning = warning;
		}

		private String source() {
			return source;
		}

		private String target() {
			return target;
		}

		private SourceTileMode tileMode() {
			return tileMode;
		}

		private Translation warning() {
			return warning;
		}

		@Override
		public String toString() {
			return label.toString();
		}
	}

	private record PropertyChoice(String value, boolean all) {

		private static PropertyChoice allChoice() {
			return new PropertyChoice(null, true);
		}

		private static PropertyChoice value(String value) {
			return new PropertyChoice(value, false);
		}

		@Override
		public String toString() {
			return all ? Translation.DIALOG_REPLACE_BLOCKS_BUILDER_PROPERTY_ALL.toString() : value;
		}
	}

	private static class PropertyChoiceCell extends ListCell<PropertyChoice> {

		@Override
		protected void updateItem(PropertyChoice item, boolean empty) {
			super.updateItem(item, empty);
			setText(null);
			if (empty || item == null) {
				setGraphic(null);
				return;
			}
			Text text = new Text(item.toString());
			text.setFill(PROPERTY_CHOICE_TEXT);
			setGraphic(text);
		}
	}

	private enum SourceTileMode {
		ANY(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_TILE_SOURCE_ANY, null),
		REQUIRE(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_TILE_SOURCE_REQUIRE, "tile"),
		EXCLUDE(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_TILE_SOURCE_EXCLUDE, "no_tile");

		private final Translation label;
		private final String wrapper;

		SourceTileMode(Translation label, String wrapper) {
			this.label = label;
			this.wrapper = wrapper;
		}

		private static SourceTileMode fromTileEntityMode(ChunkFilter.BlockReplaceTileEntityMode mode) {
			return switch (mode) {
				case REQUIRE_TILE_ENTITY -> REQUIRE;
				case EXCLUDE_TILE_ENTITY -> EXCLUDE;
				default -> ANY;
			};
		}

		private String wrap(String value) {
			return wrapper == null ? value : wrapper + "(" + value + ")";
		}

		@Override
		public String toString() {
			return label.toString();
		}
	}
}
