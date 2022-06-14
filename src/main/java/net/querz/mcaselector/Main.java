package net.querz.mcaselector;

import net.querz.mcaselector.cli.CLIJFX;
import net.querz.mcaselector.cli.ParamExecutor;
import net.querz.mcaselector.logging.Logging;
import net.querz.mcaselector.ui.Window;
import net.querz.mcaselector.text.Translation;
import net.querz.mcaselector.validation.ShutdownHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javax.swing.*;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class Main {

	public static void main(String[] args) throws ExecutionException, InterruptedException {
		Logging.setLogDir(Config.getLogDir());
		Logging.updateThreadContext();
		Logger LOGGER = LogManager.getLogger(Main.class);

		LOGGER.debug("java version {}", System.getProperty("java.version"));
		LOGGER.debug("jvm max memory {}", Runtime.getRuntime().maxMemory());

		ParamExecutor ex = new ParamExecutor(args);
		Future<Boolean> future = ex.run();
		if (future != null && future.get()) {
			System.exit(0);
		}

		if (!CLIJFX.hasJavaFX()) {
			JOptionPane.showMessageDialog(null, "Please install JavaFX for your Java version (" + System.getProperty("java.version") + ") to use MCA Selector.", "Missing JavaFX", JOptionPane.ERROR_MESSAGE);
			System.exit(0);
		}

		Config.loadFromIni();
		ShutdownHooks.addShutdownHook(Config::exportConfig);
		Translation.load(Config.getLocale());
		Locale.setDefault(Config.getLocale());

		Window.launch(Window.class, args);
	}
}
