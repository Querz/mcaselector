package net.querz.mcaselector.ui.dialog;

import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.querz.mcaselector.Config;
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.property.DataProperty;
import net.querz.mcaselector.text.Translation;
import net.querz.mcaselector.tiles.TileMap;
import net.querz.mcaselector.tiles.overlay.InhabitedTimeParser;
import net.querz.mcaselector.tiles.overlay.OverlayParser;
import net.querz.mcaselector.ui.OverlayBox;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OverlayEditorDialog extends Dialog<OverlayEditorDialog.Result> {

	private static final Image addIcon = FileHelper.getIconFromResources("img/add");

	private List<OverlayParser> overlays;

	private final ScrollPane overlaysScrollPane = new ScrollPane();
	private final VBox overlaysList = new VBox();
	private final Label add = new Label("", new ImageView(addIcon));

	private final TileMap tileMap;

	private final DataProperty<Boolean> closedWithOK = new DataProperty<>(false);

	private ChangeListener<OverlayParser> tileMapSelectedOverlayChange = (v, o, n) -> {
		if (o != n) {
			select(n);
		}
	};

	public OverlayEditorDialog(Stage primaryStage, TileMap tileMap, List<OverlayParser> values) {
		if (values == null) {
			this.overlays = new ArrayList<>();
		}
		this.tileMap = tileMap;
		this.overlays = values;

		OverlayParser originalOverlay = tileMap.getOverlay() != null ? tileMap.getOverlay().clone() : null;
		List<OverlayParser> originalOverlays = tileMap.getOverlayParsers();

		titleProperty().bind(Translation.DIALOG_EDIT_OVERLAYS_TITLE.getProperty());
		initModality(Modality.NONE);
		initStyle(StageStyle.UTILITY);
		getDialogPane().getStyleClass().add("overlay-dialog-pane");
		setResultConverter(p -> p == ButtonType.OK ? new Result(overlays) : null);
		getDialogPane().getStylesheets().addAll(primaryStage.getScene().getStylesheets());
		getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
		getDialogPane().lookupButton(ButtonType.OK).addEventFilter(ActionEvent.ACTION, e -> {
			tileMap.setOverlays(overlays);
			Config.setOverlays(overlays);
			tileMap.getWindow().getOptionBar().setEditOverlaysEnabled(true);
			tileMap.getWindow().untrackDialog(this);
			closedWithOK.set(true);
		});
		setOnCloseRequest(e -> {
			tileMap.overlayParserProperty().removeListener(tileMapSelectedOverlayChange);
			if (!closedWithOK.get()) {
				tileMap.setOverlays(originalOverlays);
				tileMap.setOverlay(originalOverlay);
				tileMap.update();
				tileMap.getWindow().getOptionBar().setEditOverlaysEnabled(true);
				tileMap.getWindow().untrackDialog(this);
			}
		});

		setResizable(true);

		// vbox in scrollpane
		// each overlay element is a borderpane
		// left: type, min, max
		// center: additional data
		// right: active, delete

		for (OverlayParser parser : overlays) {
			add(parser);
		}

		overlaysScrollPane.setContent(overlaysList);

		add.getStyleClass().add("overlay-add-label");
		add.setOnMouseReleased(e -> {
			InhabitedTimeParser newParser = new InhabitedTimeParser();
			overlays.add(newParser);
			add(newParser);
			tileMap.setOverlays(overlays);
			tileMap.setOverlay(newParser);
			tileMap.update();
		});

		VBox content = new VBox();
		content.getStyleClass().add("overlay-list");
		content.getChildren().addAll(overlaysScrollPane, add);
		VBox.setVgrow(overlaysScrollPane, Priority.ALWAYS);

		getDialogPane().setContent(content);

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
	}

	private void onTypeChange(OverlayParser oldValue, OverlayParser newValue) {
		int index = overlays.indexOf(oldValue);
		overlays.set(index, newValue);
		tileMap.clearOverlay();
		tileMap.setOverlays(overlays);
		tileMap.setOverlay(newValue);
		tileMap.update();
	}

	private void onDelete(OverlayParser deleted) {
		int index = overlays.indexOf(deleted);
		overlays.remove(index);
		overlaysList.getChildren().remove(index);
		tileMap.setOverlays(overlays);
		tileMap.update();
	}

	private void add(OverlayParser parser) {
		OverlayBox box = new OverlayBox(parser);
		box.setOnTypeChange(this::onTypeChange);
		box.setOnValuesChange(p -> {
			if (p.isActive()) {
				if (p != tileMap.getOverlay()) {
					tileMap.setOverlays(overlays);
				}
				tileMap.setOverlay(p);
				tileMap.update();
			} else if (p == tileMap.getOverlay()) {
				tileMap.clearOverlay();
				tileMap.update();
			}
		});
		box.setOnDelete(this::onDelete);
		overlaysList.getChildren().add(box);
	}

	private void select(OverlayParser parser) {
		for (Node child : overlaysList.getChildren()) {
			OverlayBox box = (OverlayBox) child;
			box.setSelected(box.valueProperty.get().same(parser));
		}
	}

	public static class Result {

		private final List<OverlayParser> overlays;

		public Result(List<OverlayParser> overlays) {
			this.overlays = overlays;
		}

		public List<OverlayParser> getOverlays() {
			return overlays;
		}

		@Override
		public String toString() {
			return Arrays.toString(overlays.toArray(new OverlayParser[0]));
		}
	}
}
