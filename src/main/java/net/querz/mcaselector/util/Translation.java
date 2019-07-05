package net.querz.mcaselector.util;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
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
	MENU_SELECTION_EXPORT_CHUNKS("menu.selection.export_chunks"),
	MENU_SELECTION_DELETE_CHUNKS("menu.selection.delete_chunks"),
	MENU_SELECTION_IMPORT_SELECTION("menu.selection.import_selection"),
	MENU_SELECTION_EXPORT_SELECTION("menu.selection.export_selection"),
	MENU_SELECTION_CLEAR_CACHE("menu.selection.clear_cache"),
	MENU_TOOLS_IMPORT_CHUNKS("menu.tools.import_chunks"),
	MENU_TOOLS_FILTER_CHUNKS("menu.tools.filter_chunks"),
	MENU_TOOLS_CHANGE_FIELDS("menu.tools.change_fields"),
	DIALOG_SETTINGS_TITLE("dialog.settings.title"),
	DIALOG_SETTINGS_LANGUAGE("dialog.settings.language"),
	DIALOG_SETTINGS_READ_THREADS("dialog.settings.read_threads"),
	DIALOG_SETTINGS_PROCESS_THREADS("dialog.settings.process_threads"),
	DIALOG_SETTINGS_WRITE_THREADS("dialog.settings.write_threads"),
	DIALOG_SETTINGS_MAX_FILES("dialog.settings.max_files"),
	DIALOG_SETTINGS_REGION_COLOR("dialog.settings.region_color"),
	DIALOG_SETTINGS_CHUNK_COLOR("dialog.settings.chunk_color"),
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
	DIALOG_IMPORT_CHUNKS_CONFIRMATION_OPTIONS_OVERWRITE("dialog.import_chunks_confirmation.options.overwrite"),
	DIALOG_EXPORT_CHUNKS_CONFIRMATION_TITLE("dialog.export_chunks_confirmation.title"),
	DIALOG_EXPORT_CHUNKS_CONFIRMATION_HEADER_SHORT("dialog.export_chunks_confirmation.header_short"),
	DIALOG_EXPORT_CHUNKS_CONFIRMATION_HEADER_VERBOSE("dialog.export_chunks_confirmation.header_verbose"),
	DIALOG_FILTER_CHUNKS_TITLE("dialog.filter_chunks.title"),
	DIALOG_FILTER_CHUNKS_FILTER_GROUP("dialog.filter_chunks.filter.group"),
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
	DIALOG_PROGRESS_RUNNING("dialog.progress.running"),
	DIALOG_PROGRESS_CANCELLING("dialog.progress.cancelling"),
	DIALOG_PROGRESS_DONE("dialog.progress.done"),
	BUTTON_CANCEL("button.cancel"),
	BUTTON_OK("button.ok");

	private static Set<Locale> availableLanguages = new HashSet<>();

	private static final Pattern languangeFilePattern = Pattern.compile("^(?<locale>-?(?<language>-?[a-z]{2})_(?<country>-?[A-Z]{2}))\\.txt$");

	static {
		try (BufferedReader br = new BufferedReader(new InputStreamReader(getResourceAsStream("lang")))) {
			String resource;
			while ((resource = br.readLine()) != null) {
				Matcher matcher = languangeFilePattern.matcher(resource);
				if (matcher.matches()) {
					String language = matcher.group("language");
					String country = matcher.group("country");
					availableLanguages.add(new Locale(language, country));
				} else {
					Debug.error("invalid language file: " + resource);
				}
			}
		} catch (IOException ex) {
			Debug.error(ex.getMessage());
		}
	}

	private static InputStream getResourceAsStream(String resource) {
		final InputStream in = getContextClassLoader().getResourceAsStream(resource);
		return in == null ? Translation.class.getClassLoader().getResourceAsStream(resource) : in;
	}

	private static ClassLoader getContextClassLoader() {
		return Thread.currentThread().getContextClassLoader();
	}

	private TranslationStringProperty translationProperty;

	Translation(String key) {
		translationProperty = new TranslationStringProperty(key, null);
	}

	@Override
	public String toString() {
		return translationProperty.getValue();
	}

	static class TranslationStringProperty extends SimpleStringProperty {

		private String key;

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
				Translation.class.getClassLoader().getResourceAsStream("lang/" + locale + ".txt")))) {
			String line;
			while ((line = bis.readLine()) != null) {
				String[] split = line.split(";", 2);
				if (split.length != 2) {
					Debug.dumpf("invalid language mapping: %s", line);
					continue;
				}
				setTranslation(split[0], split[1]);
			}
		} catch (IOException ex) {
			Debug.error("error reading " + locale + ".txt: ", ex.getMessage());
		}
	}

	public String format(Object... values) {
		return translationProperty.format(values);
	}
}
