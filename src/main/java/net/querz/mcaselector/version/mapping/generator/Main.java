package net.querz.mcaselector.version.mapping.generator;

import net.querz.mcaselector.version.mapping.minecraft.VersionManifest;
import net.querz.mcaselector.version.mapping.util.Download;
import java.io.IOException;
import java.nio.file.Path;

public class Main {

	private static final Path tmpDir = Path.of("tmp");

	public static void main(String[] args) throws IOException, InterruptedException {

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
	}
}
