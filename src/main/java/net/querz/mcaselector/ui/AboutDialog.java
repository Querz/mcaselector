package net.querz.mcaselector.ui;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.querz.mcaselector.util.Debug;
import net.querz.mcaselector.util.GithubVersionChecker;
import net.querz.mcaselector.util.Helper;
import java.awt.Desktop;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.function.Consumer;

public class AboutDialog extends Alert {

	private static final Image githubMark = Helper.getIconFromResources("img/GitHub-Mark-Light-32px");

	private static Node persistentVersionCheckResult = new HBox();

	public AboutDialog(Stage primaryStage) {
		super(AlertType.INFORMATION, null, ButtonType.OK);
		initStyle(StageStyle.UTILITY);
		getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
		getDialogPane().getStyleClass().add("about-dialog-pane");
		setTitle("About");
		setHeaderText("About MCA Selector");

		GridPane grid = new GridPane();
		grid.getStyleClass().add("about-dialog-grid-pane");

		grid.add(new Label("Version:"), 0, 0);
		HBox versionLabel = new HBox();
		versionLabel.setAlignment(Pos.CENTER);
		String applicationVersion = "0";
		try {
			applicationVersion = Helper.getManifestAttributes().getValue("Application-Version");
			versionLabel.getChildren().add(new Label(applicationVersion));
		} catch (IOException ex) {
			versionLabel.getChildren().add(new Label("unknown"));
		}
		Button checkForUpdates = new Button("check");
		final String finalApplicationVersion = applicationVersion;
		checkForUpdates.setOnAction(
				e -> handleCheckUpdate(finalApplicationVersion,
						b -> versionLabel.getChildren().set(versionLabel.getChildren().size() - 1, b)));

		versionLabel.getChildren().add(checkForUpdates);
		versionLabel.getChildren().add(persistentVersionCheckResult);
		grid.add(versionLabel, 1, 0);
		grid.add(new Label("License:"), 0, 1);
		grid.add(new Label("MIT"), 1, 1);
		grid.add(new Label("Copyright:"), 0, 2);
		grid.add(new Label("\u00A9 2018 Querz"), 1, 2);
		grid.add(new Label("Source:"), 0, 3);
		ImageView imgView = new ImageView(githubMark);
		imgView.setScaleX(0.5);
		imgView.setScaleY(0.5);
		Hyperlink source = createHyperlink("GitHub", "https://github.com/Querz/mcaselector", imgView);
		grid.add(source, 1, 3);

		getDialogPane().setContent(grid);
		getDialogPane().getStylesheets().addAll(primaryStage.getScene().getStylesheets());
	}

	private void handleCheckUpdate(String applicationVersion, Consumer<Node> resultUIHandler) {
		Label checking = new Label("checking...");
		checking.getStyleClass().add("label-hint");
		resultUIHandler.accept(checking);

		// needs to run in separate thread so we can see the "checking..." label
		Thread lookup = new Thread(() -> {
			GithubVersionChecker checker = new GithubVersionChecker("Querz", "mcaselector");
			try {
				GithubVersionChecker.VersionData version = checker.fetchLatestVersion();
				if (version != null && version.isNewerThan(applicationVersion)) {
					HBox box = new HBox();
					Hyperlink download = createHyperlink(version.getTag(), version.getLink(), null);
					download.getStyleClass().add("hyperlink-update");
					Label arrow = new Label("-->");
					arrow.getStyleClass().add("label-hint");
					box.getChildren().addAll(arrow, download);
					persistentVersionCheckResult = box;
					Platform.runLater(() -> resultUIHandler.accept(box));
				} else {
					Label upToDate = new Label("Up to date");
					upToDate.getStyleClass().add("label-hint");
					persistentVersionCheckResult = upToDate;
					Platform.runLater(() -> resultUIHandler.accept(upToDate));
				}
			} catch (Exception ex) {
				Debug.error(ex.getMessage());
				Label error = new Label("Error");
				error.getStyleClass().add("label-hint");
				Platform.runLater(() -> resultUIHandler.accept(error));
			}
		});

		lookup.setDaemon(true);
		lookup.start();
	}

	private Hyperlink createHyperlink(String text, String url, Node graphic) {
		Hyperlink hyperlink;
		if (graphic == null) {
			hyperlink = new Hyperlink(text);
		} else {
			hyperlink = new Hyperlink(text, graphic);
		}
		hyperlink.setOnAction(e -> {
			if (Desktop.isDesktopSupported()) {
				try {
					Desktop.getDesktop().browse(new URL(url).toURI());
				} catch (IOException | URISyntaxException ex) {
					ex.printStackTrace();
				}
			}
		});
		return hyperlink;
	}
}
