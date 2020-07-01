package net.querz.mcaselector;

import net.querz.mcaselector.headless.HeadlessHelper;
import net.querz.mcaselector.headless.ParamExecutor;
import net.querz.mcaselector.ui.Window;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.text.Translation;
import javax.swing.*;
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
			Debug.print("exiting");
			System.exit(0);
		}

		if (!HeadlessHelper.hasJavaFX()) {
			JOptionPane.showMessageDialog(null, "Please install JavaFX for your Java version (" + System.getProperty("java.version") + ") to use MCA Selector.", "Missing JavaFX", JOptionPane.ERROR_MESSAGE);
			System.exit(0);
		}

		Config.loadFromIni();
		Runtime.getRuntime().addShutdownHook(new Thread(Config::exportConfig));
		if (Config.debug()) {
			Debug.initLogWriter();
		}
		Translation.load(Config.getLocale());
		Locale.setDefault(Config.getLocale());
		Window.launch(Window.class, args);
	}
}
