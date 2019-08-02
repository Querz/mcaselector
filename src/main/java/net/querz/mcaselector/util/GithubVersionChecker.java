package net.querz.mcaselector.util;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public class GithubVersionChecker {

	private static final String endpointTemplate = "https://api.github.com/repos/%s/%s/releases/latest";

	private ScriptEngine engine;

	private String owner;
	private String repository;

	public GithubVersionChecker(String owner, String repository) {
		this.owner = owner;
		this.repository = repository;
		ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
		engine = scriptEngineManager.getEngineByName("javascript");
	}

	public VersionData fetchLatestVersion() throws Exception {
		String endpoint = String.format(endpointTemplate, owner, repository);
		URL url = new URL(endpoint);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("GET");

		StringBuilder stringBuilder = new StringBuilder();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
			String line;
			while ((line = reader.readLine()) != null) {
				stringBuilder.append(line);
			}
		}
		return parseJson(stringBuilder.toString());
	}

	// a dirty way to parse json without using any 3rd party dependency.
	private VersionData parseJson(String json) throws Exception {
		String script = "Java.asJSONCompatible(" + json + ")";
		Object result = engine.eval(script);
		if (!(result instanceof Map)) {
			throw new IOException("could not parse json");
		}

		int latestID = 0;
		String latestTag = null;
		String latestLink = null;

		@SuppressWarnings("unchecked")
		Map<String, Object> map = (Map<String, Object>) result;
		for (Map.Entry<String, Object> e : map.entrySet()) {
			if ("id".equals(e.getKey())) {
				latestID = (int) e.getValue();
			} else if ("tag_name".equals(e.getKey())) {
				latestTag = (String) e.getValue();
			} else if ("html_url".equals(e.getKey())) {
				latestLink = (String) e.getValue();
			}
		}

		if (latestID == 0) {
			return null;
		}

		return new VersionData(latestID, latestTag, latestLink);
	}

	public class VersionData {
		int id;
		String tag, link;

		VersionData(int id, String tag, String link) {
			this.id = id;
			this.tag = tag;
			this.link = link;
		}

		public boolean isNewerThan(VersionData version) {
			return id > version.id;
		}

		public boolean isOlderThan(VersionData version) {
			return id < version.id;
		}

		public boolean isNewerThan(String tag) {
			return this.tag.compareTo(tag) > 0;
		}

		public boolean isOlderThan(String tag) {
			return this.tag.compareTo(tag) < 0;
		}

		public String getTag() {
			return tag;
		}

		public String getLink() {
			return link;
		}

		@Override
		public String toString() {
			return "id=" + id + ", tag=" + tag + ", link=" + link;
		}
	}
}
