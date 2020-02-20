package net.querz.mcaselector.validation;

import net.querz.mcaselector.debug.Debug;
import java.util.function.Supplier;

public class ValidationHelper {

	private ValidationHelper() {}

	public static <T> T withDefault(Supplier<T> s, T def) {
		try {
			return s.get();
		} catch (Exception ex) {
			Debug.dump(new Exception("validation error", ex));
			return def;
		}
	}

	public static <T> T catchClassCastException(Supplier<T> s) {
		try {
			return s.get();
		} catch (ClassCastException ex) {
			Debug.dump(new Exception("validation error", ex));
			return null;
		}
	}
}
