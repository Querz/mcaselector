package net.querz.mcaselector.util;

import net.querz.mcaselector.Config;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Debug {

	public static void dump(Object... objects) {
		if (Config.debug()) {
			for (Object o : objects) {
				System.out.println(new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date()) + ": " + o);
			}
		}
	}

	public static void dumpf(String format, Object... objects) {
		if (Config.debug()) {
			System.out.printf(new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date()) + ": " + format + "\n", objects);
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

	public static void errorf(String format, Object... objects) {
		System.out.printf(format + "\n", objects);
	}
}
