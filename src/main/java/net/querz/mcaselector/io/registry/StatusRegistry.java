package net.querz.mcaselector.io.registry;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public final class StatusRegistry {

	private static final Logger LOGGER = LogManager.getLogger(StatusRegistry.class);

	private StatusRegistry() {}

	private static final Map<String, String> valid = new HashMap<>();

	static {
		try (BufferedReader bis = new BufferedReader(
				new InputStreamReader(Objects.requireNonNull(StatusRegistry.class.getClassLoader().getResourceAsStream("mapping/all_status.txt"))))) {
			String line;
			while ((line = bis.readLine()) != null) {
				valid.put(line, "minecraft:" + line);
				valid.put("minecraft:" + line, line);
			}
		} catch (IOException ex) {
			LOGGER.error("error reading mapping/all_status.txt", ex);
		}
	}

	public static boolean isValidName(String name) {
		return valid.containsKey(name) || name != null && name.startsWith("'") && name.endsWith("'");
	}

	public static class StatusIdentifier {
		String name;
		String nameWithNamespace;
		boolean custom = false;

		public StatusIdentifier(String name) {
			if (name != null && name.startsWith("'") && name.endsWith("'")) {
				this.name = name.substring(1, name.length() - 1);
				custom = true;
			} else if (name != null && isValidName(name)) {
				initValid(name);
			} else {
				throw new IllegalArgumentException("invalid status");
			}
		}

		public StatusIdentifier(String name, boolean custom) {
			if (custom) {
				this.name = name;
				this.custom = true;
			} else if (isValidName(name)) {
				initValid(name);
			} else {
				throw new IllegalArgumentException("invalid status");
			}
		}

		private void initValid(String name) {if (name.startsWith("minecraft:")) {
				this.name = valid.get(name);
				this.nameWithNamespace = name;
			} else {
				this.name = name;
				this.nameWithNamespace = valid.get(name);
			}
		}

		public String getStatus() {
			return name;
		}

		public String getStatusWithNamespace() {
			if (custom) {
				return name;
			}
			return nameWithNamespace;
		}

		public boolean equals(String value) {
			if (value == null) {
				return false;
			}
			if (custom) {
				return name.equals(value);
			}
			return value.startsWith("minecraft:") && value.equals(nameWithNamespace) || value.equals(name);
		}
	}
}
