package net.querz.mcaselector;

import net.querz.mcaselector.cli.CLIJFX;
import net.querz.mcaselector.cli.ParamExecutor;
import net.querz.mcaselector.config.Config;
import net.querz.mcaselector.config.ConfigProvider;
import net.querz.mcaselector.logging.Logging;
import net.querz.mcaselector.ui.Window;
import net.querz.mcaselector.text.Translation;
import net.querz.mcaselector.util.validation.ShutdownHooks;
import net.querz.mcaselector.version.VersionHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javax.swing.JOptionPane;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class Main {

	public static void main(String[] args) throws ExecutionException, InterruptedException {
		Logging.setLogDir(Config.BASE_LOG_DIR);
		Logging.updateThreadContext();
		Logger LOGGER = LogManager.getLogger(Main.class);

		LOGGER.debug("java version {}", System.getProperty("java.version"));
		LOGGER.debug("jvm max memory {}", Runtime.getRuntime().maxMemory());

		VersionHandler.init();
		ParamExecutor ex = new ParamExecutor(args);
		Future<Boolean> future = ex.run();
		if (future != null && future.get()) {
			System.exit(0);
		}

		if (!CLIJFX.hasJavaFX()) {
			JOptionPane.showMessageDialog(null, "Please install JavaFX for your Java version (" + System.getProperty("java.version") + ") to use MCA Selector.", "Missing JavaFX", JOptionPane.ERROR_MESSAGE);
			System.exit(0);
		}

		ConfigProvider.loadGlobalConfig();
		ConfigProvider.loadOverlayConfig();
		ShutdownHooks.addShutdownHook(ConfigProvider::saveAll);
		Translation.load(ConfigProvider.GLOBAL.getLocale());
		Locale.setDefault(ConfigProvider.GLOBAL.getLocale());

		Window.launch(Window.class, args);
	}
}
