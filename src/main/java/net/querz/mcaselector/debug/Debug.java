package net.querz.mcaselector.debug;

import net.querz.mcaselector.Config;
import net.querz.mcaselector.text.TextHelper;
import net.querz.mcaselector.validation.ShutdownHooks;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class Debug {

	private static final Map<ExceptionInfo, ExceptionInfo> lastExceptions = new ConcurrentHashMap<>();

	public static boolean enablePrinting = false;

	private Debug() {}

	public static void dump(Object... objects) {
		try {
			if (Config.debug()) {
				for (Object o : objects) {
					if (o instanceof Exception) {
						ExceptionInfo info = new ExceptionInfo((Exception) o);
						if (!lastExceptions.containsKey(info)) {
							lastExceptions.put(info, info);
							if (logWriter != null) {
								appendLogFile(TextHelper.getStacktraceAsString((Exception) o));
							}
							((Exception) o).printStackTrace();
						} else {
							// poke original instance
							lastExceptions.get(info).poke();
						}
					} else {
						if (logWriter != null) {
							appendLogFile(o.toString());
						}
						if (enablePrinting) {
							System.out.println(timestamp() + o);
						}
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static void dumpf(String format, Object... objects) {
		try {
			if (Config.debug()) {
				if (logWriter != null) {
					appendLogFile(String.format(format, objects));
				}
				if (enablePrinting) {
					System.out.printf(timestamp() + format + "\n", objects);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static void error(Object... objects) {
		try {
			for (Object o : objects) {
				if (o instanceof Exception) {
					ExceptionInfo info = new ExceptionInfo((Exception) o);
					if (!lastExceptions.containsKey(info)) {
						lastExceptions.put(info, info);
						if (logWriter != null) {
							appendLogFile(TextHelper.getStacktraceAsString((Exception) o));
						}
						((Exception) o).printStackTrace();
					} else {
						// poke original instance
						lastExceptions.get(info).poke();
					}
				} else {
					if (logWriter != null) {
						appendLogFile(o.toString());
					}
					System.out.println(timestamp() + o);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private static String timestamp() {
		LocalDateTime now = LocalDateTime.now();
		return String.format("[%02d:%02d:%02d.%03d] ", now.getHour(), now.getMinute(), now.getSecond(), now.get(ChronoField.MILLI_OF_SECOND));
	}

	public static void dumpException(String msg, Exception ex) {
		error(new Exception(msg), ex);
	}

	public static void errorf(String format, Object... objects) {
		try {
			if (logWriter != null) {
				appendLogFile(String.format(format, objects));
			}
			System.out.printf(timestamp() + format + "\n", objects);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static void print(Object... objects) {
		try {
			if (logWriter != null) {
				Arrays.stream(objects).forEach(o -> appendLogFile(o.toString()));
			}
			if (enablePrinting) {
				Arrays.stream(objects).forEach(x -> System.out.println(timestamp() + x));
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static void printf(String format, Object... objects) {
		try {
			if (logWriter != null) {
				appendLogFile(String.format(format, objects));
			}
			if (enablePrinting) {
				System.out.printf(timestamp() + format + "\n", objects);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static void dumpfToConsoleOnly(String format, Object... objects) {
		try {
			if (Config.debug()) {
				System.out.printf(timestamp() + format + "\n", objects);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private static class LogWriter {
		BufferedWriter br;
		ShutdownHooks.ShutdownJob shutdownHook;

		LogWriter() {
			try {
				br = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(Config.getLogFile())));
			} catch (IOException ex) {
				ex.printStackTrace();
				System.exit(0);
			}
			// logs need to be closed last, because other shutdown hooks might need logs
			shutdownHook = ShutdownHooks.addShutdownHook(this::close, 0);
			new Thread(this::repeatedFlush).start();
		}

		public synchronized void close() {
			try {
				System.out.println("closing log file");
				if (br != null) {
					if (!lastExceptions.isEmpty()) {
						for (ExceptionInfo info : lastExceptions.keySet()) {
							info.flush();
						}
						lastExceptions.clear();
					}
					try {
						br.flush();
						br.close();
					} catch (IOException ex) {
						ex.printStackTrace();
					}
				}
				try {
					ShutdownHooks.removeShutdownHook(shutdownHook);
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

		@SuppressWarnings("BusyWait")
		private void repeatedFlush() {
			while (br != null) {
				try {
					Thread.sleep(10000);

					long now = System.currentTimeMillis();

					lastExceptions.entrySet().removeIf(e -> {
						if (now - e.getValue().timestamp > 10000) {
							e.getValue().flush();
							return true;
						}
						return false;
					});

					if (br != null) {
						br.flush();
					}
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

		Debug.dumpf("os.name:                      %s", System.getProperty("os.name"));
		Debug.dumpf("os.version:                   %s", System.getProperty("os.version"));
		Debug.dumpf("user.dir:                     %s", System.getProperty("user.dir"));
		Debug.dumpf("jar.dir:                      %s", Config.DEFAULT_BASE_DIR);
		Debug.dumpf("cache.dir:                    %s", Config.getBaseCacheDir().getAbsolutePath());
		Debug.dumpf("config.file:                  %s", Config.getConfigFile().getAbsolutePath());
		Debug.dumpf("log.file                      %s", Config.getLogFile().getAbsolutePath());
		Debug.dumpf("proc.cores:                   %s", Runtime.getRuntime().availableProcessors());
		Debug.dumpf("java.version:                 %s", System.getProperty("java.version"));
		Debug.dumpf("java.vm.specification.vendor: %s", System.getProperty("java.vm.specification.vendor"));
		Debug.dumpf("jvm.max.mem:                  %d", Runtime.getRuntime().maxMemory());
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
			logWriter.br.write(timestamp() + s);
			logWriter.br.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
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
		int hashCode;

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

			hashCode = Objects.hash(exception, line, file);
		}

		@Override
		public boolean equals(Object ex) {
			return ex instanceof ExceptionInfo
					&& exception == ((ExceptionInfo) ex).exception
					&& line == ((ExceptionInfo) ex).line
					&& (file == null && ((ExceptionInfo) ex).file == null || file != null && file.equals(((ExceptionInfo) ex).file));
		}

		@Override
		public String toString() {
			return "ExceptionInfo: " + exception.getSimpleName() + " at " + file + "L" + line + " #" + hashCode;
		}

		@Override
		public int hashCode() {
			return hashCode;
		}

		void flush() {
			if (count > 0) {
				String line = " ... " + count + " more of " + oneLine;
				appendLogFile(line);
				System.out.println(line);
			}
		}

		void poke() {
			timestamp = System.currentTimeMillis();
			count++;
		}
	}
}
