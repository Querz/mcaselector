package net.querz.mcaselector.version.mapping.color;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

public class ColorMapping {

	private static final StateColors missing = new SingleStateColors(BlockColor.MISSING) {
		@Override
		public boolean hasColor(BitSet state) {
			return false;
		}
	};

	public final Map<String, StateColors> colors;

	public ColorMapping() {
		colors = new HashMap<>();
	}

	private ColorMapping(Map<String, StateColors> colors) {
		this.colors = colors;
	}

	public BlockColor getBlockColor(String name, BitSet state) {
		return colors.getOrDefault(name, missing).getColor(state);
	}

	public boolean hasBlockColor(String name, BitSet state) {
		return colors.containsKey(name) && colors.get(name).hasColor(state);
	}

	public boolean hasBlockStateColors(String name) {
		StateColors c;
		return (c = colors.get(name)) != null && c instanceof BlockStateColors;
	}

	public void addBlockColor(String name, BitSet state, BlockColor color) {
		if (colors.containsKey(name)) {
			StateColors stateColors = colors.get(name);
			stateColors.setColor(state, color);
		} else if (state == null) {
			colors.put(name, new SingleStateColors(color));
		} else {
			colors.put(name, new BlockStateColors(state, color));
		}
	}

	public void setBlockColor(String name, StateColors color) {
		colors.put(name, color);
	}

	public void compress() {
		for (Map.Entry<String, StateColors> entry : colors.entrySet()) {
			StateColors stateColors = entry.getValue();
			if (stateColors instanceof BlockStateColors blockStateColors) {
				StateColors compressedColors = blockStateColors.compress();
				colors.put(entry.getKey(), compressedColors);
			}
		}
	}

	public TintCache createTintCache(BiomeColors tints) {
		TintCache cache = new TintCache(new HashMap<>());
		for (Map.Entry<String, StateColors> entry : colors.entrySet()) {
			for (Map.Entry<BitSet, BlockColor> color : entry.getValue()) {
				if ((color.getValue().properties & BlockColor.GRASS_TINT) > 0) {
					applyTints(entry.getKey(), color.getKey(), color.getValue().color, tints, BiomeColors.BiomeTints::grassColor, cache);
				} else if ((color.getValue().properties & BlockColor.FOLIAGE_TINT) > 0) {
					applyTints(entry.getKey(), color.getKey(), color.getValue().color, tints, BiomeColors.BiomeTints::foliageColor, cache);
				} else if ((color.getValue().properties & BlockColor.WATER) > 0) {
					applyTints(entry.getKey(), color.getKey(), color.getValue().color, tints, BiomeColors.BiomeTints::waterColor, cache);
				} else if ((color.getValue().properties & BlockColor.DRY_FOLIAGE_TINT) > 0) {
					applyTints(entry.getKey(), color.getKey(), color.getValue().color, tints, BiomeColors.BiomeTints::dryFoliageColor, cache);
				}
			}
		}
		return cache;
	}

	private void applyTints(String blockName, BitSet blockState, int base, BiomeColors tints, Function<BiomeColors.BiomeTints, Integer> colorProvider, TintCache cache) {
		Map<String, StateColors> colored = cache.data.computeIfAbsent(blockName, k -> new HashMap<>());
		for (Map.Entry<String, BiomeColors.BiomeTints> biomeTints : tints.biomes.entrySet()) {
			int c = applyTint(base, colorProvider.apply(biomeTints.getValue()));
			if (blockState == null) {
				colored.put(biomeTints.getKey(), new SingleStateColors(new BlockColor(c)));
			} else {
				colored.computeIfAbsent(biomeTints.getKey(), k -> new BlockStateColors()).setColor(blockState, new BlockColor(c));
			}
		}
	}

	public LegacyTintCache createLegacyTintCache(BiomeColors tints) {
		LegacyTintCache cache = new LegacyTintCache(new HashMap<>());
		for (Map.Entry<String, StateColors> entry : colors.entrySet()) {
			BlockColor color = entry.getValue().getDefaultColor();
			if ((color.properties & BlockColor.GRASS_TINT) > 0) {
				applyTintsLegacy(entry.getKey(), color.color, tints, BiomeColors.BiomeTints::grassColor, cache);
			} else if ((color.properties & BlockColor.FOLIAGE_TINT) > 0) {
				applyTintsLegacy(entry.getKey(),color.color, tints, BiomeColors.BiomeTints::foliageColor, cache);
			} else if ((color.properties & BlockColor.WATER) > 0) {
				applyTintsLegacy(entry.getKey(), color.color, tints, BiomeColors.BiomeTints::waterColor, cache);
			} else if ((color.properties & BlockColor.DRY_FOLIAGE_TINT) > 0) {
				applyTintsLegacy(entry.getKey(), color.color, tints, BiomeColors.BiomeTints::dryFoliageColor, cache);
			}
		}
		return cache;
	}

	private void applyTintsLegacy(String blockName, int base, BiomeColors tints, Function<BiomeColors.BiomeTints, Integer> colorProvider, LegacyTintCache cache) {
		int[] colored = cache.data.computeIfAbsent(blockName, k -> new int[256]);
		for (Map.Entry<String, BiomeColors.BiomeTints> biomeTints : tints.biomes.entrySet()) {
			int c = applyTint(base, colorProvider.apply(biomeTints.getValue()));
			colored[Integer.parseInt(biomeTints.getKey())] = c;
		}
	}

	public static int applyTint(int color, int tint) {
		int nr = (tint >> 16 & 0xFF) * (color >> 16 & 0xFF) >> 8;
		int ng = (tint >> 8 & 0xFF) * (color >> 8 & 0xFF) >> 8;
		int nb = (tint & 0xFF) * (color & 0xFF) >> 8;
		return color & 0xFF000000 | nr << 16 | ng << 8 | nb;
	}

	public record TintCache(Map<String, Map<String, StateColors>> data) {

		public BlockColor getColor(String block, String biome, BitSet state) {
			Map<String, StateColors> a;
			if ((a = data.get(block)) != null) {
				StateColors b;
				if ((b = a.get(biome)) != null) {
					return b.getColor(state);
				}
			}
			return null;
		}
	}

	public record LegacyTintCache(Map<String, int[]> data) {

		public int getColor(String block, int biome, BitSet state) {
			int[] a;
			if ((a = data.get(block)) != null) {
				return a[biome];
			}
			return missing.getColor(null).color;
		}
	}

	public static class ColorMappingTypeAdapterFactory implements TypeAdapterFactory {

		private static final TypeAdapterFactory instance = new ColorMappingTypeAdapterFactory();

		private ColorMappingTypeAdapterFactory() {}

		public static TypeAdapterFactory getColorMappingTypeAdapterFactory() {
			return instance;
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
			if (type.getRawType() != ColorMapping.class) {
				return null;
			}
			return (TypeAdapter<T>) new ColorMappingTypeAdapter(gson);
		}

		private static class ColorMappingTypeAdapter extends TypeAdapter<ColorMapping> {

			private final Gson gson;
			private final TypeAdapter<StateColors> adapter;

			public ColorMappingTypeAdapter(Gson gson) {
				this.gson = gson;
				adapter = StateColors.StateColorsTypeAdapterFactory.getStateColorsTypeAdapterFactory()
						.create(gson, StateColors.StateColorsTypeAdapterFactory.token);
			}

			@Override
			public void write(JsonWriter out, ColorMapping value) throws IOException {
				out.beginObject();
				TreeMap<String, StateColors> colors = new TreeMap<>(value.colors);
				for (Map.Entry<String, StateColors> entry : colors.entrySet()) {
					out.name(entry.getKey());
					gson.toJson(entry.getValue(), entry.getValue().getClass(), out);
				}
				out.endObject();
			}

			@Override
			public ColorMapping read(JsonReader in) throws IOException {
				Map<String, StateColors> colors = new HashMap<>();
				in.beginObject();
				while (in.hasNext()) {
					String name = in.nextName();
					StateColors color = adapter.read(in);
					colors.put(name, color);
				}
				in.endObject();
				return new ColorMapping(colors);
			}
		}
	}
}
