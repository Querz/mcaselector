package net.querz.mcaselector.ui;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.querz.mcaselector.util.Helper;
import java.awt.Desktop;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

public class AboutDialog extends Alert {

	private static final Image githubMark = Helper.getIconFromResources("img/GitHub-Mark-Light-32px");

	public AboutDialog(Stage primaryStage) {
		super(AlertType.INFORMATION, null, ButtonType.OK);
		initStyle(StageStyle.UTILITY);
		getDialogPane().getStyleClass().add("about-dialog-pane");
		setTitle("About");
		setHeaderText("MCA Selector by Querz");

		GridPane grid = new GridPane();
		grid.getStyleClass().add("about-dialog-grid-pane");

		StringBuilder contentText = new StringBuilder("Version: ");
		grid.add(new Label("Version:"), 0, 0);
		try {
			grid.add(new Label(Helper.getManifestAttributes().getValue("Application-Version")), 1, 0);
		} catch (IOException ex) {
			grid.add(new Label("unknown"), 1, 0);
		}
		grid.add(new Label("License:"), 0, 1);
		grid.add(new Label("MIT"), 1, 1);
		grid.add(new Label("Copyright:"), 0, 2);
		grid.add(new Label("\u00A9 2018 Querz"), 1, 2);
		grid.add(new Label("Source:"), 0, 3);
		ImageView imgView = new ImageView(githubMark);
		imgView.setScaleX(0.5);
		imgView.setScaleY(0.5);
		Hyperlink source = new Hyperlink("GitHub", imgView);
		source.setOnAction(e -> {
			if (Desktop.isDesktopSupported()) {
				try {
					Desktop.getDesktop().browse(new URL("https://github.com/Querz/mcaselector").toURI());
				} catch (IOException | URISyntaxException ex) {
					ex.printStackTrace();
				}
			}
		});
		grid.add(source, 1, 3);
		getDialogPane().setContent(grid);
		getDialogPane().getStylesheets().addAll(primaryStage.getScene().getStylesheets());
	}
}
