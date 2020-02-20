package net.querz.mcaselector.debug;

import net.querz.mcaselector.Config;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Properties;

public final class Debug {

	private Debug() {}

	private static ExceptionInfo lastException = null;

	public static void dump(Object... objects) {
		if (Config.debug()) {
			for (Object o : objects) {
				if (o instanceof Exception) {
					if (lastException == null) {
						lastException = new ExceptionInfo((Exception) o);
						if (logWriter != null) {
							appendLogFile(getStacktraceAsString((Exception) o));
						}
						((Exception) o).printStackTrace();
					} else if (!lastException.equals((Exception) o)) {
						lastException.flush();
						lastException = new ExceptionInfo((Exception) o);
						System.out.println(lastException.getOneLine());
					} else {
						lastException.poke();
					}
				} else {
					if (logWriter != null) {
						appendLogFile(o.toString());
					}
					System.out.println(o);
				}
			}
		}
	}

	public static void dumpf(String format, Object... objects) {
		if (Config.debug()) {
			if (logWriter != null) {
				appendLogFile(String.format(format, objects));
			}
			System.out.printf(format + "\n", objects);
		}
	}

	public static void error(Object... objects) {
		for (Object o : objects) {
			if (o instanceof Exception) {
				if (lastException == null) {
					lastException = new ExceptionInfo((Exception) o);
					if (logWriter != null) {
						appendLogFile(getStacktraceAsString((Exception) o));
					}
					((Exception) o).printStackTrace();
				} else if (!lastException.equals((Exception) o)) {
					lastException.flush();
					lastException = new ExceptionInfo((Exception) o);
					System.out.println(lastException.getOneLine());
				} else {
					System.out.println(lastException.getOneLine());
					lastException.poke();
				}
			} else {
				if (logWriter != null) {
					appendLogFile(o.toString());
				}
				System.out.println(o);
			}
		}
	}

	public static void errorf(String format, Object... objects) {
		if (logWriter != null) {
			appendLogFile(String.format(format, objects));
		}
		System.out.printf(format + "\n", objects);
	}

	public static void print(Object... objects) {
		if (logWriter != null) {
			Arrays.stream(objects).forEach(o -> appendLogFile(o.toString()));
		}
		Arrays.stream(objects).forEach(System.out::println);
	}

	public static void printf(String format, Object... objects) {
		if (logWriter != null) {
			appendLogFile(String.format(format, objects));
		}
		System.out.printf(format + "\n", objects);
	}

	public static void dumpfToConsoleOnly(String format, Object... objects) {
		if (Config.debug()) {
			System.out.printf(format + "\n", objects);
		}
	}

	private static class LogWriter {
		BufferedWriter br;
		Thread shutdownHook;

		LogWriter() {
			try {
				br = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("debug.log")));
			} catch (IOException ex) {
				ex.printStackTrace();
				System.exit(0);
			}
			Runtime.getRuntime().addShutdownHook(shutdownHook = new Thread(this::close));
			new Thread(this::repeatedFlush).start();
		}

		public synchronized void close() {
			try {
				System.out.println("closing log file");
				if (br != null) {
					if (lastException != null) {
						lastException.flush();
					}
					try {
						br.flush();
						br.close();
					} catch (IOException ex) {
						ex.printStackTrace();
					}
				}
				try {
					Runtime.getRuntime().removeShutdownHook(shutdownHook);
				} catch (IllegalStateException ex) {
					// do nothing, we are already shutting down
				} catch (Exception ex) {
					System.out.println("failed to remove shutdown hook for log writer");
					ex.printStackTrace();
				}
			} finally {
				br = null;
			}
		}

		private void repeatedFlush() {
			while (br != null) {
				try {
					Thread.sleep(10000);
					br.flush();
				} catch (InterruptedException | IOException ex) {
					System.out.println("failed to flush debug log file");
					ex.printStackTrace();
				}
			}
		}
	}

	private static LogWriter logWriter;

	public static void initLogWriter() {
		if (logWriter == null) {
			logWriter = new LogWriter();
		}

		Properties sysProps = System.getProperties();
		sysProps.list(System.out);

		Debug.dumpf("os.name:                      %s", System.getProperty("os.name"));
		Debug.dumpf("os.version:                   %s", System.getProperty("os.version"));
		Debug.dumpf("proc.cores:                   %s", Runtime.getRuntime().availableProcessors());
		Debug.dumpf("java.version:                 %s", System.getProperty("java.version"));
		Debug.dumpf("java.vm.specification.vendor: %s", System.getProperty("java.vm.specification.vendor"));
		Debug.dumpf("jvm max mem:                  %d", Runtime.getRuntime().maxMemory());
	}

	public static void flushAndCloseLogWriter() {
		if (logWriter == null) {
			return;
		}
		logWriter.close();
		logWriter = null;
	}

	private static void appendLogFile(String s) {
		try {
			logWriter.br.write(s);
			logWriter.br.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static String getStacktraceAsString(Exception ex) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		ex.printStackTrace(pw);
		pw.flush();
		return sw.toString();
	}

	private static String getExceptionOneLine(Exception ex) {
		if (ex.getStackTrace().length == 0) {
			return ex.getMessage();
		}
		return String.format("%s: %s %s %s L%d",
				ex.getMessage(),
				ex.getStackTrace()[0].getFileName(),
				ex.getStackTrace()[0].getClassName(),
				ex.getStackTrace()[0].getMethodName(),
				ex.getStackTrace()[0].getLineNumber());
	}

	private static class ExceptionInfo {
		Class<? extends Exception> exception;
		int line;
		String file;
		long timestamp;
		String oneLine;
		int count = 0;

		ExceptionInfo(Exception ex) {
			exception = ex.getClass();
			if (ex.getStackTrace().length > 0) {
				line = ex.getStackTrace()[0].getLineNumber();
				file = ex.getStackTrace()[0].getFileName();
			} else {
				line = -1;
			}
			timestamp = System.currentTimeMillis();
			oneLine = getExceptionOneLine(ex);

			new Thread(() -> {
				try {
					Thread.sleep(10000);
					long remainingTime;
					while ((remainingTime = System.currentTimeMillis() - timestamp) < 10000) {
						Thread.sleep(remainingTime);
					}
					flush();
					lastException = null;
				} catch (InterruptedException e) {
					System.out.println("failed to flush exception");
					e.printStackTrace();
				}
			}).start();
		}

		boolean equals(Exception ex) {
			return exception == ex.getClass()
					&& (line > 0
					&& ex.getStackTrace().length > 0
					&& line == ex.getStackTrace()[0].getLineNumber()
					&& file.equals(ex.getStackTrace()[0].getFileName())
					|| line == -1
					&& ex.getStackTrace().length == 0);
		}

		void flush() {
			if (count > 0) {
				appendLogFile(" ... " + count + " more of " + lastException.getOneLine());
				System.out.println(lastException.getOneLine());
			}
		}

		void poke() {
			timestamp = System.currentTimeMillis();
			count++;
		}

		String getOneLine() {
			return oneLine;
		}
	}
}
