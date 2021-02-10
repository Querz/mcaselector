package net.querz.mcaselector.headless;

import net.querz.mcaselector.Config;
import net.querz.mcaselector.changer.ChangeParser;
import net.querz.mcaselector.changer.Field;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.exception.ParseException;
import net.querz.mcaselector.filter.FilterParser;
import net.querz.mcaselector.filter.GroupFilter;
import net.querz.mcaselector.io.*;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.property.DataProperty;
import net.querz.mcaselector.range.Range;
import net.querz.mcaselector.range.RangeParser;
import net.querz.mcaselector.text.Translation;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ParamExecutor {

	private final String[] args;
	private Map<String, String> params;

	public ParamExecutor(String[] args) {
		this.args = args;
	}

	public Future<Boolean> run() {

		FutureTask<Boolean> future = new FutureTask<>(() -> {}, true);

		try {
			params = new ParamParser(args).parse();

			// do nothing if we don't have any params
			if (params.size() == 0) {
				return null;
			}

			parseConfig();

			switch (parseMode()) {
				case "select":
					printHeadlessSettings();
					select(future);
					break;
				case "export":
					printHeadlessSettings();
					export(future);
					break;
				case "import":
					printHeadlessSettings();
					imp(future);
					break;
				case "delete":
					printHeadlessSettings();
					delete(future);
					break;
				case "change":
					printHeadlessSettings();
					change(future);
					break;
				case "cache":
					printHeadlessSettings();
					cache(future);
					break;
				case "printMissingTranslations":
					printMissingTranslations(future);
					break;
				case "printTranslation":
					printTranslation(future);
					break;
				case "printTranslationKeys":
					printTranslationKeys(future);
					break;
			}
		} catch (IOException ex) {
			Debug.error("error: " + ex.getMessage());
			if (Config.debug()) {
				Debug.dumpException("an error occurred while running MCA Selector in headless mode", ex);
			}
			return null;
		}

		return future;
	}

	private void select(FutureTask<Boolean> future) throws IOException {
		File output = parseFileAndCreateParentDirectories("output", "csv");
		GroupFilter query = parseQuery();
		int radius = parseRadius();

		Map<Point2i, Set<Point2i>> selection = new HashMap<>();
		ConsoleProgress progress = new ConsoleProgress();
		progress.onDone(() -> {
			SelectionHelper.exportSelection(new SelectionData(selection, false), output);
			future.run();
		});
		ChunkFilterSelector.selectFilter(query, radius, (src) -> mergeSelections(src, selection), progress, true);
	}

	private void export(FutureTask<Boolean> future) throws IOException {
		WorldDirectories outputDirectories = parseAndCreateWorldDirectories("output-region", "output-poi", "output-entities");
		testWorldDirectoriesConnections(Config.getWorldDirs(), outputDirectories);
		GroupFilter query = parseQuery();
		SelectionData selection = loadSelection();

		ConsoleProgress progress = new ConsoleProgress();
		progress.onDone(future);

		if (query != null) {
			ChunkFilterExporter.exportFilter(query, selection, outputDirectories, progress, true);
		} else if (selection != null) {
			SelectionExporter.exportSelection(selection, outputDirectories, progress);
		} else {
			throw new ParseException("missing query and/or selection");
		}
	}

	private void imp(FutureTask<Boolean> future) throws IOException {
		WorldDirectories inputDirectories = parseAndCreateWorldDirectories("input-region", "input-poi", "input-entities");
		int offsetX = parseInt("x-offset", 0);
		int offsetZ = parseInt("z-offset", 0);
		boolean overwrite = params.containsKey("overwrite");
		SelectionData sourceSelection = loadSelection("input-selection");
		SelectionData targetSelection = loadSelection();
		List<Range> sections = parseSections();

		ConsoleProgress progress = new ConsoleProgress();
		progress.onDone(future);

		DataProperty<Map<Point2i, RegionDirectories>> tempFiles = new DataProperty<>();
		ChunkImporter.importChunks(inputDirectories, progress, true, overwrite, sourceSelection, targetSelection, sections, new Point2i(offsetX, offsetZ), tempFiles);
		if (tempFiles.get() != null) {
			for (RegionDirectories tempFile : tempFiles.get().values()) {
				if (!tempFile.getRegion().delete()) {
					Debug.errorf("failed to delete temp file %s", tempFile.getRegion());
				}
				if (!tempFile.getPoi().delete()) {
					Debug.errorf("failed to delete temp file %s", tempFile.getPoi());
				}
				if (!tempFile.getEntities().delete()) {
					Debug.errorf("failed to delete temp file %s", tempFile.getEntities());
				}
			}
		}
	}

	private void delete(FutureTask<Boolean> future) throws IOException {
		GroupFilter query = parseQuery();
		SelectionData selection = loadSelection();

		ConsoleProgress progress = new ConsoleProgress();
		progress.onDone(future);

		if (query != null) {
			ChunkFilterDeleter.deleteFilter(query, selection, progress, true);
		} else if (selection != null) {
			SelectionDeleter.deleteSelection(selection, progress);
		} else {
			throw new ParseException("missing query and/or selection");
		}
	}

	private void change(FutureTask<Boolean> future) throws IOException {
		SelectionData selection = loadSelection();
		boolean force = params.containsKey("force");
		List<Field<?>> fields = parseFields();
		if (fields == null) {
			throw new ParseException("no fields to change");
		}

		ConsoleProgress progress = new ConsoleProgress();
		progress.onDone(future);

		FieldChanger.changeNBTFields(fields, force, selection, progress, true);
	}

	private void cache(FutureTask<Boolean> future) throws IOException {
		if (!HeadlessHelper.hasJavaFX()) {
			throw new IOException("no JavaFX installation found");
		}

		File output = parseAndCreateDirectory("output");
		Config.setCacheDir(output);
		Integer zoomLevel = parseZoomLevel();

		ConsoleProgress progress = new ConsoleProgress();
		progress.onDone(future);

		CacheHelper.forceGenerateCache(zoomLevel, progress);
	}

	private void printMissingTranslations(FutureTask<Boolean> future) {
		Set<Locale> locales = Translation.getAvailableLanguages();
		for (Locale locale : locales) {
			Translation.load(locale);
			boolean printedLanguage = false;
			for (Translation translation : Translation.values()) {
				if (!translation.isTranslated()) {
					if (!printedLanguage) {
						System.out.println(locale + ":");
						printedLanguage = true;
					}
					System.out.println("  " + translation.getKey());
				}
			}
		}
		future.run();
	}

	private void printTranslation(FutureTask<Boolean> future) throws ParseException {
		String l = params.get("locale");
		if (l == null) {
			throw new ParseException("no locale");
		}

		Pattern languageFilePattern = Pattern.compile("^(?<locale>-?(?<language>-?[a-z]{2})_(?<country>-?[A-Z]{2}))$");

		Locale locale;

		Matcher matcher = languageFilePattern.matcher(l);
		if (matcher.matches()) {
			String language = matcher.group("language");
			String country = matcher.group("country");
			locale = new Locale(language, country);
		} else {
			throw new ParseException("invalid locale " + l);
		}

		Translation.load(locale);

		for (Translation translation : Translation.values()) {
			System.out.println(translation.getKey() + ";" + (translation.isTranslated() ? translation.toString() : ""));
		}

		future.run();
	}

	private void printTranslationKeys(FutureTask<Boolean> future) {
		for (Translation translation : Translation.values()) {
			System.out.println(translation.getKey() + ";");
		}
		future.run();
	}

// ---------------------------------------------------------------------------------------------------------------------

	private synchronized void mergeSelections(Map<Point2i, Set<Point2i>> src, Map<Point2i, Set<Point2i>> target) {
		for (Map.Entry<Point2i, Set<Point2i>> entry : src.entrySet()) {
			if (entry.getValue() == null) {
				target.put(entry.getKey(), null);
				continue;
			}

			if (target.containsKey(entry.getKey())) {
				Set<Point2i> targetRegionSelection = target.get(entry.getKey());
				if (targetRegionSelection != null) {
					targetRegionSelection.addAll(entry.getValue());
					// select full region
					if (targetRegionSelection.size() == 1024) {
						target.put(entry.getKey(), null);
					}
				}
			} else {
				target.put(entry.getKey(), entry.getValue());
			}
		}
	}

	private String parseMode() throws ParseException {
		String mode = params.get("mode");
		if (mode == null || mode.isEmpty()) {
			throw new ParseException("missing mode");
		}
		return mode;
	}

	private int parseRadius() throws ParseException {
		String radius = params.get("radius");
		if (radius == null || radius.isEmpty()) {
			return 0;
		}
		int result;
		try {
			result = Integer.parseInt(radius);
		} catch (NumberFormatException ex) {
			throw new ParseException("invalid radius: " + ex.getMessage());
		}
		if (result < 0) {
			throw new ParseException("radius is negative");
		}
		if (result > 32) {
			throw new ParseException("radius is larger than 32");
		}
		return result;
	}

	private GroupFilter parseQuery() throws ParseException {
		String query = params.get("query");
		if (query == null || query.isEmpty()) {
			return null;
		}
		return new FilterParser(query).parse();
	}

	private List<Field<?>> parseFields() throws ParseException {
		String fields = params.get("fields");
		if (fields == null || fields.isEmpty()) {
			return null;
		}
		return new ChangeParser(fields).parse();
	}

	private WorldDirectories parseWorldDirectories(String regionKey, String poiKey, String entitiesKey) throws ParseException {
		WorldDirectories worldDirectories = new WorldDirectories();
		if (!params.containsKey(regionKey)) {
			throw new ParseException("missing required " + regionKey + " directory");
		}
		worldDirectories.setRegion(parseDirectoryAndTestExistence(regionKey));
		if (params.containsKey(poiKey)) {
			worldDirectories.setPoi(parseDirectoryAndTestExistence(poiKey));
		}
		if (params.containsKey(entitiesKey)) {
			worldDirectories.setEntities(parseDirectoryAndTestExistence(entitiesKey));
		}
		return worldDirectories;
	}

	private WorldDirectories parseAndCreateWorldDirectories(String regionKey, String poiKey, String entitiesKey) throws IOException {
		WorldDirectories worldDirectories = new WorldDirectories();
		if (!params.containsKey(regionKey)) {
			throw new ParseException("missing required " + regionKey + " directory");
		}
		worldDirectories.setRegion(parseAndCreateDirectory(regionKey));
		if (params.containsKey(poiKey)) {
			worldDirectories.setPoi(parseAndCreateDirectory(poiKey));
		}
		if (params.containsKey(entitiesKey)) {
			worldDirectories.setEntities(parseAndCreateDirectory(entitiesKey));
		}
		return worldDirectories;
	}

	// tests if 2 WorldDirectories have poi or entities
	private void testWorldDirectoriesConnections(WorldDirectories a, WorldDirectories b) throws ParseException {
		if (a.getPoi() != null && b.getPoi() == null) {
			throw new ParseException("missing output poi directory");
		}
		if (a.getEntities() != null && b.getEntities() == null) {
			throw new ParseException("missing output entities directory");
		}
	}

	private File parseFileAndTestExistence(String key, String ending) throws ParseException {
		String f = params.get(key);
		if (f == null) {
			throw new ParseException("missing " + key + " file");
		}
		File file = new File(f);
		if (!file.isFile()) {
			throw new ParseException(file + " is not a file");
		}
		if (!file.exists()) {
			throw new ParseException(file + " does not exist");
		}
		testFileEnding(file, ending);
		return file;
	}

	private File parseFileAndCreateParentDirectories(String key, String ending) throws IOException {
		String f = params.get(key);
		if (f == null) {
			throw new ParseException("missing " + key + " file");
		}
		File file = new File(f);
		testFileEnding(file, ending);
		File parent = file.getParentFile();
		if (parent != null && !parent.exists() && !parent.mkdirs()) {
			throw new IOException("failed to create directory " + parent);
		}
		return file;
	}

	private void testFileEnding(File file, String ending) throws ParseException {
		if (!file.getName().endsWith("." + ending)) {
			String[] split = file.getName().split("\\.");
			String end = split.length == 1 ? split[0] : ("." + split[split.length - 1]);
			throw new ParseException("." + ending + " file required, but got " + end);
		}
	}

	private File parseDirectoryAndTestExistence(String key) throws ParseException {
		String f = params.get(key);
		if (f == null) {
			throw new ParseException("missing " + key + " directory");
		}
		File file = new File(f);
		if (!file.isDirectory()) {
			throw new ParseException(file + " is not a directory");
		}
		if (!file.exists()) {
			throw new ParseException(file + " does not exist");
		}
		return file;
	}

	private File parseAndCreateDirectory(String key) throws IOException {
		String f = params.get(key);
		if (f == null) {
			throw new ParseException("missing " + key + " directory");
		}
		File file = new File(f);
		if (!file.isDirectory()) {
			throw new ParseException(file + " is not a directory");
		}
		if (!file.exists() && !file.mkdirs()) {
			throw new IOException("failed to create directory " + file);
		}
		return file;
	}

	private List<Range> parseSections() {
		List<Range> ranges = null;
		if (params.containsKey("sections")) {
			ranges = RangeParser.parseRanges(params.get("sections"), ",");
		}
		return ranges;
	}

	private SelectionData loadSelection() throws ParseException {
		return loadSelection("selection");
	}

	private SelectionData loadSelection(String key) throws ParseException {
		if (params.containsKey(key)) {
			File input = parseFileAndTestExistence(key, "csv");
			return SelectionHelper.importSelection(input);
		}
		return null;
	}

	private Integer parseZoomLevel() throws ParseException {
		String value = params.get("zoom-level");
		if (value != null && !value.isEmpty()) {
			int zoomLevel;
			try {
				zoomLevel = Integer.parseInt(value);
			} catch (NumberFormatException ex) {
				throw new ParseException(ex.getMessage());
			}
			for (int z = Config.getMinZoomLevel(); z <= Config.getMaxZoomLevel(); z *= 2) {
				if (zoomLevel == z) {
					return zoomLevel;
				}
			}
			throw new ParseException("invalid zoom level");
		}
		return null;
	}

	private int parseInt(String key, int def) throws ParseException {
		String value = params.getOrDefault(key, "" + def);
		if (value != null && !value.isEmpty()) {
			try {
				return Integer.parseInt(value);
			} catch (NumberFormatException ex) {
				throw new ParseException(ex.getMessage());
			}
		}
		return def;
	}

	private int parsePositiveInt(String key, int def) throws ParseException {
		String value = params.getOrDefault(key, "" + def);
		if (value != null) {
			try {
				int i = Integer.parseInt(value);
				if (i <= 0) {
					throw new ParseException("number cannot be negative: \"" + value + "\"");
				}
				return i;
			} catch (NumberFormatException ex) {
				throw new ParseException("invalid int: " + ex.getMessage());
			}
		}
		return 1;
	}

	private void parseConfig() throws IOException {
		Config.setWorldDirs(parseWorldDirectories("region", "poi", "entities"));
		if (params.containsKey("debug")) {
			Config.setDebug(true);
			Debug.initLogWriter();
		}
		Config.setDebug(params.containsKey("debug"));
		Config.setLoadThreads(parsePositiveInt("read-threads", Config.DEFAULT_LOAD_THREADS));
		Config.setProcessThreads(parsePositiveInt("process-threads", Config.DEFAULT_PROCESS_THREADS));
		Config.setWriteThreads(parsePositiveInt("write-threads",Config.DEFAULT_WRITE_THREADS));
		Config.setMaxLoadedFiles(parsePositiveInt("max-loaded-files", Config.DEFAULT_MAX_LOADED_FILES));
	}

	private void printHeadlessSettings() {
		Debug.print("read threads:    " + Config.getLoadThreads());
		Debug.print("process threads: " + Config.getProcessThreads());
		Debug.print("write threads:   " + Config.getWriteThreads());
	}
}
