package net.querz.mcaselector;

import net.querz.mcaselector.ui.Window;
import java.util.Locale;

public class Main {

	public static void main(String[] args) {
		Locale.setDefault(Locale.ENGLISH);
		Config.importArgs(args);
		Window.launch(Window.class, args);
	}
}
