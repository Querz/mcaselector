package net.querz.mcaselector.text;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum Translation {

	STATUS_REGION("status.region"),
	STATUS_CHUNK("status.chunk"),
	STATUS_BLOCK("status.block"),
	STATUS_SELECTED("status.selected"),
	STATUS_QUEUE("status.queue"),
	STATUS_TOTAL("status.total"),
	STATUS_OVERLAY("status.overlay"),
	MENU_FILE("menu.file"),
	MENU_VIEW("menu.view"),
	MENU_SELECTION("menu.selection"),
	MENU_TOOLS("menu.tools"),
	MENU_ABOUT("menu.about"),
	MENU_FILE_OPEN_WORLD("menu.file.open_world"),
	MENU_FILE_OPEN_DIMENSION("menu.file.open_dimension"),
	MENU_FILE_OPEN_RECENT_WORLD("menu.file.open_recent_world"),
	MENU_FILE_OPEN_RECENT_WORLD_CLEAR("menu.file.open_recent_world.clear"),
	MENU_FILE_SETTINGS("menu.file.settings"),
	MENU_FILE_RENDER_SETTINGS("menu.file.render_settings"),
	MENU_FILE_QUIT("menu.file.quit"),
	MENU_VIEW_RELOAD("menu.view.reload"),
	MENU_VIEW_CHUNK_GRID("menu.view.chunk_grid"),
	MENU_VIEW_REGION_GRID("menu.view.region_grid"),
	MENU_VIEW_COORDINATES("menu.view.coordinates"),
	MENU_VIEW_GOTO("menu.view.goto"),
	MENU_VIEW_RESET_ZOOM("menu.view.reset_zoom"),
	MENU_VIEW_SAVE_SCREENSHOT("menu.view.save_screenshot"),
	MENU_VIEW_CLEAR_CACHE("menu.view.clear_cache"),
	MENU_VIEW_CLEAR_ALL_CACHE("menu.view.clear_all_cache"),
	MENU_SELECTION_CLEAR("menu.selection.clear"),
	MENU_SELECTION_INVERT("menu.selection.invert"),
	MENU_SELECTION_INVERT_REGIONS("menu.selection.invert_regions"),
	MENU_SELECTION_COPY_CHUNKS("menu.selection.copy_chunks"),
	MENU_SELECTION_PASTE_CHUNKS("menu.selection.paste_chunks"),
	MENU_SELECTION_EXPORT_CHUNKS("menu.selection.export_chunks"),
	MENU_SELECTION_DELETE_CHUNKS("menu.selection.delete_chunks"),
	MENU_SELECTION_IMPORT_SELECTION("menu.selection.import_selection"),
	MENU_SELECTION_EXPORT_SELECTION("menu.selection.export_selection"),
	MENU_SELECTION_EXPORT_IMAGE("menu.selection.export_image"),
	MENU_SELECTION_CLEAR_CACHE("menu.selection.clear_cache"),
	MENU_TOOLS_IMPORT_CHUNKS("menu.tools.import_chunks"),
	MENU_TOOLS_FILTER_CHUNKS("menu.tools.filter_chunks"),
	MENU_TOOLS_CHANGE_FIELDS("menu.tools.change_fields"),
	MENU_TOOLS_EDIT_NBT("menu.tools.edit_nbt"),
	MENU_TOOLS_SWAP_CHUNKS("menu.tools.swap_chunks"),
	MENU_TOOLS_EDIT_OVERLAYS("menu.tools.edit_overlays"),
	MENU_TOOLS_NEXT_OVERLAY("menu.tools.next_overlay"),
	MENU_TOOLS_NEXT_OVERLAY_TYPE("menu.tools.next_overlay_type"),
	MENU_TOOLS_SUM_SELECTION("menu.tools.sum_selection"),
	DIALOG_SELECT_WORLD_TITLE("dialog.select_world.title"),
	DIALOG_SETTINGS_TITLE("dialog.settings.title"),
	DIALOG_SETTINGS_GLOBAL_LANGUAGE("dialog.settings.global.language"),
	DIALOG_SETTINGS_GLOBAL_LANGUAGE_LANGUAGE("dialog.settings.global.language.language"),
	DIALOG_SETTINGS_PROCESSING_PROCESS_PROCESS_THREADS("dialog.settings.processing.process.process_threads"),
	DIALOG_SETTINGS_PROCESSING_PROCESS("dialog.settings.processing.process"),
	DIALOG_SETTINGS_PROCESSING_PROCESS_WRITE_THREADS("dialog.settings.processing.process.write_threads"),
	DIALOG_SETTINGS_PROCESSING_FILES_MAX_FILES("dialog.settings.processing.files.max_files"),
	DIALOG_SETTINGS_GLOBAL_SELECTION_REGION_COLOR("dialog.settings.global.selection.region_color"),
	DIALOG_SETTINGS_GLOBAL_SELECTION_CHUNK_COLOR("dialog.settings.global.selection.chunk_color"),
	DIALOG_SETTINGS_GLOBAL_SELECTION_PASTED_CHUNKS_COLOR("dialog.settings.global.selection.pasted_chunks_color"),
	DIALOG_SETTINGS_RENDERING_SHADE_SHADE("dialog.settings.rendering.shade.shade"),
	DIALOG_SETTINGS_RENDERING_SHADE("dialog.settings.rendering.shade"),
	DIALOG_SETTINGS_RENDERING_SHADE_SHADE_WATER("dialog.settings.rendering.shade.shade_water"),
	DIALOG_SETTINGS_RENDERING_BACKGROUND_SHOW_NONEXISTENT_REGIONS("dialog.settings.rendering.background.show_nonexistent_regions"),
	DIALOG_SETTINGS_RENDERING_SMOOTH_SMOOTH_RENDERING("dialog.settings.rendering.smooth.smooth_rendering"),
	DIALOG_SETTINGS_RENDERING_SMOOTH_SMOOTH_OVERLAYS("dialog.settings.rendering.smooth.smooth_overlays"),
	DIALOG_SETTINGS_RENDERING_BACKGROUND_BACKGROUND_PATTERN("dialog.settings.rendering.background.background_pattern"),
	DIALOG_SETTINGS_RENDERING_BACKGROUND("dialog.settings.rendering.background"),
	DIALOG_SETTINGS_GLOBAL_MISC_MC_SAVES_DIR("dialog.settings.global.misc.mc_saves_dir"),
	DIALOG_SETTINGS_GLOBAL_MISC_PRINT_DEBUG("dialog.settings.global.misc.print_debug"),
	DIALOG_SETTINGS_GLOBAL_MISC_SHOW_LOG_FILE("dialog.settings.global.misc.show_log_file"),
	DIALOG_SETTINGS_RESET("dialog.settings.reset"),
	DIALOG_SETTINGS_TAB_GLOBAL("dialog.settings.tab_global"),
	DIALOG_SETTINGS_TAB_PROCESSING("dialog.settings.tab_processing"),
	DIALOG_SETTINGS_TAB_RENDERING("dialog.settings.tab_rendering"),
	DIALOG_SETTINGS_TAB_WORLD("dialog.settings.tab_world"),
	DIALOG_SETTINGS_GLOBAL_MISC("dialog.settings.global.misc"),
	DIALOG_SETTINGS_PROCESSING_FILES("dialog.settings.processing.files"),
	DIALOG_SETTINGS_RENDERING_SMOOTH("dialog.settings.rendering.smooth"),
	DIALOG_SETTINGS_GLOBAL_SELECTION("dialog.settings.global.selection"),
	DIALOG_SETTINGS_WORLD_PATHS("dialog.settings.world.paths"),
	DIALOG_SETTINGS_WORLD_PATHS_POI("dialog.settings.world.paths.poi"),
	DIALOG_SETTINGS_WORLD_PATHS_ENTITIES("dialog.settings.world.paths.entities"),
	DIALOG_SETTINGS_RENDERING_LAYERS_RENDER_HEIGHT("dialog.settings.rendering.layers.render_height"),
	DIALOG_SETTINGS_RENDERING_LAYERS_RENDER_LAYER_ONLY("dialog.settings.rendering.layers.render_layer_only"),
	DIALOG_SETTINGS_RENDERING_LAYERS("dialog.settings.rendering.layers"),
	DIALOG_SETTINGS_RENDERING_LAYERS_RENDER_CAVES("dialog.settings.rendering.layers.render_caves"),
	DIALOG_GOTO_TITLE("dialog.goto.title"),
	DIALOG_CONFIRMATION_QUESTION("dialog.confirmation.question"),
	DIALOG_DELETE_CHUNKS_CONFIRMATION_TITLE("dialog.delete_chunks_confirmation.title"),
	DIALOG_DELETE_CHUNKS_CONFIRMATION_HEADER_SHORT("dialog.delete_chunks_confirmation.header_short"),
	DIALOG_DELETE_CHUNKS_CONFIRMATION_HEADER_VERBOSE("dialog.delete_chunks_confirmation.header_verbose"),
	DIALOG_IMAGE_EXPORT_CONFIRMATION_TITLE("dialog.image_export_confirmation.title"),
	DIALOG_IMAGE_EXPORT_CONFIRMATION_HEADER_SHORT("dialog.image_export_confirmation.header_short"),
	DIALOG_IMAGE_EXPORT_CONFIRMATION_HEADER_VERBOSE("dialog.image_export_confirmation.header_verbose"),
	DIALOG_IMPORT_CHUNKS_CONFIRMATION_TITLE("dialog.import_chunks_confirmation.title"),
	DIALOG_IMPORT_CHUNKS_CONFIRMATION_HEADER("dialog.import_chunks_confirmation.header"),
	DIALOG_IMPORT_CHUNKS_CONFIRMATION_OPTIONS("dialog.import_chunks_confirmation.options"),
	DIALOG_IMPORT_CHUNKS_CONFIRMATION_OPTIONS_OFFSET("dialog.import_chunks_confirmation.options.offset"),
	DIALOG_IMPORT_CHUNKS_CONFIRMATION_OPTIONS_OVERWRITE("dialog.import_chunks_confirmation.options.overwrite"),
	DIALOG_IMPORT_CHUNKS_CONFIRMATION_OPTIONS_SELECTION_ONLY("dialog.import_chunks_confirmation.options.selection_only"),
	DIALOG_IMPORT_CHUNKS_CONFIRMATION_OPTIONS_Y_OFFSET("dialog.import_chunks_confirmation.options.y_offset"),
	DIALOG_IMPORT_CHUNKS_CONFIRMATION_OPTIONS_SECTIONS("dialog.import_chunks_confirmation.options.sections"),
	DIALOG_IMPORT_CHUNKS_CONFIRMATION_WARNING("dialog.import_chunks_confirmation.warning"),
	DIALOG_EXPORT_CHUNKS_CONFIRMATION_TITLE("dialog.export_chunks_confirmation.title"),
	DIALOG_EXPORT_CHUNKS_CONFIRMATION_HEADER_SHORT("dialog.export_chunks_confirmation.header_short"),
	DIALOG_EXPORT_CHUNKS_CONFIRMATION_HEADER_VERBOSE("dialog.export_chunks_confirmation.header_verbose"),
	DIALOG_IMPORT_SELECTION_TITLE("dialog.import_selection.title"),
	DIALOG_IMPORT_SELECTION_HEADER("dialog.import_selection.header"),
	DIALOG_IMPORT_SELECTION_OVERWRITE("dialog.import_selection.overwrite"),
	DIALOG_IMPORT_SELECTION_MERGE("dialog.import_selection.merge"),
	DIALOG_FILTER_CHUNKS_TITLE("dialog.filter_chunks.title"),
	DIALOG_FILTER_CHUNKS_FILTER_ADD_TOOLTIP("dialog.filter_chunks.filter.add.tooltip"),
	DIALOG_FILTER_CHUNKS_FILTER_DELETE_TOOLTIP("dialog.filter_chunks.filter.delete.tooltip"),
	DIALOG_FILTER_CHUNKS_FILTER_TYPE_TOOLTIP("dialog.filter_chunks.filter.type.tooltip"),
	DIALOG_FILTER_CHUNKS_FILTER_OPERATOR_TOOLTIP("dialog.filter_chunks.filter.operator.tooltip"),
	DIALOG_FILTER_CHUNKS_FILTER_COMPARATOR_TOOLTIP("dialog.filter_chunks.filter.comparator.tooltip"),
	DIALOG_FILTER_CHUNKS_SELECT("dialog.filter_chunks.select"),
	DIALOG_FILTER_CHUNKS_SELECT_TOOLTIP("dialog.filter_chunks.select.tooltip"),
	DIALOG_FILTER_CHUNKS_EXPORT("dialog.filter_chunks.export"),
	DIALOG_FILTER_CHUNKS_EXPORT_TOOLTIP("dialog.filter_chunks.export.tooltip"),
	DIALOG_FILTER_CHUNKS_DELETE("dialog.filter_chunks.delete"),
	DIALOG_FILTER_CHUNKS_DELETE_TOOLTIP("dialog.filter_chunks.delete.tooltip"),
	DIALOG_FILTER_CHUNKS_SELECTION_ONLY("dialog.filter_chunks.selection_only"),
	DIALOG_FILTER_CHUNKS_OVERWRITE_SELECTION("dialog.filter_chunks.overwrite_selection"),
	DIALOG_FILTER_CHUNKS_SELECTION_RADIUS("dialog.filter_chunks.selection_radius"),
	DIALOG_FILTER_CHUNKS_TAB_QUERY("dialog.filter_chunks.tab_query"),
	DIALOG_FILTER_CHUNKS_TAB_SCRIPT("dialog.filter_chunks.tab_script"),
	DIALOG_EDIT_OVERLAYS_TITLE("dialog.edit_overlays.title"),
	DIALOG_EDIT_OVERLAYS_OVERLAY_ACTIVE_TOOLTIP("dialog.edit_overlays.overlay_active.tooltip"),
	DIALOG_EDIT_OVERLAYS_DELETE_TOOLTIP("dialog.edit_overlays.delete.tooltip"),
	DIALOG_CHANGE_NBT_TITLE("dialog.change_nbt.title"),
	DIALOG_CHANGE_NBT_CHANGE("dialog.change_nbt.change"),
	DIALOG_CHANGE_NBT_CHANGE_TOOLTIP("dialog.change_nbt.change.tooltip"),
	DIALOG_CHANGE_NBT_FORCE("dialog.change_nbt.force"),
	DIALOG_CHANGE_NBT_FORCE_TOOLTIP("dialog.change_nbt.force.tooltip"),
	DIALOG_CHANGE_NBT_SELECTION_ONLY("dialog.change_nbt.selection_only"),
	DIALOG_CHANGE_NBT_SELECTION_ONLY_TOOLTIP("dialog.change_nbt.selection_only.tooltip"),
	DIALOG_CHANGE_NBT_CONFIRMATION_TITLE("dialog.change_nbt_confirmation.title"),
	DIALOG_CHANGE_NBT_CONFIRMATION_HEADER_SHORT("dialog.change_nbt_confirmation.header_short"),
	DIALOG_CHANGE_NBT_TAB_QUERY("dialog.change_nbt.tab_query"),
	DIALOG_CHANGE_NBT_TAB_SCRIPT("dialog.change_nbt.tab_script"),
	DIALOG_EDIT_NBT_TITLE("dialog.edit_nbt.title"),
	DIALOG_EDIT_NBT_PLACEHOLDER_LOADING("dialog.edit_nbt.placeholder.loading"),
	DIALOG_EDIT_NBT_PLACEHOLDER_NO_CHUNK_DATA("dialog.edit_nbt.placeholder.no_chunk_data"),
	DIALOG_EDIT_NBT_PLACEHOLDER_NO_REGION_FILE("dialog.edit_nbt.placeholder.no_region_file"),
	DIALOG_EDIT_ARRAY_INDEX("dialog.edit_array.index"),
	DIALOG_EDIT_ARRAY_VALUE("dialog.edit_array.value"),
	DIALOG_ABOUT_TITLE("dialog.about.title"),
	DIALOG_ABOUT_HEADER("dialog.about.header"),
	DIALOG_ABOUT_VERSION("dialog.about.version"),
	DIALOG_ABOUT_VERSION_UNKNOWN("dialog.about.version.unknown"),
	DIALOG_ABOUT_VERSION_CHECK("dialog.about.version.check"),
	DIALOG_ABOUT_VERSION_CHECKING("dialog.about.version.checking"),
	DIALOG_ABOUT_VERSION_UP_TO_DATE("dialog.about.version.up_to_date"),
	DIALOG_ABOUT_VERSION_ERROR("dialog.about.version.error"),
	DIALOG_ABOUT_LICENSE("dialog.about.license"),
	DIALOG_ABOUT_COPYRIGHT("dialog.about.copyright"),
	DIALOG_ABOUT_SOURCE("dialog.about.source"),
	DIALOG_PROGRESS_NO_FILES("dialog.progress.no_files"),
	DIALOG_PROGRESS_RUNNING("dialog.progress.running"),
	DIALOG_PROGRESS_CANCELLING("dialog.progress.cancelling"),
	DIALOG_PROGRESS_COLLECTING_DATA("dialog.progress.collecting_data"),
	DIALOG_PROGRESS_DONE("dialog.progress.done"),
	DIALOG_PROGRESS_SCANNING_FILES("dialog.progress.scanning_files"),
	DIALOG_PROGRESS_TITLE_LOADING_WORLD("dialog.progress.title.loading_world"),
	DIALOG_PROGRESS_TITLE_DELETING_SELECTION("dialog.progress.title.deleting_selection"),
	DIALOG_PROGRESS_TITLE_CREATING_IMAGE("dialog.progress.title.creating_image"),
	DIALOG_PROGRESS_TITLE_SAVING_IMAGE("dialog.progress.title.saving_image"),
	DIALOG_PROGRESS_TITLE_EXPORTING_SELECTION("dialog.progress.title.exporting_selection"),
	DIALOG_PROGRESS_TITLE_IMPORTING_CHUNKS("dialog.progress.title.importing_chunks"),
	DIALOG_PROGRESS_TITLE_DELETING_FILTERED_CHUNKS("dialog.progress.title.deleting_filtered_chunks"),
	DIALOG_PROGRESS_TITLE_EXPORTING_FILTERED_CHUNKS("dialog.progress.title.exporting_filtered_chunks"),
	DIALOG_PROGRESS_TITLE_SELECTING_FILTERED_CHUNKS("dialog.progress.title.selecting_filtered_chunks"),
	DIALOG_PROGRESS_TITLE_CHANGING_NBT_DATA("dialog.progress.title.changing_nbt_data"),
	DIALOG_PROGRESS_TITLE_SAVING_CHUNK("dialog.progress.title.saving_chunk"),
	DIALOG_PROGRESS_TITLE_RUNNING_BEFORE("dialog.progress.title.running_before"),
	DIALOG_PROGRESS_TITLE_RUNNING_AFTER("dialog.progress.title.running_after"),
	DIALOG_PROGRESS_TITLE_SUMMING("dialog.progress.title.summing"),
	DIALOG_ERROR_BUTTON_COPY_TO_CLIPBOARD("dialog.error.button.copy_to_clipboard"),
	DIALOG_ERROR_TITLE("dialog.error.title"),
	DIALOG_ERROR_HEADER("dialog.error.header"),
	DIALOG_ERROR_COPIED_TO_CLIPBOARD("dialog.error.copied_to_clipboard"),
	DIALOG_UNSAVED_SELECTION_TITLE("dialog.unsaved_selection.title"),
	DIALOG_UNSAVED_SELECTION_HEADER("dialog.unsaved_selection.header"),
	BUTTON_CANCEL("button.cancel"),
	BUTTON_OK("button.ok");


	private static final Logger LOGGER = LogManager.getLogger(Translation.class);

	private static final Set<Locale> availableLanguages = new HashSet<>();

	private static final Pattern languageFilePattern = Pattern.compile("^(?<locale>-?(?<language>-?[a-z]{2})_(?<country>-?[A-Z]{2}))\\.txt$");

	static {
		String[] langFiles = getResourceListing(Translation.class, "lang");
		if (langFiles != null) {
			for (String langFile : langFiles) {
				Matcher matcher = languageFilePattern.matcher(langFile);
				if (matcher.matches()) {
					String language = matcher.group("language");
					String country = matcher.group("country");
					availableLanguages.add(new Locale(language, country));
				} else {
					LOGGER.error("invalid language file: {}", langFile);
				}
			}
		}
	}

	private final TranslationStringProperty translationProperty;

	Translation(String key) {
		translationProperty = new TranslationStringProperty(key, null);
	}

	@Override
	public String toString() {
		return translationProperty.getValue();
	}

	static class TranslationStringProperty extends SimpleStringProperty {

		private final String key;

		public TranslationStringProperty(String key, String value) {
			super(value);
			this.key = key;
		}

		@Override
		public String getValue() {
			if (super.getValue() == null) {
				return "[" + key + "]";
			}
			return super.getValue();
		}

		public boolean isTranslated() {
			return super.getValue() != null;
		}

		public String format(Object... values) {
			String value = super.getValue();
			if (value == null) {
				return "[" + key + "]";
			}
			return String.format(value, values);
		}

		public String getKey() {
			return key;
		}
	}

	public boolean isTranslated() {
		return translationProperty.isTranslated();
	}

	public String getKey() {
		return translationProperty.key;
	}

	public StringProperty getProperty() {
		return translationProperty;
	}

	public static Set<Locale> getAvailableLanguages() {
		return availableLanguages;
	}

	private static void setTranslation(String key, String translation) {
		for (Translation t : Translation.values()) {
			if (t.translationProperty.getKey().equals(key)) {
				t.translationProperty.setValue(translation);
			}
		}
	}

	private static void clearTranslations() {
		for (Translation t : Translation.values()) {
			t.translationProperty.setValue(null);
		}
	}

	public static void load(Locale locale) {
		if (!availableLanguages.contains(locale)) {
			throw new IllegalArgumentException("unsupported locale " + locale);
		}

		clearTranslations();

		try (BufferedReader bis = new BufferedReader(new InputStreamReader(
				Objects.requireNonNull(Translation.class.getClassLoader().getResourceAsStream("lang/" + locale + ".txt")), StandardCharsets.UTF_8))) {
			String line;
			while ((line = bis.readLine()) != null) {
				String[] split = line.split(";", 2);
				if (split.length != 2) {
					LOGGER.error("invalid language mapping: {}", line);
					continue;
				}
				setTranslation(split[0], split[1].replace("\\n", "\n"));
			}
		} catch (IOException ex) {
			LOGGER.error("error reading {}.txt", locale, ex);
		}
	}

	public String format(Object... values) {
		return translationProperty.format(values);
	}

	private static String[] getResourceListing(Class<?> clazz, String path) {
		URL dirURL = clazz.getClassLoader().getResource(path);
		if (dirURL != null && dirURL.getProtocol().equals("file")) {
			try {
				return new File(dirURL.toURI()).list();
			} catch (URISyntaxException ex) {
				LOGGER.error("failed to list resources", ex);
				return null;
			}
		}

		if (dirURL == null) {
			String me = clazz.getName().replace(".", "/")+".class";
			dirURL = clazz.getClassLoader().getResource(me);
		}

		if (dirURL != null && dirURL.getProtocol().equals("jar")) {
			String jarPath = dirURL.getPath().substring(5, dirURL.getPath().lastIndexOf('!'));
			try (JarFile jar = new JarFile(URLDecoder.decode(jarPath, StandardCharsets.UTF_8))) {
				Enumeration<JarEntry> entries = jar.entries();
				Set<String> result = new HashSet<>();

				while (entries.hasMoreElements()) {
					String name = entries.nextElement().getName();
					if (name.startsWith(path)) {
						String entry = name.substring(path.length());
						int checkSubdir = entry.indexOf("/");
						if (entry.length() > 1) {
							entry = entry.substring(checkSubdir + 1);
							result.add(entry);
						}
					}
				}
				return result.toArray(new String[0]);
			} catch (IOException ex) {
				LOGGER.error("failed to decode jar file", ex);
				return null;
			}
		}
		throw new UnsupportedOperationException("cannot list files for URL " + dirURL);
	}
}
