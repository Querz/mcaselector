package net.querz.mcaselector.filter.filters;

import net.querz.mcaselector.filter.*;
import net.querz.mcaselector.io.mca.ChunkData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class CustomFilter extends TextFilter<String> {

	private static final Logger LOGGER = LogManager.getLogger(CustomFilter.class);

	private static final String baseScript = "import net.querz.nbt.*; def test() {%s}";

	private static ScriptEngine engine;
	private static final Object lock = new Object();

	private static final Comparator[] comparators = {
		Comparator.EQUAL,
		Comparator.NOT_EQUAL
	};

	public CustomFilter() {
		this(Operator.AND, Comparator.EQUAL, null);
	}

	private CustomFilter(Operator operator, Comparator comparator, String value) {
		super(FilterType.CUSTOM, operator, comparator, value);
		if (engine == null) {
			ScriptEngineManager factory = new ScriptEngineManager();
			engine = factory.getEngineByName("Groovy");
			engine.put("region", null);
			engine.put("poi", null);
			engine.put("entities", null);
		}
	}

	@Override
	public Comparator[] getComparators() {
		return comparators;
	}


	@Override
	public void setFilterValue(String raw) {
		try {
			String script = String.format(baseScript, raw);
			engine.eval(script);
			setValue(raw);
			setRawValue(raw);
			setValid(true);
		} catch (ScriptException e) {
			setValue(null);
			setValid(false);
		}
	}

	@Override
	public String toString() {
		return "Custom " + getComparator().getQueryString() + " " + escape(getFilterValue());
	}

	@Override
	public Filter<String> clone() {
		return new CustomFilter(getOperator(), getComparator(), value);
	}

	@Override
	public String getFormatText() {
		return "[Groovy]";
	}

	@Override
	public boolean matches(ChunkData data) {
		return switch (getComparator()) {
			case EQUAL -> isEqual(value, data);
			case NOT_EQUAL -> !isEqual(value, data);
			default -> false;
		};
	}

	public boolean isEqual(String value, ChunkData data) {
		synchronized (lock) {
			engine.put("region", data.region() != null && data.region().getData() != null ? data.region().getData() : null);
			engine.put("poi", data.poi() != null && data.poi().getData() != null ? data.poi().getData() : null);
			engine.put("entities", data.entities() != null && data.entities().getData() != null ? data.entities().getData() : null);

			try {
				Object result = ((Invocable) engine).invokeFunction("test");
				return result instanceof Boolean && (boolean) result;
			} catch (ScriptException | NoSuchMethodException ex) {
				LOGGER.warn("failed to invoke custom script", ex);
			}
		}
		return false;
	}

	@Override
	public boolean contains(String value, ChunkData data) {
		throw new UnsupportedOperationException("\"contains\" not allowed in CustomFilter");
	}

	@Override
	public boolean containsNot(String value, ChunkData data) {
		throw new UnsupportedOperationException("\"!contains\" not allowed in CustomFilter");
	}

	@Override
	public boolean intersects(String value, ChunkData data) {
		throw new UnsupportedOperationException("\"intersects\" not allowed in CustomFilter");
	}
}
