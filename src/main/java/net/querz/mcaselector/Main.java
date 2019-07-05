package net.querz.mcaselector;

import net.querz.mcaselector.ui.Window;
import net.querz.mcaselector.util.Debug;
import net.querz.mcaselector.util.Translation;

import java.util.Locale;

public class Main {

	public static void main(String[] args) {
		Config.loadFromIni();
		Translation.load(Config.getLocale());
		Locale.setDefault(Config.getLocale());
		Debug.dumpf("java version: %s", System.getProperty("java.version"));
		Debug.dumpf("jvm max mem:  %d", Runtime.getRuntime().maxMemory());
		Runtime.getRuntime().addShutdownHook(new Thread(Config::exportConfig));
		Window.launch(Window.class, args);
	}
}
