package net.querz.mcaselector.ui.component;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import net.querz.mcaselector.io.GroovyScriptEngine;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.reactfx.Subscription;
import javax.script.ScriptException;
import java.io.Closeable;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GroovyCodeArea extends CodeArea implements Closeable {

	private final ExecutorService executor;
	private final Subscription highlightSubscription;
	private Subscription evalSubscription;
	private GroovyScriptEngine engine;

	private final StringProperty error = new SimpleStringProperty();

	public ObservableValue<String> errorProperty() {
		return error;
	}

	private static final String[] KEYWORDS = new String[] {
			"abstract", "as", "assert", "boolean", "break", "byte",
			"case", "catch", "char", "class", "const",
			"continue", "def", "default", "do", "double", "else",
			"enum", "extends", "false", "final", "finally", "float",
			"for", "goto", "if", "implements", "import", "in",
			"instanceof", "int", "interface", "long", "native",
			"new", "null", "package", "private", "protected", "public",
			"return", "short", "static", "strictfp", "super",
			"switch", "synchronized", "this", "threadsafe", "throw", "throws",
			"transient", "true", "try", "void", "volatile", "while"
	};

	private static final String KEYWORD_PATTERN = "(?<KEYWORD>\\b(" + String.join("|", KEYWORDS) + ")\\b)";
	private static final String PAREN_PATTERN = "(?<PAREN>[()])";
	private static final String BRACE_PATTERN = "(?<BRACE>[{}])";
	private static final String BRACKET_PATTERN = "(?<BRACKET>[\\[\\]])";
	private static final String SEMICOLON_PATTERN = "(?<SEMICOLON>;)";
	private static final String STRING_PATTERN = "(?<STRING>[\"']([^\"'\\\\]|\\\\.)*[\"'])";
	private static final String COMMENT_PATTERN = "(?<COMMENT>//[^\n]*|/\\*(.|\\R)*?\\*/)";
	private static final String FUNCTION_PATTERN = "(?<FUNCTION>[a-zA-Z_$][\\w$]*)\\(";

	private static final Pattern PATTERN = Pattern.compile(
			KEYWORD_PATTERN + "|" +
			PAREN_PATTERN + "|" +
			BRACE_PATTERN + "|" +
			BRACKET_PATTERN + "|" +
			SEMICOLON_PATTERN + "|" +
			STRING_PATTERN + "|" +
			COMMENT_PATTERN + "|" +
			FUNCTION_PATTERN
	);

	public GroovyCodeArea(boolean eval) {
		executor = Executors.newSingleThreadExecutor();
		setParagraphGraphicFactory(LineNumberFactory.get(this));
		highlightSubscription = multiPlainChanges()
				.successionEnds(Duration.ofMillis(500))
				.retainLatestUntilLater(executor)
				.supplyTask(this::computeHighlightingAsync)
				.awaitLatest(multiPlainChanges())
				.filterMap(t -> {
					if (t.isSuccess()) {
						return Optional.of(t.get());
					} else {
						t.getFailure().printStackTrace();
						return Optional.empty();
					}
				})
				.subscribe(h -> {
					System.out.println(h);
					setStyleSpans(0, h);
				});

		if (eval) {
			engine = new GroovyScriptEngine();
			evalSubscription = multiPlainChanges()
					.successionEnds(Duration.ofMillis(500))
					.retainLatestUntilLater(executor)
					.supplyTask(this::evalAsync)
					.awaitLatest(multiPlainChanges())
					.filterMap(t -> {
						if (t.isSuccess()) {
							return Optional.of(t.get());
						} else {
							t.getFailure().printStackTrace();
							return Optional.empty();
						}
					})
					.subscribe(error::set);
		}

		getStylesheets().add(GroovyCodeArea.class.getClassLoader().getResource("style/component/groovy-code-area.css").toExternalForm());
	}

	private Task<String> evalAsync() {
		String text = getText();
		Task<String> task = new Task<>() {
			@Override
			protected String call() {
				try {
					engine.eval(text);
					return "";
				} catch (ScriptException e) {
					return readInfo(e, text);
				}
			}
		};
		executor.execute(task);
		return task;
	}

	private final Pattern infoPattern = Pattern.compile("Script\\d+.groovy: \\d+: (.*) @ line (\\d+), column (\\d+).");

	private String readInfo(ScriptException ex, String script) {
		Matcher m = infoPattern.matcher(ex.getMessage());
		if (m.find()) {
			String msg = m.group(1);
			int line = Integer.parseInt(m.group(2));
			int column = Integer.parseInt(m.group(3));
			String[] scriptLines = script.split("\n", line + 1);
			String scriptLine = scriptLines[line - 1];
			String scriptLineLeft = scriptLine.substring(0, column - 1);
			int tabs = (int) scriptLineLeft.chars().filter(c -> c == '\t').count();
			String arrow = "-".repeat(column + tabs * 3 - 1) + "^";
			scriptLine = scriptLine.replace("\t", "    "); // replace all tabs with spaces because we can't set the tab size in labels
			return String.format("%s: line %d, column %d.\n%s\n%s", msg, line, column, scriptLine, arrow);
		}
		return "";
	}

	private Task<StyleSpans<Collection<String>>> computeHighlightingAsync() {
		String text = getText();
		Task<StyleSpans<Collection<String>>> task = new Task<>() {
			@Override
			protected StyleSpans<Collection<String>> call() {
				return computeHighlighting(text);
			}
		};
		executor.execute(task);
		return task;
	}

	private static StyleSpans<Collection<String>> computeHighlighting(String text) {
		Matcher matcher = PATTERN.matcher(text);
		int lastKwEnd = 0;
		StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
		while(matcher.find()) {
			String match; // we need this to get the actual length of the match, excluding any trailing characters
			String styleClass =
					(match = matcher.group("KEYWORD")) != null ? "keyword" :
					(match = matcher.group("PAREN")) != null ? "paren" :
					(match = matcher.group("BRACE")) != null ? "brace" :
					(match = matcher.group("BRACKET")) != null ? "bracket" :
					(match = matcher.group("SEMICOLON")) != null ? "semicolon" :
					(match = matcher.group("STRING")) != null ? "string" :
					(match = matcher.group("COMMENT")) != null ? "comment" :
					(match = matcher.group("FUNCTION")) != null ? "function" :
					null; /* never happens */ assert styleClass != null;
			spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
			spansBuilder.add(Collections.singleton(styleClass), match.length());
			lastKwEnd = matcher.start() + match.length();
			matcher.region(lastKwEnd, text.length()); // manually set the next search region to avoid skipping groups with trailing characters
		}
		spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
		return spansBuilder.create();
	}

	public void setText(String text) {
		replaceText(0, 0, text);
	}

	@Override
	public void close() {
		highlightSubscription.unsubscribe();
		if (evalSubscription != null) {
			evalSubscription.unsubscribe();
		}
		executor.shutdown();
	}
}
