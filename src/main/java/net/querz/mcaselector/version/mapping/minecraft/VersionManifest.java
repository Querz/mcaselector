package net.querz.mcaselector.version.mapping.minecraft;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import net.querz.mcaselector.version.mapping.util.Download;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class VersionManifest {

	@SerializedName("latest")
	private Latest latest;
	@SerializedName("versions")
	private List<MinecraftVersion> versions;
	private transient final Map<String, MinecraftVersion> versionMap = new HashMap<>();
	private static final Gson GSON = new GsonBuilder().create();

	private VersionManifest() {}

	public static VersionManifest load(Path path) throws IOException {
		try (BufferedReader reader = Files.newBufferedReader(path)) {
			VersionManifest vm = GSON.fromJson(reader, VersionManifest.class);
			vm.versions.forEach(v -> vm.versionMap.put(v.id(), v));
			return vm;
		}
	}

	public static void download(Path output) throws IOException {
		Download.to("https://launchermeta.mojang.com/mc/game/version_manifest.json", output);
	}

	public MinecraftVersion getVersionByID(String id) {
		return versionMap.get(id);
	}

	public MinecraftVersion latestRelease() {
		return getVersionByID(latest.release);
	}

	public MinecraftVersion latestSnapshot() {
		return getVersionByID(latest.snapshot);
	}

	public List<MinecraftVersion> getVersions() {
		return Collections.unmodifiableList(versions);
	}

	public List<MinecraftVersion> getReleases() {
		return versions.stream().filter(v -> v.type() == MinecraftVersion.Type.RELEASE).toList();
	}

	public List<MinecraftVersion> getSnapshots() {
		return versions.stream().filter(v -> v.type() == MinecraftVersion.Type.SNAPSHOT).toList();
	}

	public List<MinecraftVersion> getOldAlpha() {
		return versions.stream().filter(v -> v.type() == MinecraftVersion.Type.OLD_ALPHA).toList();
	}

	public List<MinecraftVersion> getOldBeta() {
		return versions.stream().filter(v -> v.type() == MinecraftVersion.Type.OLD_BETA).toList();
	}

	private record Latest(
			@SerializedName("release") String release,
			@SerializedName("snapshot") String snapshot) {}
}
