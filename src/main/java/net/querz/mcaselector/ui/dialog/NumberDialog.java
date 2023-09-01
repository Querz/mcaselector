package net.querz.mcaselector.ui.dialog;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
public class NumberDialog extends Stage {

    public NumberDialog(Stage primaryStage, int number) {
        initStyle(StageStyle.UTILITY);
        setResizable(false);
        initModality(Modality.APPLICATION_MODAL);
        initOwner(primaryStage);

        Label numberLabel = new Label(String.valueOf(number));
        StackPane.setAlignment(numberLabel, Pos.CENTER);

        StackPane pane = new StackPane(numberLabel);
        pane.getStyleClass().add("dialog-pane");
        pane.getStylesheets().addAll(primaryStage.getScene().getStylesheets());

        Scene scene = new Scene(pane, 100, 30);
        setScene(scene);
    }
}