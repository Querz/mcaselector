package net.querz.mcaselector.logging;

import org.apache.logging.log4j.Logger;

public class ExceptionInfo extends MappableExceptionInfo {

	private final Throwable throwable;

	long timestamp;
	private int count;

	public ExceptionInfo(Throwable t) {
		super(t);
		throwable = t;
		timestamp = System.currentTimeMillis();
		count = 0;
	}

	public void log(Logger logger) {
		if (count > 0) {
			logger.warn(" ... {} more of {}", count, getExceptionOneLine(throwable));
		}
	}

	public void update() {
		timestamp = System.currentTimeMillis();
		count++;
	}
}
