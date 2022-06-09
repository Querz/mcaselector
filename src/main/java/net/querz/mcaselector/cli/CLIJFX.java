package net.querz.mcaselector.cli;

import javafx.application.Platform;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class CLIJFX {

	public static void launch() throws ExecutionException, InterruptedException {
		CompletableFuture<Void> started = new CompletableFuture<>();
		Platform.startup(() -> started.complete(null));
		started.get();
	}
}
