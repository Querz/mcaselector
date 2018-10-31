package net.querz.mcaselector;

import net.querz.mcaselector.ui.Window;
import net.querz.mcaselector.util.Debug;
import java.util.Locale;

public class Main {

	public static void main(String[] args) {
		Locale.setDefault(Locale.ENGLISH);
		Config.loadFromIni();
		Debug.dumpf("java version: %s", System.getProperty("java.version"));
		Debug.dumpf("jvm max mem:  %d", Runtime.getRuntime().maxMemory());
		Runtime.getRuntime().addShutdownHook(new Thread(Config::exportConfig));
		Window.launch(Window.class, args);
	}
}
