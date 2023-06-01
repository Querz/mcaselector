package net.querz.mcaselector.cli;

import net.querz.mcaselector.changer.ChangeParser;
import net.querz.mcaselector.changer.Field;
import net.querz.mcaselector.config.Config;
import net.querz.mcaselector.config.ConfigProvider;
import net.querz.mcaselector.config.GlobalConfig;
import net.querz.mcaselector.config.WorldConfig;
import net.querz.mcaselector.filter.FilterParser;
import net.querz.mcaselector.filter.filters.GroupFilter;
import net.querz.mcaselector.io.*;
import net.querz.mcaselector.io.job.*;
import net.querz.mcaselector.overlay.Overlay;
import net.querz.mcaselector.overlay.OverlayParser;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.point.Point3i;
import net.querz.mcaselector.property.DataProperty;
import net.querz.mcaselector.range.Range;
import net.querz.mcaselector.range.RangeParser;
import net.querz.mcaselector.selection.Selection;
import net.querz.mcaselector.selection.SelectionData;
import net.querz.mcaselector.tile.OverlayPool;
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
		options.addOption(Option.builder()
			.longOpt("sections")
			.desc("One or a range of section indices to import into the target world during chunk import")
			.hasArg()
			.build());
		options.addOption(Option.builder()
			.longOpt("render-height")
			.desc("The highest Y level to render in image mode")
			.hasArg()
			.build());
		options.addOption(Option.builder()
			.longOpt("render-caves")
			.desc("Enabled cave rendering in image mode")
			.build());
		options.addOption(Option.builder()
			.longOpt("render-layer-only")
			.desc("Only render the layer specified by --render-height in image mode")
			.build());
		options.addOption(Option.builder()
			.longOpt("render-shade")
			.desc("Enable or disable shading of terrain and water in image mode")
			.hasArg()
			.build());
		options.addOption(Option.builder()
			.longOpt("render-water-shade")
			.desc("Enable or disable shading of water in image mode")
			.hasArg()
			.build());
		options.addOption(Option.builder()
			.longOpt("overlay-type")
			.desc("The type of overlay to be rendered in image mode")
			.hasArg()
			.build());
		options.addOption(Option.builder()
			.longOpt("overlay-min-value")
			.desc("The minimum value to be used for the overlay in image mode")
			.hasArg()
			.build());
		options.addOption(Option.builder()
			.longOpt("overlay-max-value")
			.desc("The maximum value to be used for the overlay in image mode")
			.hasArg()
			.build());
		options.addOption(Option.builder()
			.longOpt("overlay-data")
			.desc("Additional data to be used for the overlay in image mode")
			.hasArg()
			.build());
		options.addOption(Option.builder()
			.longOpt("overlay-min-hue")
			.desc("The minimum hue for the overlay gradient, ranging from 0.0 to 1.0")
			.hasArg()
			.build());
		options.addOption(Option.builder()
			.longOpt("overlay-max-hue")
			.desc("The maximum hue for the overlay gradient, ranging from 0.0 to 1.0; When smaller than overlay-min-hue the gradient is flipped")
			.hasArg()
			.build());
		options.addOption(Option.builder()
			.longOpt("fields")
			.desc("The fields to change")
			.hasArg()
			.build());
		options.addOption(Option.builder()
			.longOpt("zoom-level")
			.desc("The zoom level for the cache to be generated. When not specified, all zoom levels will be generated")
			.hasArg()
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
		if (Arrays.asList(args).contains("--use-alternative-command-parsing")) {
			try {
				String[] altArgs = new CustomCommandParser(args).parse();
				List<String> altList = new ArrayList<>(Arrays.asList(altArgs));
				altList.remove("--use-alternative-command-parsing");
				this.args = altList.toArray(new String[0]);
				LOGGER.warn("preprocessed args with custom command parser, original args: {}", Arrays.toString(args));
				return;
			} catch (net.querz.mcaselector.exception.ParseException e) {
				throw new RuntimeException(e);
			}
		}
		this.args = args;
	}

	public Future<Boolean> run() {
		LOGGER.debug("raw args: {}", Arrays.toString(args));
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

		LOGGER.debug("parsed args: {}", parsedArgsToString());

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

			JobHandler.setTrimSaveData(false);

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
			"y-offset", "z-offset", "overwrite", "force", "sections", "render-height", "render-caves", "render-layer-only",
			"render-shade", "render-water-shade", "overlay-type", "overlay-min-value", "overlay-max-value",
			"overlay-data", "overlay-min-hue", "overlay-max-hue", "fields", "zoom-level", "world", "region", "poi", "entities",
			"source-world", "source-region", "source-poi", "source-entities", "output-world", "output-region",
			"output-poi", "output-entities", "debug", "process-threads", "write-threads"
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
		ConfigProvider.GLOBAL = new GlobalConfig();
		ConfigProvider.GLOBAL.setDebug(line.hasOption("debug"));
		ConfigProvider.GLOBAL.setProcessThreads(parseInt("process-threads", GlobalConfig.DEFAULT_PROCESS_THREADS, 1, 128));
		ConfigProvider.GLOBAL.setProcessThreads(parseInt("write-threads", GlobalConfig.DEFAULT_WRITE_THREADS, 1, 128));
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

	private float parseFloat(String key, boolean mandatory, float def, float min, float max) throws ParseException {
		if (!line.hasOption(key)) {
			if (mandatory) {
				throw new ParseException(String.format("missing mandatory %s parameter", key));
			}
			return def;
		}
		String value = line.getOptionValue(key);
		float f;
		try {
			f = Float.parseFloat(value);
		} catch (NumberFormatException ex) {
			throw new ParseException(String.format("parameter for argument %s is not a valid decimal number", key));
		}
		if (f < min) {
			throw new ParseException(String.format("%s cannot be smaller than %f", key, min));
		}
		if (f > max) {
			throw new ParseException(String.format("%s cannot be larger than %f", key, max));
		}
		return f;
	}

	private boolean parseBoolean(String key, boolean mandatory, boolean def) throws ParseException {
		if (!line.hasOption(key)) {
			if (mandatory) {
				throw new ParseException(String.format("missing mandatory %s parameter", key));
			}
			return def;
		}
		String value = line.getOptionValue(key);
		if ("true".equalsIgnoreCase(value) || "1".equals(value)) {
			return true;
		} else if ("false".equalsIgnoreCase(value) || "0".equals(value)) {
			return false;
		} else {
			throw new ParseException(String.format("invalid boolean value %s for %s", value, key));
		}
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
			if (!poi.mkdirs()) {
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
		for (int z = Config.MIN_ZOOM_LEVEL; z <= Config.MAX_ZOOM_LEVEL; z *= 2) {
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
		ConfigProvider.WORLD = new WorldConfig();
		ConfigProvider.WORLD.setWorldDirs(parseWorldDirectories(""));
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
		ConfigProvider.WORLD = new WorldConfig();
		ConfigProvider.WORLD.setWorldDirs(parseWorldDirectories(""));
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
		ConfigProvider.WORLD = new WorldConfig();
		ConfigProvider.WORLD.setWorldDirs(parseWorldDirectories(""));
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
		ConfigProvider.WORLD = new WorldConfig();
		ConfigProvider.WORLD.setWorldDirs(parseWorldDirectories(""));
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
		ConfigProvider.WORLD = new WorldConfig();
		ConfigProvider.WORLD.setWorldDirs(parseWorldDirectories(""));
		Selection selection = loadSelection(false, false);
		boolean force = line.hasOption("force");
		List<Field<?>> fields = parseFields(true);

		CLIProgress progress = new CLIProgress("changing fields");
		progress.onDone(future);

		FieldChanger.changeNBTFields(fields, force, selection, progress, true);
	}

	private void cache(FutureTask<Boolean> future) throws ParseException, ExecutionException, InterruptedException {
		ConfigProvider.WORLD = new WorldConfig();
		ConfigProvider.WORLD.setWorldDirs(parseWorldDirectories(""));
		if (!CLIJFX.hasJavaFX()) {
			throw new ParseException("no JavaFX installation found");
		}

		File output = parseDirAndCreate("output");
		ConfigProvider.WORLD.setCacheDir(output);
		Integer zoomLevel = parseZoomLevel();

		CLIJFX.launch();

		CLIProgress progress = new CLIProgress("generating cache");
		progress.onDone(future);

		CacheHelper.forceGenerateCache(zoomLevel, progress);
	}

	private void image(FutureTask<Boolean> future) throws ParseException, ExecutionException, InterruptedException {
		ConfigProvider.WORLD = new WorldConfig();
		ConfigProvider.WORLD.setWorldDirs(parseWorldDirectories(""));
		if (!CLIJFX.hasJavaFX()) {
			throw new ParseException("no JavaFX installation found");
		}

		File output = parseFileAndCreateParentDirectories("output", "png");
		Selection selection = loadSelection(false, true);
		SelectionData data = new SelectionData(selection, ConfigProvider.WORLD.getWorldDirs());
		if (data.getWidth() *  16 * data.getHeight() * 16 > Integer.MAX_VALUE) {
			throw new ParseException(String.format("dimensions of %dx%d too large to generate an image", data.getWidth() * 16, data.getHeight() * 16));
		}
		// render height, cave render, layer only, shade, shade water
		int renderHeight = parseInt("render-height", 319, -64, 319);
		if (line.hasOption("render-caves") && line.hasOption("render-layer-only")) {
			throw new ParseException("render-caves and render-layer-only cannot be used together");
		}
		boolean renderCaves = line.hasOption("render-caves");
		boolean renderLayerOnly = line.hasOption("render-layer-only");

		if ((renderCaves || renderLayerOnly) && (line.hasOption("render-shade") || line.hasOption("render-water-shade"))) {
			throw new ParseException("render-shade or render-water-shade cannot be used with render-caves or render-layer-only");
		}
		boolean renderShade = parseBoolean("render-shade", false, !renderCaves && !renderLayerOnly);
		boolean renderWaterShade = parseBoolean("render-water-shade", false, !renderCaves && !renderLayerOnly);

		ConfigProvider.WORLD.setRenderHeight(renderHeight);
		ConfigProvider.WORLD.setRenderCaves(renderCaves);
		ConfigProvider.WORLD.setRenderLayerOnly(renderLayerOnly);
		ConfigProvider.WORLD.setShade(renderShade);
		ConfigProvider.WORLD.setShadeWater(renderWaterShade);

		CLIJFX.launch();

		DataProperty<int[]> pixels = new DataProperty<>();
		DataProperty<IOException> saveException = new DataProperty<>();
		CLIProgress saveProgress = new CLIProgress("saving image");
		saveProgress.onDone(() -> {
			if (saveException.get() != null) {
				throw new RuntimeException(saveException.get());
			}
			future.run();
		});
		CLIProgress generateProgress = new CLIProgress("generating image");
		generateProgress.onDone(() -> {
			if (!generateProgress.taskCancelled() && pixels.get() != null) {
				try {
					ImageHelper.saveImageData(pixels.get(), (int) data.getWidth() * 16, (int) data.getHeight() * 16, output, saveProgress);
				} catch (IOException ex) {
					saveException.set(ex);
				}
			}
		});

		OverlayPool overlayPool = null;
		if (line.hasOption("overlay-type")) {
			String type = line.getOptionValue("overlay-type");
			String min = line.getOptionValue("overlay-min-value");
			String max = line.getOptionValue("overlay-max-value");
			String additionalData = line.getOptionValue("overlay-data");
			float minHue = parseFloat("overlay-min-hue", false, 0.66666667f, 0.0f, 1.0f);
			float maxHue = parseFloat("overlay-max-hue", false, 0.0f, 0.0f, 1.0f);
			try {
				Overlay overlay = new OverlayParser(type, min, max, additionalData, minHue, maxHue).parse();
				overlayPool = new OverlayPool(null);
				overlayPool.switchTo(new File(ConfigProvider.WORLD.getCacheDir(), "cache.db").toString(), List.of(overlay));
				overlayPool.setParser(overlay);
			} catch (Exception ex) {
				throw new ParseException(ex.getMessage());
			}
		}

		pixels.set(SelectionImageExporter.exportSelectionImage(data, overlayPool, generateProgress));
	}

	private String parsedArgsToString() {
		StringBuilder sb = new StringBuilder("{");
		for (int o = 0; o < line.getOptions().length; o++) {
			Option option = line.getOptions()[o];
			sb.append(option.getLongOpt()).append(": [");
			if (option.getValues() != null) {
				for (int v = 0; v < option.getValues().length; v++) {
					sb.append(option.getValues()[v]).append(v < option.getValues().length - 1 ? ", " : "");
				}
			}
			sb.append("]").append(o < line.getOptions().length - 1 ? ", " : "");
		}
		sb.append("}");
		return sb.toString();
	}
}
