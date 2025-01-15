package net.querz.mcaselector.version.mapping.generator;

import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.version.mapping.minecraft.VersionManifest;
import net.querz.mcaselector.version.mapping.util.Command;
import net.querz.mcaselector.version.mapping.util.Download;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.ListTag;
import net.querz.nbt.LongTag;
import net.querz.nbt.NBTUtil;
import net.querz.nbt.Tag;
import net.querz.nbt.io.NBTWriter;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Properties;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

public class Main {

	private static final Path tmpDir = Path.of("tmp");

	public static void main(String[] args) throws IOException, InterruptedException {
		String mcVersion = "1.21.4";
//
		Path tmp = tmpDir.resolve(mcVersion);
//		ColorConfig cfg = new ColorConfig();
//		Path mf = tmpDir.resolve("version_manifest.json");
//		Download.to("https://launchermeta.mojang.com/mc/game/version_manifest.json", mf);
//		VersionManifest manifest = VersionManifest.of(mf);
//		cfg.generate(manifest.getVersionByID(mcVersion), tmp);
//		Path c = tmp.resolve("config.json");
//		cfg.save(c);
//
//		ColorConfig cfg2 = new ColorConfig(c);
////		cfg2.colors.colors.forEach((k, v) -> {
////			System.out.println(k + " = " + v.getClass());
////		});
//		Path c2 = tmp.resolve("config2.json");
//		cfg2.save(c2);


//		Tag t = NBTUtil.read(new File("/Users/rb/IdeaProjects/mcaselector/tmp/1.21.4/world/level.dat"));
//		Tag t = NBTUtil.read(new File("/Users/rb/Library/Application Support/minecraft/saves/1_21_4 debug/level.dat"));
//		System.out.println(NBTUtil.toSNBT(t, "\t"));
//		System.exit(0);

		Tag levelDatMin = NBTUtil.fromSNBT("""
				{
					Data: {
						WorldGenSettings: {
							seed: 0L,
							dimensions: {
								"minecraft:overworld": {
									generator: {
										type: "minecraft:debug"
									},
									type: "minecraft:overworld"
								}
							}
						},
						Version: {
							Snapshot: 0b,
							Series: "main",
							Id: 4189,
							Name: "1.21.4"
						},
						GameRules: {
							spawnChunkRadius: "13"
						},
						SpawnY: 0,
						SpawnZ: 240,
						SpawnX: 240,
						version: 19133,
						LevelName: "world",
						DataVersion: 4189
					}
				}
			""");
		System.out.println(NBTUtil.toSNBT(levelDatMin));
		Path levelDat = tmp.resolve("world/level.dat");
		if (!Files.exists(levelDat.getParent())) {
			Files.createDirectories(levelDat.getParent());
		}
		try (GZIPOutputStream go = new GZIPOutputStream(Files.newOutputStream(levelDat, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
			new NBTWriter().writeNamed(go, "", levelDatMin);
			go.flush();
		}
//		NBTUtil.write(levelDat.toFile(), levelDatMin, true);
		Path levelDatOld = tmp.resolve("world/level.dat_old");
		Files.copy(levelDat, levelDatOld);




//		System.exit(0);

//		long[] forceLoad_0_0 = new long[1024];
//		for (int i = 0; i < 1024; i++) {
//			forceLoad_0_0[i] = new Point2i(i >> 5, i & 0x1F).asLong();
//		}

//		long[] forceLoad_0_0 = new long[] {
//				new Point2i(4, 4).asLong(),
//				new Point2i(13, 4).asLong(),
//				new Point2i(22, 4).asLong(),
//				new Point2i(27, 4).asLong(),
//				new Point2i(4, 13).asLong(),
//				new Point2i(13, 13).asLong(),
//				new Point2i(22, 13).asLong(),
//				new Point2i(27, 13).asLong(),
//				new Point2i(4, 22).asLong(),
//				new Point2i(13, 22).asLong(),
//				new Point2i(22, 22).asLong(),
//				new Point2i(27, 22).asLong(),
//				new Point2i(4, 27).asLong(),
//				new Point2i(13, 27).asLong(),
//				new Point2i(22, 27).asLong(),
//				new Point2i(27, 27).asLong()
//		};
//		CompoundTag data = new CompoundTag();
//		data.putLongArray("Forced", forceLoad_0_0);
//		data.putInt("DataVersion", 4189);
//		CompoundTag cd = new CompoundTag();
//		cd.put("data", data);
//		Path chunksDat = tmp.resolve("world/data/chunks.dat");
//		if (!Files.exists(chunksDat.getParent())) {
//			Files.createDirectories(chunksDat.getParent());
//		}
//		try (OutputStream out = Files.newOutputStream(chunksDat)) {
//			NBTUtil.write(out, cd);
//		}

		Path eulaTxt = tmp.resolve("eula.txt");
		String eula = null;
		if (!Files.exists(eulaTxt) || (eula = Files.readString(eulaTxt)).contains("eula=false")) {
			Command.exec(tmp, "java", "-jar", "server.jar", "--nogui");
			Path serverProperties = tmp.resolve("server.properties");
			Properties properties = new Properties();
			properties.load(Files.newInputStream(serverProperties));
			properties.setProperty("level-type", "minecraft:debug");
			properties.store(Files.newOutputStream(serverProperties), "Minecraft server properties");
			if (eula == null) {
				eula = Files.readString(eulaTxt);
			}
			eula = eula.replace("eula=false", "eula=true");
			Files.writeString(eulaTxt, eula);
		}

		Command.redirect(tmp,
				"printf", "stop", "|",
				"java", "-jar", "server.jar", "--nogui");
	}
}
