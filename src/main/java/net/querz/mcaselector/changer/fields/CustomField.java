package net.querz.mcaselector.changer.fields;

import net.querz.mcaselector.changer.Field;
import net.querz.mcaselector.changer.FieldType;
import net.querz.mcaselector.io.mca.ChunkData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class CustomField extends Field<String> {

	private static final Logger LOGGER = LogManager.getLogger(CustomField.class);

	private static final String baseScript = "import net.querz.nbt.*; def apply() {%s}";

	private static ScriptEngine engine;
	private static final Object lock = new Object();

	public CustomField() {
		super(FieldType.CUSTOM);

		if (engine == null) {
			ScriptEngineManager factory = new ScriptEngineManager();
			engine = factory.getEngineByName("Groovy");
			engine.put("region", null);
			engine.put("poi", null);
			engine.put("entities", null);
		}
	}

	@Override
	public String getOldValue(ChunkData data) {
		return null;
	}

	@Override
	public boolean parseNewValue(String s) {
		try {
			String script = String.format(baseScript, s);
			engine.eval(script);
			setNewValue(s);
			return true;
		} catch (ScriptException e) {
			return super.parseNewValue(s);
		}
	}

	@Override
	public void change(ChunkData data) {
		// this needs to be thread safe because all threads use the same ScriptEngine
		synchronized (lock) {
			engine.put("region", data.region() != null && data.region().getData() != null ? data.region().getData() : null);
			engine.put("poi", data.poi() != null && data.poi().getData() != null ? data.poi().getData() : null);
			engine.put("entities", data.entities() != null && data.entities().getData() != null ? data.entities().getData() : null);

			try {
				((Invocable) engine).invokeFunction("apply");
			} catch (ScriptException | NoSuchMethodException ex) {
				LOGGER.warn("failed to invoke custom script", ex);
			}
		}
	}

	@Override
	public void force(ChunkData data) {
		change(data);
	}
}
