package net.querz.mcaselector.version.mapping.generator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;
import net.querz.mcaselector.version.mapping.color.*;
import net.querz.mcaselector.version.mapping.minecraft.*;
import net.querz.mcaselector.version.mapping.util.BitSetAdapter;
import net.querz.mcaselector.version.mapping.util.Download;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ColorConfig {

	@SerializedName("states") public BlockStates states;
	@SerializedName("colors") public ColorMapping colors;
	@SerializedName("tints") public BiomeColors tints;

	private static final Gson GSON = new GsonBuilder()
			.registerTypeAdapter(BitSet.class, new BitSetAdapter())
			.registerTypeHierarchyAdapter(BlockColor.class, new BlockColor.BlockColorAdapter())
			.registerTypeAdapter(SingleStateColors.class, new SingleStateColors.SingleStateColorsAdapter())
			.registerTypeAdapter(BlockStates.class, new BlockStates.BlockStatesTypeAdapter())
			.registerTypeAdapter(BiomeColors.class, new BiomeColors.BiomeColorsTypeAdapter())
			.registerTypeAdapterFactory(ColorMapping.ColorMappingTypeAdapterFactory.getColorMappingTypeAdapterFactory())
			.registerTypeAdapterFactory(StateColors.StateColorsTypeAdapterFactory.getStateColorsTypeAdapterFactory())
			.enableComplexMapKeySerialization()
			.disableHtmlEscaping()
			.setPrettyPrinting()
			.create();

	public ColorConfig() {}

	public static ColorConfig load(Path path) throws IOException {
		return GSON.fromJson(Files.newBufferedReader(path), ColorConfig.class);
	}

	public void save(Path path) throws IOException {
		String json = GSON.toJson(this);
		Files.writeString(path, json);
	}

	public void generate(MinecraftVersion version, Path tmp) throws IOException, InterruptedException {
		Path versionJson = tmp.resolve("version.json");
		Path clientJar = tmp.resolve("client.jar");
		Path serverJar = tmp.resolve("server.jar");
		Path generated = tmp.resolve("generated");

		MinecraftVersionFile versionFile;
		// download version.json
		if (Files.exists(versionJson)) {
			versionFile = MinecraftVersionFile.of(versionJson);
		} else {
			versionFile = MinecraftVersionFile.download(version, versionJson);
		}

		// download server jar
		if (!Files.exists(serverJar)) {
			Download.to(versionFile.getDownloads().server().url(), serverJar);
		}

		// download client jar
		if (!Files.exists(clientJar)) {
			Download.to(versionFile.getDownloads().client().url(), clientJar);
		}

		// generate reports
		if (!Files.exists(generated)) {
			Report.generate(serverJar, generated);
		}

		// load blocks.json
		Path blocksJson = generated.resolve("reports/blocks.json");
		Blocks blocks = new Blocks(blocksJson);
		BlockStates blockStates = blocks.generateBlockStates();
		this.states = blockStates;

		Map<String, String> waterloggedTrue = new HashMap<>();
		waterloggedTrue.put("waterlogged", "true");
		Map<String, String> waterloggedFalse = new HashMap<>();
		waterloggedFalse.put("waterlogged", "false");

		ColorMapping mapping = new ColorMapping();
		BiomeColors tints = new BiomeColors();
		try (FileSystem fs = FileSystems.newFileSystem(clientJar)) {
			Path assetBase = fs.getPath("assets/minecraft");
			Path assetBlockstates = assetBase.resolve("blockstates");
			Path assetModels = assetBase.resolve("models");
			Path assetTextures = assetBase.resolve("textures");

			for (Map.Entry<String, Blocks.Block> states : blocks.states.entrySet()) {
				String blockName = states.getKey();

				// air doesn't have a model
				if (isAir(blockName)) {
					continue;
				}

				// load blockstates/<blockName>.json
				Path assetBlockState = assetBlockstates.resolve(trimNS(blockName) + ".json");
				JsonObject jsonBlockstate = readJSONAsset(assetBlockState);

				// check all variants
				JsonObject variants;
				JsonArray multipart;
				if ((variants = jsonBlockstate.getAsJsonObject("variants")) != null) {
					// check if this block can be waterlogged
					boolean canBeWaterlogged = states.getValue().properties().containsKey("waterlogged");

					for (Map.Entry<String, JsonElement> variant : variants.entrySet()) {
						// get model of variant
						String model;
						if (variant.getValue().isJsonArray()) {
							model = trimNS(variant.getValue().getAsJsonArray().get(0).getAsJsonObject().get("model").getAsString());
						} else {
							model = trimNS(variant.getValue().getAsJsonObject().get("model").getAsString());
						}

						Map<String, String> textureMapping = resolveTextureMapping(model, assetModels);
						String topTexture = trimNS(getTopTextureName(textureMapping));
						Path assetTexture = assetTextures.resolve(topTexture + ".png");
						int color = averageColor(assetTexture);
						BitSet blockStateBits = blockStates.getState(variant.getKey());
						if (canBeWaterlogged && blockStateBits != null) {
							BitSet blockStateBitsWT = blockStates.getState(waterloggedTrue);
							blockStateBitsWT.or(blockStateBits);
							mapping.addBlockColor(blockName, blockStateBitsWT, new BlockColor(color, getProperties(blockName)));
							BitSet blockStateBitsWF = blockStates.getState(waterloggedFalse);
							blockStateBitsWF.or(blockStateBits);
							mapping.addBlockColor(blockName, blockStateBitsWF, new BlockColor(color, getProperties(blockName)));
						} else {
							mapping.addBlockColor(blockName, blockStateBits, new BlockColor(color, getProperties(blockName)));
						}
					}
				} else if ((multipart = jsonBlockstate.getAsJsonArray("multipart")) != null) {
					JsonObject part = multipart.get(0).getAsJsonObject();
					String model;
					if (part.get("apply").isJsonObject()) {
						model = trimNS(part.getAsJsonObject("apply").get("model").getAsString());
					} else {
						model = trimNS(part.getAsJsonArray("apply").get(0).getAsJsonObject().get("model").getAsString());
					}

					Map<String, String> textureMapping = resolveTextureMapping(model, assetModels);
					String topTexture = trimNS(getTopTextureName(textureMapping));
					Path assetTexture = assetTextures.resolve(topTexture + ".png");
					int color = averageColor(assetTexture);
					mapping.addBlockColor(blockName, null, new BlockColor(color, getProperties(blockName)));
				}
			}

			// extract biome tints
			Path biomes = generated.resolve("data/minecraft/worldgen/biome");
			Path assetGrass = assetBase.resolve("textures/colormap/grass.png");
			Path assetFoliage = assetBase.resolve("textures/colormap/foliage.png");
			BufferedImage grassTints = ImageIO.read(Files.newInputStream(assetGrass));
			BufferedImage foliageTints = ImageIO.read(Files.newInputStream(assetFoliage));

			try (DirectoryStream<Path> ds = Files.newDirectoryStream(biomes)) {
				for (Path b : ds) {
					if (!Files.isRegularFile(b)) {
						continue;
					}
					Biome biome = new Biome(b);
					int grassTint = Objects.requireNonNullElseGet(
							biome.effects.grassTint(),
							() -> getColorMapping(biome.temperature, biome.downfall, grassTints));
					int foliageTint = Objects.requireNonNullElseGet(
							biome.effects.foliageTint(),
							() -> getColorMapping(biome.temperature, biome.downfall, foliageTints));
					String fileName = b.getFileName().toString();
					tints.addTints(
							fileName.substring(0, fileName.length() - 5),
							new BiomeColors.BiomeTints(grassTint, foliageTint, biome.effects.waterTint()));
				}
			}
		}
		mapping.compress();
		this.colors = mapping;
		this.tints = tints;
	}

	private String trimNS(String s) {
		return s.substring(s.indexOf(':') + 1);
	}

	private JsonObject readJSONAsset(Path path) throws IOException {
		try (BufferedReader reader = Files.newBufferedReader(path)) {
			return JsonParser.parseReader(reader).getAsJsonObject();
		}
	}

	private static boolean isAir(String blockName) {
		return switch (blockName) {
			case "minecraft:cave_air",
				 "minecraft:void_air",
				 "minecraft:air" -> true;
			default -> false;
		};
	}

	private static int getProperties(String blockName) {
		return isTransparent(blockName)
				| hasGrassTint(blockName)
				| hasFoliageTint(blockName)
				| isWater(blockName)
				| isFoliage(blockName);
	}

	private static int isTransparent(String blockName) {
		return switch (blockName) {
			case "minecraft:cave_air",
				 "minecraft:void_air",
				 "minecraft:air",
				 "minecraft:barrier",
				 "minecraft:light",
				 "minecraft:structure_void" -> BlockColor.TRANSPARENT;
			default -> 0;
		};
	}

	private static int hasGrassTint(String blockName) {
		return switch (blockName) {
			case "minecraft:fern",
				 "minecraft:grass_block",
				 "minecraft:large_fern",
				 "minecraft:melon_stem",
				 "minecraft:attached_melon_stem",
				 "minecraft:pumpkin_stem",
				 "minecraft:attached_pumpkin_stem",
				 "minecraft:short_grass",
				 "minecraft:tall_grass" -> BlockColor.GRASS_TINT;
			default -> 0;
		};
	}

	private static int hasFoliageTint(String blockName) {
		return switch (blockName) {
			case "minecraft:acacia_leaves",
				 "minecraft:dark_oak_leaves",
				 "minecraft:jungle_leaves",
				 "minecraft:mangrove_leaves",
				 "minecraft:oak_leaves",
				 "minecraft:vine" -> BlockColor.FOLIAGE_TINT;
			default -> 0;
		};
	}

	private static int isWater(String blockName) {
		return switch(blockName) {
			case "minecraft:water",
				 "minecraft:bubble_column" -> BlockColor.WATER;
			default -> 0;
		};
	}

	private static int isFoliage(String blockName) {
		return switch(blockName) {
			case "minecraft:acacia_leaves",
				 "minecraft:azalea_leaves",
				 "minecraft:birch_leaves",
				 "minecraft:cherry_leaves",
				 "minecraft:dark_oak_leaves",
				 "minecraft:flowering_azalea_leaves",
				 "minecraft:jungle_leaves",
				 "minecraft:mangrove_leaves",
				 "minecraft:oak_leaves",
				 "minecraft:pale_oak_leaves",
				 "minecraft:spruce_leaves" -> BlockColor.FOLIAGE;
			default -> 0;
		};
	}

	private Map<String, String> resolveTextureMapping(String model, Path assetBlockmodels) throws IOException {
		Path modelPath = assetBlockmodels.resolve(model + ".json");
		Map<String, String> mapping = new HashMap<>();
		Path parent = modelPath;

		while (parent != null) {
			try (BufferedReader reader = Files.newBufferedReader(parent)) {
				JsonObject pRoot = JsonParser.parseReader(reader).getAsJsonObject();
				if (pRoot.has("textures")) {
					for (Map.Entry<String, JsonElement> entry : pRoot.getAsJsonObject("textures").entrySet()) {
						mapping.putIfAbsent(entry.getKey(), entry.getValue().getAsString());
					}
				}
				if (!pRoot.has("parent")) {
					parent = null;
				} else {
					String ps = trimNS(pRoot.get("parent").getAsString());
					parent = assetBlockmodels.resolve(ps + ".json");
				}
			}
		}
		return mapping;
	}

	private String getTopTextureName(Map<String, String> mapping) {
		if (mapping.containsKey("top")) {
			return resolveTextureReference("top", mapping);
		} else if (mapping.containsKey("up")) {
			return resolveTextureReference("up", mapping);
		} else if (mapping.containsKey("all")) {
			return resolveTextureReference("all", mapping);
		} else if (mapping.containsKey("particle")) {
			return resolveTextureReference("particle", mapping);
		}
		throw new IllegalStateException("block doesn't have top texture");
	}

	private String resolveTextureReference(String key, Map<String, String> mapping) {
		String t = mapping.get(key);
		while (t.startsWith("#")) {
			t = mapping.get(t.substring(1));
		}
		return t;
	}

	private int averageColor(Path img) {
		try (InputStream inputStream = Files.newInputStream(img)) {
			BufferedImage image = ImageIO.read(inputStream);
			long r = 0, g = 0, b = 0;
			int c = 0;
			for (int x = 0; x < image.getWidth(); x++) {
				for (int y = 0; y < image.getHeight(); y++) {
					int p = image.getRGB(x, y);
					if (p >> 24 != 0) {
						r += p >> 16 & 0xFF;
						g += p >> 8 & 0xFF;
						b += p & 0xFF;
						c++;
					}
				}
			}
			int ir = (int) (r / c);
			int ig = (int) (g / c);
			int ib = (int) (b / c);
			return (ir << 16) + (ig << 8) + ib;
		} catch (IOException e) {
			// ignore
		}
		return 0xffffff;
	}

	private int getColorMapping(double temperature, double downfall, BufferedImage map) {
		double adjTemperature = Math.max(0.0, Math.min(1.0, temperature));
		double adjDownfall = Math.max(0.0, Math.min(1.0, downfall)) * adjTemperature;
		int pixelX = (int) (255 - adjTemperature * 255);
		int pixelY = (int) (255 - adjDownfall * 255);
		return map.getRGB(pixelX, pixelY) & 0xFFFFFF;
	}
}
