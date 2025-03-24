package net.querz.mcaselector.version.mapping.generator;

import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.logging.Logging;
import net.querz.mcaselector.version.mapping.minecraft.MinecraftVersion;
import net.querz.mcaselector.version.mapping.minecraft.VersionManifest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {

	private static final Path tmpDir = Path.of("tmp");

	public static void main(String[] args) throws IOException, InterruptedException {
		if (args.length != 1) {
			System.err.println("invalid program parameter, expected Minecraft version");
			System.exit(1);
		}
		String mcVersion = args[0];

		Path tmp = tmpDir.resolve(mcVersion);
		Path log = tmp.resolve("logs_mcaselector");
		Logging.setLogDir(log.toFile());
		Logging.updateThreadContext();
		Path mf = tmpDir.resolve("version_manifest.json");
		if (!Files.exists(mf)) {
			VersionManifest.download(mf);
		}
		VersionManifest manifest = VersionManifest.load(mf);
		MinecraftVersion version = manifest.getVersionByID(mcVersion);
		// if this version doesn't exist, download version_manifest.json again
		if (version == null) {
			Files.deleteIfExists(mf);
			VersionManifest.download(mf);
			manifest = VersionManifest.load(mf);
			version = manifest.getVersionByID(mcVersion);
			if (version == null) {
				System.err.println("invalid Minecraft version " + mcVersion);
				System.exit(1);
			}
		}

		Path configs = tmp.resolve("configs");
		if (!Files.exists(configs)) {
			Files.createDirectories(configs);
		}

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

		Path ev = configs.resolve("entities_" + mcVersion + ".json");
		entityConfig.save(ev);

		EntityConfig oldEntityConfig = FileHelper.loadFromResource("mapping/registry/entities.json", EntityConfig::load);
		entityConfig.merge(oldEntityConfig);

		Path e = configs.resolve("entities.json");
		entityConfig.save(e);

		// ------------------------------------------------

		StructureConfig structureConfig = new StructureConfig();
		structureConfig.generate(version, tmp);

		Path sv = configs.resolve("structures_" + mcVersion + ".json");
		structureConfig.save(sv);

		StructureConfig oldStructureConfig = FileHelper.loadFromResource("mapping/registry/structures.json", StructureConfig::load);
		structureConfig.merge(oldStructureConfig);

		Path s = configs.resolve("structures.json");
		structureConfig.save(s);

		// ------------------------------------------------

		BlockConfig blockConfig = new BlockConfig();
		blockConfig.generate(version, tmp);

		Path bv = configs.resolve("blocks_" + mcVersion + ".json");
		blockConfig.save(bv);

		BlockConfig oldBlockConfig = FileHelper.loadFromResource("mapping/registry/blocks.json", BlockConfig::load);
		blockConfig.merge(oldBlockConfig);

		Path b = configs.resolve("blocks.json");
		blockConfig.save(b);

		// ------------------------------------------------

		BiomeConfig biomeConfig = new BiomeConfig();
		biomeConfig.generate(version, tmp);

		Path pv = configs.resolve("biomes_" + mcVersion + ".json");
		biomeConfig.save(pv);

		BiomeConfig oldBiomeConfig = FileHelper.loadFromResource("mapping/registry/biomes.json", BiomeConfig::load);
		biomeConfig.merge(oldBiomeConfig);

		Path p = configs.resolve("biomes.json");
		biomeConfig.save(p);

		// ------------------------------------------------

		System.exit(0);
	}
}
