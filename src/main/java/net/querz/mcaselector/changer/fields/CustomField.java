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

	private static final String baseScript = """
				var obj = new Object()
				var ByteTag = Java.type("net.querz.nbt.ByteTag")
				var ShortTag = Java.type("net.querz.nbt.ShortTag")
				var IntTag = Java.type("net.querz.nbt.IntTag")
				var LongTag = Java.type("net.querz.nbt.LongTag")
				var FloatTag = Java.type("net.querz.nbt.FloatTag")
				var DoubleTag = Java.type("net.querz.nbt.DoubleTag")
				var StringTag = Java.type("net.querz.nbt.StringTag")
				var ByteArrayTag = Java.type("net.querz.nbt.ByteArrayTag")
				var IntArrayTag = Java.type("net.querz.nbt.IntArrayTag")
				var LongArrayTag = Java.type("net.querz.nbt.LongArrayTag")
				var ListTag = Java.type("net.querz.nbt.ListTag")
				var CompoundTag = Java.type("net.querz.nbt.CompoundTag")
				obj.apply = function() {%s}
				""";

	private static ScriptEngine engine;
	private static final Object lock = new Object();

	public CustomField() {
		super(FieldType.CUSTOM);

		if (engine == null) {
			ScriptEngineManager factory = new ScriptEngineManager();
			engine = factory.getEngineByName("JavaScript");
			engine.put("region", null);
			engine.put("poi", null);
			engine.put("entities", null);
		}
	}

	@Override
	public String getOldValue(ChunkData root) {
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
	public void change(ChunkData root) {
		// this needs to be thread safe because all threads use the same ScriptEngine
		synchronized (lock) {
			engine.put("region", root.region() != null && root.region().getData() != null ? root.region().getData() : null);
			engine.put("poi", root.poi() != null && root.poi().getData() != null ? root.poi().getData() : null);
			engine.put("entities", root.entities() != null && root.entities().getData() != null ? root.entities().getData() : null);
			Object o = engine.get("obj");

			try {
				((Invocable) engine).invokeMethod(o, "apply");
			} catch (ScriptException | NoSuchMethodException ex) {
				LOGGER.warn("failed to invoke custom script", ex);
			}
		}
	}

	@Override
	public void force(ChunkData root) {
		change(root);
	}
}
