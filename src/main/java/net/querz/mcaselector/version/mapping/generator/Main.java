package net.querz.mcaselector.version.mapping.generator;

import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.version.mapping.color.*;
import net.querz.mcaselector.version.mapping.minecraft.Blocks;
import net.querz.mcaselector.version.mapping.minecraft.MinecraftVersion;
import net.querz.mcaselector.version.mapping.minecraft.VersionManifest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class Main {

	private static final Path tmpDir = Path.of("tmp");

	public static void main(String[] args) throws IOException, InterruptedException {
		if (args.length != 1) {
			System.err.println("invalid program parameter, expected Minecraft version");
			System.exit(1);
		}
		String mcVersion = args[0];

		Path tmp = tmpDir.resolve(mcVersion);
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

		System.exit(0);
	}

	private static void convert(MinecraftVersion version, Path tmp, Path configs, Path colorsTxt, Path biomeTxt) throws IOException {
		ColorConfig cfg = new ColorConfig();

		Blocks blocks = Blocks.load(tmp.resolve("blocks.json"));
		cfg.states = blocks.generateBlockStates();

		Map<String, String> waterloggedTrue = new HashMap<>();
		waterloggedTrue.put("waterlogged", "true");
		Map<String, String> waterloggedFalse = new HashMap<>();
		waterloggedFalse.put("waterlogged", "false");

		cfg.colors = new ColorMapping();
		try (Stream<String> lines = Files.lines(colorsTxt)) {
			lines.forEach(line -> {
				String[] split = line.split(";");
				String name = split[0];
				String properties = "";
				int oldColor = Integer.parseInt(split[1], 16);
				if (name.contains(":")) {
					String[] data = name.split(":");
					name = data[0];
					properties = data[1];
				}
				name = "minecraft:" + name;

				BitSet blockState = cfg.states.getState(properties);

				boolean canBeWaterlogged = blocks.states.get(name).properties().containsKey("waterlogged");
				if (canBeWaterlogged && blockState != null) {
					BitSet blockStateWT = cfg.states.getState(waterloggedTrue);
					blockStateWT.or(blockState);
					cfg.colors.addBlockColor(name, blockStateWT, new BlockColor(oldColor, ColorConfig.colorProperties.get(name)));
					BitSet blockStateWF = cfg.states.getState(waterloggedFalse);
					blockStateWF.or(blockState);
					cfg.colors.addBlockColor(name, blockStateWF, new BlockColor(oldColor, ColorConfig.colorProperties.get(name)));
				} else {
					cfg.colors.addBlockColor(name, blockState, new BlockColor(oldColor, ColorConfig.colorProperties.get(name)));
				}
			});
		}

		cfg.tints = new BiomeColors();
		try (Stream<String> lines = Files.lines(biomeTxt)) {
			lines.forEach(line -> {
				String[] split = line.split(";");
//				String name = "minecraft:" + split[0];
				String name = split[0];
				int grass = Integer.parseInt(split[1], 16);
				int foliage = Integer.parseInt(split[2], 16);
				int water = Integer.parseInt(split[3], 16);
				cfg.tints.addTints(name, new BiomeColors.BiomeTints(grass, foliage, water));
			});
		}

		cfg.save(configs.resolve("colors.json"));
	}
}
