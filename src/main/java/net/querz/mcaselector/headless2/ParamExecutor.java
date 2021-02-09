package net.querz.mcaselector.headless2;

import net.querz.mcaselector.Config;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.filter.GroupFilter;
import net.querz.mcaselector.headless.ConsoleProgress;
import net.querz.mcaselector.headless.FilterParser;
import net.querz.mcaselector.headless.ParamParser;
import net.querz.mcaselector.headless.ParseException;
import net.querz.mcaselector.io.ChunkFilterExporter;
import net.querz.mcaselector.io.ChunkFilterSelector;
import net.querz.mcaselector.io.SelectionData;
import net.querz.mcaselector.io.SelectionExporter;
import net.querz.mcaselector.io.SelectionHelper;
import net.querz.mcaselector.io.WorldDirectories;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.text.Translation;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParamExecutor {

	private String[] args;
	private Map<String, String> params;

	public ParamExecutor(String[] args) throws IOException {
		this.args = args;
		params = new ParamParser(args).parse();
	}

	public Future<Boolean> run() throws IOException {

		// do nothing if we don't have any params
		if (params.size() == 0) {
			return null;
		}

		FutureTask<Boolean> future = new FutureTask<>(() -> {}, true);

		parseConfig();

		switch (parseMode()) {
			case "select":
				select(future);
				break;
			case "export":
				break;
			case "import":
				break;
			case "delete":
				break;
			case "change":
				break;
			case "cache":
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

		return future;
	}

	private void select(FutureTask<Boolean> future) throws IOException {
		File output = parseFileAndCreateParentDirectories("output", "csv");
		GroupFilter query = parseQuery();
		int radius = parseRadius(0);

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
			SelectionExporter.exportSelection(selection, output, progress);
		} else {
			throw new ParseException("missing parameter --query and/or --selection");
		}
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

	private int parseRadius(int def) throws ParseException {
		String radius = params.get("radius");
		if (radius == null || radius.isEmpty()) {
			return def;
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

	private WorldDirectories parseWorldDirectories(String regionKey, String poiKey, String entitiesKey) throws ParseException {
		WorldDirectories worldDirectories = new WorldDirectories();
		if (!params.containsKey(regionKey)) {
			throw new ParseException("missing required " + regionKey + " directory");
		}
		worldDirectories.setRegion(parseDirectoryAndTestExistence(params.get(regionKey)));
		if (params.containsKey(poiKey)) {
			worldDirectories.setPoi(parseDirectoryAndTestExistence(params.get(poiKey)));
		}
		if (params.containsKey(entitiesKey)) {
			worldDirectories.setEntities(parseDirectoryAndTestExistence(params.get(entitiesKey)));
		}
		return worldDirectories;
	}

	private WorldDirectories parseAndCreateWorldDirectories(String regionKey, String poiKey, String entitiesKey) throws IOException {
		WorldDirectories worldDirectories = new WorldDirectories();
		if (!params.containsKey(regionKey)) {
			throw new ParseException("missing required " + regionKey + " directory");
		}
		worldDirectories.setRegion(parseAndCreateDirectory(params.get(regionKey)));
		if (params.containsKey(poiKey)) {
			worldDirectories.setPoi(parseAndCreateDirectory(params.get(poiKey)));
		}
		if (params.containsKey(entitiesKey)) {
			worldDirectories.setEntities(parseAndCreateDirectory(params.get(entitiesKey)));
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

	private File parseFileAndTestExistence(String f, String ending) throws ParseException {
		if (f == null) {
			throw new ParseException("missing file");
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

	private File parseFileAndCreateParentDirectories(String f, String ending) throws IOException {
		if (f == null) {
			throw new ParseException("missing file");
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

	private File parseDirectoryAndTestExistence(String f) throws ParseException {
		if (f == null) {
			throw new ParseException("missing directory");
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

	private File parseAndCreateDirectory(String f) throws IOException {
		if (f == null) {
			throw new ParseException("missing directory");
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

	private SelectionData loadSelection() throws ParseException {
		if (params.containsKey("selection")) {
			File input = parseFileAndTestExistence(params.get("selection"), "csv");
			return SelectionHelper.importSelection(input);
		}
		return null;
	}

	private int parsePositiveInt(String value) throws ParseException {
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
		Config.setLoadThreads(parsePositiveInt(params.getOrDefault("read-threads", "" + Config.DEFAULT_LOAD_THREADS)));
		Config.setProcessThreads(parsePositiveInt(params.getOrDefault("process-threads", "" + Config.DEFAULT_PROCESS_THREADS)));
		Config.setWriteThreads(parsePositiveInt(params.getOrDefault("write-threads", "" + Config.DEFAULT_WRITE_THREADS)));
		Config.setMaxLoadedFiles(parsePositiveInt(params.getOrDefault("max-loaded-files", "" + Config.DEFAULT_MAX_LOADED_FILES)));
	}
}
