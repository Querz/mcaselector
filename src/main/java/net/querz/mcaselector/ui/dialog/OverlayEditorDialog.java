package net.querz.mcaselector.ui.dialog;

import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.querz.mcaselector.text.Translation;
import net.querz.mcaselector.tiles.overlay.InhabitedTimeParser;
import net.querz.mcaselector.tiles.overlay.OverlayParser;
import net.querz.mcaselector.ui.OverlayBox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OverlayEditorDialog extends Dialog<OverlayEditorDialog.Result> {

	private static List<OverlayParser> overlays = new ArrayList<>();

	static {
		overlays.add(new InhabitedTimeParser());
	}

	private final ScrollPane overlaysScrollPane = new ScrollPane();
	private final VBox overlaysList = new VBox();

	public OverlayEditorDialog(Stage primaryStage) {
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
			OverlayBox box = new OverlayBox(parser);
			box.setOnTypeChange(this::onTypeChange);
			overlaysList.getChildren().add(box);
		}

		overlaysScrollPane.setContent(overlaysList);

		getDialogPane().setContent(overlaysScrollPane);
	}

	private void onTypeChange(OverlayParser oldValue, OverlayParser newValue) {
		int index = overlays.indexOf(oldValue);
		overlays.set(index, newValue);
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
