package net.querz.mcaselector.ui.dialog;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import net.querz.mcaselector.text.TextHelper;
import net.querz.mcaselector.text.Translation;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.Objects;

public class ErrorDialog extends Alert {

	private final VBox content = new VBox();

	public ErrorDialog(Window owner, String errorMessage) {
		super(AlertType.ERROR, null, ButtonType.CLOSE);
		init(owner);
		setContentText(errorMessage);
		showAndWait();
	}

	// adds a copy-to-clipboard button
	public ErrorDialog(Window owner, Exception ex) {
		super(AlertType.ERROR, null, ButtonType.CLOSE);
		init(owner);
		getDialogPane().getStylesheets().add(Objects.requireNonNull(ErrorDialog.class.getClassLoader().getResource("style/component/error-dialog.css")).toExternalForm());

		String errorMessage = TextHelper.getStacktraceAsString(ex);
		ButtonType copyToClipboard = new ButtonType(Translation.DIALOG_ERROR_BUTTON_COPY_TO_CLIPBOARD.toString(), ButtonBar.ButtonData.BACK_PREVIOUS);
		getDialogPane().getButtonTypes().addFirst(copyToClipboard);

		TextArea errorText = new TextArea();
		errorText.setEditable(false);
		errorText.setText(errorMessage);

		Label copiedToClipboard = new Label();
		copiedToClipboard.getStyleClass().add("copied-to-clipboard-label");

		getDialogPane().lookupButton(copyToClipboard).addEventFilter(ActionEvent.ACTION, e -> {
			copyTextToClipboard(errorMessage);
			showLabelTextForXSeconds(copiedToClipboard, Translation.DIALOG_ERROR_COPIED_TO_CLIPBOARD.toString(), 3);
			e.consume();
		});

		content.getChildren().addAll(errorText, copiedToClipboard);
		getDialogPane().setContent(content);
		showAndWait();
	}

	private void init(Window primaryStage) {
		initStyle(StageStyle.UTILITY);
		getDialogPane().getStyleClass().add("error-dialog-pane");
		titleProperty().bind(Translation.DIALOG_ERROR_TITLE.getProperty());
		headerTextProperty().bind(Translation.DIALOG_ERROR_HEADER.getProperty());
		getDialogPane().getStylesheets().addAll(primaryStage.getScene().getStylesheets());
	}

	private void copyTextToClipboard(String text) {
		StringSelection stringSelection = new StringSelection(text);
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents(stringSelection, null);
	}

	private boolean showingCopiedToClipboardText = false;

	private void showLabelTextForXSeconds(Label label, String text, int seconds) {
		if (showingCopiedToClipboardText) {
			return;
		}
		showingCopiedToClipboardText = true;
		label.setText(text);
		Thread t = new Thread(() -> {
			try {
				Thread.sleep(seconds * 1000L);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			Platform.runLater(() -> {
				label.setText(null);
				showingCopiedToClipboardText = false;
			});
		});
		t.start();
	}
}
