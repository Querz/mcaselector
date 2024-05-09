package net.querz.mcaselector.io;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class GroovyScriptEngine {

	private ScriptEngine engine;

	public GroovyScriptEngine() {
		init();
	}

	public void init() {
		engine = new ScriptEngineManager().getEngineByName("Groovy");
	}

	public void eval(String script) throws ScriptException {
		engine.eval(script);
	}

	public void run(String function, Object... args) throws ScriptException, NoSuchMethodException {
		((Invocable) engine).invokeFunction(function, args);
	}

	public boolean test(String function, Object... args) throws ScriptException, NoSuchMethodException {
		Object result = ((Invocable) engine).invokeFunction(function, args);
		return result instanceof Boolean && (boolean) result;
	}
}
