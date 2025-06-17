package net.querz.mcaselector.ui.dialog;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.querz.mcaselector.config.ConfigProvider;
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.overlay.OverlayType;
import net.querz.mcaselector.overlay.overlays.ScriptOverlay;
import net.querz.mcaselector.ui.UIFactory;
import net.querz.mcaselector.ui.component.CodeEditor;
import net.querz.mcaselector.ui.component.PersistentDialogProperties;
import net.querz.mcaselector.util.property.DataProperty;
import net.querz.mcaselector.text.Translation;
import net.querz.mcaselector.tile.TileMap;
import net.querz.mcaselector.overlay.overlays.InhabitedTimeOverlay;
import net.querz.mcaselector.overlay.Overlay;
import net.querz.mcaselector.ui.component.OverlayBox;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class OverlayEditorDialog extends Dialog<OverlayEditorDialog.Result> implements PersistentDialogProperties {

	private static final Image addIcon = FileHelper.getIconFromResources("img/add");

	private final List<Overlay> overlays;

	private final TabPane tabs = new TabPane();
	private final ScrollPane overlaysScrollPane = new ScrollPane();
	private final VBox overlaysList = new VBox();
	private final Label add = new Label("", new ImageView(addIcon));

	private final TileMap tileMap;

	private final DataProperty<Boolean> closedWithOK = new DataProperty<>(false);

	private static final String initScript = """
			import net.querz.mcaselector.io.mca.ChunkData;
			import net.querz.nbt.*;
			
			int get(ChunkData data) {
			\t
			}""";

	private static final CodeEditor codeEditor = new CodeEditor(initScript);
	private static int lastSelectedTab;

	private final ChangeListener<Overlay> tileMapSelectedOverlayChange = (v, o, n) -> {
		if (o != n) {
			select(n);
		}
	};

	public OverlayEditorDialog(Stage primaryStage, TileMap tileMap, List<Overlay> values) {
		if (values == null) {
			this.overlays = new ArrayList<>();
		} else {
			this.overlays = values;
		}
		this.tileMap = tileMap;

		Overlay originalOverlay = tileMap.getOverlay() != null ? tileMap.getOverlay().clone() : null;
		List<Overlay> originalOverlays = tileMap.getOverlays();

		setResultConverter(p -> p == ButtonType.OK ? new Result(overlays) : null);

		titleProperty().bind(Translation.DIALOG_EDIT_OVERLAYS_TITLE.getProperty());
		initModality(Modality.NONE);
		initStyle(StageStyle.UTILITY);
		getDialogPane().getStyleClass().add("overlay-dialog-pane");
		getDialogPane().getStylesheets().addAll(primaryStage.getScene().getStylesheets());
		getDialogPane().getStylesheets().add(Objects.requireNonNull(OverlayEditorDialog.class.getClassLoader().getResource("style/component/overlay-editor-dialog.css")).toExternalForm());

		codeEditor.setOwner(getDialogPane().getScene().getWindow());
		codeEditor.setRecentFiles(ConfigProvider.GLOBAL.getRecentOverlayScripts());
		codeEditor.setSource(ConfigProvider.GLOBAL.getOverlayScript());
		codeEditor.setOnSave(f -> {
			String path = f.toString();
			for (Overlay overlay : overlays) {
				if (overlay.getType() == OverlayType.SCRIPT && path.equals(overlay.getRawMultiValues())) {
					overlay.setMultiValuesString(path);
				}
			}
		});

		getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
		getDialogPane().lookupButton(ButtonType.OK).addEventFilter(ActionEvent.ACTION, e -> {
			tileMap.setOverlays(overlays);
			ConfigProvider.OVERLAY.setOverlays(overlays);
			tileMap.getWindow().getOptionBar().setEditOverlaysEnabled(true);
			tileMap.getWindow().untrackDialog(this);
			closedWithOK.set(true);
			initPersistentLocationOnClose(this);
			lastSelectedTab = tabs.getSelectionModel().getSelectedIndex();
		});
		setOnCloseRequest(e -> {
			tileMap.overlayParserProperty().removeListener(tileMapSelectedOverlayChange);
			if (!closedWithOK.get()) {
				tileMap.setOverlays(originalOverlays);
				tileMap.setOverlay(originalOverlay);
				tileMap.draw();
				tileMap.getWindow().getOptionBar().setEditOverlaysEnabled(true);
				tileMap.getWindow().untrackDialog(this);
				initPersistentLocationOnClose(this);
				lastSelectedTab = tabs.getSelectionModel().getSelectedIndex();
			}
		});

		setResizable(true);

		// vbox in scrollpane
		// each overlay element is a borderpane
		// left: type, min, max
		// center: additional data
		// right: active, delete

		for (Overlay parser : overlays) {
			add(parser);
		}

		overlaysScrollPane.setContent(overlaysList);

		add.getStyleClass().add("overlay-add-label");
		add.setOnMouseReleased(e -> {
			InhabitedTimeOverlay newParser = new InhabitedTimeOverlay();
			overlays.add(newParser);
			add(newParser);
			tileMap.setOverlays(overlays);
			tileMap.setOverlay(newParser);
			tileMap.draw();
		});

		Button createScriptOverlay = UIFactory.button(Translation.DIALOG_EDIT_OVERLAYS_CREATE_OVERLAY);
		createScriptOverlay.setOnAction(e -> {
			if (codeEditor.save(false)) {
				ScriptOverlay overlay = new ScriptOverlay();
				overlay.setRawMultiValues(codeEditor.getSource().file().toString());
				overlays.add(overlay);
				add(overlay);
				tileMap.setOverlays(overlays);
				tileMap.setOverlay(overlay);
				tileMap.draw();
			}
		});

		VBox presets = new VBox();
		presets.getStyleClass().add("overlay-list");
		presets.getChildren().addAll(overlaysScrollPane, add);
		VBox.setVgrow(overlaysScrollPane, Priority.ALWAYS);

		VBox script = new VBox();
		VBox.setVgrow(codeEditor, Priority.ALWAYS);
		script.getChildren().addAll(codeEditor, createScriptOverlay);

		Tab presetsTab = UIFactory.tab(Translation.DIALOG_EDIT_OVERLAYS_TAB_PRESETS);
		presetsTab.setContent(presets);
		Tab scriptTab = UIFactory.tab(Translation.DIALOG_EDIT_OVERLAYS_TAB_SCRIPT);
		scriptTab.setContent(script);

		tabs.getTabs().addAll(presetsTab, scriptTab);

		getDialogPane().setContent(tabs);

		getDialogPane().setOnKeyPressed(e -> {
			switch (e.getCode()) {
				case O -> tileMap.nextOverlay();
				case N -> tileMap.nextOverlayType();
			}
		});

		select(tileMap.getOverlay());

		tileMap.overlayParserProperty().addListener(tileMapSelectedOverlayChange);

		tileMap.getWindow().getOptionBar().setEditOverlaysEnabled(false);
		tileMap.getWindow().trackDialog(this);

		initPersistentLocationOnOpen(this);
		Platform.runLater(() -> tabs.getSelectionModel().select(lastSelectedTab));
	}

	private void onTypeChange(Overlay oldValue, Overlay newValue) {
		int index = overlays.indexOf(oldValue);
		overlays.set(index, newValue);
		tileMap.clearOverlay();
		tileMap.setOverlays(overlays);
		tileMap.setOverlay(newValue);
		tileMap.draw();
	}

	private void onDelete(Overlay deleted) {
		int index = overlays.indexOf(deleted);
		overlays.remove(index);
		overlaysList.getChildren().remove(index);
		tileMap.setOverlays(overlays);
		tileMap.draw();
	}

	private void add(Overlay parser) {
		OverlayBox box = new OverlayBox(parser);
		box.setOnTypeChange(this::onTypeChange);
		box.setOnValuesChange(p -> {
			if (p.isActive()) {
				if (p != tileMap.getOverlay()) {
					tileMap.setOverlays(overlays);
				}
				tileMap.setOverlay(p);
				tileMap.draw();
			} else if (p == tileMap.getOverlay()) {
				tileMap.clearOverlay();
				tileMap.draw();
			}
		});
		box.setOnDelete(this::onDelete);
		overlaysList.getChildren().add(box);
	}

	private void select(Overlay parser) {
		for (Node child : overlaysList.getChildren()) {
			OverlayBox box = (OverlayBox) child;
			box.setSelected(box.valueProperty.get().same(parser));
		}
	}

	public static class Result {

		private final List<Overlay> overlays;

		public Result(List<Overlay> overlays) {
			this.overlays = overlays;
		}

		public List<Overlay> getOverlays() {
			return overlays;
		}

		@Override
		public String toString() {
			return Arrays.toString(overlays.toArray(new Overlay[0]));
		}
	}
}
