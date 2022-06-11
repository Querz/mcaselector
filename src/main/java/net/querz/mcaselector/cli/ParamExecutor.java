package net.querz.mcaselector.cli;

import net.querz.mcaselector.Config;
import net.querz.mcaselector.changer.ChangeParser;
import net.querz.mcaselector.changer.Field;
import net.querz.mcaselector.filter.FilterParser;
import net.querz.mcaselector.filter.filters.GroupFilter;
import net.querz.mcaselector.io.*;
import net.querz.mcaselector.io.job.*;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.point.Point3i;
import net.querz.mcaselector.property.DataProperty;
import net.querz.mcaselector.range.Range;
import net.querz.mcaselector.range.RangeParser;
import net.querz.mcaselector.selection.Selection;
import net.querz.mcaselector.selection.SelectionData;
import org.apache.commons.cli.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

public final class ParamExecutor {

	private static final Logger LOGGER = LogManager.getLogger(ParamExecutor.class);

	private static final Options options = new Options();
	private static final Options helpOptions = new Options();

	static {
		options.addOption(Option.builder("h")
			.longOpt("help")
			.desc("Print all available command line options")
			.build());
		options.addOption(Option.builder("v")
			.longOpt("version")
			.desc("Shows the current version of MCA Selector")
			.build());

		options.addOption(Option.builder("m")
			.longOpt("mode")
			.desc("""
				The mode to run. Available modes are:
				select    Create a selection from a filter query and save it as a CSV file
				export    Export chunks based on a filter query and/or a selection
				import    Import chunks with an optional offset
				delete    Delete chunks based on a filter query and/or a selection
				change    Change NBT values in an entire world or only in chunks based on a selection
				cache     Generate the cache images for an entire world
				image     Generate a single image based on a selection
				""")
			.hasArg()
			.build());
		options.addOption(Option.builder("o")
			.longOpt("output")
			.desc("Specify the output file or directory")
			.hasArg()
			.build());
		options.addOption(Option.builder("q")
			.longOpt("query")
			.desc("The query to run")
			.hasArg()
			.build());
		options.addOption(Option.builder("s")
			.longOpt("selection")
			.desc("The selection to be applied to the target world")
			.hasArg()
			.build());
		options.addOption(Option.builder()
			.longOpt("source-selection")
			.desc("The selection to be applied to the source world")
			.hasArg()
			.build());
		options.addOption(Option.builder("r")
			.longOpt("radius")
			.desc("The radius to be applied for a selection")
			.hasArg()
			.build());
		options.addOption(Option.builder()
			.longOpt("x-offset")
			.desc("The offset in x-direction for chunk import")
			.hasArg()
			.build());
		options.addOption(Option.builder()
			.longOpt("y-offset")
			.desc("The offset in y-direction for chunk import")
			.hasArg()
			.build());
		options.addOption(Option.builder()
			.longOpt("z-offset")
			.desc("The offset in z-direction for chunk import")
			.hasArg()
			.build());
		options.addOption(Option.builder()
			.longOpt("overwrite")
			.desc("Whether to overwrite existing chunks in the target world during chunk import")
			.build());
		options.addOption(Option.builder()
			.longOpt("force")
			.desc("Whether to force NBT tags during NBT change")
			.build());

		// world
		options.addOption(Option.builder("w")
			.longOpt("world")
			.desc("The target world of this operation")
			.hasArgs()
			.build());
		options.addOption(Option.builder()
			.longOpt("region")
			.desc("The specific region folder to be uses for the target world, overwrites the region folder detected by --world")
			.hasArg()
			.build());
		options.addOption(Option.builder()
			.longOpt("poi")
			.desc("The specific poi folder to be uses for the target world, overwrites the poi folder detected by --world")
			.hasArg()
			.build());
		options.addOption(Option.builder()
			.longOpt("entities")
			.desc("The specific entities folder to be uses for the target world, overwrites the entities folder detected by --world")
			.hasArg()
			.build());
		options.addOption(Option.builder()
			.longOpt("source-world")
			.desc("The source world of this operation")
			.hasArgs()
			.build());
		options.addOption(Option.builder()
			.longOpt("source-region")
			.desc("The specific region folder to be uses for the source world, overwrites the region folder detected by --source-world")
			.hasArg()
			.build());
		options.addOption(Option.builder()
			.longOpt("source-poi")
			.desc("The specific poi folder to be uses for the source world, overwrites the poi folder detected by --source-world")
			.hasArg()
			.build());
		options.addOption(Option.builder()
			.longOpt("source-entities")
			.desc("The specific entities folder to be uses for the source world, overwrites the entities folder detected by --source-world")
			.hasArg()
			.build());
		options.addOption(Option.builder()
			.longOpt("output-world")
			.desc("The output world folder of this operation")
			.hasArgs()
			.build());
		options.addOption(Option.builder()
			.longOpt("output-region")
			.desc("The specific region folder to be uses for the output world, overwrites the region folder detected by --output-world")
			.hasArg()
			.build());
		options.addOption(Option.builder()
			.longOpt("output-poi")
			.desc("The specific poi folder to be uses for the output world, overwrites the poi folder detected by --output-world")
			.hasArg()
			.build());
		options.addOption(Option.builder()
			.longOpt("output-entities")
			.desc("The specific entities folder to be uses for the output world, overwrites the entities folder detected by --output-world")
			.hasArg()
			.build());

		// config stuff
		options.addOption(Option.builder("d")
			.longOpt("debug")
			.desc("Enable logging of debug level logs")
			.build());
		options.addOption(Option.builder()
			.longOpt("process-threads")
			.desc("Set the number of threads to be used for processing")
			.hasArg()
			.build());
		options.addOption(Option.builder()
			.longOpt("write-threads")
			.desc("Set the number threads to use for writing files")
			.hasArg()
			.build());

		// all the above options should appear in --help
		for (Option option : options.getOptions()) {
			helpOptions.addOption(option);
		}

		// everything below should not appear in --help
		options.addOption(Option.builder()
			.longOpt("locale")
			.desc("Set the locale for debugging the language files")
			.hasArgs()
			.build());
	}

	private final String[] args;
	private CommandLine line = null;

	public ParamExecutor(String[] args) {
		this.args = args;
		LOGGER.debug("args: {}", Arrays.toString(args));
	}

	public Future<Boolean> run() {
		if (args.length == 0) {
			return null;
		}

		FutureTask<Boolean> future = new FutureTask<>(() -> {}, true);

		CommandLineParser parser = new DefaultParser();
		try {
			line = parser.parse(options, args);
		} catch (ParseException ex) {
			printError(ex.getMessage());
			future.run();
			return future;
		}

		if (line.hasOption("help")) {
			printHelp();
			future.run();
			return future;
		}

		if (line.hasOption("version")) {
			printVersion();
			future.run();
			return future;
		}

		if (!line.hasOption("mode")) {
			printError("missing mode");
			future.run();
			return future;
		}

		try {
			parseConfig();

			String mode = line.getOptionValue("mode");
			switch (mode) {
				case "select" -> select(future);
				case "export" -> export(future);
				case "import" -> imp(future);
				case "delete" -> delete(future);
				case "change" -> change(future);
				case "cache" -> cache(future);
				case "image" -> image(future);

				// for updating and debugging translations
				case "printMissingTranslations" -> Translations.printMissingTranslations(future);
				case "printTranslation" -> Translations.printTranslation(line, future);
				case "printTranslationKeys" -> Translations.printTranslationKeys(future);

				default -> {
					printError("invalid mode %s", mode);
					future.run();
					return future;
				}
			}

		} catch (Exception ex) {
			printError(ex.getMessage());
			future.run();
			return future;
		}

		return future;

	}

	private void printHelp() {
		String[] helpOrder = new String[]{
			"help", "version", "mode", "output", "query", "selection", "source-selection", "radius", "x-offset",
			"y-offset", "z-offset", "overwrite", "force", "world", "region", "poi", "entities", "source-world",
			"source-region", "source-poi", "source-entities", "output-world", "output-region", "output-poi",
			"output-entities", "debug", "process-threads", "write-threads"
		};
		Map<String, Integer> helpOptionOrderLookup = new HashMap<>();
		for (int i = 0; i < helpOrder.length; i++) {
			helpOptionOrderLookup.put(helpOrder[i], i);
		}
		HelpFormatter formatter = new HelpFormatter();
		formatter.setWidth(128);
		formatter.setOptionComparator(Comparator.comparingInt(o -> helpOptionOrderLookup.get(o.getLongOpt())));
		formatter.printHelp("java -jar mcaselector.jar <args>", helpOptions);
	}

	private void printVersion() {
		try {
			String applicationVersion = FileHelper.getManifestAttributes().getValue("Application-Version");
			System.out.println(applicationVersion);
		} catch (IOException ex) {
			System.out.println("dev");
		}
	}

	private void parseConfig() throws ParseException {
		Config.setDebug(line.hasOption("debug"));
		Config.setProcessThreads(parseInt("process-threads", Config.DEFAULT_PROCESS_THREADS, 1, 128));
		Config.setProcessThreads(parseInt("write-threads", Config.DEFAULT_WRITE_THREADS, 1, 128));
	}

	private void printError(String msg, Object... params) {
		System.out.printf("error: %s\n", String.format(msg, params));
	}

	private int parseInt(String key, int def) throws ParseException {
		String value = line.getOptionValue(key, "" + def);
		if (value != null && !value.isEmpty()) {
			try {
				return Integer.parseInt(value);
			} catch (NumberFormatException ex) {
				throw new ParseException(String.format("parameter for argument %s is not a valid number", key));
			}
		}
		return def;
	}

	private int parseInt(String key, int def, int min, int max) throws ParseException {
		int i = parseInt(key, def);
		if (i < min) {
			throw new ParseException(String.format("%s cannot be smaller than %d", key, min));
		}
		if (i > max) {
			throw new ParseException(String.format("%s cannot be larger than %d", key, max));
		}
		return i;
	}

	private WorldDirectories parseWorldDirectories(String prefix) throws ParseException {
		String worldName = prefix.isEmpty() ? "world" : (prefix + "-world");
		String regionName = prefix.isEmpty() ? "region" : (prefix + "-region");
		String poiName = prefix.isEmpty() ? "poi" : (prefix + "-poi");
		String entitiesName = prefix.isEmpty() ? "entities" : (prefix + "-entities");

		if (!line.hasOption(worldName)) {
			throw new ParseException(String.format("missing mandatory %s parameter", worldName));
		}
		File world = new File(line.getOptionValue(worldName));
		checkDir(world, worldName);
		String[] contents = world.list((d, n) -> n.equals("region") && d.isDirectory());
		if ((contents == null || contents.length == 0) && !line.hasOption(regionName)) {
			throw new ParseException(String.format("no region folder detected in %s folder and no custom %s folder provided", worldName, regionName));
		}

		File region;
		if (line.hasOption(regionName)) {
			region = new File(line.getOptionValue(regionName));
			checkDir(region, regionName);
		} else {
			region = new File(world, "region");
		}

		File poi;
		if (line.hasOption(poiName)) {
			poi = new File(line.getOptionValue(poiName));
			checkDir(poi, poiName);
		} else {
			poi = new File(world, "poi");
			if (!poi.exists() || !poi.isDirectory()) {
				poi = null;
			}
		}

		File entities;
		if (line.hasOption(entitiesName)) {
			entities = new File(line.getOptionValue(entitiesName));
			checkDir(entities, entitiesName);
		} else {
			entities = new File(world, "entities");
			if (!entities.exists() || !entities.isDirectory()) {
				entities = null;
			}
		}

		return new WorldDirectories(region, poi, entities);
	}

	private WorldDirectories parseAndCreateWorldDirectories(String prefix) throws ParseException {
		String worldName = prefix.isEmpty() ? "world" : (prefix + "-world");
		String regionName = prefix.isEmpty() ? "region" : (prefix + "-region");
		String poiName = prefix.isEmpty() ? "poi" : (prefix + "-poi");
		String entitiesName = prefix.isEmpty() ? "entities" : (prefix + "-entities");

		if (!line.hasOption(worldName)) {
			throw new ParseException(String.format("missing mandatory %s parameter", worldName));
		}
		File world = parseDirAndCreate(worldName);

		File region;
		if (line.hasOption(regionName)) {
			region = parseDirAndCreate(regionName);
		} else {
			region = new File(world, "region");
			if (!region.mkdirs()) {
				throw new ParseException(String.format("failed to create region directory %s", region));
			}
		}

		File poi;
		if (line.hasOption(poiName)) {
			poi = parseDirAndCreate(poiName);
		} else {
			poi = new File(world, "poi");
			if (!region.mkdirs()) {
				throw new ParseException(String.format("failed to create poi directory %s", poi));
			}
		}

		File entities;
		if (line.hasOption(entitiesName)) {
			entities = parseDirAndCreate(entitiesName);
		} else {
			entities = new File(world, "entities");
			if (!entities.mkdirs()) {
				throw new ParseException(String.format("failed to create entities directory %s", entities));
			}
		}
		return new WorldDirectories(region, poi, entities);
	}

	private void checkDir(File dir, String name) throws ParseException {
		if (!dir.exists()) {
			throw new ParseException(String.format("%s directory does not exist", name));
		}
		if (!dir.isDirectory()) {
			throw new ParseException(String.format("%s is not a directory", name));
		}
	}

	private File parseDirAndCreate(String key) throws ParseException {
		if (!line.hasOption(key)) {
			throw new ParseException(String.format("missing mandatory %s parameter", key));
		}
		File dir = new File(line.getOptionValue(key));
		if (dir.exists() && !dir.isDirectory()) {
			throw new ParseException(String.format("%s is not a directory", dir));
		}
		if (!dir.exists() && !dir.mkdirs()) {
			throw new ParseException(String.format("failed to create directory %s", dir));
		}
		return dir;
	}

	private File parseFileAndCreateParentDirectories(String key, String fileEnding) throws ParseException {
		if (!line.hasOption(key)) {
			throw new ParseException(String.format("missing mandatory %s parameter", key));
		}
		String fileString = line.getOptionValue(key);
		if (!fileString.toLowerCase().endsWith("." + fileEnding))  {
			throw new ParseException(String.format("output file has invalid format, .%s required", fileEnding));
		}
		File output = new File(fileString);
		File parent = output.getParentFile();
		if (parent != null && !parent.exists() && !parent.mkdirs()) {
			throw new ParseException(String.format("failed to create parent directory for %s", output));
		}
		return output;
	}

	private GroupFilter parseQuery(boolean mandatory) throws ParseException {
		if (!line.hasOption("query")) {
			if (mandatory) {
				throw new ParseException("missing mandatory query parameter");
			}
			return null;
		}
		String query = line.getOptionValue("query");
		try {
			return new FilterParser(query).parse();
		} catch (Exception ex) {
			throw new ParseException(String.format("failed to parse query: %s", ex.getMessage()));
		}
	}

	private Selection loadSelection(boolean source, boolean mandatory) throws ParseException {
		String name = source ? "source-selection" : "selection";
		if (!line.hasOption(name)) {
			if (mandatory) {
				throw new ParseException(String.format("missing mandatory %s parameter", name));
			}
			return null;
		}
		String fileString = line.getOptionValue(name);
		if (!fileString.toLowerCase().endsWith(".csv"))  {
			throw new ParseException(String.format("%s file has invalid format, .csv required", name));
		}
		File file = new File(fileString);
		try {
			return Selection.readFromFile(file);
		} catch (Exception ex) {
			throw new ParseException(String.format("failed to load %s: %s", name, ex.getMessage()));
		}
	}

	private void saveSelection(Selection selection, File output) throws RuntimeException {
		try {
			selection.saveToFile(output);
		} catch (IOException ex) {
			throw new RuntimeException(String.format("failed to save selection to %s", output), ex);
		}
	}

	private List<Range> parseSections(boolean mandatory) throws ParseException {
		if (!line.hasOption("sections")) {
			if (mandatory) {
				throw new ParseException("missing mandatory sections parameter");
			}
			return null;
		}
		return RangeParser.parseRanges(line.getOptionValue("sections"), ",");
	}

	private List<Field<?>> parseFields(boolean mandatory) throws ParseException {
		if (!line.hasOption("fields")) {
			if (mandatory) {
				throw new ParseException("missing mandatory fields parameter");
			}
			return null;
		}
		try {
			return new ChangeParser(line.getOptionValue("fields")).parse();
		} catch (Exception ex) {
			throw new ParseException(ex.getMessage());
		}
	}

	private Integer parseZoomLevel() throws ParseException {
		String value = line.getOptionValue("zoom-level");
		if (value == null) {
			return null;
		}
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

	private void handleException(Runnable r) {
		try {
			r.run();
		} catch (Exception ex) {
			LOGGER.error(ex);
			printError(ex.getMessage());
		}
	}

	// modes

	private void select(FutureTask<Boolean> future) throws ParseException {
		Config.setWorldDirs(parseWorldDirectories(""));
		File output = parseFileAndCreateParentDirectories("output", "csv");
		GroupFilter query = parseQuery(true);
		Selection selectionData = loadSelection(false, false);
		int radius = parseInt("radius", 0, 0, 128);

		Selection selection = new Selection();
		CLIProgress progress = new CLIProgress("selecting chunks");
		progress.onDone(() -> {
			handleException(() -> saveSelection(selection, output));
			future.run();
		});
		ChunkFilterSelector.selectFilter(query, selectionData, radius, selection::merge, progress, true);
	}

	private void export(FutureTask<Boolean> future) throws ParseException {
		Config.setWorldDirs(parseWorldDirectories(""));
		WorldDirectories output = parseAndCreateWorldDirectories("output");
		GroupFilter query = parseQuery(false);
		Selection selection = loadSelection(false, false);

		CLIProgress progress = new CLIProgress("exporting chunks");
		progress.onDone(future);
		if (query != null) {
			ChunkFilterExporter.exportFilter(query, selection, output, progress, true);
		} else if (selection != null) {
			SelectionExporter.exportSelection(selection, output, progress);
		} else {
			throw new ParseException("missing --query and/or --selection parameter");
		}
	}

	private void imp(FutureTask<Boolean> future) throws ParseException {
		Config.setWorldDirs(parseWorldDirectories(""));
		WorldDirectories source = parseWorldDirectories("source");
		int offsetX = parseInt("x-offset", 0);
		int offsetY = parseInt("y-offset", 0, -24, 24);
		int offsetZ = parseInt("z-offset", 0);
		Point3i offset = new Point3i(offsetX, offsetY, offsetZ);
		boolean overwrite = line.hasOption("overwrite");
		Selection sourceSelection = loadSelection(true, false);
		Selection targetSelection = loadSelection(false, false);
		List<Range> sections = parseSections(false);

		CLIProgress progress = new CLIProgress("importing chunks");
		progress.onDone(future);

		DataProperty<Map<Point2i, RegionDirectories>> tempFiles = new DataProperty<>();
		ChunkImporter.importChunks(source, progress, true, overwrite, sourceSelection, targetSelection, sections, offset, tempFiles);
		if (tempFiles.get() != null) {
			for (RegionDirectories tempFile : tempFiles.get().values()) {
				if (!tempFile.getRegion().delete()) {
					LOGGER.warn("failed to delete temp file {}", tempFile.getRegion());
				}
				if (!tempFile.getPoi().delete()) {
					LOGGER.warn("failed to delete temp file {}", tempFile.getPoi());
				}
				if (!tempFile.getEntities().delete()) {
					LOGGER.warn("failed to delete temp file {}", tempFile.getEntities());
				}
			}
		}
	}

	private void delete(FutureTask<Boolean> future) throws ParseException {
		Config.setWorldDirs(parseWorldDirectories(""));
		GroupFilter query = parseQuery(false);
		Selection selection = loadSelection(false, false);

		CLIProgress progress = new CLIProgress("deleting chunks");
		progress.onDone(future);

		if (query != null) {
			ChunkFilterDeleter.deleteFilter(query, selection, progress, true);
		} else if (selection != null) {
			SelectionDeleter.deleteSelection(selection, progress);
		} else {
			throw new ParseException("missing --query and/or --selection parameter");
		}
	}

	private void change(FutureTask<Boolean> future) throws ParseException {
		Config.setWorldDirs(parseWorldDirectories(""));
		Selection selection = loadSelection(false, false);
		boolean force = line.hasOption("force");
		List<Field<?>> fields = parseFields(true);

		CLIProgress progress = new CLIProgress("changing fields");
		progress.onDone(future);

		FieldChanger.changeNBTFields(fields, force, selection, progress, true);
	}

	private void cache(FutureTask<Boolean> future) throws ParseException, ExecutionException, InterruptedException {
		Config.setWorldDirs(parseWorldDirectories(""));
		if (CLIJFX.hasJavaFX()) {
			throw new ParseException("no JavaFX installation found");
		}

		File output = parseDirAndCreate("output");
		Config.setCacheDir(output);
		Integer zoomLevel = parseZoomLevel();

		CLIJFX.launch();

		CLIProgress progress = new CLIProgress("generating cache");
		progress.onDone(future);

		CacheHelper.forceGenerateCache(zoomLevel, progress);
	}

	private void image(FutureTask<Boolean> future) throws ParseException, ExecutionException, InterruptedException {
		Config.setWorldDirs(parseWorldDirectories(""));
		if (CLIJFX.hasJavaFX()) {
			throw new ParseException("no JavaFX installation found");
		}

		File output = parseFileAndCreateParentDirectories("output", "png");
		Selection selection = loadSelection(false, true);
		SelectionData data = new SelectionData(selection, Config.getWorldDirs());
		if (data.getWidth() *  16 * data.getHeight() * 16 > Integer.MAX_VALUE) {
			throw new ParseException(String.format("dimensions of %dx%d too large to generate an image", data.getWidth() * 16, data.getHeight() * 16));
		}

		CLIJFX.launch();

		DataProperty<int[]> pixels = new DataProperty<>();
		DataProperty<IOException> saveException = new DataProperty<>();
		CLIProgress saveProgress = new CLIProgress("generating image");
		saveProgress.onDone(() -> {
			if (saveException.get() != null) {
				throw new RuntimeException(saveException.get());
			}
			future.run();
		});
		CLIProgress generateProgress = new CLIProgress("saving image");
		generateProgress.onDone(() -> {
			if (!generateProgress.taskCancelled() && pixels.get() != null) {
				try {
					ImageHelper.saveImageData(pixels.get(), (int) data.getWidth() * 16, (int) data.getHeight() * 16, output, saveProgress);
				} catch (IOException ex) {
					saveException.set(ex);
				}
			}
		});

		// TODO: parse Overlays
		pixels.set(SelectionImageExporter.exportSelectionImage(data, null, generateProgress));
	}
}
