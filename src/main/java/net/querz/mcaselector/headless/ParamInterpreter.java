package net.querz.mcaselector.headless;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public class ParamInterpreter {

	private Map<String, String> params;
	private Map<ActionKey, Consumer<String>> actions = new HashMap<>();

	public ParamInterpreter(Map<String, String> params) {
		this.params = params;
	}

	public void registerAction(String key, String value, Consumer<String> action) {
		actions.put(new ActionKey(key, value), action);
	}

	public void execute() {
		for (Map.Entry<String, String> param : params.entrySet()) {
			ActionKey key = new ActionKey(param.getKey(), null);
			if (actions.containsKey(key)) {
				actions.get(key).accept(param.getValue());
			}
			ActionKey keyValue = new ActionKey(param.getKey(), param.getValue());
			if (actions.containsKey(keyValue)) {
				actions.get(keyValue).accept(param.getValue());
			}
		}
	}

	private class ActionKey {
		String key;
		String value;

		public ActionKey(String key, String value) {
			this.key = key;
			this.value = value;
		}

		@Override
		public int hashCode() {
			if (key == null) {
				return 0;
			}
			return Objects.hash(key, value);
		}

		@Override
		public boolean equals(Object other) {
			if (!(other instanceof ActionKey)) {
				return false;
			}
			if (key == null) {
				return ((ActionKey) other).key == null;
			}
			if (value == null) {
				return key.equals(((ActionKey) other).key) && ((ActionKey) other).value == null;
			}
			return key.equals(((ActionKey) other).key) && value.equals(((ActionKey) other).value);
		}
	}
}
