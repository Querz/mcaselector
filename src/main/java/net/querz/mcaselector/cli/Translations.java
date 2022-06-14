package net.querz.mcaselector.cli;

import net.querz.mcaselector.text.Translation;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
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
				try (OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream("src/main/resources/lang/" + locale + ".txt"), StandardCharsets.UTF_8)) {
					boolean first = true;
					for (Translation translation : Translation.values()) {
						osw.write((first ? "" : "\n") + translation.getKey() + ";" + (translation.isTranslated() ? translation.toString().replace("\n", "\\n") : ""));
						first = false;
					}
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
		} else {
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
			try (OutputStreamWriter osw = new OutputStreamWriter(System.out, StandardCharsets.UTF_8)) {
				for (Translation translation : Translation.values()) {
					osw.write(translation.getKey() + ";" + (translation.isTranslated() ? translation.toString().replace("\n", "\\n") : "") + "\n");
				}
			} catch (IOException ex) {
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
}
