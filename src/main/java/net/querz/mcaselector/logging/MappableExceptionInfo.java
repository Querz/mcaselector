package net.querz.mcaselector.logging;

import java.util.Objects;

public class MappableExceptionInfo {

	private Class<? extends Throwable> clazz;

	private final int line;
	private final String file;

	private int hashCode;

	public MappableExceptionInfo(Throwable t) {
		clazz = t.getClass();
		if (t.getCause() == null) {
			if (t.getStackTrace().length > 0) {
				StackTraceElement element = t.getStackTrace()[0];
				line = element.getLineNumber();
				file = element.getFileName();
			} else {
				line = -1;
				file = null;
			}
		} else {
			// get root cause
			Throwable c = t;
			while (c.getCause() != null) {
				c = c.getCause();
			}
			if (c.getStackTrace().length > 0) {
				StackTraceElement element = c.getStackTrace()[0];
				line = element.getLineNumber();
				file = element.getFileName();
			} else {
				line = -1;
				file = null;
			}
		}
	}

	protected String getExceptionOneLine(Throwable t) {
		if (t.getStackTrace().length == 0) {
			return t.getMessage();
		}
		StackTraceElement element = t.getStackTrace()[0];
		String trace = String.format("%s: %s %s %s L%d",
				t.getMessage(),
				element.getFileName(),
				element.getClassName(),
				element.getMethodName(),
				element.getLineNumber());

		if (t.getCause() != null) {
			// get root cause
			Throwable c = t;
			while (c.getCause() != null) {
				c = c.getCause();
			}

			StackTraceElement cause = c.getStackTrace()[0];
			return String.format("%s, cause: %s: %s %s %s L%s",
					trace,
					c.getMessage(),
					cause.getFileName(),
					cause.getClassName(),
					cause.getMethodName(),
					cause.getLineNumber());
		} else {
			return trace;
		}
	}

	@Override
	public int hashCode() {
		if (hashCode == 0) {
			return hashCode = Objects.hash(clazz, line, file);
		}
		return hashCode;
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof MappableExceptionInfo)) {
			return false;
		}
		MappableExceptionInfo ex = (MappableExceptionInfo) other;
		return clazz == ex.clazz && line == ex.line && (file == null && ex.file == null || file != null && file.equals(ex.file));
	}
}
