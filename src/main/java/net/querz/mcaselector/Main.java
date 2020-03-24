package net.querz.mcaselector;

import com.sun.javafx.application.PlatformImpl;
import javafx.application.Platform;
import net.querz.mcaselector.headless.ParamExecutor;
import net.querz.mcaselector.ui.Window;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.text.Translation;

import java.lang.reflect.Field;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {

	public static void main(String[] args) throws ExecutionException, InterruptedException {
		Debug.dumpf("java version: %s", System.getProperty("java.version"));
		Debug.dumpf("jvm max mem:  %d", Runtime.getRuntime().maxMemory());

		Future<Boolean> headless = new ParamExecutor(args).parseAndRun();
		if (headless != null && headless.get()) {
			// we already ran headless mode, so we exit here
			Debug.print("exiting");
			System.exit(0);
		}

		Config.loadFromIni();
		Runtime.getRuntime().addShutdownHook(new Thread(Config::exportConfig));

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {

			try {
				Field pendingRunnables = PlatformImpl.class.getDeclaredField("pendingRunnables");

				pendingRunnables.setAccessible(true);

				AtomicInteger i = (AtomicInteger) pendingRunnables.get(null);

				System.out.println("pendingRunnables: " + i.get());


			} catch (Exception e) {
				e.printStackTrace();
			}

		}));


		if (Config.debug()) {
			Debug.initLogWriter();
		}
		Translation.load(Config.getLocale());
		Locale.setDefault(Config.getLocale());
		Window.launch(Window.class, args);
	}
}
