package net.querz.mcaselector.ui.dialog;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import net.querz.mcaselector.text.Translation;

class BorderedTitledPane extends StackPane {

	BorderedTitledPane(Translation titleString, Node content) {
		Label title = new Label(" " + titleString.toString() + " ");
		title.getStyleClass().remove("label");
		title.getStyleClass().add("bordered-titled-title");
		StackPane.setAlignment(title, Pos.TOP_LEFT);

		StackPane contentPane = new StackPane();
		content.getStyleClass().add("bordered-titled-content");
		StackPane.setAlignment(content, Pos.CENTER_LEFT);
		contentPane.getChildren().add(content);

		getStyleClass().add("bordered-titled-border");
		getChildren().addAll(title, contentPane);
	}
}