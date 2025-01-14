package net.querz.mcaselector.version.mapping.generator;

import net.querz.mcaselector.version.mapping.minecraft.VersionManifest;
import net.querz.mcaselector.version.mapping.util.Download;

import java.io.IOException;
import java.nio.file.Path;

public class Test {


	private static Path tmpDir = Path.of("tmp");

	public static void main(String[] args) throws IOException, InterruptedException {

//		String tmpDir = "C:\\Users\\Raphael\\IdeaProjects\\mcaselector-color-calculator\\tmp\\";
		String mcVersion = "1.21.4";

		Path tmp = tmpDir.resolve(mcVersion);
		ColorConfig cfg = new ColorConfig();
		Path mf = tmpDir.resolve("version_manifest.json");
		Download.to("https://launchermeta.mojang.com/mc/game/version_manifest.json", mf);
		VersionManifest manifest = VersionManifest.of(mf);
		cfg.generate(manifest.getVersionByID(mcVersion), tmp);
		Path c = tmp.resolve("config.json");
		cfg.save(c);

		ColorConfig cfg2 = new ColorConfig(c);
//		cfg2.colors.colors.forEach((k, v) -> {
//			System.out.println(k + " = " + v.getClass());
//		});
		Path c2 = tmp.resolve("config2.json");
		cfg2.save(c2);



//		Path colorMapping = Path.of("C:\\Users\\Raphael\\IdeaProjects\\mcaselector-color-calculator\\src\\main\\resources\\color_mapping.json");
////		Path colorMapping = Path.of("/Users/rb/IdeaProjects/color_calculator/src/main/resources/color_mapping.json");
//		ColorMapping mapping = new ColorMapping(colorMapping);
//
////		System.out.println(ColorMapping.GSON.toJson(mapping));
//
//		Path stateIndex = Path.of("C:\\Users\\Raphael\\IdeaProjects\\mcaselector-color-calculator\\src\\main\\resources\\state_index.json");
////		Path stateIndex = Path.of("/Users/rb/IdeaProjects/color_calculator/src/main/resources/state_index.json");
//		BlockStates blockstates = new BlockStates(stateIndex);
//
//		CompoundTag c = new CompoundTag();
//		c.putString("candles", "1");
//		c.putString("lit", "true");
//		c.putString("waterlogged", "false");
////		c.putString("waterlogged", "true");
//
//		BitSet states = blockstates.getState(c);
//		System.out.println(states);
//
//		BlockColor color = mapping.getBlockColor("purple_candle", states);
//		System.out.println(color);


//		System.out.println(BlockStates.GSON.toJson(blockstates));



		// ---------------handle version manifest and version file


//		Path versionManifest = Path.of("C:\\Users\\Raphael\\IdeaProjects\\mcaselector-color-calculator\\tmp\\version_manifest.json");
//		if (!Files.exists(versionManifest)) {
//			System.out.println("downloading version manifest");
//			VersionManifest.download(versionManifest);
//		}
//		VersionManifest vm = VersionManifest.of(versionManifest);
//		System.out.println(vm.latestRelease());
//
//
//		Path minecraftVersionFile = Path.of("C:\\Users\\Raphael\\IdeaProjects\\mcaselector-color-calculator\\tmp\\1.21.4\\version.json");
//		if (!Files.exists(minecraftVersionFile)) {
//			System.out.println("downloading minecraft version file");
//			MinecraftVersionFile.download(vm.latestRelease(), minecraftVersionFile);
//		}
//		MinecraftVersionFile mvf = MinecraftVersionFile.of(minecraftVersionFile);
//		System.out.println(mvf.getDownloads());




//		System.out.println(VersionManifest.getVersionByID("1.20.5-pre1"));

	}
}
