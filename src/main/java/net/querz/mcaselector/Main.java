package net.querz.mcaselector;

import net.querz.mcaselector.headless.ParamExecutor;
import net.querz.mcaselector.ui.Window;
import net.querz.mcaselector.util.Debug;
import net.querz.mcaselector.util.Translation;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class Main {

	public static void main(String[] args) throws ExecutionException, InterruptedException {
		Debug.dumpf("java version: %s", System.getProperty("java.version"));
		Debug.dumpf("jvm max mem:  %d", Runtime.getRuntime().maxMemory());

		Future<Boolean> headless = new ParamExecutor(args).parseAndRun();
		if (headless != null && headless.get()) {
			// we already ran headless mode, so we exit here
			System.out.println("exiting");
			System.exit(0);
		}

		Config.loadFromIni();
		Translation.load(Config.getLocale());
		Locale.setDefault(Config.getLocale());
		Runtime.getRuntime().addShutdownHook(new Thread(Config::exportConfig));
		Window.launch(Window.class, args);
	}
}
