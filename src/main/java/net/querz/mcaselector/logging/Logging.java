package net.querz.mcaselector.logging;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;

import java.io.File;

public final class Logging {

	private Logging() {}

	public static final String TRACE = "TRACE";
	public static final String DEBUG = "DEBUG";
	public static final String INFO = "INFO";
	public static final String WARN = "WARN";
	public static final String ERROR = "ERROR";
	public static final String FATAL = "FATAL";

	private static final String dynamicLogLevelKey = "dynamicLogLevel";

	private static String logLevel = INFO;

	public static void setLogLevel(String logLevel) {
		Logging.logLevel = logLevel;
		LoggerContext ctx = (LoggerContext) LogManager.getContext(LogManager.class.getClassLoader(), false);
		Configuration config = ctx.getConfiguration();
		LoggerConfig loggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
		loggerConfig.setLevel(fromString(logLevel));
		ctx.updateLoggers();
	}

	public static String getLogLevel() {
		return logLevel;
	}

	public static void updateThreadContext() {
		ThreadContext.put(dynamicLogLevelKey, logLevel);
	}

	public static void setLogDir(File dir) {
		System.setProperty("logDir", dir.getAbsolutePath());
	}

	private static Level fromString(String level) {
		return switch (level) {
			case TRACE -> Level.TRACE;
			case DEBUG -> Level.DEBUG;
			case INFO -> Level.INFO;
			case WARN -> Level.WARN;
			case ERROR -> Level.ERROR;
			case FATAL -> Level.FATAL;
			default -> throw new IllegalArgumentException("invalid log level string " + level);
		};
	}
}
