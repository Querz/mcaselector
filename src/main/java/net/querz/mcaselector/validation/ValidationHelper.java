package net.querz.mcaselector.validation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.function.Supplier;

public class ValidationHelper {

	private static final Logger LOGGER = LogManager.getLogger(ValidationHelper.class);

	private ValidationHelper() {}

	public static <T> T withDefault(Supplier<T> s, T def) {
		try {
			return s.get();
		} catch (Exception ex) {
			LOGGER.warn("validation error", ex);
			return def;
		}
	}

	public static <T> T silent(Supplier<T> s, T def) {
		try {
			return s.get();
		} catch (Exception ex) {
			return def;
		}
	}

	public static <T> T catchClassCastException(Supplier<T> s) {
		try {
			return s.get();
		} catch (ClassCastException ex) {
			LOGGER.warn("validation error", ex);
			return null;
		}
	}
}
