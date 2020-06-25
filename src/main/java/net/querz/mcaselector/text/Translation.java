package net.querz.mcaselector.text;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import net.querz.mcaselector.debug.Debug;
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
	STATUS_REGION_TOOLTIP("status.region.tooltip"),
	STATUS_CHUNK("status.chunk"),
	STATUS_CHUNK_TOOLTIP("status.chunk.tooltip"),
	STATUS_BLOCK("status.block"),
	STATUS_BLOCK_TOOLTIP("status.block.tooltip"),
	STATUS_SELECTED("status.selected"),
	STATUS_SELECTED_TOOLTIP("status.selected.tooltip"),
	STATUS_VISIBLE("status.visible"),
	STATUS_VISIBLE_TOOLTIP("status.visible.tooltip"),
	STATUS_TOTAL("status.total"),
	STATUS_TOTAL_TOOLTIP("status.total.tooltip"),
	MENU_FILE("menu.file"),
	MENU_VIEW("menu.view"),
	MENU_SELECTION("menu.selection"),
	MENU_TOOLS("menu.tools"),
	MENU_ABOUT("menu.about"),
	MENU_FILE_OPEN("menu.file.open"),
	MENU_FILE_SETTINGS("menu.file.settings"),
	MENU_FILE_QUIT("menu.file.quit"),
	MENU_VIEW_CHUNK_GRID("menu.view.chunk_grid"),
	MENU_VIEW_REGION_GRID("menu.view.region_grid"),
	MENU_VIEW_GOTO("menu.view.goto"),
	MENU_VIEW_CLEAR_CACHE("menu.view.clear_cache"),
	MENU_VIEW_CLEAR_ALL_CACHE("menu.view.clear_all_cache"),
	MENU_SELECTION_CLEAR("menu.selection.clear"),
	MENU_SELECTION_COPY_CHUNKS("menu.selection.copy_chunks"),
	MENU_SELECTION_PASTE_CHUNKS("menu.selection.paste_chunks"),
	MENU_SELECTION_EXPORT_CHUNKS("menu.selection.export_chunks"),
	MENU_SELECTION_DELETE_CHUNKS("menu.selection.delete_chunks"),
	MENU_SELECTION_IMPORT_SELECTION("menu.selection.import_selection"),
	MENU_SELECTION_EXPORT_SELECTION("menu.selection.export_selection"),
	MENU_SELECTION_CLEAR_CACHE("menu.selection.clear_cache"),
	MENU_TOOLS_IMPORT_CHUNKS("menu.tools.import_chunks"),
	MENU_TOOLS_FILTER_CHUNKS("menu.tools.filter_chunks"),
	MENU_TOOLS_CHANGE_FIELDS("menu.tools.change_fields"),
	MENU_TOOLS_EDIT_NBT("menu.tools.edit_nbt"),
	MENU_TOOLS_SWAP_CHUNKS("menu.tools.swap_chunks"),
	DIALOG_SETTINGS_TITLE("dialog.settings.title"),
	DIALOG_SETTINGS_LANGUAGE("dialog.settings.language"),
	DIALOG_SETTINGS_READ_THREADS("dialog.settings.read_threads"),
	DIALOG_SETTINGS_PROCESS_THREADS("dialog.settings.process_threads"),
	DIALOG_SETTINGS_WRITE_THREADS("dialog.settings.write_threads"),
	DIALOG_SETTINGS_MAX_FILES("dialog.settings.max_files"),
	DIALOG_SETTINGS_REGION_COLOR("dialog.settings.region_color"),
	DIALOG_SETTINGS_CHUNK_COLOR("dialog.settings.chunk_color"),
	DIALOG_SETTINGS_SHADE("dialog.settings.shade"),
	DIALOG_SETTINGS_SHADE_WATER("dialog.settings.shade_water"),
	DIALOG_SETTINGS_PRINT_DEBUG("dialog.settings.print_debug"),
	DIALOG_SETTINGS_RESET("dialog.settings.reset"),
	DIALOG_GOTO_TITLE("dialog.goto.title"),
	DIALOG_CONFIRMATION_QUESTION("dialog.confirmation.question"),
	DIALOG_DELETE_CHUNKS_CONFIRMATION_TITLE("dialog.delete_chunks_confirmation.title"),
	DIALOG_DELETE_CHUNKS_CONFIRMATION_HEADER_SHORT("dialog.delete_chunks_confirmation.header_short"),
	DIALOG_DELETE_CHUNKS_CONFIRMATION_HEADER_VERBOSE("dialog.delete_chunks_confirmation.header_verbose"),
	DIALOG_IMPORT_CHUNKS_CONFIRMATION_TITLE("dialog.import_chunks_confirmation.title"),
	DIALOG_IMPORT_CHUNKS_CONFIRMATION_HEADER("dialog.import_chunks_confirmation.header"),
	DIALOG_IMPORT_CHUNKS_CONFIRMATION_OPTIONS("dialog.import_chunks_confirmation.options"),
	DIALOG_IMPORT_CHUNKS_CONFIRMATION_OPTIONS_OFFSET("dialog.import_chunks_confirmation.options.offset"),
	DIALOG_IMPORT_CHUNKS_CONFIRMATION_OPTIONS_OVERWRITE("dialog.import_chunks_confirmation.options.overwrite"),
	DIALOG_IMPORT_CHUNKS_CONFIRMATION_OPTIONS_SELECTION_ONLY("dialog.import_chunks_confirmation.options.selection_only"),
	DIALOG_IMPORT_CHUNKS_CONFIRMATION_OPTIONS_SECTIONS("dialog.import_chunks_confirmation.options.sections"),
	DIALOG_IMPORT_CHUNKS_CONFIRMATION_WARNING("dialog.import_chunks_confirmation.warning"),
	DIALOG_EXPORT_CHUNKS_CONFIRMATION_TITLE("dialog.export_chunks_confirmation.title"),
	DIALOG_EXPORT_CHUNKS_CONFIRMATION_HEADER_SHORT("dialog.export_chunks_confirmation.header_short"),
	DIALOG_EXPORT_CHUNKS_CONFIRMATION_HEADER_VERBOSE("dialog.export_chunks_confirmation.header_verbose"),
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
	DIALOG_FILTER_CHUNKS_SELECTION_RADIUS("dialog.filter_chunks.selection_radius"),
	DIALOG_FILTER_CHUNKS_SELECTION_ONLY_TOOLTIP("dialog.filter_chunks.selection_only.tooltip"),
	DIALOG_CHANGE_NBT_TITLE("dialog.change_nbt.title"),
	DIALOG_CHANGE_NBT_CHANGE("dialog.change_nbt.change"),
	DIALOG_CHANGE_NBT_CHANGE_TOOLTIP("dialog.change_nbt.change.tooltip"),
	DIALOG_CHANGE_NBT_FORCE("dialog.change_nbt.force"),
	DIALOG_CHANGE_NBT_FORCE_TOOLTIP("dialog.change_nbt.force.tooltip"),
	DIALOG_CHANGE_NBT_SELECTION_ONLY("dialog.change_nbt.selection_only"),
	DIALOG_CHANGE_NBT_SELECTION_ONLY_TOOLTIP("dialog.change_nbt.selection_only.tooltip"),
	DIALOG_CHANGE_NBT_CONFIRMATION_TITLE("dialog.change_nbt_confirmation.title"),
	DIALOG_CHANGE_NBT_CONFIRMATION_HEADER_SHORT("dialog.change_nbt_confirmation.header_short"),
	DIALOG_CHANGE_NBT_CONFIRMATION_HEADER_VERBOSE("dialog.change_nbt_confirmation.header_verbose"),
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
	DIALOG_PROGRESS_TITLE("dialog.progress.title"),
	DIALOG_PROGRESS_NO_FILES("dialog.progress.no_files"),
	DIALOG_PROGRESS_RUNNING("dialog.progress.running"),
	DIALOG_PROGRESS_CANCELLING("dialog.progress.cancelling"),
	DIALOG_PROGRESS_COLLECTING_DATA("dialog.progress.collecting_data"),
	DIALOG_PROGRESS_DONE("dialog.progress.done"),
	DIALOG_PROGRESS_TITLE_DELETING_SELECTION("dialog.progress.title.deleting_selection"),
	DIALOG_PROGRESS_TITLE_EXPORTING_SELECTION("dialog.progress.title.exporting_selection"),
	DIALOG_PROGRESS_TITLE_IMPORTING_CHUNKS("dialog.progress.title.importing_chunks"),
	DIALOG_PROGRESS_TITLE_DELETING_FILTERED_CHUNKS("dialog.progress.title.deleting_filtered_chunks"),
	DIALOG_PROGRESS_TITLE_EXPORTING_FILTERED_CHUNKS("dialog.progress.title.exporting_filtered_chunks"),
	DIALOG_PROGRESS_TITLE_SELECTING_FILTERED_CHUNKS("dialog.progress.title.selecting_filtered_chunks"),
	DIALOG_PROGRESS_TITLE_CHANGING_NBT_DATA("dialog.progress.title.changing_nbt_data"),
	BUTTON_CANCEL("button.cancel"),
	BUTTON_OK("button.ok");

	private static final Set<Locale> availableLanguages = new HashSet<>();

	private static final Pattern languangeFilePattern = Pattern.compile("^(?<locale>-?(?<language>-?[a-z]{2})_(?<country>-?[A-Z]{2}))\\.txt$");

	static {
		String[] langFiles = getResourceListing(Translation.class, "lang");
		if (langFiles != null) {
			for (String langFile : langFiles) {
				Matcher matcher = languangeFilePattern.matcher(langFile);
				if (matcher.matches()) {
					String language = matcher.group("language");
					String country = matcher.group("country");
					availableLanguages.add(new Locale(language, country));
				} else {
					Debug.error("invalid language file: " + langFile);
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
					Debug.dumpf("invalid language mapping: %s", line);
					continue;
				}
				setTranslation(split[0], split[1].replace("\\n", "\n"));
			}
		} catch (IOException ex) {
			Debug.dumpException(String.format("error reading %s.txt", locale), ex);
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
				Debug.dumpException("failed to list resources", ex);
				return null;
			}
		}

		if (dirURL == null) {
			String me = clazz.getName().replace(".", "/")+".class";
			dirURL = clazz.getClassLoader().getResource(me);
		}

		if (dirURL != null && dirURL.getProtocol().equals("jar")) {
			String jarPath = dirURL.getPath().substring(5, dirURL.getPath().indexOf("!"));
			JarFile jar;
			try {
				jar = new JarFile(URLDecoder.decode(jarPath, "UTF-8"));
			} catch (IOException ex) {
				Debug.dumpException("failed to decode jar file", ex);
				return null;
			}
			Enumeration<JarEntry> entries = jar.entries();
			Set<String> result = new HashSet<>();

			while(entries.hasMoreElements()) {
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
		}
		throw new UnsupportedOperationException("cannot list files for URL " + dirURL);
	}
}
