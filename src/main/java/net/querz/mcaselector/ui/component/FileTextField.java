package net.querz.mcaselector.ui.component;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import net.querz.mcaselector.config.ConfigProvider;
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.property.DataProperty;
import net.querz.mcaselector.ui.DialogHelper;

import java.io.File;

public class FileTextField extends HBox {

	private static final Image openIcon = FileHelper.getIconFromResources("img/folder");

	private final TextField textField = new TextField();
	private final Button openButton = new Button(null, new ImageView(openIcon));

	public FileTextField() {
		getStyleClass().add("file-text-field");
		getStylesheets().add(FileTextField.class.getClassLoader().getResource("style/component/file-text-field.css").toExternalForm());


		HBox.setHgrow(textField, Priority.ALWAYS);

		openButton.setOnAction(e -> {
			// either open the directory from the text field if it's valid or open the default directory
			String lastOpenDirectory = null;
			if (textField.getText() != null && !textField.getText().isEmpty()) {
				File fieldFile = new File(textField.getText());
				if (fieldFile.exists() && fieldFile.isDirectory()) {
					lastOpenDirectory = fieldFile + "";
				}
			}
			if (lastOpenDirectory == null || lastOpenDirectory.isEmpty()) {
				lastOpenDirectory = FileHelper.getLastOpenedDirectory("open_world", ConfigProvider.GLOBAL.getMcSavesDir());
			}

			File file = DialogHelper.createDirectoryChooser(lastOpenDirectory).showDialog(getScene().getWindow());

			if (file != null) {
				textField.setText(file + "");
			}
		});

		DataProperty<Integer> location = new DataProperty<>(0);
		textField.textProperty().addListener((observable, oldValue, newValue) ->
				location.set(textField.getText().length()));
		textField.focusedProperty().addListener((observable, oldValue, newValue) ->
				Platform.runLater(() -> textField.positionCaret(location.get())));

		getChildren().addAll(textField, openButton);
	}

	public void setFile(File initFile) {
		if (initFile != null) {
			textField.setText(initFile + "");
		}
	}

	public File getFile() {
		String text = textField.getText();
		if (text == null || text.isEmpty()) {
			return null;
		}
		return new File(text);
	}
}
