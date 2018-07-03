package net.querz.mcaselector.util;

import net.querz.mcaselector.Config;

public class Debug {

	public static void dump(Object... objects) {
		if (Config.debug()) {
			for (Object o : objects) {
				System.out.println(o);
			}
		}
	}

	public static void error(Object... objects) {
		for (Object o : objects) {
			if (o instanceof Exception) {
				((Exception) o).printStackTrace();
			} else {
				System.out.println(o);
			}
		}
	}
}
