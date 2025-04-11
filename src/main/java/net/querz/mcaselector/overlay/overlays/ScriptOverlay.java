package net.querz.mcaselector.overlay.overlays;

import net.querz.mcaselector.io.GroovyScriptEngine;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.overlay.AmountOverlay;
import net.querz.mcaselector.overlay.OverlayType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javax.script.ScriptException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ScriptOverlay extends AmountOverlay {

	private static final Logger LOGGER = LogManager.getLogger(ScriptOverlay.class);

	private static final String[] empty = new String[0];

	private final GroovyScriptEngine engine = new GroovyScriptEngine();

	public ScriptOverlay() {
		super(OverlayType.DATA_VERSION);
	}

	@Override
	public int parseValue(ChunkData data) {
		try {
			return engine.invoke("get", data);
		} catch (ScriptException | NoSuchMethodException ex) {
			// TODO: remove
			ex.printStackTrace();
			LOGGER.warn("failed to invoke get function in custom overlay script", ex);
		}
		return 0;
	}

	@Override
	public String name() {
		return "Script";
	}

	@Override
	public boolean setMultiValuesString(String raw) {
		if (raw == null || !raw.endsWith(".groovy")) {
			setMultiValues(empty);
			return false;
		}

		String code;
		try {
			code = Files.readString(Path.of(raw));
			engine.eval(code);
			setMultiValues(new String[]{code});
			setRawMultiValues(code);
			return true;
		} catch (IllegalArgumentException | IOException | ScriptException e) {
			setMultiValues(empty);
			return false;
		}
	}
}
