package net.querz.mcaselector.changer.fields;

import net.querz.mcaselector.changer.Field;
import net.querz.mcaselector.changer.FieldType;
import net.querz.mcaselector.filter.RegionMatcher;
import net.querz.mcaselector.io.GroovyScriptEngine;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.util.point.Point2i;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javax.script.ScriptException;

public class ScriptField extends Field<String> {

	private static final Logger LOGGER = LogManager.getLogger(ScriptField.class);

	private final GroovyScriptEngine engine = new GroovyScriptEngine();

	public ScriptField() {
		super(FieldType.SCRIPT);
	}

	@Override
	public String getOldValue(ChunkData data) {
		return null;
	}

	@Override
	public boolean parseNewValue(String s) {
		try {
			engine.eval(s);
			setNewValue(s);
			return true;
		} catch (ScriptException e) {
			return super.parseNewValue(s);
		}
	}

	@Override
	public void change(ChunkData data) {
		force(data);
	}

	@Override
	public void force(ChunkData data) {
		try {
			engine.run("apply", data);
		} catch (ScriptException | NoSuchMethodException ex) {
			LOGGER.warn("failed to invoke apply function in custom script", ex);
		}
	}

	public void before() {
		try {
			engine.run("before");
		} catch (ScriptException ex) {
			LOGGER.warn("failed to invoke before function in custom script", ex);
		} catch (NoSuchMethodException ex) {
			// ignore, this function is not mandatory
		}
	}

	public void after() {
		try {
			engine.run("after");
		} catch (ScriptException ex) {
			LOGGER.warn("failed to invoke after function in custom script", ex);
		} catch (NoSuchMethodException ex) {
			// ignore, this function is not mandatory
		}
	}

	public boolean matchesRegion(Point2i region) {
		try {
			return engine.test("matchesRegion", region.getX(), region.getZ());
		} catch (ScriptException ex) {
			LOGGER.warn("failed to invoke matchesRegion function in custom script", ex);
		} catch (NoSuchMethodException ex) {
			System.out.println(ex.getMessage());
			// ignore, this function is not mandatory
		}
		return false;
	}
}
