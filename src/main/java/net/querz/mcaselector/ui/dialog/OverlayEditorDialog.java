package net.querz.mcaselector.ui.dialog;

import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.text.Translation;
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

	public OverlayEditorDialog(Stage primaryStage, List<OverlayParser> values) {
		if (values == null) {
			this.overlays = new ArrayList<>();
		}
		this.overlays = values;
		titleProperty().bind(Translation.DIALOG_EDIT_OVERLAYS_TITLE.getProperty());
		initStyle(StageStyle.UTILITY);
		getDialogPane().getStyleClass().add("overlay-dialog-pane");
		setResultConverter(p -> p == ButtonType.OK ? new Result(overlays) : null);
		getDialogPane().getStylesheets().addAll(primaryStage.getScene().getStylesheets());
		getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

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
		});

		VBox content = new VBox();
		content.getStyleClass().add("overlay-list");
		content.getChildren().addAll(overlaysScrollPane, add);

		getDialogPane().setContent(content);
	}

	private void onTypeChange(OverlayParser oldValue, OverlayParser newValue) {
		int index = overlays.indexOf(oldValue);
		overlays.set(index, newValue);
	}

	private void onDelete(OverlayParser deleted) {
		int index = overlays.indexOf(deleted);
		overlays.remove(index);
		overlaysList.getChildren().remove(index);
	}

	private void add(OverlayParser parser) {
		OverlayBox box = new OverlayBox(parser);
		box.setOnTypeChange(this::onTypeChange);
		box.setOnDelete(this::onDelete);
		overlaysList.getChildren().add(box);
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
