package net.querz.mcaselector.cli;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class CLIJFX {

	public static void launch() throws ExecutionException, InterruptedException {
		CompletableFuture<Void> started = new CompletableFuture<>();
		javafx.application.Platform.startup(() -> started.complete(null));
		started.get();
	}

	public static boolean hasJavaFX() {
		try  {
			Class.forName("javafx.scene.paint.Color");
			return true;
		}  catch (ClassNotFoundException e) {
			return false;
		}
	}

}
