package net.querz.mcaselector.version.mapping.registry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import javafx.scene.image.Image;
import net.querz.mcaselector.config.ConfigProvider;
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.io.ImageHelper;
import net.querz.mcaselector.version.mapping.generator.StructureConfig;
import java.util.*;
import java.util.function.BiConsumer;

public final class StructureRegistry {

	private StructureRegistry() {}

	private static final Gson GSON = new GsonBuilder()
			.setPrettyPrinting()
			.create();

	private static final Set<String> valid = new HashSet<>();
	private static final Map<String, Set<String>> alts = new HashMap<>();
	private static final Map<String, StructureIcon> icons = new HashMap<>();
	private static final TreeMap<String, StructureMenuIcon> displayNames = new TreeMap<>(String::compareToIgnoreCase);

	private static Image defaultIcon = null;
	private static Image moreIcon = null;
	private static StructureIcon defaultStructureIcon = null;

	static {
		init();
	}

	public static void init() {
		valid.clear();
		alts.clear();
		icons.clear();
		displayNames.clear();

		int size = ConfigProvider.WORLD.getStructureIconSize();
		int borderSize = ConfigProvider.WORLD.getStructureIconBorderSize();

		defaultIcon = ImageHelper.renderOutline(FileHelper.getIconFromResources("img/structure/red_x"), size, borderSize);
		defaultStructureIcon = new StructureIcon(defaultIcon, null, Float.MAX_VALUE);
		moreIcon = ImageHelper.renderOutline(FileHelper.getIconFromResources("img/structure/more"), size, borderSize);

		FileHelper.loadFromResource("mapping/registry/structures.json", r -> {
			List<StructureConfig.StructureData> set = GSON.fromJson(r, new TypeToken<>(){});
			for (StructureConfig.StructureData s : set) {
				displayNames.put(s.display(), new StructureMenuIcon(ImageHelper.renderOutline(loadIcon(s.icon()), 16, 1, false), "minecraft:" + s.name()));
				for (String structureName : s.allNames()) {
					valid.add(structureName);
					String name;
					if (!containsUpperCase(structureName)) {
						name = "minecraft:" + structureName;
						valid.add(name);
					} else {
						name = structureName;
					}
					if (valid.contains(structureName) && icons.containsKey(name)) {
						if (!icons.get(name).parents.isEmpty()) {
							icons.get(name).parents.add("minecraft:" + s.name());
						}
						continue;
					}
					Image icon = s.icon().isEmpty() ? defaultIcon : FileHelper.getIconFromResources("img/structure/" + s.icon());
					Set<String> parents;
					if (s.alt() == null || structureName.equals(s.name())) {
						parents = Collections.emptySet();
					} else {
						parents = new HashSet<>();
						parents.add("minecraft:" + s.name());
					}
					icons.put(name, new StructureIcon(ImageHelper.renderOutline(icon, size, borderSize), parents, s.maxScale()));
				}
			}
			for (String name : valid) {
				alts.computeIfAbsent(name, k -> new HashSet<>()).add(name);
				if (containsUpperCase(name)) {
					Set<String> alt = alts.get(name);
					String lower = name.toLowerCase();
					if (valid.contains(lower)) {
						alt.add(lower);
						alts.computeIfAbsent(lower, k -> new HashSet<>()).add(name);
					}
					String lowerNamespace = "minecraft:" + lower;
					if (valid.contains(lowerNamespace)) {
						alt.add(lowerNamespace);
						alts.computeIfAbsent(lowerNamespace, k -> new HashSet<>()).add(name);
					}
				} else if (name.startsWith("minecraft:")) {
					alts.get(name).add(name.substring(10));
				} else {
					alts.get(name).add("minecraft:" + name);
				}
			}
			return null;
		});
	}

	private static boolean containsUpperCase(String s) {
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (Character.isUpperCase(c)) {
				return true;
			}
		}
		return false;
	}

	private static Image loadIcon(String name) {
		name = name.isEmpty() ? "red_x" : name;
		return FileHelper.getIconFromResources("img/structure/" + name);
	}

	public static boolean isValidName(String name) {
		return valid.contains(name);
	}

	public static Set<String> getAlts(String name) {
		return alts.getOrDefault(name, Collections.singleton(name));
	}

	public static Image getIcon(String name, float scale) {
		StructureIcon icon = icons.getOrDefault(name, defaultStructureIcon);
		if (scale > icon.maxScale) {
			return null;
		}
		if (icon.parents.isEmpty()) {
			return ConfigProvider.GLOBAL.getStructureIcons().getOrDefault(name, true) ? icon.icon : null;
		}
		for (String parent : icon.parents) {
			if (ConfigProvider.GLOBAL.getStructureIcons().getOrDefault(parent, true)) {
				return icon.icon;
			}
		}
		return null;
	}

	public static Image getDefaultIcon() {
		return defaultIcon;
	}

	public static Image getMoreIcon() {
		return moreIcon;
	}

	public static void forEachDisplayName(BiConsumer<String, StructureMenuIcon> consumer) {
		displayNames.forEach(consumer);
	}

	private record StructureIcon(Image icon, Set<String> parents, float maxScale) {}
	public record StructureMenuIcon(Image icon, String id) {}
}
