package net.querz.mcaselector.util.property;

import net.querz.mcaselector.util.validation.OSHelper;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public final class MSWindowsRegistry {

	private static final String subKey = "Software\\MCA Selector";

	private MSWindowsRegistry() {}

	public static String getValue(String identifier) {
		if (!OSHelper.isWindows()) {
			return null;
		}

		String value = readRegistry("HKCU", subKey, identifier);
		if (value == null) {
			value = readRegistry("HKLM", subKey, identifier);
		}
		return value;
	}

	private static String readRegistry(String root, String subKey, String identifier) {
		try {
			Process process = Runtime.getRuntime().exec(
				new String[] {"reg", "query", root + "\\" + subKey, "/v", identifier});

			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.contains("REG_SZ")) {
					String[] parts = line.split("REG_SZ");
					return parts[parts.length - 1].trim();
				}
			}
		} catch (Exception e) {
			return null;
		}
		return null;
	}
}
