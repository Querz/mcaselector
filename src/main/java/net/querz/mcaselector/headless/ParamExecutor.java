package net.querz.mcaselector.headless;

import net.querz.mcaselector.Config;
import net.querz.mcaselector.changer.Field;
import net.querz.mcaselector.filter.GroupFilter;
import net.querz.mcaselector.headless.ParamInterpreter.ActionKey;
import net.querz.mcaselector.io.ChunkFilterDeleter;
import net.querz.mcaselector.io.ChunkFilterExporter;
import net.querz.mcaselector.io.ChunkFilterSelector;
import net.querz.mcaselector.io.ChunkImporter;
import net.querz.mcaselector.io.FieldChanger;
import net.querz.mcaselector.io.SelectionData;
import net.querz.mcaselector.io.SelectionDeleter;
import net.querz.mcaselector.io.SelectionExporter;
import net.querz.mcaselector.io.SelectionHelper;
import net.querz.mcaselector.io.CacheHelper;
import net.querz.mcaselector.property.DataProperty;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.range.Range;
import net.querz.mcaselector.range.RangeParser;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class ParamExecutor {

	private final String[] args;

	public ParamExecutor(String[] args) {
		this.args = args;
	}

	public Future<Boolean> parseAndRun() {

		FutureTask<Boolean> future = new FutureTask<>(() -> {}, true);

		DataProperty<Map<String, String>> params = new DataProperty<>();

		try {
			params.set(new ParamParser(args).parse());
			ParamInterpreter pi = new ParamInterpreter(params.get());

			// register parameter dependencies and restrictions
			pi.registerDependencies("headless", null, new ActionKey("mode", null));
			pi.registerDependencies("mode", null, new ActionKey("headless", null));
			pi.registerRestrictions("mode", "select", "export", "import", "delete", "change", "cache");
			pi.registerDependencies("mode", null, new ActionKey("world", null)); // every mode param needs a world dir
			pi.registerDependencies("mode", "select", new ActionKey("output", null), new ActionKey("query", null));
			pi.registerDependencies("mode", "export", new ActionKey("output", null));
			pi.registerDependencies("mode", "import", new ActionKey("input", null));
			pi.registerDependencies("mode", "change", new ActionKey("query", null));
			pi.registerDependencies("mode", "cache", new ActionKey("output", null));
			pi.registerDependencies("world", null, new ActionKey("mode", null)); // world param needs mode param
			pi.registerSoftDependencies("output", null, new ActionKey("mode", "select"), new ActionKey("mode", "export"), new ActionKey("mode", "cache"));
			pi.registerSoftDependencies("input", null, new ActionKey("mode", "export"), new ActionKey("mode", "import"), new ActionKey("mode", "delete"), new ActionKey("mode", "change"));
			pi.registerSoftDependencies("query", null, new ActionKey("mode", "select"), new ActionKey("mode", "export"), new ActionKey("mode", "delete"), new ActionKey("mode", "change"));
			pi.registerSoftDependencies("radius", null, new ActionKey("mode", "select"));
			pi.registerDependencies("force", null, new ActionKey("mode", "change"));
			pi.registerDependencies("offset-x", null, new ActionKey("mode", "import"));
			pi.registerDependencies("offset-z", null, new ActionKey("mode", "import"));
			pi.registerDependencies("overwrite", null, new ActionKey("mode", "import"));
			pi.registerDependencies("sections", null, new ActionKey("mode", "import"));
			pi.registerDependencies("selection", null, new ActionKey("mode", "import"));
			pi.registerDependencies("zoom-level", null, new ActionKey("mode", "cache"));
			for (int z = Config.getMinZoomLevel(); z <= Config.getMaxZoomLevel(); z *= 2) {
				pi.registerRestrictions("zoom-level", z + "");
			}
			pi.registerDependencies("debug", null, new ActionKey("headless", null));
			pi.registerDependencies("read-threads", null, new ActionKey("headless", null));
			pi.registerDependencies("process-threads", null, new ActionKey("headless", null));
			pi.registerDependencies("write-threads", null, new ActionKey("headless", null));

			parseConfig(params.get());

			DataProperty<Boolean> isHeadless = new DataProperty<>();

			pi.registerAction("headless", null, v -> runModeHeadless(isHeadless::set));
			pi.registerAction("mode", "select", v -> runModeSelect(params.get(), future));
			pi.registerAction("mode", "export", v -> runModeExport(params.get(), future));
			pi.registerAction("mode", "import", v -> runModeImport(params.get(), future));
			pi.registerAction("mode", "delete", v -> runModeDelete(params.get(), future));
			pi.registerAction("mode", "change", v -> runModeChange(params.get(), future));
			pi.registerAction("mode", "cache", v -> runModeCache(params.get(), future));

			pi.execute();

			if (isHeadless.get() != null && isHeadless.get()) {
				return future;
			}

		} catch (Exception ex) {
			Debug.error("Error: " + ex.getMessage());
			if (params.get() != null && params.get().containsKey("debug")) {
				Debug.dumpException("Error executing in headless mode", ex);
			}
			future.run();
			return future;
		}
		return null;
	}

	private static void runModeHeadless(Consumer<Boolean> isHeadless) {
		isHeadless.accept(true);
	}

	private void parseConfig(Map<String, String> params) throws IOException {
		if (params.containsKey("debug")) {
			Config.setDebug(true);
			Debug.initLogWriter();
		}
		Config.setDebug(params.containsKey("debug"));
		Config.setLoadThreads(parsePositiveInt(params.getOrDefault("read-threads", "" + Config.DEFAULT_LOAD_THREADS)));
		Config.setProcessThreads(parsePositiveInt(params.getOrDefault("process-threads", "" + Config.DEFAULT_PROCESS_THREADS)));
		Config.setWriteThreads(parsePositiveInt(params.getOrDefault("write-threads", "" + Config.DEFAULT_WRITE_THREADS)));
		Config.setMaxLoadedFiles(parsePositiveInt(params.getOrDefault("max-loaded-files", "" + Config.DEFAULT_MAX_LOADED_FILES)));
	}

	private static void runModeCache(Map<String, String> params, FutureTask<Boolean> future) throws IOException {
		if (!HeadlessHelper.hasJavaFX()) {
			throw new IOException("no JavaFX installation found");
		}

		File world = parseDirectory(params.get("world"));
		checkDirectoryForFiles(world, FileHelper.MCA_FILE_PATTERN);
		Config.setWorldDir(world);

		File output = parseDirectory(params.get("output"));
		createDirectoryIfNotExists(output);
		checkDirectoryIsEmpty(output);
		Config.setCacheDir(output);

		printHeadlessSettings();

		Integer zoomLevel = params.containsKey("zoom-level") ? parseInt(params.get("zoom-level")) : null;

		ConsoleProgress progress = new ConsoleProgress();
		progress.onDone(future);

		CacheHelper.forceGenerateCache(zoomLevel, progress);
	}

	private static void runModeChange(Map<String, String> params, FutureTask<Boolean> future) throws IOException {
		File world = parseDirectory(params.get("world"));
		checkDirectoryForFiles(world, FileHelper.MCA_FILE_PATTERN);
		Config.setWorldDir(world);

		List<Field<?>> fields = new ChangeParser(params.get("query")).parse();

		Map<Point2i, Set<Point2i>> selection = loadSelection(params, "input");

		printHeadlessSettings();

		boolean force = params.containsKey("force");

		ConsoleProgress progress = new ConsoleProgress();
		progress.onDone(future);

		FieldChanger.changeNBTFields(fields, force, selection, progress);
	}



	private static void runModeDelete(Map<String, String> params, FutureTask<Boolean> future) throws IOException {
		File world = parseDirectory(params.get("world"));
		checkDirectoryForFiles(world, FileHelper.MCA_FILE_PATTERN);
		Config.setWorldDir(world);

		printHeadlessSettings();

		GroupFilter g = null;
		if (params.containsKey("query")) {
			g = new FilterParser(params.get("query")).parse();
			Debug.print("filter set: " + g);
		}

		Map<Point2i, Set<Point2i>> selection = loadSelection(params, "input");

		ConsoleProgress progress = new ConsoleProgress();
		progress.onDone(future);

		if (g != null) {
			ChunkFilterDeleter.deleteFilter(g, selection, progress, true);
		} else if (selection != null) {
			SelectionDeleter.deleteSelection(selection, progress);
		} else {
			throw new ParseException("missing parameter --query and/or --selection");
		}
	}

	private static void runModeImport(Map<String, String> params, FutureTask<Boolean> future) throws IOException {
		File world = parseDirectory(params.get("world"));
		createDirectoryIfNotExists(world);
		Config.setWorldDir(world);
		// don't check for files, world dir can be empty.
		// this might be used to just apply an offset to the input without merging anything.

		File input = parseDirectory(params.get("input"));
		checkDirectoryForFiles(world, FileHelper.MCA_FILE_PATTERN);

		Config.setWorldDir(world);

		int offsetX = parseInt(params.get("offset-x"));
		int offsetZ = parseInt(params.get("offset-z"));
		boolean overwrite = params.containsKey("overwrite");
		File selectionFile = null;
		if (params.containsKey("selection")) {
			 selectionFile = parseFile(params.get("selection"), "csv");
		}
		SelectionData selection = null;
		if (selectionFile != null && selectionFile.exists()) {
			selection = SelectionHelper.importSelection(selectionFile);
		}

		List<Range> ranges = null;
		if (params.containsKey("sections")) {
			ranges = RangeParser.parseRanges(params.get("sections"), ",");
		}

		printHeadlessSettings();

		ConsoleProgress progress = new ConsoleProgress();
		progress.onDone(future);

		DataProperty<Map<Point2i, File>> tempFiles = new DataProperty<>();
		ChunkImporter.importChunks(input, progress, true, overwrite, null, selection, ranges, new Point2i(offsetX, offsetZ), tempFiles);
		if (tempFiles.get() != null) {
			for (File tempFile : tempFiles.get().values()) {
				if (!tempFile.delete()) {
					Debug.errorf("failed to delete temp file %s", tempFile);
				}
			}
		}
	}

	private static void runModeExport(Map<String, String> params, FutureTask<Boolean> future) throws IOException {
		File world = parseDirectory(params.get("world"));
		checkDirectoryForFiles(world, FileHelper.MCA_FILE_PATTERN);
		Config.setWorldDir(world);

		File output = parseDirectory(params.get("output"));
		createDirectoryIfNotExists(output);
		checkDirectoryIsEmpty(output);

		printHeadlessSettings();

		GroupFilter g = null;
		if (params.containsKey("query")) {
			g = new FilterParser(params.get("query")).parse();
			Debug.print("filter set: " + g);
		}

		Map<Point2i, Set<Point2i>> selection = loadSelection(params, "input");

		Debug.print("exporting chunks...");

		ConsoleProgress progress = new ConsoleProgress();
		progress.onDone(future);

		if (g != null) {
			ChunkFilterExporter.exportFilter(g, selection, output, progress, true);
		} else if (selection != null) {
			SelectionExporter.exportSelection(selection, output, progress);
		} else {
			throw new ParseException("missing parameter --query and/or --selection");
		}
	}

	private static void runModeSelect(Map<String, String> params, FutureTask<Boolean> future) throws IOException {
		File world = parseDirectory(params.get("world"));
		checkDirectoryForFiles(world, FileHelper.MCA_FILE_PATTERN);
		Config.setWorldDir(world);

		File output = parseFile(params.get("output"), "csv");
		createParentDirectoryIfNotExists(output);

		printHeadlessSettings();

		GroupFilter g = new FilterParser(params.get("query")).parse();

		Debug.print("filter set: " + g);

		int radius = 0;
		if (params.containsKey("radius")) {
			radius = parseInt(params.get("radius"));
			Debug.print("radius set: " + radius);
		}

		Debug.print("selecting chunks...");

		Map<Point2i, Set<Point2i>> selection = new HashMap<>();

		ConsoleProgress progress = new ConsoleProgress();
		progress.onDone(() -> {
			SelectionHelper.exportSelection(new SelectionData(selection, false), output);
			future.run();
		});

		ChunkFilterSelector.selectFilter(g, radius, selection::putAll, progress, true);
	}

	private static void printHeadlessSettings() {
		Debug.print("read threads:    " + Config.getLoadThreads());
		Debug.print("process threads: " + Config.getProcessThreads());
		Debug.print("write threads:   " + Config.getWriteThreads());
	}

	private static int parseInt(String value) throws ParseException {
		if (value != null && !value.isEmpty()) {
			try {
				return Integer.parseInt(value);
			} catch (NumberFormatException ex) {
				throw new ParseException(ex.getMessage());
			}
		}
		return 0;
	}

	private static int parsePositiveInt(String value) throws ParseException {
		if (value != null) {
			try {
				int i = Integer.parseInt(value);
				if (i <= 0) {
					throw new ParseException("number cannot be negative: \"" + value + "\"");
				}
				return i;
			} catch (NumberFormatException ex) {
				throw new ParseException(ex.getMessage());
			}
		}
		return 1;
	}

	private static Map<Point2i, Set<Point2i>> loadSelection(Map<String, String> params, String key) throws ParseException {
		if (params.containsKey(key)) {
			Debug.print("loading selection...");

			File input = parseFile(params.get(key), "csv");
			fileMustExist(input);
			return SelectionHelper.importSelection(input).getSelection();
		}
		return null;
	}

	private static File parseFile(String value, String ending) throws ParseException {
		File file = new File(value);
		if (file.getName().equals("." + ending) || !file.getName().endsWith("." + ending)) {
			throw new ParseException("invalid file \"" + value + "\", expected ." + ending + " file");
		}
		return file;
	}

	private static File parseDirectory(String value) {
		return new File(value);
	}

	private static void fileMustExist(File file) throws ParseException {
		if (!file.exists()) {
			throw new ParseException("file \"" + file + "\" does not exist");
		}
	}

	private static void createDirectoryIfNotExists(File file) throws IOException {
		if (!file.exists() && !file.mkdirs()) {
			throw new IOException("unable to create directory \"" + file + "\"");
		}
	}

	private static void checkDirectoryForFiles(File dir, String regexp) {
		Pattern p = Pattern.compile(regexp);
		File[] found = dir.listFiles((d, n) -> p.matcher(n).find());
		if (found == null || found.length == 0) {
			throw new IllegalArgumentException("no valid files found in \"" + dir + "\"");
		}
	}

	private static void checkDirectoryIsEmpty(File dir) {
		File[] found = dir.listFiles();
		if (found != null && found.length > 0) {
			throw new IllegalArgumentException("directory \"" + dir + "\" is not empty");
		}
	}

	private static void createParentDirectoryIfNotExists(File file) throws IOException {
		if (file.getParentFile() != null && !file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
			throw new IOException("unable to create directory for \"" + file + "\"");
		}
	}
}
