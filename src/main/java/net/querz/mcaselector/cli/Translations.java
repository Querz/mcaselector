package net.querz.mcaselector.cli;

import net.querz.mcaselector.text.Translation;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.FutureTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Translations {

	static void printMissingTranslations(FutureTask<Boolean> future) {
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

	static void printTranslation(CommandLine line, FutureTask<Boolean> future) throws ParseException {
		String l = line.getOptionValue("locale");
		if (l == null) {
			throw new ParseException("no locale");
		}

		if (l.equals("updateResources")) {
			Set<Locale> locales = Translation.getAvailableLanguages();
			for (Locale locale : locales) {
				Translation.load(locale);
				saveTranslationResource(locale);
			}
		} else {
			Pattern languageFilePattern = Pattern.compile("^(?<locale>-?(?<language>-?[a-z]{2})_(?<country>-?[A-Z]{2}))$");
			Locale locale;
			Matcher matcher = languageFilePattern.matcher(l);
			if (matcher.matches()) {
				String language = matcher.group("language");
				String country = matcher.group("country");
				locale = Locale.of(language, country);
			} else {
				throw new ParseException("invalid locale " + l);
			}

			Translation.load(locale);
			try (OutputStreamWriter osw = new OutputStreamWriter(System.out, StandardCharsets.UTF_8)) {
				for (Translation translation : Translation.values()) {
					osw.write(translation.getKey() + ";" + (translation.isTranslated() ? translation.toString().replace("\n", "\\n") : "") + "\n");
				}
			} catch (IOException ex) {
				//noinspection CallToPrintStackTrace
				ex.printStackTrace();
			}
		}
		future.run();
	}

	static void printTranslationKeys(FutureTask<Boolean> future) {
		for (Translation translation : Translation.values()) {
			System.out.println(translation.getKey() + ";");
		}
		future.run();
	}

	static void importTranslations(CommandLine line, FutureTask<Boolean> future) throws ParseException, IOException {
		String f = line.getOptionValue("import-file");
		Path file = Path.of(f);
		if (!Files.isRegularFile(file)) {
			throw new ParseException("no import file");
		}

		List<String> lines = Files.readAllLines(file);

		Locale currentLanguage = null;
		for (String importLine : lines) {
			String l = importLine.trim();
			if (l.isEmpty()) {
				continue;
			}
			if (l.matches("^[a-z]{2}_[A-Z]{2}$")) {
				String[] ll = l.split("_");
				saveTranslationResource(currentLanguage);
				currentLanguage = Locale.of(ll[0], ll[1]);
				if (currentLanguage == null) {
					throw new ParseException("invalid locale " + l);
				}
				Translation.load(currentLanguage);
				continue;
			}
			String[] t = l.split(";", 2);
			System.out.println(l);
			Translation.setTranslation(t[0], t[1]);
		}
		saveTranslationResource(currentLanguage);
		future.run();
	}

	private static void saveTranslationResource(Locale locale) {
		if (locale == null) {
			return;
		}
		try (OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream("src/main/resources/lang/" + locale + ".txt"), StandardCharsets.UTF_8)) {
			boolean first = true;
			for (Translation translation : Translation.values()) {
				osw.write((first ? "" : "\n") + translation.getKey() + ";" + (translation.isTranslated() ? translation.toString().replace("\n", "\\n") : ""));
				first = false;
			}
		} catch (IOException ex) {
			//noinspection CallToPrintStackTrace
			ex.printStackTrace();
		}
	}
}
