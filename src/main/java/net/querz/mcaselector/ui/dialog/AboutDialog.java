package net.querz.mcaselector.ui.dialog;

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
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.github.VersionChecker;
import net.querz.mcaselector.text.Translation;
import net.querz.mcaselector.ui.UIFactory;
import java.io.IOException;
import java.util.function.Consumer;

public class AboutDialog extends Alert {

	private static final Image githubMark = FileHelper.getIconFromResources("img/GitHub-Mark-Light-32px");

	private static Node persistentVersionCheckResult = new HBox();

	public AboutDialog(Stage primaryStage) {
		super(AlertType.INFORMATION, null, ButtonType.OK);
		initStyle(StageStyle.UTILITY);
		getDialogPane().getStylesheets().add(AboutDialog.class.getClassLoader().getResource("style/component/about-dialog.css").toExternalForm());
		getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
		getDialogPane().getStyleClass().add("about-dialog-pane");
		titleProperty().bind(Translation.DIALOG_ABOUT_TITLE.getProperty());
		headerTextProperty().bind(Translation.DIALOG_ABOUT_HEADER.getProperty());

		GridPane grid = new GridPane();
		grid.getStyleClass().add("about-dialog-grid-pane");

		grid.add(UIFactory.label(Translation.DIALOG_ABOUT_VERSION), 0, 0);
		HBox versionLabel = new HBox();
		versionLabel.setAlignment(Pos.CENTER_LEFT);
		String applicationVersion = "0";
		try {
			applicationVersion = FileHelper.getManifestAttributes().getValue("Application-Version");
			versionLabel.getChildren().add(new Label(applicationVersion));
		} catch (IOException ex) {
			versionLabel.getChildren().add(UIFactory.label(Translation.DIALOG_ABOUT_VERSION_UNKNOWN));
		}
		Button checkForUpdates = UIFactory.button(Translation.DIALOG_ABOUT_VERSION_CHECK);
		final String finalApplicationVersion = applicationVersion;
		checkForUpdates.setOnAction(
				e -> handleCheckUpdate(finalApplicationVersion,
						b -> versionLabel.getChildren().set(versionLabel.getChildren().size() - 1, b)));

		versionLabel.getChildren().add(checkForUpdates);
		versionLabel.getChildren().add(persistentVersionCheckResult);
		grid.add(versionLabel, 1, 0);
		grid.add(UIFactory.label(Translation.DIALOG_ABOUT_LICENSE), 0, 1);
		grid.add(new Label("MIT"), 1, 1);
		grid.add(UIFactory.label(Translation.DIALOG_ABOUT_COPYRIGHT), 0, 2);
		grid.add(new Label("\u00A9 2018 - 2024 Querz"), 1, 2);
		grid.add(UIFactory.label(Translation.DIALOG_ABOUT_SOURCE), 0, 3);
		ImageView imgView = new ImageView(githubMark);
		imgView.setScaleX(0.5);
		imgView.setScaleY(0.5);
		Hyperlink source = UIFactory.hyperlink("GitHub", "https://github.com/Querz/mcaselector", imgView);
		grid.add(source, 1, 3);

		getDialogPane().setContent(grid);
		getDialogPane().getStylesheets().addAll(primaryStage.getScene().getStylesheets());
	}

	private void handleCheckUpdate(String applicationVersion, Consumer<Node> resultUIHandler) {
		Label checking = UIFactory.label(Translation.DIALOG_ABOUT_VERSION_CHECKING);
		checking.getStyleClass().add("label-hint");
		resultUIHandler.accept(checking);

		// needs to run in separate thread so we can see the "checking..." label
		Thread lookup = new Thread(() -> {
			VersionChecker checker = new VersionChecker("Querz", "mcaselector");
			try {
				VersionChecker.VersionData version = checker.fetchLatestVersion();
				if (version != null && version.isNewerThan(applicationVersion)) {
					HBox box = new HBox();
					String hyperlinkText = version.getTag() + (version.isPrerelease() ? " (pre)" : "");
					Hyperlink download = UIFactory.hyperlink(hyperlinkText, version.getLink(), null);
					download.getStyleClass().add("hyperlink-update");
					Label arrow = new Label("\u2192");
					arrow.getStyleClass().add("label-hint");
					box.getChildren().addAll(arrow, download);
					persistentVersionCheckResult = box;
					Platform.runLater(() -> resultUIHandler.accept(box));
				} else {
					Label upToDate = UIFactory.label(Translation.DIALOG_ABOUT_VERSION_UP_TO_DATE);
					upToDate.getStyleClass().add("label-hint");
					persistentVersionCheckResult = upToDate;
					Platform.runLater(() -> resultUIHandler.accept(upToDate));
				}
			} catch (Exception ex) {
				Label error = UIFactory.label(Translation.DIALOG_ABOUT_VERSION_ERROR);
				error.getStyleClass().add("label-hint");
				Platform.runLater(() -> resultUIHandler.accept(error));
			}
		});

		lookup.setDaemon(true);
		lookup.start();
	}
}
