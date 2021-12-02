package net.querz.mcaselector.headless;

import javafx.application.Platform;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class HeadlessJFX {

	public static void launch() throws ExecutionException, InterruptedException {
		CompletableFuture<Void> started = new CompletableFuture<>();
		Platform.startup(() -> started.complete(null));
		started.get();
	}
}
