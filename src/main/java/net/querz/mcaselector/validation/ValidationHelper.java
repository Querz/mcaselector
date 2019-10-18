package net.querz.mcaselector.validation;

import net.querz.mcaselector.debug.Debug;
import java.util.function.Supplier;

public class ValidationHelper {

	private ValidationHelper() {}

	public static <T> T withDefault(Supplier<T> s, T def) {
		try {
			return s.get();
		} catch (Exception ex) {
			if (ex.getStackTrace().length > 0) {
				Debug.dumpf("validation error: %s (%s) at L%d in %s#%s",
						ex.getMessage(),
						ex.getClass().getName(),
						ex.getStackTrace()[0].getLineNumber(),
						ex.getStackTrace()[0].getFileName(),
						ex.getStackTrace()[0].getMethodName());
			}
			return def;
		}
	}

	public static <T> T catchClassCastException(Supplier<T> s) {
		try {
			return s.get();
		} catch (ClassCastException ex) {
			if (ex.getStackTrace().length > 0) {
				Debug.dumpf("validation error: %s (%s) at L%d in %s#%s",
						ex.getMessage(),
						ex.getClass().getName(),
						ex.getStackTrace()[0].getLineNumber(),
						ex.getStackTrace()[0].getFileName(),
						ex.getStackTrace()[0].getMethodName());
			}
			return null;
		}
	}
}
