package net.querz.mcaselector.headless;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ParamInterpreter {

	private final Map<String, String> params;
	private final Set<String> knownParams = new HashSet<>();
	private final Map<ActionKey, ExceptionConsumer<String, ? extends IOException>> actions = new HashMap<>();
	private final Map<ActionKey, Set<ActionKey>> dependencies = new HashMap<>();
	private final Map<ActionKey, Set<ActionKey>> softDependencies = new HashMap<>();
	private final Map<String, Set<String>> restrictions = new HashMap<>();

	public ParamInterpreter(Map<String, String> params) {
		this.params = params;
	}

	public void registerAction(String key, String value, ExceptionConsumer<String, ? extends IOException> action) {
		actions.put(new ActionKey(key, value), action);
		knownParams.add(key);
	}

	public void registerDependencies(String key, String value, ActionKey... dependencies) {
		Set<ActionKey> restr = this.dependencies.computeIfAbsent(new ActionKey(key, value), k -> new HashSet<>());
		restr.addAll(Arrays.asList(dependencies));
		addKnownParams(dependencies);
		knownParams.add(key);
	}

	public void registerSoftDependencies(String key, String value, ActionKey... dependencies) {
		Set<ActionKey> restr = softDependencies.computeIfAbsent(new ActionKey(key, value), k -> new HashSet<>());
		restr.addAll(Arrays.asList(dependencies));
		addKnownParams(dependencies);
		knownParams.add(key);
	}

	public void registerRestrictions(String key, String... values) {
		Set<String> restr = restrictions.computeIfAbsent(key, k -> new HashSet<>());
		restr.addAll(Arrays.asList(values));
		knownParams.add(key);
	}

	private void addKnownParams(ActionKey... params) {
		for (ActionKey param : params) {
			knownParams.add(param.key);
		}
	}

	public void execute() throws IOException {
		// check dependencies and restrictions
		for (Map.Entry<String, String> param : params.entrySet()) {
			if (!knownParams.contains(param.getKey())) {
				throw new IllegalArgumentException("unknown param \"--" + param.getKey() + "\"");
			}

			Set<ActionKey> dependencies = new HashSet<>();
			Set<ActionKey> keyValueDep = this.dependencies.get(new ActionKey(param.getKey(), param.getValue()));
			if (keyValueDep != null) {
				dependencies.addAll(keyValueDep);
			}


			Set<ActionKey> all = this.dependencies.get(new ActionKey(param.getKey(), null));
			if (all != null) {
				dependencies.addAll(all);
			}

			for (ActionKey dep : dependencies) {
				if (!params.containsKey(dep.key)) {
					throw new IllegalArgumentException("missing param \"--" + dep.key + "\"");
				}
				if (dep.value != null && (params.get(dep.key) == null || !params.get(dep.key).equals(dep.value))) {
					throw new IllegalArgumentException("invalid value \"" + params.get(dep.key) + "\" for param \"--" + dep.key + "\" in context");
				}
			}

			if (restrictions.get(param.getKey()) != null && !restrictions.get(param.getKey()).contains(param.getValue())) {
				throw new IllegalArgumentException("invalid value \"" + param.getValue() + "\" for param \"--" + param.getKey() + "\"");
			}

			// soft dependencies: we need only ONE of the params

			Set<ActionKey> softDependencies = new HashSet<>();
			Set<ActionKey> keyValueSoft = this.softDependencies.get(new ActionKey(param.getKey(), param.getValue()));
			if (keyValueSoft != null) {
				softDependencies.addAll(keyValueSoft);
			}

			Set<ActionKey> keySoft = this.softDependencies.get(new ActionKey(param.getKey(), null));
			if (keySoft != null) {
				softDependencies.addAll(keySoft);
			}


			if (softDependencies.size() > 0) {
				int found = 0;
				for (ActionKey dep : softDependencies) {
					if (params.containsKey(dep.key)) {
						if (dep.value != null) {
							if (params.get(dep.key).equals(dep.value)) {
								found++;
							}
						} else {
							found++;
						}
					}
				}
				if (found == 0) {
					throw new IllegalArgumentException("did not find mandatory param required to complete \"--" + param.getKey() + " " + param.getValue() + "\"");
				}
				if (found > 1) {
					throw new IllegalArgumentException("found more than one optional param to complete \"--" + param.getKey() + " " + param.getValue() + "\"");
				}
			}
		}

		// execute actions
		// actions that are only registered to a key will be executed first
		for (Map.Entry<String, String> param : params.entrySet()) {
			ActionKey key = new ActionKey(param.getKey(), null);
			if (actions.containsKey(key) && actions.get(key) != null) {
				actions.get(key).accept(param.getValue());
				continue;
			}
			ActionKey keyValue = new ActionKey(param.getKey(), param.getValue());
			if (actions.containsKey(keyValue) && actions.get(keyValue) != null) {
				actions.get(keyValue).accept(param.getValue());
			}
		}
	}

	public static class ActionKey {
		private final String key;
		private final String value;

		private final int hashCode;

		public ActionKey(String key, String value) {
			this.key = key;
			this.value = value;

			hashCode = key == null ? 0 : Objects.hash(key, value);
		}

		@Override
		public int hashCode() {
			return hashCode;
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

		@Override
		public String toString() {
			return "ActionKey " + key + "/" + value;
		}
	}
}
