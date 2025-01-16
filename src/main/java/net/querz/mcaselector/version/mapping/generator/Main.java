package net.querz.mcaselector.version.mapping.generator;

import net.querz.mcaselector.version.mapping.minecraft.MinecraftVersion;
import net.querz.mcaselector.version.mapping.minecraft.VersionManifest;
import net.querz.mcaselector.version.mapping.util.Download;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {

	private static final Path tmpDir = Path.of("tmp");

	public static void main(String[] args) throws IOException, InterruptedException {
		String mcVersion = "1.21.4";

		Path tmp = tmpDir.resolve(mcVersion);
		Path configs = tmp.resolve("configs");
		if (!Files.exists(configs)) {
			Files.createDirectories(configs);
		}
		Path mf = tmpDir.resolve("version_manifest.json");
		if (!Files.exists(mf)) {
			Download.to("https://launchermeta.mojang.com/mc/game/version_manifest.json", mf);
		}
		VersionManifest manifest = VersionManifest.load(mf);
		MinecraftVersion version = manifest.getVersionByID(mcVersion);

		// ------------------------------------------------

		HeightmapConfig heightmapConfig = new HeightmapConfig();
		heightmapConfig.generate(version, tmp);

		Path h = configs.resolve("heightmaps.json");
		heightmapConfig.save(h);

		// ------------------------------------------------

		ColorConfig colorConfig = new ColorConfig();
		colorConfig.generate(version, tmp);

		Path c = configs.resolve("colors.json");
		colorConfig.save(c);

		// ------------------------------------------------

		EntityConfig entityConfig = new EntityConfig();
		entityConfig.generate(version, tmp);

		Path e = configs.resolve("entities.json");
		entityConfig.save(e);

		// ------------------------------------------------

		StructureConfig structureConfig = new StructureConfig();
		structureConfig.generate(version, tmp);

		Path s = configs.resolve("structures.json");
		structureConfig.save(s);

		System.exit(0);
	}
}
