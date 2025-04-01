package net.querz.mcaselector.version.mapping.generator;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.version.mapping.color.*;
import net.querz.mcaselector.version.mapping.minecraft.*;
import net.querz.mcaselector.version.mapping.util.*;
import net.querz.nbt.CompoundTag;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class ColorConfig {

	@SerializedName("states") public BlockStates states;
	@SerializedName("colors") public ColorMapping colors;
	@SerializedName("tints") public BiomeColors tints;
	public transient ColorMapping.TintCache tintCache;
	public transient ColorMapping.LegacyTintCache legacyTintCache;

	public static final ColorProperties colorProperties = FileHelper.loadFromResource(
			"mapping/color_properties.json",
			ColorProperties::load);

	private static final Gson GSON = new GsonBuilder()
			.registerTypeAdapter(BitSet.class, new BitSetAdapter())
			.registerTypeHierarchyAdapter(BlockColor.class, new BlockColor.BlockColorAdapter())
			.registerTypeAdapter(SingleStateColors.class, new SingleStateColors.SingleStateColorsAdapter())
			.registerTypeAdapter(BlockStates.class, new BlockStates.BlockStatesTypeAdapter())
			.registerTypeAdapter(BiomeColors.class, new BiomeColors.BiomeColorsTypeAdapter())
			.registerTypeAdapterFactory(ColorMapping.ColorMappingTypeAdapterFactory.getColorMappingTypeAdapterFactory())
			.registerTypeAdapterFactory(StateColors.StateColorsTypeAdapterFactory.getStateColorsTypeAdapterFactory())
			.registerTypeHierarchyAdapter(Set.class, new CollectionAdapter())
			.enableComplexMapKeySerialization()
			.disableHtmlEscaping()
			.setPrettyPrinting()
			.create();

	public ColorConfig() {}

	public static ColorConfig load(Path path) throws IOException {
		try (BufferedReader reader = Files.newBufferedReader(path)) {
			return load(reader);
		}
	}

	public static ColorConfig load(Reader reader) {
		ColorConfig cfg = GSON.fromJson(reader, ColorConfig.class);
		cfg.tintCache = cfg.colors.createTintCache(cfg.tints);
		return cfg;
	}

	public static ColorConfig loadLegacy(Path path) throws IOException {
		try (BufferedReader reader = Files.newBufferedReader(path)) {
			return loadLegacy(reader);
		}
	}

	public static ColorConfig loadLegacy(Reader reader) {
		ColorConfig cfg = GSON.fromJson(reader, ColorConfig.class);
		cfg.legacyTintCache = cfg.colors.createLegacyTintCache(cfg.tints);
		return cfg;
	}

	public void save(Path path) throws IOException {
		String json = GSON.toJson(this);
		Files.writeString(path, json);
	}

	public BlockColor getColor(String name, String biome, CompoundTag tag) {
		BitSet blockState = states.getState(tag);
		BlockColor blockColor = colors.getBlockColor(name, blockState);
		if ((blockColor.properties & BlockColor.TINTED) > 0) {
			BlockColor tinted = tintCache.getColor(name, biome, blockState);
			return tinted == null ? tintCache.getColor(name, "minecraft:plains", null) : tinted;
		}
		return blockColor;
	}

	public int getLegacyColor(String name, int biome, CompoundTag tag) {
		BitSet blockState = states.getState(tag);
		BlockColor blockColor = colors.getBlockColor(name, blockState);
		if ((blockColor.properties & BlockColor.TINTED) > 0) {
			return legacyTintCache.getColor(name, biome, blockState);
		}
		return blockColor.color;
	}

	public void generate(MinecraftVersion version, Path tmp) throws IOException, InterruptedException {
		Path versionJson = tmp.resolve("version.json");
		Path clientJar = tmp.resolve("client.jar");
		Path serverJar = tmp.resolve("server.jar");
		Path generated = tmp.resolve("generated");

		// download version.json
		if (!Files.exists(versionJson)) {
			MinecraftVersionFile.download(version, versionJson);
		}
		MinecraftVersionFile versionFile = MinecraftVersionFile.load(versionJson);

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
		Blocks blocks = Blocks.load(blocksJson);
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
				if (colorProperties.isAir(blockName)) {
					continue;
				}

				// apply static color if configured
				if (colorProperties.staticColor.containsKey(blockName)) {
					mapping.setBlockColor(blockName, new SingleStateColors(new BlockColor(colorProperties.staticColor.get(blockName), colorProperties.get(blockName))));
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
							mapping.addBlockColor(blockName, blockStateBitsWT, new BlockColor(color, colorProperties.get(blockName)));
							BitSet blockStateBitsWF = blockStates.getState(waterloggedFalse);
							blockStateBitsWF.or(blockStateBits);
							mapping.addBlockColor(blockName, blockStateBitsWF, new BlockColor(color, colorProperties.get(blockName)));
						} else {
							mapping.addBlockColor(blockName, blockStateBits, new BlockColor(color, colorProperties.get(blockName)));
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
					mapping.addBlockColor(blockName, null, new BlockColor(color, colorProperties.get(blockName)));
				}

				// apply static tint if configured
				if (colorProperties.staticTint.containsKey(blockName)) {
					BlockColor rawColor = mapping.getBlockColor(blockName, null);
					BlockColor tinted = new BlockColor(ColorMapping.applyTint(rawColor.color, colorProperties.staticTint.get(blockName)), rawColor.properties);
					mapping.setBlockColor(blockName, new SingleStateColors(tinted));
				}
			}

			// extract biome tints
			Path biomes = generated.resolve("data/minecraft/worldgen/biome");
			Path assetGrass = assetBase.resolve("textures/colormap/grass.png");
			Path assetFoliage = assetBase.resolve("textures/colormap/foliage.png");
			Path assetDryFoliage = assetBase.resolve("textures/colormap/dry_foliage.png");
			BufferedImage grassTints = ImageIO.read(Files.newInputStream(assetGrass));
			BufferedImage foliageTints = ImageIO.read(Files.newInputStream(assetFoliage));
			BufferedImage dryFoliageTints = ImageIO.read(Files.newInputStream(assetDryFoliage));

			try (DirectoryStream<Path> ds = Files.newDirectoryStream(biomes)) {
				for (Path b : ds) {
					if (!Files.isRegularFile(b)) {
						continue;
					}
					Biome biome = Biome.load(b);
					int grassTint = Objects.requireNonNullElseGet(
							biome.effects.grassTint(),
							() -> getColorMapping(biome.temperature, biome.downfall, grassTints));
					int foliageTint = Objects.requireNonNullElseGet(
							biome.effects.foliageTint(),
							() -> getColorMapping(biome.temperature, biome.downfall, foliageTints));
					int dryFoliageTint = Objects.requireNonNullElseGet(
							biome.effects.dryFoliageTint(),
							() -> getColorMapping(biome.temperature, biome.downfall, dryFoliageTints));
					String fileName = b.getFileName().toString();
					tints.addTints(
							"minecraft:" + fileName.substring(0, fileName.length() - 5),
							new BiomeColors.BiomeTints(grassTint, foliageTint, biome.effects.waterTint(), dryFoliageTint));
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
		// we use javafx Image instead of BufferedImage because BufferedImage#getRGB()
		// returns wrong color values depending on the color space for some reason.
		try (InputStream inputStream = Files.newInputStream(img)) {
			Image image = new Image(inputStream);
			PixelReader pr = image.getPixelReader();
			long r = 0, g = 0, b = 0;
			int c = 0;
			for (int x = 0; x < image.getWidth(); x++) {
				for (int y = 0; y < image.getHeight(); y++) {
					int p = pr.getArgb(x, y);
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
			return (ir << 16) | (ig << 8) | ib;
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

	public record ColorProperties(
			@SerializedName("air") Set<String> air,
			@SerializedName("transparent") Set<String> transparent,
			@SerializedName("grass_tint") Set<String> grassTint,
			@SerializedName("foliage_tint") Set<String> foliageTint,
			@SerializedName("dry_foliage_tint") Set<String> dryFoliageTint,
			@SerializedName("water") Set<String> water,
			@SerializedName("foliage") Set<String> foliage,
			@SerializedName("static_tint") Map<String, Integer> staticTint,
			@SerializedName("static_color") Map<String, Integer> staticColor) {

		private static final Gson GSON = new GsonBuilder()
				.setPrettyPrinting()
				.registerTypeAdapter(Integer.class, new HexColorAdapter())
				.create();

		public static ColorProperties load(Path path) throws IOException {
			try (BufferedReader reader = Files.newBufferedReader(path)) {
				return load(reader);
			}
		}

		public static ColorProperties load(Reader reader) throws IOException {
			return GSON.fromJson(reader, ColorProperties.class);
		}

		public boolean isAir(String blockName) {
			return air.contains(blockName);
		}

		public int getTransparent(String blockName) {
			return transparent.contains(blockName) ? BlockColor.TRANSPARENT : 0;
		}

		public int getGrassTint(String blockName) {
			return grassTint.contains(blockName) ? BlockColor.GRASS_TINT : 0;
		}

		public int getFoliageTint(String blockName) {
			return foliageTint.contains(blockName) ? BlockColor.FOLIAGE_TINT : 0;
		}

		public int getWater(String blockName) {
			return water.contains(blockName) ? BlockColor.WATER : 0;
		}

		public int getFoliage(String blockName) {
			return foliage.contains(blockName) ? BlockColor.FOLIAGE : 0;
		}

		public int getStaticTint(String blockName) {
			return staticTint.containsKey(blockName) ? BlockColor.STATIC_TINT : 0;
		}

		public int getStaticColor(String blockName) {
			return staticColor.containsKey(blockName) ? BlockColor.STATIC_COLOR : 0;
		}

		public int getDryFoliageTint(String blockName) {
			return dryFoliageTint.contains(blockName) ? BlockColor.DRY_FOLIAGE_TINT : 0;
		}

		public int get(String blockName) {
			return getTransparent(blockName)
					| getGrassTint(blockName)
					| getFoliageTint(blockName)
					| getWater(blockName)
					| getFoliage(blockName)
					| getStaticTint(blockName)
					| getStaticColor(blockName)
					| getDryFoliageTint(blockName);
		}
	}
}
