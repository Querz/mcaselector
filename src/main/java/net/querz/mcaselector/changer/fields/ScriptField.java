package net.querz.mcaselector.changer.fields;

import net.querz.mcaselector.changer.Field;
import net.querz.mcaselector.changer.FieldType;
import net.querz.mcaselector.io.GroovyScriptEngine;
import net.querz.mcaselector.io.mca.ChunkData;
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
	public String getOldValue(ChunkData root) {
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
	public void change(ChunkData root) {
		try {
			engine.run("apply", root);
		} catch (ScriptException | NoSuchMethodException e) {
			LOGGER.warn("failed to invoke apply function in custom script");
		}
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
	public void force(ChunkData root) {
		change(root);
	}
}
