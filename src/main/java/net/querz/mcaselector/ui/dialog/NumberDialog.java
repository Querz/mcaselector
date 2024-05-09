package net.querz.mcaselector.ui.dialog;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.querz.mcaselector.io.FileHelper;

public class NumberDialog extends Stage {

	private static final Image clipboardIcon = FileHelper.getIconFromResources("img/clipboard");

	public NumberDialog(Stage primaryStage, long number, String title) {
		initStyle(StageStyle.UTILITY);
		setResizable(false);
		initModality(Modality.APPLICATION_MODAL);
		initOwner(primaryStage);

		Label titleLabel = new Label(title);

		Label numberLabel = new Label(String.valueOf(number));
		numberLabel.getStyleClass().add("number-label");
		// the label should always stretch to fit the number
		numberLabel.setMinWidth(Region.USE_PREF_SIZE);

		ImageView icon = new ImageView(clipboardIcon);
		icon.setFitWidth(16);
		icon.setFitHeight(16);
		Button copyButton = new Button(null, icon);
		copyButton.setOnAction(e -> {
			ClipboardContent content = new ClipboardContent();
			content.putString(String.valueOf(number));
			Clipboard.getSystemClipboard().setContent(content);
		});

		HBox box = new HBox(numberLabel, copyButton);
		box.getStyleClass().add("number-box");

		VBox contentBox = new VBox(titleLabel, new Separator(), box);
		contentBox.getStyleClass().add("content-box");

		StackPane pane = new StackPane(contentBox);
		StackPane.setAlignment(contentBox, Pos.CENTER);
		pane.getStyleClass().add("dialog-pane");
		pane.getStylesheets().addAll(primaryStage.getScene().getStylesheets());

		Scene scene = new Scene(pane);
		scene.getStylesheets().add(NumberDialog.class.getClassLoader().getResource("style/component/number-dialog.css").toExternalForm());
		setScene(scene);

		Platform.runLater(numberLabel::requestFocus);
	}
}