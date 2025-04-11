package net.querz.mcaselector.filter.filters;

import net.querz.mcaselector.filter.*;
import net.querz.mcaselector.io.GroovyScriptEngine;
import net.querz.mcaselector.io.mca.ChunkData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javax.script.*;

public class ScriptFilter extends TextFilter<String> {

	private static final Logger LOGGER = LogManager.getLogger(ScriptFilter.class);

	private final GroovyScriptEngine engine = new GroovyScriptEngine();

	private static final Comparator[] comparators = {
			Comparator.EQUAL,
	};

	public ScriptFilter() {
		this(Operator.AND, Comparator.EQUAL, null);
	}

	private ScriptFilter(Operator operator, Comparator comparator, String value) {
		super(FilterType.SCRIPT, operator, comparator, value);
	}

	@Override
	public Comparator[] getComparators() {
		return comparators;
	}

	@Override
	public void setFilterValue(String raw) {
		try {
			engine.eval(raw);
			setValue(raw);
			setRawValue(raw);
			setValid(true);
		} catch (ScriptException e) {
			setValue(null);
			setValid(false);
		}
	}

	@Override
	public Filter<String> clone() {
		return new ScriptFilter(getOperator(), getComparator(), value);
	}

	@Override
	public String getFormatText() {
		return "[Groovy]";
	}

	@Override
	public boolean matches(ChunkData data) {
		try {
			return engine.test("filter", data);
		} catch (ScriptException | NoSuchMethodException ex) {
			LOGGER.warn("failed to invoke filter function in custom filter script", ex);
		}
		return false;
	}

	public void before() {
		try {
			engine.run("before");
		} catch (ScriptException | NoSuchMethodException ex) {
			LOGGER.warn("failed to invoke before function in custom script", ex);
		}
	}

	public void after() {
		try {
			engine.run("after");
		} catch (ScriptException | NoSuchMethodException ex) {
			LOGGER.warn("failed to invoke after function in custom script", ex);
		}
	}

	@Override
	public boolean contains(String value, ChunkData data) {
		throw new UnsupportedOperationException("\"contains\" not allowed in ScriptFilter");
	}

	@Override
	public boolean containsNot(String value, ChunkData data) {
		throw new UnsupportedOperationException("\"!contains\" not allowed in ScriptFilter");
	}

	@Override
	public boolean intersects(String value, ChunkData data) {
		throw new UnsupportedOperationException("\"intersects\" not allowed in ScriptFilter");
	}
}
