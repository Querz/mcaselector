package net.querz.mcaselector.ui.dialog;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.geometry.Bounds;
import javafx.geometry.BoundingBox;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.TextFlow;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import net.querz.mcaselector.config.GlobalConfig;
import net.querz.mcaselector.text.Translation;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.mapping.blockstate.BlockStateCatalog;
import net.querz.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@SuppressWarnings("unchecked")
class ReplaceBlocksRuleBuilderModelTest {

	private static final Object javaFxStartupLock = new Object();
	private static final double POPUP_ATTACHMENT_TOLERANCE = 2.0;
	private static boolean javaFxStarted;

	@Test
	void restoresLiteralSourceAndNamedTarget() {
		ReplaceBlocksRuleBuilderDialog.ParsedRule parsed = ReplaceBlocksRuleBuilderDialog.parseEditableRule(
				"literal(minecraft:acacia_stairs)",
				"minecraft:andesite_stairs");

		assertNotNull(parsed);
		assertEquals(ChunkFilter.BlockReplaceSourceType.LITERAL_NAME, parsed.source().getType());
		assertEquals("minecraft:acacia_stairs", parsed.source().getName());
		assertEquals(ChunkFilter.BlockReplaceType.NAME, parsed.target().getType());
		assertEquals("minecraft:andesite_stairs", parsed.target().getName());
	}

	@Test
	void restoresSelectedPropertiesAndSourceConditions() {
		ReplaceBlocksRuleBuilderDialog.ParsedRule parsed = ReplaceBlocksRuleBuilderDialog.parseEditableRule(
				"biome(minecraft:forest, y(-16..31, tile(props({Name:\"minecraft:acacia_stairs\",Properties:{facing:\"north\",half:\"top\"}}))))",
				"{Name:\"minecraft:andesite_stairs\",Properties:{facing:\"south\",half:\"bottom\"}}");

		assertNotNull(parsed);
		assertEquals(ChunkFilter.BlockReplaceSourceType.SELECTED_PROPERTIES, parsed.source().getType());
		assertEquals(ChunkFilter.BlockReplaceTileEntityMode.REQUIRE_TILE_ENTITY, parsed.source().getTileEntityMode());
		assertEquals(-16, parsed.source().getMinY());
		assertEquals(31, parsed.source().getMaxY());
		assertEquals(Map.of("facing", "north", "half", "top"),
				ReplaceBlocksRuleBuilderDialog.editableStateProperties(parsed.source().getState()));
		assertEquals(Map.of("facing", "south", "half", "bottom"),
				ReplaceBlocksRuleBuilderDialog.editableStateProperties(parsed.target().getState()));
		assertEquals(java.util.Set.of("minecraft:forest"), parsed.source().getBiomes());
	}

	@Test
	void keepsAdvancedStateAndTileTargetsAsFallbackData() {
		ReplaceBlocksRuleBuilderDialog.ParsedRule parsed = ReplaceBlocksRuleBuilderDialog.parseEditableRule(
				"{Name:\"minecraft:chest\",Properties:{facing:\"north\"},Custom:1b}",
				"minecraft:barrel;{id:\"minecraft:barrel\"}");

		assertNotNull(parsed);
		assertEquals(ChunkFilter.BlockReplaceSourceType.EXACT_STATE, parsed.source().getType());
		assertNull(ReplaceBlocksRuleBuilderDialog.editableStateProperties(parsed.source().getState()));
		assertEquals(ChunkFilter.BlockReplaceType.NAME_TILE, parsed.target().getType());
		assertNotNull(parsed.target().getTile());
	}

	@Test
	void wrapsPlainTableSourcesAsLiteralWhenLoadingForEdit() {
		ReplaceBlocksRuleBuilderDialog.ParsedRule parsed = ReplaceBlocksRuleBuilderDialog.parseEditableRule(
				"minecraft:stone",
				"minecraft:dirt");

		assertNotNull(parsed);
		assertEquals(ChunkFilter.BlockReplaceSourceType.LITERAL_NAME, parsed.source().getType());
		assertEquals("minecraft:stone", parsed.source().getName());
	}

	@Test
	void rejectsMalformedRules() {
		assertNull(ReplaceBlocksRuleBuilderDialog.parseEditableRule("literal(", "minecraft:stone"));
	}

	@Test
	void parsesEveryRuleFromMultiRulePresetWithoutLosingState() {
		List<ReplaceBlocksRuleBuilderDialog.Rule> rules = ReplaceBlocksRuleBuilderDialog.parseSimpleRules(
				"props({Name:\"minecraft:acacia_stairs\",Properties:{facing:\"north\"}})="
						+ "{Name:\"minecraft:andesite_stairs\",Properties:{facing:\"south\"}}, "
						+ "biome(minecraft:plains, literal(minecraft:stone))=minecraft:dirt");

		assertEquals(2, rules.size());
		ReplaceBlocksRuleBuilderDialog.ParsedRule stateRule = ReplaceBlocksRuleBuilderDialog.parseEditableRule(
				rules.get(0).from(), rules.get(0).to());
		assertNotNull(stateRule);
		assertEquals(Map.of("facing", "north"),
				ReplaceBlocksRuleBuilderDialog.editableStateProperties(stateRule.source().getState()));
		assertEquals(Map.of("facing", "south"),
				ReplaceBlocksRuleBuilderDialog.editableStateProperties(stateRule.target().getState()));
	}

	@Test
	void duplicatePresetSourcesKeepOnlyTheLastTarget() {
		String value = "literal(stone)=minecraft:dirt, literal(minecraft:stone)=minecraft:gold_block";

		assertEquals("literal(minecraft:stone)=minecraft:gold_block",
				ReplaceBlocksRuleBuilderDialog.normalizeRulesValue(value));
		List<ReplaceBlocksRuleBuilderDialog.Rule> rules = ReplaceBlocksRuleBuilderDialog.parseSimpleRules(value);
		assertEquals(1, rules.size());
		assertEquals("literal(minecraft:stone)", rules.get(0).from());
		assertEquals("minecraft:gold_block", rules.get(0).to());
	}

	@Test
	void customPresetAppendsUnknownExactTargetAndShowsTargetWarning() throws Throwable {
		runOnJavaFxThread(() -> {
			Stage primaryStage = showPrimaryStage();
			ReplaceBlocksRuleBuilderDialog dialog = showDialog(primaryStage, "");
			try {
				String unknownTarget = "minecraft:task2_missing_target";

				applyCustomPreset(dialog, "literal(minecraft:stone)=" + unknownTarget);

				assertEquals(1, fieldValue(dialog, "ruleItems", List.class).size());
				assertTrue(fieldValue(dialog, "result", TextArea.class).getText().contains(unknownTarget));
				BlockStateCatalog catalog = fieldValue(dialog, "catalog", BlockStateCatalog.class);
				assertEquals(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_CATALOG_TARGET_UNKNOWN
						.format(unknownTarget, catalog.version()),
						fieldValue(dialog, "validation", Label.class).getText());
			} finally {
				closeDialog(dialog, primaryStage);
			}
		});
	}

	@Test
	void customPresetWarnsForEachDeterminableExactSourceWhenTargetsAreKnown() throws Throwable {
		runOnJavaFxThread(() -> {
			Stage primaryStage = showPrimaryStage();
			ReplaceBlocksRuleBuilderDialog dialog = showDialog(primaryStage, "");
			try {
				List<String> unknownSources = List.of(
						"minecraft:task2_missing_literal",
						"minecraft:task2_missing_properties",
						"minecraft:task2_missing_state");
				List<String> presetValues = List.of(
						"literal(" + unknownSources.get(0) + ")=minecraft:stone",
						"props({Name:\"" + unknownSources.get(1) + "\",Properties:{axis:\"x\"}})=minecraft:stone",
						"{Name:\"" + unknownSources.get(2) + "\",Properties:{axis:\"x\"}}=minecraft:stone");
				BlockStateCatalog catalog = fieldValue(dialog, "catalog", BlockStateCatalog.class);

				for (int i = 0; i < presetValues.size(); i++) {
					applyCustomPreset(dialog, presetValues.get(i));

					assertEquals(i + 1, fieldValue(dialog, "ruleItems", List.class).size());
					assertEquals(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_CATALOG_SOURCE_UNKNOWN
							.format(unknownSources.get(i), catalog.version()),
							fieldValue(dialog, "validation", Label.class).getText());
				}
			} finally {
				closeDialog(dialog, primaryStage);
			}
		});
	}

	@Test
	void customPresetDoesNotGuessUnknownSourceFromRegex() throws Throwable {
		runOnJavaFxThread(() -> {
			Stage primaryStage = showPrimaryStage();
			ReplaceBlocksRuleBuilderDialog dialog = showDialog(primaryStage, "");
			try {
				String unknownLookingRegex = "minecraft:task2_missing_.*";

				applyCustomPreset(dialog, "regex(" + unknownLookingRegex + ")=minecraft:stone");

				assertEquals(1, fieldValue(dialog, "ruleItems", List.class).size());
				TextArea result = fieldValue(dialog, "result", TextArea.class);
				assertEquals(ReplaceBlocksDiagnostics.diagnoseValue(result.getText(), true).message(),
						fieldValue(dialog, "validation", Label.class).getText());
			} finally {
				closeDialog(dialog, primaryStage);
			}
		});
	}

	@Test
	void customPresetDoesNotTreatBareLegacyRegexAsExactSource() throws Throwable {
		runOnJavaFxThread(() -> {
			Stage primaryStage = showPrimaryStage();
			ReplaceBlocksRuleBuilderDialog dialog = showDialog(primaryStage, "");
			try {
				applyCustomPreset(dialog, "minecraft:task2_missing_.*=minecraft:stone");

				assertEquals(1, fieldValue(dialog, "ruleItems", List.class).size());
				TextArea result = fieldValue(dialog, "result", TextArea.class);
				assertEquals(ReplaceBlocksDiagnostics.diagnoseValue(result.getText(), true).message(),
						fieldValue(dialog, "validation", Label.class).getText());
			} finally {
				closeDialog(dialog, primaryStage);
			}
		});
	}

	@Test
	void customPresetPreservesValidLookingBareLegacyRegexSemantics() throws Throwable {
		runOnJavaFxThread(() -> {
			Stage primaryStage = showPrimaryStage();
			ReplaceBlocksRuleBuilderDialog dialog = showDialog(primaryStage, "");
			try {
				applyCustomPreset(dialog, "minecraft:task2_missing_block=minecraft:stone");

				assertEquals(1, fieldValue(dialog, "ruleItems", List.class).size());
				TextArea result = fieldValue(dialog, "result", TextArea.class);
				assertEquals(ReplaceBlocksDiagnostics.diagnoseValue(result.getText(), true).message(),
						fieldValue(dialog, "validation", Label.class).getText());
			} finally {
				closeDialog(dialog, primaryStage);
			}
		});
	}

	@Test
	void customPresetDoesNotTreatQuotedLegacyRegexAsExactSource() throws Throwable {
		runOnJavaFxThread(() -> {
			Stage primaryStage = showPrimaryStage();
			ReplaceBlocksRuleBuilderDialog dialog = showDialog(primaryStage, "");
			try {
				applyCustomPreset(dialog, "'task2:missing_.*'=minecraft:stone");

				assertEquals(1, fieldValue(dialog, "ruleItems", List.class).size());
				TextArea result = fieldValue(dialog, "result", TextArea.class);
				assertEquals(ReplaceBlocksDiagnostics.diagnoseValue(result.getText(), true).message(),
						fieldValue(dialog, "validation", Label.class).getText());
			} finally {
				closeDialog(dialog, primaryStage);
			}
		});
	}

	@Test
	void customPresetTargetWarningWinsOverEarlierSourceWarning() throws Throwable {
		runOnJavaFxThread(() -> {
			Stage primaryStage = showPrimaryStage();
			ReplaceBlocksRuleBuilderDialog dialog = showDialog(primaryStage, "");
			try {
				String unknownSource = "minecraft:task2_missing_source_first";
				String unknownTarget = "minecraft:task2_missing_target_second";

				applyCustomPreset(dialog,
						"literal(" + unknownSource + ")=minecraft:stone, "
								+ "literal(minecraft:dirt)=" + unknownTarget);

				assertEquals(2, fieldValue(dialog, "ruleItems", List.class).size());
				BlockStateCatalog catalog = fieldValue(dialog, "catalog", BlockStateCatalog.class);
				assertEquals(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_CATALOG_TARGET_UNKNOWN
						.format(unknownTarget, catalog.version()),
						fieldValue(dialog, "validation", Label.class).getText());
			} finally {
				closeDialog(dialog, primaryStage);
			}
		});
	}

	@Test
	void customPresetDoesNotWarnForDuplicateUnknownRuleThatWasSkipped() throws Throwable {
		runOnJavaFxThread(() -> {
			String duplicateSource = "minecraft:task2_duplicate_unknown_source";
			Stage primaryStage = showPrimaryStage();
			ReplaceBlocksRuleBuilderDialog dialog = showDialog(primaryStage,
					"literal(" + duplicateSource + ")=minecraft:stone");
			try {
				String skippedUnknownTarget = "minecraft:task2_skipped_unknown_target";

				applyCustomPreset(dialog,
						"literal(" + duplicateSource + ")=" + skippedUnknownTarget + ", "
								+ "literal(minecraft:dirt)=minecraft:stone");

				assertEquals(2, fieldValue(dialog, "ruleItems", List.class).size());
				TextArea result = fieldValue(dialog, "result", TextArea.class);
				assertFalse(result.getText().contains(skippedUnknownTarget));
				assertEquals(ReplaceBlocksDiagnostics.diagnoseValue(result.getText(), true).message(),
						fieldValue(dialog, "validation", Label.class).getText());
			} finally {
				closeDialog(dialog, primaryStage);
			}
		});
	}

	@Test
	void biomeCompletionUsesTokenAtCaret() {
		String value = "minecraft:birch_forest;minecraft:pla";

		assertEquals("minecraft:pla", ReplaceBlocksRuleBuilderDialog.biomeToken(value, value.length()));
		assertEquals("minecraft:birch_forest", ReplaceBlocksRuleBuilderDialog.biomeToken(value, 8));
		assertEquals("", ReplaceBlocksRuleBuilderDialog.biomeToken("minecraft:plains;", 17));
	}

	@Test
	void normalizesEquivalentStateRulesForDuplicateDetection() {
		String first = ReplaceBlocksRuleBuilderDialog.normalizeRule(
				"tile(props({Name:\"minecraft:acacia_log\",Properties:{axis:\"y\"}}))",
				"{Name:\"minecraft:acacia_leaves\",Properties:{waterlogged:\"true\",distance:\"2\",persistent:\"false\"}}");
		String reordered = ReplaceBlocksRuleBuilderDialog.normalizeRule(
				"tile( props( { Properties: { axis: \"y\" }, Name: \"minecraft:acacia_log\" } ) )",
				"{ Properties: { persistent: \"false\", distance: \"2\", waterlogged: \"true\" }, Name: \"minecraft:acacia_leaves\" }");

		assertEquals(first, reordered);
	}

	@Test
	void formatsOnlyTheSelectedRulesForPresetSaving() {
		List<ReplaceBlocksRuleBuilderDialog.Rule> selected = List.of(
				new ReplaceBlocksRuleBuilderDialog.Rule("minecraft:stone", "minecraft:dirt"),
				new ReplaceBlocksRuleBuilderDialog.Rule("minecraft:oak_log", "minecraft:air"));

		assertEquals("literal(minecraft:stone)=minecraft:dirt, literal(minecraft:oak_log)=minecraft:air",
				ReplaceBlocksRuleBuilderDialog.formatRulesValue(selected));
	}

	@Test
	void returnsEveryVisibleRuleIntersectingTheMarquee() {
		List<ReplaceBlocksRuleBuilderDialog.VisibleRuleBounds> rows = List.of(
				new ReplaceBlocksRuleBuilderDialog.VisibleRuleBounds(0, new Rectangle2D(0, 0, 600, 24)),
				new ReplaceBlocksRuleBuilderDialog.VisibleRuleBounds(1, new Rectangle2D(0, 24, 600, 24)),
				new ReplaceBlocksRuleBuilderDialog.VisibleRuleBounds(2, new Rectangle2D(0, 48, 600, 24)));

		assertEquals(List.of(0, 1), ReplaceBlocksRuleBuilderDialog.intersectingRuleIndices(
				new Rectangle2D(16, 8, 120, 38), rows));
	}

	@Test
	void recognizesControlEnterForAddingRules() {
		assertTrue(ReplaceBlocksRuleBuilderDialog.isBuilderAddShortcut(KeyCode.ENTER, true));
		assertTrue(ReplaceBlocksRuleBuilderDialog.isBuilderAddShortcut(KeyCode.SEPARATOR, true));
		assertFalse(ReplaceBlocksRuleBuilderDialog.isBuilderAddShortcut(KeyCode.ENTER, false));
		assertFalse(ReplaceBlocksRuleBuilderDialog.isBuilderAddShortcut(KeyCode.DOWN, true));
	}

	@Test
	void permitsMarqueeFromRulesAndBlankTableAreas() {
		assertTrue(ReplaceBlocksRuleBuilderDialog.canStartRuleMarquee(true, false));
		assertFalse(ReplaceBlocksRuleBuilderDialog.canStartRuleMarquee(true, true));
		assertFalse(ReplaceBlocksRuleBuilderDialog.canStartRuleMarquee(false, false));
	}

	@Test
	void movesAutocompleteHighlightWithoutChangingTheQuery() {
		assertEquals(0, ReplaceBlocksRuleBuilderDialog.popupSelectionTarget(KeyCode.DOWN, -1, 20, 12));
		assertEquals(11, ReplaceBlocksRuleBuilderDialog.popupSelectionTarget(KeyCode.PAGE_DOWN, 0, 20, 12));
		assertEquals(0, ReplaceBlocksRuleBuilderDialog.popupSelectionTarget(KeyCode.PAGE_UP, 11, 20, 12));
		assertEquals(-1, ReplaceBlocksRuleBuilderDialog.popupSelectionTarget(KeyCode.ENTER, 0, 20, 12));
	}

	@Test
	void revealsAutocompleteHighlightOnlyOutsideTheVisibleRange() {
		assertFalse(ReplaceBlocksRuleBuilderDialog.popupNeedsReveal(0, 0, 4));
		assertFalse(ReplaceBlocksRuleBuilderDialog.popupNeedsReveal(3, 0, 4));
		assertFalse(ReplaceBlocksRuleBuilderDialog.popupNeedsReveal(4, 0, 4));
		assertTrue(ReplaceBlocksRuleBuilderDialog.popupNeedsReveal(5, 0, 4));
		assertTrue(ReplaceBlocksRuleBuilderDialog.popupNeedsReveal(4, 5, 9));
	}

	@Test
	void pageNavigationUsesTheActuallyVisiblePopupRows() {
		assertEquals(5, ReplaceBlocksRuleBuilderDialog.popupPageSize(3, 7, 12));
		assertEquals(12, ReplaceBlocksRuleBuilderDialog.popupPageSize(3, 2, 12));
		assertEquals(1, ReplaceBlocksRuleBuilderDialog.popupPageSize(3, 2, 0));
	}

	@Test
	void treatsPartiallyClippedPopupRowsAsOutsideTheVisibleRange() {
		BoundingBox viewport = new BoundingBox(0, 0, 320, 120);

		assertTrue(ReplaceBlocksRuleBuilderDialog.isPopupCellFullyVisible(
				new BoundingBox(0, 0, 300, 24), viewport));
		assertFalse(ReplaceBlocksRuleBuilderDialog.isPopupCellFullyVisible(
				new BoundingBox(0, -1, 300, 24), viewport));
		assertFalse(ReplaceBlocksRuleBuilderDialog.isPopupCellFullyVisible(
				new BoundingBox(0, 100, 300, 24), viewport));
	}

	@Test
	void clearedAutocompleteIndexDoesNotLeaveAHighlightedCell() {
		assertTrue(ReplaceBlocksRuleBuilderDialog.isAutocompleteCellHighlighted(2, 2, false));
		assertFalse(ReplaceBlocksRuleBuilderDialog.isAutocompleteCellHighlighted(2, -1, false));
		assertFalse(ReplaceBlocksRuleBuilderDialog.isAutocompleteCellHighlighted(2, 2, true));
	}

	@Test
	void emptySuggestionsRequireExplicitExpansion() {
		List<String> names = List.of("minecraft:acacia", "minecraft:stone");

		assertEquals(List.of(), ReplaceBlocksRuleBuilderDialog.suggestionsForQuery(names, "", false));
		assertEquals(names, ReplaceBlocksRuleBuilderDialog.suggestionsForQuery(names, "", true));
		assertEquals(List.of("minecraft:acacia"),
				ReplaceBlocksRuleBuilderDialog.suggestionsForQuery(names, "aca", false));
	}

	@Test
	void clearsAnExplicitEmptyCatalogAfterItsPopupCloses() throws Throwable {
		runOnJavaFxThread(() -> {
			ComboBox<String> comboBox = new ComboBox<>(FXCollections.observableArrayList(
					"minecraft:acacia", "minecraft:stone"));
			comboBox.setEditable(true);
			comboBox.getSelectionModel().select(0);
			comboBox.getEditor().clear();

			ReplaceBlocksRuleBuilderDialog.clearEmptyExplicitCatalog(comboBox);

			assertTrue(comboBox.getItems().isEmpty());
			assertEquals(-1, comboBox.getSelectionModel().getSelectedIndex());
			assertNull(comboBox.getValue());
			assertEquals("", comboBox.getEditor().getText());
			comboBox.getSelectionModel().selectNext();
			assertEquals(-1, comboBox.getSelectionModel().getSelectedIndex());
		});
	}

	@Test
	void clearsOnlyTheCatalogFromTheMatchingExplicitClose() {
		assertTrue(ReplaceBlocksRuleBuilderDialog.shouldClearExplicitCatalog(true, false, ""));
		assertFalse(ReplaceBlocksRuleBuilderDialog.shouldClearExplicitCatalog(false, false, ""));
		assertFalse(ReplaceBlocksRuleBuilderDialog.shouldClearExplicitCatalog(true, true, ""));
		assertFalse(ReplaceBlocksRuleBuilderDialog.shouldClearExplicitCatalog(true, false, "minecraft:stone"));
	}

	@Test
	void recognizesOnlyTheComboBoxArrowAsExplicitExpansion() {
		HBox arrowButton = new HBox();
		arrowButton.getStyleClass().add("arrow-button");
		StackPane arrow = new StackPane();
		arrowButton.getChildren().add(arrow);

		assertTrue(ReplaceBlocksRuleBuilderDialog.isComboBoxArrowTarget(arrow));
		assertFalse(ReplaceBlocksRuleBuilderDialog.isComboBoxArrowTarget(new StackPane()));
	}

	@Test
	void popupHighlightDoesNotUseEditableComboBoxFocusOrSelection() throws Throwable {
		runOnJavaFxThread(() -> {
			ComboBox<String> comboBox = new ComboBox<>(FXCollections.observableArrayList(
					"minecraft:acacia_button",
					"minecraft:acacia_door",
					"minecraft:acacia_fence"));
			comboBox.setEditable(true);
			comboBox.setSkin(new ComboBoxListViewSkin<>(comboBox));
			StackPane root = new StackPane(comboBox);
			new Scene(root);
			root.applyCss();
			comboBox.getEditor().setText("aca");

			assertTrue(ReplaceBlocksRuleBuilderDialog.focusPopupSuggestion(comboBox, 1));
			assertEquals("aca", comboBox.getEditor().getText());
			assertNull(comboBox.getValue());
			assertEquals(-1, comboBox.getSelectionModel().getSelectedIndex());
			ListView<?> popup = (ListView<?>) ((ComboBoxListViewSkin<?>) comboBox.getSkin()).getPopupContent();
			assertEquals(-1, popup.getFocusModel().getFocusedIndex());
		});
	}

	@Test
	void movingAutocompleteHighlightClearsNativePopupSelection() throws Throwable {
		runOnJavaFxThread(() -> {
			ComboBox<String> comboBox = new ComboBox<>(FXCollections.observableArrayList(
					"minecraft:acacia_button",
					"minecraft:acacia_door",
					"minecraft:acacia_fence"));
			comboBox.setEditable(true);
			comboBox.setSkin(new ComboBoxListViewSkin<>(comboBox));
			StackPane root = new StackPane(comboBox);
			new Scene(root);
			root.applyCss();

			ListView<?> popup = (ListView<?>) ((ComboBoxListViewSkin<?>) comboBox.getSkin()).getPopupContent();
			popup.getSelectionModel().select(0);
			popup.getFocusModel().focus(0);

			assertTrue(ReplaceBlocksRuleBuilderDialog.focusPopupSuggestion(comboBox, 2));
			assertEquals(-1, popup.getSelectionModel().getSelectedIndex());
			assertEquals(-1, popup.getFocusModel().getFocusedIndex());
		});
	}

	@Test
	void builderComboBoxAddsBuilderOnlyPopupStyle() throws Throwable {
		runOnJavaFxThread(() -> {
			ComboBox<String> comboBox = new ComboBox<>(FXCollections.observableArrayList("one", "two"));
			ReplaceBlocksRuleBuilderDialog.configureBuilderComboBox(comboBox);
			comboBox.setSkin(new ComboBoxListViewSkin<>(comboBox));

			ListView<?> popup = (ListView<?>) ((ComboBoxListViewSkin<?>) comboBox.getSkin()).getPopupContent();
			assertTrue(popup.getStyleClass().contains("replace-blocks-builder-dropdown"));

			comboBox.setSkin(new ComboBoxListViewSkin<>(comboBox));
			ListView<?> replacementPopup = (ListView<?>) ((ComboBoxListViewSkin<?>) comboBox.getSkin()).getPopupContent();
			assertTrue(replacementPopup.getStyleClass().contains("replace-blocks-builder-dropdown"));
		});
	}

	@Test
	void autocompleteComboBoxesLimitPopupWidthMeasurement() throws Throwable {
		runOnJavaFxThread(() -> {
			Stage primaryStage = new Stage();
			primaryStage.setScene(new Scene(new StackPane()));
			try {
				ReplaceBlocksRuleBuilderDialog dialog = new ReplaceBlocksRuleBuilderDialog(primaryStage, "");
				Object from = fieldValue(dialog, "from", Object.class);
				Object to = fieldValue(dialog, "to", Object.class);

				assertEquals(32, fieldValue(from, "block", ComboBox.class).getProperties()
						.get("comboBoxRowsToMeasureWidth"));
				assertEquals(32, fieldValue(to, "block", ComboBox.class).getProperties()
						.get("comboBoxRowsToMeasureWidth"));
				assertEquals(32, fieldValue(from, "biomeNames", ComboBox.class).getProperties()
						.get("comboBoxRowsToMeasureWidth"));
				assertNull(fieldValue(dialog, "presets", ComboBox.class).getProperties()
						.get("comboBoxRowsToMeasureWidth"));
			} finally {
				primaryStage.close();
			}
		});
	}

	@Test
	void emptyAutocompleteCatalogCellsUsePlainText() throws Throwable {
		runOnJavaFxThread(() -> {
			Stage primaryStage = new Stage();
			primaryStage.setScene(new Scene(new StackPane()));
			try {
				ReplaceBlocksRuleBuilderDialog dialog = new ReplaceBlocksRuleBuilderDialog(primaryStage, "");
				Object from = fieldValue(dialog, "from", Object.class);
				assertEmptyQueryCellUsesPlainText(fieldValue(from, "block", ComboBox.class),
						"minecraft:acacia_button");
				assertEmptyQueryCellUsesPlainText(fieldValue(from, "biomeNames", ComboBox.class),
						"minecraft:badlands");
			} finally {
				primaryStage.close();
			}
		});
	}

	private static void assertEmptyQueryCellUsesPlainText(ComboBox<String> comboBox, String item)
			throws Exception {
		ListCell<String> cell = comboBox.getCellFactory().call(new ListView<>());
		Method updateItem = declaredMethod(cell, "updateItem", Object.class, boolean.class);

		comboBox.getEditor().clear();
		updateItem.invoke(cell, item, false);
		assertEquals(item, cell.getText());
		assertNull(cell.getGraphic());

		comboBox.getEditor().setText("a");
		updateItem.invoke(cell, item, false);
		assertNull(cell.getText());
		assertInstanceOf(TextFlow.class, cell.getGraphic());
	}

	@Test
	void catalogControlExplainsThatItDoesNotMigrateIds() throws Throwable {
		runOnJavaFxThread(() -> {
			Translation.load(Locale.UK);
			Stage primaryStage = new Stage();
			primaryStage.setScene(new Scene(new StackPane()));
			try {
				ReplaceBlocksRuleBuilderDialog dialog = new ReplaceBlocksRuleBuilderDialog(primaryStage, "");
				Label note = (Label) dialog.getDialogPane().lookup(".replace-blocks-builder-catalog-note");

				assertNotNull(note);
				assertTrue(note.isWrapText());
				assertTrue(note.getText().contains("IDs are not converted"));
				assertTrue(note.getText().contains("If the Builder has content"));
				assertTrue(note.getText().contains("switching asks for confirmation"));
			} finally {
				primaryStage.close();
			}
		});
	}

	@Test
	void changeDialogPreloadsBlockCatalogOnADaemonThread() throws Exception {
		Thread loader = ChangeNBTDialog.BlockCatalogPreloader.preload();

		assertTrue(loader.isDaemon());
		assertEquals("replace-blocks-catalog-preload", loader.getName());
		loader.join(TimeUnit.SECONDS.toMillis(10));
		assertFalse(loader.isAlive());
		assertFalse(BlockStateCatalog.available().isEmpty());
	}

	@Test
	void restoredBuilderContentIsACleanBaselineUntilTheDraftChanges() throws Throwable {
		runOnJavaFxThread(() -> {
			Stage primaryStage = showPrimaryStage();
			ReplaceBlocksRuleBuilderDialog dialog = showDialog(primaryStage,
					"literal(minecraft:stone)=minecraft:dirt");
			try {
				assertFalse(invokeBoolean(dialog, "isBuilderDirty"));

				Object from = fieldValue(dialog, "from", Object.class);
				ComboBox<String> block = fieldValue(from, "block", ComboBox.class);
				block.getEditor().setText("minecraft:acacia_log");

				assertTrue(invokeBoolean(dialog, "isBuilderDirty"));

				block.getEditor().clear();

				assertFalse(invokeBoolean(dialog, "isBuilderDirty"),
						"reverting the draft to the opening state should make the session clean");
			} finally {
				closeDialog(dialog, primaryStage);
			}
		});
	}

	@Test
	void resettingRestoredRulesRemainsDirtyAfterTheBuilderBecomesEmpty() throws Throwable {
		runOnJavaFxThread(() -> {
			Stage primaryStage = showPrimaryStage();
			ReplaceBlocksRuleBuilderDialog dialog = showDialog(primaryStage,
					"literal(minecraft:stone)=minecraft:dirt");
			try {
				assertFalse(invokeBoolean(dialog, "isBuilderDirty"));

				declaredMethod(dialog, "resetBuilder").invoke(dialog);

				assertTrue(fieldValue(dialog, "ruleItems", List.class).isEmpty());
				assertTrue(invokeBoolean(dialog, "isBuilderDirty"),
						"clearing restored rules must not erase the session's dirty state");
			} finally {
				closeDialog(dialog, primaryStage);
			}
		});
	}

	@Test
	void unchangedRestoredBuilderClosesWithoutDiscardConfirmation() throws Throwable {
		runOnJavaFxThread(() -> {
			Stage primaryStage = showPrimaryStage();
			ReplaceBlocksRuleBuilderDialog dialog = showDialog(primaryStage,
					"literal(minecraft:stone)=minecraft:dirt");
			try {
				AtomicInteger confirmations = autoRespondToDiscardConfirmation(dialog, false);

				fireWindowCloseRequest(dialog);

				assertEquals(0, confirmations.get());
				assertFalse(dialog.isShowing());
			} finally {
				closeDialog(dialog, primaryStage);
			}
		});
	}

	@Test
	void titleBarCloseStaysOpenWhenDiscardIsDeclined() throws Throwable {
		runOnJavaFxThread(() -> {
			Stage primaryStage = showPrimaryStage();
			ReplaceBlocksRuleBuilderDialog dialog = showDialog(primaryStage, "");
			try {
				setFromText(dialog, "minecraft:stone");
				AtomicInteger confirmations = autoRespondToDiscardConfirmation(dialog, false);

				WindowEvent closeRequest = fireWindowCloseRequest(dialog);

				assertEquals(1, confirmations.get());
				assertTrue(closeRequest.isConsumed());
				assertTrue(dialog.isShowing());
			} finally {
				closeDialog(dialog, primaryStage);
			}
		});
	}

	@Test
	void titleBarCloseStaysOpenWhenDiscardConfirmationIsClosed() throws Throwable {
		runOnJavaFxThread(() -> {
			Stage primaryStage = showPrimaryStage();
			ReplaceBlocksRuleBuilderDialog dialog = showDialog(primaryStage, "");
			try {
				setFromText(dialog, "minecraft:stone");
				AtomicInteger confirmations = autoCloseConfirmation(dialog);

				WindowEvent closeRequest = fireWindowCloseRequest(dialog);

				assertEquals(1, confirmations.get());
				assertTrue(closeRequest.isConsumed());
				assertTrue(dialog.isShowing());
			} finally {
				closeDialog(dialog, primaryStage);
			}
		});
	}

	@Test
	void titleBarCloseDiscardsDirtyBuilderWhenConfirmed() throws Throwable {
		runOnJavaFxThread(() -> {
			Stage primaryStage = showPrimaryStage();
			ReplaceBlocksRuleBuilderDialog dialog = showDialog(primaryStage, "");
			try {
				setFromText(dialog, "minecraft:stone");
				AtomicInteger confirmations = autoRespondToDiscardConfirmation(dialog, true);

				fireWindowCloseRequest(dialog);

				assertEquals(1, confirmations.get());
				assertFalse(dialog.isShowing());
			} finally {
				closeDialog(dialog, primaryStage);
			}
		});
	}

	@Test
	void builderCancelStaysOpenWhenDiscardIsDeclined() throws Throwable {
		runOnJavaFxThread(() -> {
			Stage primaryStage = showPrimaryStage();
			ReplaceBlocksRuleBuilderDialog dialog = showDialog(primaryStage, "");
			try {
				setFromText(dialog, "minecraft:stone");
				AtomicInteger confirmations = autoRespondToDiscardConfirmation(dialog, false);

				((Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL)).fire();

				assertEquals(1, confirmations.get());
				assertTrue(dialog.isShowing());
			} finally {
				closeDialog(dialog, primaryStage);
			}
		});
	}

	@Test
	void discardConfirmationExplainsTheTransactionAndUsesExplicitActions() throws Throwable {
		runOnJavaFxThread(() -> {
			Translation.load(Locale.UK);
			Stage primaryStage = showPrimaryStage();
			ReplaceBlocksRuleBuilderDialog dialog = showDialog(primaryStage, "");
			try {
				setFromText(dialog, "minecraft:stone");
				AtomicReference<Throwable> alertStateFailure = new AtomicReference<>();
				AtomicInteger confirmations = autoRespondToDiscardConfirmation(dialog, false,
						() -> assertTransactionalDiscardConfirmation(dialog), alertStateFailure);

				WindowEvent closeRequest = fireWindowCloseRequest(dialog);

				assertNull(alertStateFailure.get());
				assertEquals(1, confirmations.get());
				assertTrue(closeRequest.isConsumed());
				assertTrue(dialog.isShowing());
			} finally {
				closeDialog(dialog, primaryStage);
			}
		});
	}

	@Test
	void selectingTheActiveCatalogIsANoop() throws Throwable {
		runOnJavaFxThread(() -> {
			Stage primaryStage = showPrimaryStage();
			ReplaceBlocksRuleBuilderDialog dialog = showDialog(primaryStage, "");
			try {
				ComboBox<BlockStateCatalog> selector = catalogSelector(dialog);
				ComboBox<?> presets = fieldValue(dialog, "presets", ComboBox.class);
				presets.getSelectionModel().selectFirst();
				Object selectedPreset = presets.getValue();
				AtomicInteger confirmations = autoRespondToConfirmation(dialog, true);

				selector.setValue(selector.getValue());

				assertEquals(0, confirmations.get());
				assertSame(selectedPreset, presets.getValue());
			} finally {
				closeDialog(dialog, primaryStage);
			}
		});
	}

	@Test
	void emptyBuilderSwitchesWithoutConfirmationAndClearsTransientPresetState() throws Throwable {
		runOnJavaFxThread(() -> {
			Stage primaryStage = showPrimaryStage();
			ReplaceBlocksRuleBuilderDialog dialog = showDialog(primaryStage, "");
			try {
				ComboBox<BlockStateCatalog> selector = catalogSelector(dialog);
				BlockStateCatalog requested = differentCatalog(selector);
				ComboBox<?> presets = fieldValue(dialog, "presets", ComboBox.class);
				presets.getSelectionModel().selectFirst();
				presets.show();
				assertTrue(presets.isShowing());
				AtomicInteger confirmations = autoRespondToConfirmation(dialog, true);

				selector.setValue(requested);

				assertEquals(0, confirmations.get());
				assertSame(requested, fieldValue(dialog, "catalog", BlockStateCatalog.class));
				assertNull(presets.getValue());
				assertFalse(presets.isShowing());
			} finally {
				closeDialog(dialog, primaryStage);
			}
		});
	}

	@Test
	void cancellingNonEmptyCatalogSwitchPreservesEveryBuilderStateWithoutReentry() throws Throwable {
		runOnJavaFxThread(() -> {
			Stage primaryStage = showPrimaryStage();
			ReplaceBlocksRuleBuilderDialog dialog = showDialog(primaryStage,
					"literal(minecraft:stone)=minecraft:dirt");
			try {
				FullBuilderState state = populateFullBuilderState(dialog);
				ComboBox<BlockStateCatalog> selector = catalogSelector(dialog);
				BlockStateCatalog original = selector.getValue();
				BlockStateCatalog requested = differentCatalog(selector);
				AtomicReference<Throwable> alertStateFailure = new AtomicReference<>();
				AtomicInteger confirmations = autoRespondToConfirmation(dialog, false,
						() -> {
							assertCatalogState(dialog, selector, original);
							assertFullBuilderState(dialog, state);
						}, alertStateFailure);

				selector.setValue(requested);

				assertNull(alertStateFailure.get(), "selector changed before cancellation confirmation");
				assertEquals(1, confirmations.get(), "selector rollback must not request confirmation again");
				assertSame(original, selector.getValue());
				assertSame(original, fieldValue(dialog, "catalog", BlockStateCatalog.class));
				assertFullBuilderState(dialog, state);
			} finally {
				closeDialog(dialog, primaryStage);
			}
		});
	}

	@Test
	void confirmingNonEmptyCatalogSwitchUsesNewCatalogAndCompletelyResetsBuilder() throws Throwable {
		runOnJavaFxThread(() -> {
			Translation.load(Locale.UK);
			Stage primaryStage = showPrimaryStage();
			ReplaceBlocksRuleBuilderDialog dialog = showDialog(primaryStage,
					"literal(minecraft:stone)=minecraft:dirt");
			try {
				FullBuilderState state = populateFullBuilderState(dialog);
				ComboBox<BlockStateCatalog> selector = catalogSelector(dialog);
				BlockStateCatalog original = selector.getValue();
				BlockStateCatalog requested = differentCatalog(selector);
				AtomicReference<Throwable> alertStateFailure = new AtomicReference<>();
				AtomicInteger confirmations = autoRespondToConfirmation(dialog, true,
						() -> {
							assertCatalogState(dialog, selector, original);
							assertFullBuilderState(dialog, state);
							assertCatalogConfirmationText(dialog, original, requested);
						}, alertStateFailure);

				selector.setValue(requested);

				assertNull(alertStateFailure.get(), "selector changed before confirmation was accepted");
				assertEquals(1, confirmations.get());
				assertSame(requested, selector.getValue());
				assertSame(requested, fieldValue(dialog, "catalog", BlockStateCatalog.class));
				assertTrue(fieldValue(dialog, "ruleItems", List.class).isEmpty());
				assertEquals(-1, fieldValue(dialog, "rules", TableView.class).getSelectionModel().getSelectedIndex());
				assertInputReset(fieldValue(dialog, "from", Object.class), true, state.fromPropertyEditor());
				assertInputReset(fieldValue(dialog, "to", Object.class), false, state.toPropertyEditor());
				ComboBox<?> presets = fieldValue(dialog, "presets", ComboBox.class);
				assertNull(presets.getValue());
				assertFalse(presets.isShowing());
				assertEquals("", fieldValue(dialog, "result", TextArea.class).getText());
				assertEquals("", fieldValue(dialog, "validation", Label.class).getText());
				assertTrue(dialog.getDialogPane().lookupButton(ButtonType.OK).isDisabled());
				assertTrue(fieldValue(dialog, "previewButton", Node.class).isDisabled());
			} finally {
				closeDialog(dialog, primaryStage);
			}
		});
	}

	@Test
	void nonDefaultExtraNbtUsesRealCatalogSwitchConfirmation() throws Throwable {
		runOnJavaFxThread(() -> {
			Stage primaryStage = showPrimaryStage();
			ReplaceBlocksRuleBuilderDialog dialog = showDialog(primaryStage, "");
			try {
				Object from = fieldValue(dialog, "from", Object.class);
				ComboBox<?> tileEntityMode = fieldValue(from, "tileEntityMode", ComboBox.class);
				tileEntityMode.getSelectionModel().select(1);
				Object selectedMode = tileEntityMode.getValue();
				ComboBox<BlockStateCatalog> selector = catalogSelector(dialog);
				BlockStateCatalog original = selector.getValue();
				BlockStateCatalog requested = differentCatalog(selector);
				AtomicReference<Throwable> alertStateFailure = new AtomicReference<>();
				AtomicInteger confirmations = autoRespondToConfirmation(dialog, false,
						() -> {
							assertCatalogState(dialog, selector, original);
							assertSame(selectedMode, tileEntityMode.getValue());
						}, alertStateFailure);

				selector.setValue(requested);

				assertNull(alertStateFailure.get(), "selector changed before Extra NBT confirmation");
				assertEquals(1, confirmations.get());
				assertSame(original, selector.getValue());
				assertSame(selectedMode, tileEntityMode.getValue());
			} finally {
				closeDialog(dialog, primaryStage);
			}
		});
	}

	@Test
	void nonDefaultPropertyUsesRealCatalogSwitchConfirmation() throws Throwable {
		runOnJavaFxThread(() -> {
			Stage primaryStage = showPrimaryStage();
			ReplaceBlocksRuleBuilderDialog dialog = showDialog(primaryStage, "");
			try {
				Object from = fieldValue(dialog, "from", Object.class);
				ComboBox<String> block = fieldValue(from, "block", ComboBox.class);
				block.getEditor().setText("minecraft:acacia_stairs");
				ComboBox<?> property = firstPropertyEditor(from);
				property.getSelectionModel().select(1);
				Object selectedProperty = property.getValue();
				setBooleanField(from, "suppressSuggestions", true);
				try {
					block.getEditor().clear();
				} finally {
					setBooleanField(from, "suppressSuggestions", false);
				}
				ComboBox<BlockStateCatalog> selector = catalogSelector(dialog);
				BlockStateCatalog original = selector.getValue();
				BlockStateCatalog requested = differentCatalog(selector);
				AtomicReference<Throwable> alertStateFailure = new AtomicReference<>();
				AtomicInteger confirmations = autoRespondToConfirmation(dialog, false,
						() -> {
							assertCatalogState(dialog, selector, original);
							assertEquals("", block.getEditor().getText());
							assertSame(selectedProperty, property.getValue());
						}, alertStateFailure);

				selector.setValue(requested);

				assertNull(alertStateFailure.get(), "selector changed before property confirmation");
				assertEquals(1, confirmations.get());
				assertSame(original, selector.getValue());
				assertEquals("", block.getEditor().getText());
				assertSame(selectedProperty, property.getValue());
			} finally {
				closeDialog(dialog, primaryStage);
			}
		});
	}

	@Test
	void popupNavigationFilterReceivesKeysFromPopupScene() throws Throwable {
		runOnJavaFxThread(() -> {
			ComboBox<String> comboBox = new ComboBox<>(FXCollections.observableArrayList(
					"minecraft:acacia_button",
					"minecraft:acacia_door",
					"minecraft:acacia_fence"));
			comboBox.setEditable(true);
			comboBox.setSkin(new ComboBoxListViewSkin<>(comboBox));
			StackPane root = new StackPane(comboBox);
			new Scene(root);
			root.applyCss();
			comboBox.getEditor().setText("aca");
			int[] highlightedIndex = {-1};
			ReplaceBlocksRuleBuilderDialog.installAutocompletePopupKeyFilter(comboBox, event -> {
				highlightedIndex[0] = ReplaceBlocksRuleBuilderDialog.popupSelectionTarget(
						event.getCode(), highlightedIndex[0], comboBox.getItems().size(), 3);
				ReplaceBlocksRuleBuilderDialog.focusPopupSuggestion(comboBox, highlightedIndex[0]);
				event.consume();
			});

			ListView<?> popup = (ListView<?>) ((ComboBoxListViewSkin<?>) comboBox.getSkin()).getPopupContent();
			popup.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.DOWN,
					false, false, false, false));

			assertEquals(0, highlightedIndex[0]);
			assertEquals("aca", comboBox.getEditor().getText());
			assertNull(comboBox.getValue());
			assertEquals(-1, comboBox.getSelectionModel().getSelectedIndex());
			assertEquals(-1, popup.getFocusModel().getFocusedIndex());
		});
	}

	@Test
	void openingAutocompletePopupClearsNativeNavigationBeforeFirstArrowKey() throws Throwable {
		runOnJavaFxThread(() -> {
			ComboBox<String> comboBox = new ComboBox<>(FXCollections.observableArrayList(
					"minecraft:acacia_button",
					"minecraft:acacia_door",
					"minecraft:acacia_fence"));
			comboBox.setEditable(true);
			comboBox.setSkin(new ComboBoxListViewSkin<>(comboBox));
			StackPane root = new StackPane(comboBox);
			new Scene(root);
			root.applyCss();
			ReplaceBlocksRuleBuilderDialog.installAutocompletePopupKeyFilter(comboBox, event -> {});

			ListView<?> popup = (ListView<?>) ((ComboBoxListViewSkin<?>) comboBox.getSkin()).getPopupContent();
			popup.getSelectionModel().select(0);
			popup.getFocusModel().focus(0);
			comboBox.fireEvent(new javafx.event.Event(ComboBoxBase.ON_SHOWN));

			assertEquals(-1, popup.getSelectionModel().getSelectedIndex());
			assertEquals(-1, popup.getFocusModel().getFocusedIndex());
			assertEquals(0, ReplaceBlocksRuleBuilderDialog.popupSelectionTarget(
					KeyCode.DOWN, -1, comboBox.getItems().size(), 3));
		});
	}

	@Test
	void firstFocusedEmptyCatalogPopupsStayAttachedAfterLateResize() throws Throwable {
		assertFirstFocusedEmptyCatalogPopupStaysAttachedAfterLateResize("from", "block");
		assertFirstFocusedEmptyCatalogPopupStaysAttachedAfterLateResize("to", "block");
		assertFirstFocusedEmptyCatalogPopupStaysAttachedAfterLateResize("from", "biomeNames");
	}

	@Test
	void popupPositionTrackingKeepsAPopupAttachedBelowItsField() throws Throwable {
		AtomicReference<Stage> stageReference = new AtomicReference<>();
		AtomicReference<ComboBox<String>> comboBoxReference = new AtomicReference<>();
		try {
			runOnJavaFxThread(() -> {
				ComboBox<String> comboBox = new ComboBox<>(FXCollections.observableArrayList("a", "b", "c"));
				comboBox.setEditable(true);
				comboBox.setVisibleRowCount(3);
				ReplaceBlocksRuleBuilderDialog.installAutocompletePopupKeyFilter(comboBox, event -> {});
				Stage stage = new Stage();
				stage.setX(100);
				stage.setY(100);
				stage.setScene(new Scene(new StackPane(comboBox), 400, 200));
				stage.show();
				stage.getScene().getRoot().applyCss();
				stage.getScene().getRoot().layout();
				comboBox.show();
				assertTrue(comboBox.isShowing());
				stageReference.set(stage);
				comboBoxReference.set(comboBox);
			});

			// Drain callbacks scheduled by ON_SHOWN before simulating a later geometry update.
			runOnJavaFxThread(() -> {});

			runOnJavaFxThread(() -> {
				ComboBox<String> comboBox = comboBoxReference.get();
				Bounds comboBounds = comboBox.localToScreen(comboBox.getBoundsInLocal());
				Window popupWindow = popupWindow(comboBox);
				double belowY = comboBounds.getMaxY();
				popupWindow.setY(belowY);
				popupWindow.setHeight(popupWindow.getHeight() + 32);
				Bounds popupBounds = popupContent(comboBox).localToScreen(
						popupContent(comboBox).getLayoutBounds());
				assertEquals(comboBounds.getMaxY(), popupBounds.getMinY(), POPUP_ATTACHMENT_TOLERANCE);
				assertTrue(popupBounds.getCenterY() > comboBounds.getCenterY());
			});
		} finally {
			runOnJavaFxThread(() -> {
				if (comboBoxReference.get() != null) {
					comboBoxReference.get().hide();
				}
				if (stageReference.get() != null) {
					stageReference.get().close();
				}
			});
		}
	}

	@Test
	void popupPositionTrackingBoundsSynchronousGeometryFeedback() throws Throwable {
		AtomicReference<Stage> stageReference = new AtomicReference<>();
		AtomicReference<ComboBox<String>> comboBoxReference = new AtomicReference<>();
		try {
			runOnJavaFxThread(() -> {
				ComboBox<String> comboBox = new ComboBox<>(FXCollections.observableArrayList("a", "b", "c"));
				comboBox.setEditable(true);
				comboBox.setVisibleRowCount(3);
				ReplaceBlocksRuleBuilderDialog.installAutocompletePopupKeyFilter(comboBox, event -> {});
				Stage stage = new Stage();
				stage.setX(100);
				stage.setY(Screen.getPrimary().getVisualBounds().getMaxY() - 100);
				stage.setScene(new Scene(new StackPane(comboBox), 400, 100));
				stage.show();
				stage.getScene().getRoot().applyCss();
				stage.getScene().getRoot().layout();
				comboBox.show();
				assertTrue(comboBox.isShowing());
				stageReference.set(stage);
				comboBoxReference.set(comboBox);
			});

			runOnJavaFxThread(() -> {});

			runOnJavaFxThread(() -> {
				ComboBox<String> comboBox = comboBoxReference.get();
				Bounds comboBounds = comboBox.localToScreen(comboBox.getBoundsInLocal());
				Window popupWindow = popupWindow(comboBox);
				double detachedY = comboBounds.getMinY() - popupWindow.getHeight() - 40;

				AtomicInteger feedbackCount = new AtomicInteger();
				boolean[] resetting = {false};
				ChangeListener<Number> feedback = (observable, oldY, newY) -> {
					if (!resetting[0] && feedbackCount.get() < 10
							&& Math.abs(newY.doubleValue() - detachedY) > 0.5) {
						feedbackCount.incrementAndGet();
						resetting[0] = true;
						try {
							popupWindow.setY(detachedY);
						} finally {
							resetting[0] = false;
						}
					}
				};
				popupWindow.yProperty().addListener(feedback);
				try {
					popupWindow.setY(detachedY);
				} finally {
					popupWindow.yProperty().removeListener(feedback);
				}

				assertTrue(feedbackCount.get() <= 4,
						"popup positioning must not spin synchronously on persistent geometry feedback");

				popupWindow.setHeight(popupWindow.getHeight() + 1);
				Bounds popupBounds = popupContent(comboBox).localToScreen(popupContent(comboBox).getLayoutBounds());
				assertEquals(comboBounds.getMinY(), popupBounds.getMaxY(), 0.5,
						"the tracker must still recover on a later external geometry change");
			});
		} finally {
			runOnJavaFxThread(() -> {
				if (comboBoxReference.get() != null) {
					comboBoxReference.get().hide();
				}
				if (stageReference.get() != null) {
					stageReference.get().close();
				}
			});
		}
	}

	@Test
	void popupHorizontalBoundsStayInsideTheBuilderWindow() {
		ReplaceBlocksAutocomplete.HorizontalPopupBounds constrained = ReplaceBlocksAutocomplete.constrainHorizontalBounds(
				333, 460, 333, 270, 0, 706, 4);
		assertEquals(333, constrained.x(), 0.01);
		assertEquals(369, constrained.width(), 0.01);

		constrained = ReplaceBlocksAutocomplete.constrainHorizontalBounds(
				650, 460, 650, 100, 0, 706, 4);
		assertEquals(242, constrained.x(), 0.01);
		assertEquals(460, constrained.width(), 0.01);
	}

	private static void assertFirstFocusedEmptyCatalogPopupStaysAttachedAfterLateResize(
			String inputFieldName, String comboBoxFieldName) throws Throwable {
		AtomicReference<Stage> primaryStageReference = new AtomicReference<>();
		AtomicReference<ReplaceBlocksRuleBuilderDialog> dialogReference = new AtomicReference<>();
		AtomicReference<ComboBox<?>> comboBoxReference = new AtomicReference<>();
		try {
			runOnJavaFxThread(() -> {
				Stage primaryStage = new Stage();
				Scene primaryScene = new Scene(new StackPane(), 1200, 900);
				primaryScene.getStylesheets().add(ReplaceBlocksRuleBuilderDialog.class.getClassLoader()
						.getResource("style/base.css").toExternalForm());
				primaryStage.setScene(primaryScene);
				primaryStage.setX(50);
				primaryStage.setY(50);
				primaryStage.show();
				primaryStageReference.set(primaryStage);
				ReplaceBlocksRuleBuilderDialog dialog = new ReplaceBlocksRuleBuilderDialog(primaryStage, "");
				dialogReference.set(dialog);
				dialog.setX(100);
				dialog.setY(Screen.getPrimary().getVisualBounds().getMaxY() - 250);
				dialog.show();
				dialog.getDialogPane().applyCss();
				dialog.getDialogPane().layout();

				Field inputField = ReplaceBlocksRuleBuilderDialog.class.getDeclaredField(inputFieldName);
				inputField.setAccessible(true);
				Object input = inputField.get(dialog);
				Field comboBoxField = input.getClass().getDeclaredField(comboBoxFieldName);
				comboBoxField.setAccessible(true);
				ComboBox<?> comboBox = (ComboBox<?>) comboBoxField.get(input);
				comboBoxReference.set(comboBox);
				firePrimaryMouseEvent(comboBox.getEditor(), MouseEvent.MOUSE_PRESSED, true);
				firePrimaryMouseEvent(comboBox.getEditor(), MouseEvent.MOUSE_RELEASED, false);
				assertTrue(comboBox.getEditor().isFocused());
			});

			runOnJavaFxThread(() -> {
				ComboBox<?> comboBox = comboBoxReference.get();
				Node arrowButton = comboBox.lookup(".arrow-button");
				assertNotNull(arrowButton);
				firePrimaryMouseEvent(arrowButton, MouseEvent.MOUSE_PRESSED, true);
				firePrimaryMouseEvent(arrowButton, MouseEvent.MOUSE_RELEASED, false);
				assertTrue(comboBox.isShowing());
			});

			// Drain callbacks scheduled by ON_SHOWN before simulating a later geometry update.
			runOnJavaFxThread(() -> {});

			waitForJavaFxCondition(() -> popupIsAttached(comboBoxReference.get()),
					inputFieldName + "." + comboBoxFieldName + " popup was not initially attached");

			runOnJavaFxThread(() -> {
				ComboBox<?> comboBox = comboBoxReference.get();
				Bounds comboBounds = comboBox.localToScreen(comboBox.getBoundsInLocal());
				Window popupWindow = popupWindow(comboBox);
				double originalHeight = popupWindow.getHeight();
				ListView<?> popup = popupContent(comboBox);
				Bounds popupBounds = popup.localToScreen(popup.getLayoutBounds());
				Screen screen = Screen.getScreensForRectangle(
						comboBounds.getMinX(), comboBounds.getMinY(),
						comboBounds.getWidth(), comboBounds.getHeight()).stream()
						.findFirst().orElse(Screen.getPrimary());
				Bounds visualBounds = new BoundingBox(
						screen.getVisualBounds().getMinX(), screen.getVisualBounds().getMinY(),
						screen.getVisualBounds().getWidth(), screen.getVisualBounds().getHeight());
				boolean above = Math.abs(comboBounds.getMinY() - popupBounds.getMaxY())
						<= POPUP_ATTACHMENT_TOLERANCE;
				double availableHeight = above
						? comboBounds.getMinY() - visualBounds.getMinY()
						: visualBounds.getMaxY() - comboBounds.getMaxY();
				double growth = Math.min(32, availableHeight - originalHeight - 4);
				assertTrue(growth > 0,
						inputFieldName + "." + comboBoxFieldName + " has no room for a late popup resize");
				popupWindow.setHeight(originalHeight + growth);
			});

			waitForJavaFxCondition(() -> popupIsAttached(comboBoxReference.get()),
					inputFieldName + "." + comboBoxFieldName + " popup detached after a late resize");
		} finally {
			runOnJavaFxThread(() -> {
				if (dialogReference.get() != null) {
					dialogReference.get().setOnCloseRequest(null);
					dialogReference.get().close();
				}
				if (primaryStageReference.get() != null) {
					primaryStageReference.get().close();
				}
			});
		}
	}

	private static Window popupWindow(ComboBox<?> comboBox) {
		ListView<?> popup = popupContent(comboBox);
		assertNotNull(popup.getScene());
		return popup.getScene().getWindow();
	}

	private static ListView<?> popupContent(ComboBox<?> comboBox) {
		return (ListView<?>) ((ComboBoxListViewSkin<?>) comboBox.getSkin()).getPopupContent();
	}

	@Test
	void marqueeOverlaysGeneratedPreviewWithoutChangingBuilderLayout() throws Throwable {
		runOnJavaFxThread(() -> {
			Stage primaryStage = new Stage();
			primaryStage.setScene(new Scene(new StackPane()));
			try {
				ReplaceBlocksRuleBuilderDialog dialog = new ReplaceBlocksRuleBuilderDialog(primaryStage, "");
				Field marqueeField = ReplaceBlocksRuleBuilderDialog.class.getDeclaredField("ruleMarquee");
				marqueeField.setAccessible(true);
				Rectangle marquee = (Rectangle) marqueeField.get(dialog);
				StackPane layer = assertInstanceOf(StackPane.class, dialog.getDialogPane().getContent());
				double initialWidth = layer.prefWidth(-1);
				double initialHeight = layer.prefHeight(-1);

				marquee.setWidth(2000);
				marquee.setHeight(1200);

				assertEquals(initialWidth, layer.prefWidth(-1));
				assertEquals(initialHeight, layer.prefHeight(-1));
				assertSame(layer, marquee.getParent());
				assertSame(marquee, layer.getChildren().getLast());
			} finally {
				primaryStage.close();
			}
		});
	}

	@Test
	void marqueeUsesSubtleBlueOverlayStyle() throws Exception {
		InputStream resource = ReplaceBlocksRuleBuilderDialog.class.getClassLoader()
				.getResourceAsStream("style/component/change-nbt-dialog.css");
		assertNotNull(resource);
		try (InputStream input = resource) {
			String css = new String(input.readAllBytes(), StandardCharsets.UTF_8);

			assertTrue(css.contains("-fx-fill: rgba(74, 163, 255, 0.12);"));
			assertTrue(css.contains("-fx-stroke: #4aa3ff;"));
			assertTrue(css.contains("-fx-stroke-width: 1;"));
		}
	}

	@Test
	void builderDropdownUsesTheNativePopupFrameInsteadOfTheGlobalBlackBorder() throws Exception {
		InputStream resource = ReplaceBlocksRuleBuilderDialog.class.getClassLoader()
				.getResourceAsStream("style/component/change-nbt-dialog.css");
		assertNotNull(resource);
		try (InputStream input = resource) {
			String css = new String(input.readAllBytes(), StandardCharsets.UTF_8);

			assertTrue(css.contains(".combo-box-popup .replace-blocks-builder-dropdown {"));
			assertTrue(css.contains("-fx-border-width: 0;"));
		}
	}

	@Test
	void builderPrimaryActionUsesTheSharedButtonStyle() throws Exception {
		InputStream resource = ReplaceBlocksRuleBuilderDialog.class.getClassLoader()
				.getResourceAsStream("style/component/change-nbt-dialog.css");
		assertNotNull(resource);
		try (InputStream input = resource) {
			String css = new String(input.readAllBytes(), StandardCharsets.UTF_8);
			assertFalse(css.contains("replace-blocks-builder-add-rule"));
			assertTrue(css.contains(".replace-blocks-builder-preset-action:disabled"));
			assertTrue(css.contains("-fx-opacity: 0.55;"));
		}
	}

	@Test
	void builderWorkflowMessagesDoNotFallBackToEnglishInOtherLanguages() throws Exception {
		List<String> locales = List.of(
				"cs_CZ", "de_DE", "es_ES", "fr_FR", "hu_HU", "it_IT", "ja_JP", "ko_KR",
				"nl_NL", "pl_PL", "pt_BR", "pt_PT", "ru_RU", "sv_SE", "tr_TR", "uk_UA",
				"zh_CN", "zh_TW");
		List<String> keys = List.of(
				"dialog.replace_blocks.builder.catalog_switch.message",
				"dialog.replace_blocks.builder.preset.save.header",
				"dialog.replace_blocks.builder.preset.replace_confirm",
				"dialog.replace_blocks.builder.discard.title",
				"dialog.replace_blocks.builder.discard.header",
				"dialog.replace_blocks.builder.help.biome.syntax");

		for (String locale : locales) {
			for (String key : keys) {
				assertNotEquals(builderTranslation("en_GB", key), builderTranslation(locale, key),
						() -> locale + " still uses the English Builder text for " + key);
			}
		}
	}

	@Test
	void emptyBuilderUsesCompactEmptyStateAndReadablePrimaryAction() throws Throwable {
		runOnJavaFxThread(() -> {
			Stage primaryStage = showPrimaryStage();
			ReplaceBlocksRuleBuilderDialog dialog = showDialog(primaryStage, "");
			try {
				TableView<?> rules = fieldValue(dialog, "rules", TableView.class);
				StackPane rulesArea = (StackPane) dialog.getDialogPane().lookup(".replace-blocks-builder-rules-area");
				Label placeholder = (Label) rules.getPlaceholder();
				TextArea result = fieldValue(dialog, "result", TextArea.class);
				Node sourceConstraints = dialog.getDialogPane().lookup(".replace-blocks-builder-source-constraints");
				boolean hasAddRuleButton = dialog.getDialogPane().lookupAll(".button").stream()
						.filter(Button.class::isInstance)
						.map(Button.class::cast)
						.anyMatch(button -> button.getText().equals(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_ADD_RULE.toString()));

				assertNotNull(rulesArea);
				assertEquals(Priority.NEVER, VBox.getVgrow(rulesArea));
				assertEquals(160, rules.getMaxHeight());
				assertEquals("No rules have been added yet. Fill in the source and target blocks, then click Add rule.", placeholder.getText());
				assertTrue(result.isWrapText());
				assertEquals(40, result.getMinHeight());
				assertNotNull(sourceConstraints);
				assertEquals(2, GridPane.getColumnSpan(sourceConstraints));
				assertTrue(hasAddRuleButton);
			} finally {
				closeDialog(dialog, primaryStage);
			}
		});
	}

	@Test
	void builderKeepsAdvancedSyntaxGuidanceOutOfPersistentFooter() throws Throwable {
		runOnJavaFxThread(() -> {
			Stage primaryStage = showPrimaryStage();
			ReplaceBlocksRuleBuilderDialog dialog = showDialog(primaryStage, "");
			try {
				boolean hasPersistentAdvancedGuidance = dialog.getDialogPane().lookupAll(".label").stream()
						.filter(Label.class::isInstance)
						.map(Label.class::cast)
						.anyMatch(label -> label.getText().equals(
								Translation.DIALOG_REPLACE_BLOCKS_BUILDER_ADVANCED.toString()));

				assertFalse(hasPersistentAdvancedGuidance);
			} finally {
				closeDialog(dialog, primaryStage);
			}
		});
	}

	private static String builderTranslation(String locale, String key) throws Exception {
		String resourceName = "lang/" + locale + ".txt";
		try (InputStream input = ReplaceBlocksRuleBuilderDialog.class.getClassLoader()
				.getResourceAsStream(resourceName)) {
			assertNotNull(input, resourceName);
			return new String(input.readAllBytes(), StandardCharsets.UTF_8).lines()
					.filter(line -> line.startsWith(key + ";"))
					.map(line -> line.substring(key.length() + 1))
					.findFirst()
					.orElseThrow(() -> new AssertionError("missing translation key " + key + " in " + resourceName));
		}
	}

	@Test
	void sourceYLabelsKeepTheirPreferredWidth() throws Throwable {
		runOnJavaFxThread(() -> {
			Stage primaryStage = showPrimaryStage();
			ReplaceBlocksRuleBuilderDialog dialog = showDialog(primaryStage, "");
			try {
				List<Label> labels = dialog.getDialogPane().lookupAll(".replace-blocks-builder-y-range .label").stream()
						.filter(Label.class::isInstance)
						.map(Label.class::cast)
						.toList();
				assertEquals(2, labels.size());
				assertTrue(labels.stream().allMatch(label -> label.getMinWidth() == Region.USE_PREF_SIZE));
			} finally {
				closeDialog(dialog, primaryStage);
			}
		});
	}

	@Test
	void populatedBuilderExpandsRulesAndResultAreas() throws Throwable {
		runOnJavaFxThread(() -> {
			Stage primaryStage = showPrimaryStage();
			ReplaceBlocksRuleBuilderDialog dialog = showDialog(primaryStage, "minecraft:stone=minecraft:dirt");
			try {
				TableView<?> rules = fieldValue(dialog, "rules", TableView.class);
				StackPane rulesArea = (StackPane) dialog.getDialogPane().lookup(".replace-blocks-builder-rules-area");
				TextArea result = fieldValue(dialog, "result", TextArea.class);

				assertEquals(Priority.ALWAYS, VBox.getVgrow(rulesArea));
				assertEquals(Double.MAX_VALUE, rules.getMaxHeight());
				assertEquals(52, result.getMinHeight());
				assertEquals(68, result.getPrefHeight());
				assertEquals(92, result.getMaxHeight());
			} finally {
				closeDialog(dialog, primaryStage);
			}
		});
	}

	@Test
	void routesAutocompleteKeysBeforeComboBoxBehavior() {
		assertTrue(ReplaceBlocksRuleBuilderDialog.isAutocompletePopupKey(KeyCode.UP));
		assertTrue(ReplaceBlocksRuleBuilderDialog.isAutocompletePopupKey(KeyCode.DOWN));
		assertTrue(ReplaceBlocksRuleBuilderDialog.isAutocompletePopupKey(KeyCode.PAGE_UP));
		assertTrue(ReplaceBlocksRuleBuilderDialog.isAutocompletePopupKey(KeyCode.PAGE_DOWN));
		assertTrue(ReplaceBlocksRuleBuilderDialog.isAutocompletePopupKey(KeyCode.ENTER));
		assertTrue(ReplaceBlocksRuleBuilderDialog.isAutocompletePopupKey(KeyCode.TAB));
		assertFalse(ReplaceBlocksRuleBuilderDialog.isAutocompletePopupKey(KeyCode.LEFT));
		assertFalse(ReplaceBlocksRuleBuilderDialog.isAutocompletePopupKey(KeyCode.A));
	}

	@Test
	void startsMarqueeOnlyAfterARealDrag() {
		assertFalse(ReplaceBlocksRuleBuilderDialog.isRuleMarqueeDrag(0, 0));
		assertFalse(ReplaceBlocksRuleBuilderDialog.isRuleMarqueeDrag(2, 1));
		assertTrue(ReplaceBlocksRuleBuilderDialog.isRuleMarqueeDrag(3, 0));
		assertTrue(ReplaceBlocksRuleBuilderDialog.isRuleMarqueeDrag(0, 3));
	}

	@Test
	void recognizesWhenLinuxJavaFxControlTestsHaveNoDisplay() {
		assertFalse(supportsJavaFxControlTests("Linux", null, null));
		assertFalse(supportsJavaFxControlTests("Linux", "", ""));
		assertTrue(supportsJavaFxControlTests("Linux", ":99", null));
		assertTrue(supportsJavaFxControlTests("Windows 11", null, null));
	}

	private static Stage showPrimaryStage() {
		Stage primaryStage = new Stage();
		primaryStage.setScene(new Scene(new StackPane(), 1200, 900));
		primaryStage.show();
		return primaryStage;
	}

	private static ReplaceBlocksRuleBuilderDialog showDialog(Stage primaryStage, String initialValue) {
		ReplaceBlocksRuleBuilderDialog dialog = new ReplaceBlocksRuleBuilderDialog(primaryStage, initialValue);
		dialog.show();
		dialog.getDialogPane().applyCss();
		dialog.getDialogPane().layout();
		return dialog;
	}

	private static void closeDialog(ReplaceBlocksRuleBuilderDialog dialog, Stage primaryStage) {
		dialog.setOnCloseRequest(null);
		dialog.close();
		primaryStage.close();
	}

	private static void applyCustomPreset(ReplaceBlocksRuleBuilderDialog dialog, String value)
			throws ReflectiveOperationException {
		ComboBox<Object> presets = fieldValue(dialog, "presets", ComboBox.class);
		Object builtin = presets.getItems().getFirst();
		Constructor<?> constructor = builtin.getClass().getDeclaredConstructors()[0];
		constructor.setAccessible(true);
		Object custom = constructor.newInstance(null,
				new GlobalConfig.ReplaceBlocksUserPreset("Task 2", value));
		presets.getItems().add(custom);
		presets.setValue(custom);
		Method applyPreset = declaredMethod(dialog, "applyPreset", ActionEvent.class);
		applyPreset.invoke(dialog, new ActionEvent());
	}

	@SuppressWarnings("unchecked")
	private static ComboBox<BlockStateCatalog> catalogSelector(ReplaceBlocksRuleBuilderDialog dialog) {
		return (ComboBox<BlockStateCatalog>) dialog.getDialogPane().lookupAll(".combo-box").stream()
				.filter(ComboBox.class::isInstance)
				.map(ComboBox.class::cast)
				.filter(comboBox -> !comboBox.getItems().isEmpty()
						&& comboBox.getItems().getFirst() instanceof BlockStateCatalog)
				.findFirst()
				.orElseThrow();
	}

	private static BlockStateCatalog differentCatalog(ComboBox<BlockStateCatalog> selector) {
		return selector.getItems().stream()
				.filter(candidate -> candidate != selector.getValue())
				.findFirst()
				.orElseThrow();
	}

	private static AtomicInteger autoRespondToConfirmation(
			ReplaceBlocksRuleBuilderDialog dialog, boolean confirm) {
		return autoRespondToConfirmation(dialog, confirm, () -> {}, new AtomicReference<>());
	}

	private static AtomicInteger autoRespondToConfirmation(
			ReplaceBlocksRuleBuilderDialog dialog, boolean confirm,
			ThrowingRunnable beforeClick, AtomicReference<Throwable> beforeClickFailure) {
		AtomicInteger requests = new AtomicInteger();
		Window owner = dialog.getDialogPane().getScene().getWindow();
		Platform.runLater(() -> {
			for (Window window : Window.getWindows()) {
				if (window == owner || !window.isShowing() || window.getScene() == null) {
					continue;
				}
				Node node = window.getScene().lookup(".dialog-pane");
				if (!(node instanceof DialogPane pane)) {
					continue;
				}
				Node button = pane.lookupButton(confirm ? ButtonType.OK : ButtonType.CANCEL);
				if (button instanceof Button alertButton) {
					requests.incrementAndGet();
					try {
						beforeClick.run();
					} catch (Throwable ex) {
						beforeClickFailure.set(ex);
					}
					alertButton.fire();
					return;
				}
			}
		});
		return requests;
	}

	private static AtomicInteger autoRespondToDiscardConfirmation(
			ReplaceBlocksRuleBuilderDialog dialog, boolean discard) {
		return autoRespondToDiscardConfirmation(dialog, discard, () -> {}, new AtomicReference<>());
	}

	private static AtomicInteger autoRespondToDiscardConfirmation(
			ReplaceBlocksRuleBuilderDialog dialog, boolean discard,
			ThrowingRunnable beforeClick, AtomicReference<Throwable> beforeClickFailure) {
		AtomicInteger requests = new AtomicInteger();
		Window owner = dialog.getDialogPane().getScene().getWindow();
		Platform.runLater(() -> {
			for (Window window : Window.getWindows()) {
				if (window == owner || !window.isShowing() || window.getScene() == null) {
					continue;
				}
				Node node = window.getScene().lookup(".dialog-pane");
				if (!(node instanceof DialogPane pane)) {
					continue;
				}
				ButtonType action = pane.getButtonTypes().stream()
						.filter(button -> discard
								? button.getButtonData() == ButtonBar.ButtonData.OK_DONE
								: button.getButtonData() == ButtonBar.ButtonData.CANCEL_CLOSE)
						.findFirst()
						.orElse(null);
				Node button = action == null ? null : pane.lookupButton(action);
				if (button instanceof Button alertButton) {
					requests.incrementAndGet();
					try {
						beforeClick.run();
					} catch (Throwable ex) {
						beforeClickFailure.set(ex);
					}
					alertButton.fire();
					return;
				}
			}
		});
		return requests;
	}

	private static void assertTransactionalDiscardConfirmation(
			ReplaceBlocksRuleBuilderDialog dialog) {
		Window owner = dialog.getDialogPane().getScene().getWindow();
		DialogPane pane = Window.getWindows().stream()
				.filter(window -> window != owner && window.isShowing() && window.getScene() != null)
				.map(window -> window.getScene().lookup(".dialog-pane"))
				.filter(DialogPane.class::isInstance)
				.map(DialogPane.class::cast)
				.findFirst()
				.orElseThrow();
		ButtonType discard = pane.getButtonTypes().stream()
				.filter(button -> button.getButtonData() == ButtonBar.ButtonData.OK_DONE)
				.findFirst()
				.orElseThrow();
		ButtonType continueEditing = pane.getButtonTypes().stream()
				.filter(button -> button.getButtonData() == ButtonBar.ButtonData.CANCEL_CLOSE)
				.findFirst()
				.orElseThrow();

		assertEquals("Discard changes", ((Button) pane.lookupButton(discard)).getText());
		assertEquals("Continue editing", ((Button) pane.lookupButton(continueEditing)).getText());
		assertTrue(pane.getContentText().contains("ReplaceBlocks field"));
		assertTrue(pane.getContentText().contains("before the Builder opened"));
	}

	private static AtomicInteger autoCloseConfirmation(ReplaceBlocksRuleBuilderDialog dialog) {
		AtomicInteger requests = new AtomicInteger();
		Window owner = dialog.getDialogPane().getScene().getWindow();
		Platform.runLater(() -> {
			for (Window window : Window.getWindows()) {
				if (window == owner || !window.isShowing() || window.getScene() == null
						|| !(window.getScene().lookup(".dialog-pane") instanceof DialogPane)) {
					continue;
				}
				requests.incrementAndGet();
				Event.fireEvent(window, new WindowEvent(window, WindowEvent.WINDOW_CLOSE_REQUEST));
				return;
			}
		});
		return requests;
	}

	private static WindowEvent fireWindowCloseRequest(ReplaceBlocksRuleBuilderDialog dialog) {
		Window window = dialog.getDialogPane().getScene().getWindow();
		WindowEvent closeRequest = new WindowEvent(window, WindowEvent.WINDOW_CLOSE_REQUEST);
		Event.fireEvent(window, closeRequest);
		return closeRequest;
	}

	private static void setFromText(ReplaceBlocksRuleBuilderDialog dialog, String text)
			throws ReflectiveOperationException {
		Object from = fieldValue(dialog, "from", Object.class);
		fieldValue(from, "block", ComboBox.class).getEditor().setText(text);
	}

	private static void assertCatalogConfirmationText(ReplaceBlocksRuleBuilderDialog dialog,
			BlockStateCatalog oldCatalog, BlockStateCatalog newCatalog)
			throws ReflectiveOperationException {
		Window owner = dialog.getDialogPane().getScene().getWindow();
		Window confirmationWindow = Window.getWindows().stream()
				.filter(window -> window != owner && window.isShowing() && window.getScene() != null)
				.filter(window -> window.getScene().lookup(".dialog-pane") instanceof DialogPane)
				.findFirst()
				.orElseThrow();
		DialogPane pane = (DialogPane) confirmationWindow.getScene().lookup(".dialog-pane");
		ReplaceBlocksCatalogModel model = fieldValue(dialog, "catalogModel", ReplaceBlocksCatalogModel.class);
		String oldLabel = model.label(oldCatalog);
		String newLabel = model.label(newCatalog);

		assertEquals(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_CATALOG_SWITCH_TITLE.toString(),
				((Stage) confirmationWindow).getTitle());
		assertEquals(Translation.DIALOG_REPLACE_BLOCKS_BUILDER_CATALOG_SWITCH_MESSAGE.format(oldLabel, newLabel),
				pane.getContentText());
		assertTrue(pane.getContentText().contains(oldLabel));
		assertTrue(pane.getContentText().contains(newLabel));
	}

	private static void assertCatalogState(ReplaceBlocksRuleBuilderDialog dialog,
			ComboBox<BlockStateCatalog> selector, BlockStateCatalog expected)
			throws ReflectiveOperationException {
		assertSame(expected, selector.getValue());
		assertSame(expected, fieldValue(dialog, "catalog", BlockStateCatalog.class));
		assertSame(expected,
				fieldValue(dialog, "catalogModel", ReplaceBlocksCatalogModel.class).selected());
	}

	private static FullBuilderState populateFullBuilderState(ReplaceBlocksRuleBuilderDialog dialog)
			throws ReflectiveOperationException {
		Object from = fieldValue(dialog, "from", Object.class);
		Object to = fieldValue(dialog, "to", Object.class);
		ComboBox<String> fromBlock = fieldValue(from, "block", ComboBox.class);
		ComboBox<String> toBlock = fieldValue(to, "block", ComboBox.class);
		fromBlock.getEditor().setText("minecraft:acacia_stairs");
		toBlock.getEditor().setText("minecraft:andesite_stairs");
		ComboBox<?> fromProperty = firstPropertyEditor(from);
		ComboBox<?> toProperty = firstPropertyEditor(to);
		fromProperty.getSelectionModel().select(1);
		toProperty.getSelectionModel().select(1);
		ComboBox<?> tileEntityMode = fieldValue(from, "tileEntityMode", ComboBox.class);
		tileEntityMode.getSelectionModel().select(1);
		fieldValue(from, "minY", TextField.class).setText("-16");
		fieldValue(from, "maxY", TextField.class).setText("31");
		fieldValue(from, "biomeNames", ComboBox.class).getEditor().setText("minecraft:forest");
		TableView<?> rules = fieldValue(dialog, "rules", TableView.class);
		rules.getSelectionModel().selectFirst();
		ComboBox<?> presets = fieldValue(dialog, "presets", ComboBox.class);
		presets.getSelectionModel().selectFirst();
		setIntField(from, "blockSuggestionRevision", 40);
		setIntField(from, "biomeSuggestionRevision", 50);
		setIntField(to, "blockSuggestionRevision", 40);
		setIntField(to, "biomeSuggestionRevision", 50);
		setIntField(from, "blockSuggestionHighlight", 2);
		setIntField(from, "biomeSuggestionHighlight", 3);
		setBooleanField(from, "blockExplicitCatalog", true);
		setBooleanField(from, "biomeExplicitCatalog", true);
		fromProperty.show();
		assertTrue(fromProperty.isShowing());

		return new FullBuilderState(
				fromBlock.getEditor().getText(),
				toBlock.getEditor().getText(),
				fromProperty, fromProperty.getValue(),
				toProperty, toProperty.getValue(),
				tileEntityMode.getValue(),
				fieldValue(from, "minY", TextField.class).getText(),
				fieldValue(from, "maxY", TextField.class).getText(),
				fieldValue(from, "biomeNames", ComboBox.class).getEditor().getText(),
				List.copyOf(fieldValue(dialog, "ruleItems", List.class)),
				rules.getSelectionModel().getSelectedIndex(),
				presets.getValue(),
				fieldValue(dialog, "result", TextArea.class).getText(),
				fieldValue(dialog, "validation", Label.class).getText(),
				40, 50, 2, 3, true, true);
	}

	private static void assertFullBuilderState(ReplaceBlocksRuleBuilderDialog dialog, FullBuilderState state)
			throws ReflectiveOperationException {
		Object from = fieldValue(dialog, "from", Object.class);
		Object to = fieldValue(dialog, "to", Object.class);
		assertEquals(state.fromText(), fieldValue(from, "block", ComboBox.class).getEditor().getText());
		assertEquals(state.toText(), fieldValue(to, "block", ComboBox.class).getEditor().getText());
		assertSame(state.fromPropertyValue(), state.fromPropertyEditor().getValue());
		assertSame(state.toPropertyValue(), state.toPropertyEditor().getValue());
		assertSame(state.tileMode(), fieldValue(from, "tileEntityMode", ComboBox.class).getValue());
		assertEquals(state.minY(), fieldValue(from, "minY", TextField.class).getText());
		assertEquals(state.maxY(), fieldValue(from, "maxY", TextField.class).getText());
		assertEquals(state.biome(), fieldValue(from, "biomeNames", ComboBox.class).getEditor().getText());
		assertEquals(state.rules(), fieldValue(dialog, "ruleItems", List.class));
		assertEquals(state.selectedRuleIndex(),
				fieldValue(dialog, "rules", TableView.class).getSelectionModel().getSelectedIndex());
		assertSame(state.selectedPreset(), fieldValue(dialog, "presets", ComboBox.class).getValue());
		assertEquals(state.result(), fieldValue(dialog, "result", TextArea.class).getText());
		assertEquals(state.validation(), fieldValue(dialog, "validation", Label.class).getText());
		assertEquals(state.blockRevision(), intField(from, "blockSuggestionRevision"));
		assertEquals(state.biomeRevision(), intField(from, "biomeSuggestionRevision"));
		assertEquals(state.blockHighlight(), intField(from, "blockSuggestionHighlight"));
		assertEquals(state.biomeHighlight(), intField(from, "biomeSuggestionHighlight"));
		assertEquals(state.blockExplicit(), booleanField(from, "blockExplicitCatalog"));
		assertEquals(state.biomeExplicit(), booleanField(from, "biomeExplicitCatalog"));
		assertTrue(state.fromPropertyEditor().isShowing());
	}

	private static void assertInputReset(Object input, boolean source, ComboBox<?> oldPropertyEditor)
			throws ReflectiveOperationException {
		assertEquals("", fieldValue(input, "block", ComboBox.class).getEditor().getText());
		assertFalse(fieldValue(input, "block", ComboBox.class).isShowing());
		assertTrue(fieldValue(input, "propertyEditors", Map.class).isEmpty());
		assertFalse(oldPropertyEditor.isShowing());
		assertTrue(intField(input, "blockSuggestionRevision") > 40);
		assertEquals(-1, intField(input, "blockSuggestionHighlight"));
		assertFalse(booleanField(input, "blockExplicitCatalog"));
		if (source) {
			ComboBox<?> tileEntityMode = fieldValue(input, "tileEntityMode", ComboBox.class);
			assertEquals(0, tileEntityMode.getSelectionModel().getSelectedIndex());
			assertFalse(tileEntityMode.isShowing());
			assertEquals("", fieldValue(input, "minY", TextField.class).getText());
			assertEquals("", fieldValue(input, "maxY", TextField.class).getText());
			ComboBox<?> biomeNames = fieldValue(input, "biomeNames", ComboBox.class);
			assertEquals("", biomeNames.getEditor().getText());
			assertFalse(biomeNames.isShowing());
			assertTrue(intField(input, "biomeSuggestionRevision") > 50);
			assertEquals(-1, intField(input, "biomeSuggestionHighlight"));
			assertFalse(booleanField(input, "biomeExplicitCatalog"));
		}
	}

	private static ComboBox<?> firstPropertyEditor(Object input) throws ReflectiveOperationException {
		Map<?, ?> propertyEditors = fieldValue(input, "propertyEditors", Map.class);
		assertFalse(propertyEditors.isEmpty());
		return (ComboBox<?>) propertyEditors.values().iterator().next();
	}

	private static <T> T fieldValue(Object target, String name, Class<T> type)
			throws ReflectiveOperationException {
		Field field = declaredField(target, name);
		return type.cast(field.get(target));
	}

	private static int intField(Object target, String name) throws ReflectiveOperationException {
		return declaredField(target, name).getInt(target);
	}

	private static boolean booleanField(Object target, String name) throws ReflectiveOperationException {
		return declaredField(target, name).getBoolean(target);
	}

	private static void setIntField(Object target, String name, int value) throws ReflectiveOperationException {
		declaredField(target, name).setInt(target, value);
	}

	private static void setBooleanField(Object target, String name, boolean value) throws ReflectiveOperationException {
		declaredField(target, name).setBoolean(target, value);
	}

	private static Field declaredField(Object target, String name) throws NoSuchFieldException {
		Class<?> type = target.getClass();
		while (type != null) {
			try {
				Field field = type.getDeclaredField(name);
				field.setAccessible(true);
				return field;
			} catch (NoSuchFieldException ex) {
				type = type.getSuperclass();
			}
		}
		throw new NoSuchFieldException(name);
	}

	private static Method declaredMethod(Object target, String name, Class<?>... parameterTypes)
			throws NoSuchMethodException {
		Class<?> type = target.getClass();
		while (type != null) {
			try {
				Method method = type.getDeclaredMethod(name, parameterTypes);
				method.setAccessible(true);
				return method;
			} catch (NoSuchMethodException ex) {
				type = type.getSuperclass();
			}
		}
		throw new NoSuchMethodException(name);
	}

	private static boolean invokeBoolean(Object target, String method) throws ReflectiveOperationException {
		return (boolean) declaredMethod(target, method).invoke(target);
	}

	private record FullBuilderState(
			String fromText, String toText,
			ComboBox<?> fromPropertyEditor, Object fromPropertyValue,
			ComboBox<?> toPropertyEditor, Object toPropertyValue,
			Object tileMode, String minY, String maxY, String biome,
			List<?> rules, int selectedRuleIndex, Object selectedPreset,
			String result, String validation,
			int blockRevision, int biomeRevision, int blockHighlight, int biomeHighlight,
			boolean blockExplicit, boolean biomeExplicit) {}

	private static void runOnJavaFxThread(ThrowingRunnable action) throws Throwable {
		assumeTrue(supportsJavaFxControlTests(System.getProperty("os.name"),
				System.getenv("DISPLAY"), System.getenv("WAYLAND_DISPLAY")),
				"JavaFX controls need a display on Linux");
		initializeJavaFx();
		CountDownLatch completed = new CountDownLatch(1);
		AtomicReference<Throwable> failure = new AtomicReference<>();
		Platform.runLater(() -> {
			try {
				action.run();
			} catch (Throwable ex) {
				failure.set(ex);
			} finally {
				completed.countDown();
			}
		});
		assertTrue(completed.await(10, TimeUnit.SECONDS));
		if (failure.get() != null) {
			throw failure.get();
		}
	}

	private static void firePrimaryMouseEvent(Node target, javafx.event.EventType<MouseEvent> type,
			boolean primaryButtonDown) {
		target.fireEvent(new MouseEvent(type, 1, 1, 1, 1, MouseButton.PRIMARY, 1,
				false, false, false, false, primaryButtonDown, false, false,
				false, false, false, null));
	}

	private static boolean supportsJavaFxControlTests(String osName, String display, String waylandDisplay) {
		return osName == null || !osName.toLowerCase().contains("linux")
				|| display != null && !display.isBlank()
				|| waylandDisplay != null && !waylandDisplay.isBlank();
	}

	private static void initializeJavaFx() throws InterruptedException {
		synchronized (javaFxStartupLock) {
			if (javaFxStarted) {
				return;
			}
			CountDownLatch initialized = new CountDownLatch(1);
			try {
				Platform.startup(initialized::countDown);
				assertTrue(initialized.await(10, TimeUnit.SECONDS));
			} catch (IllegalStateException ex) {
				// Another test class may already have initialized the toolkit.
			}
			Platform.setImplicitExit(false);
			javaFxStarted = true;
		}
	}

	private static boolean popupIsAttached(ComboBox<?> comboBox) {
		Bounds comboBounds = comboBox.localToScreen(comboBox.getBoundsInLocal());
		ListView<?> popup = popupContent(comboBox);
		Bounds popupBounds = popup.localToScreen(popup.getLayoutBounds());
		return comboBounds != null && popupBounds != null
				&& (Math.abs(comboBounds.getMinY() - popupBounds.getMaxY()) <= POPUP_ATTACHMENT_TOLERANCE
						|| Math.abs(comboBounds.getMaxY() - popupBounds.getMinY())
						<= POPUP_ATTACHMENT_TOLERANCE);
	}

	private static void waitForJavaFxCondition(BooleanSupplier condition, String message) throws InterruptedException {
		AtomicBoolean satisfied = new AtomicBoolean();
		CountDownLatch complete = new CountDownLatch(1);
		Platform.runLater(() -> new AnimationTimer() {
			private long deadline;

			@Override
			public void handle(long now) {
				if (deadline == 0) {
					deadline = now + TimeUnit.SECONDS.toNanos(5);
				}
				if (condition.getAsBoolean()) {
					satisfied.set(true);
				}
				if (satisfied.get() || now >= deadline) {
					stop();
					complete.countDown();
				}
			}
		}.start());
		assertTrue(complete.await(10, TimeUnit.SECONDS), "JavaFX condition wait did not complete: " + message);
		assertTrue(satisfied.get(), message);
	}

	@FunctionalInterface
	private interface ThrowingRunnable {
		void run() throws Throwable;
	}
}
