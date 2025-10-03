package net.querz.mcaselector.version.mapping.util;

import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.version.mapping.minecraft.MinecraftVersion;
import net.querz.mcaselector.version.mapping.minecraft.MinecraftVersionFile;
import net.querz.mcaselector.version.mapping.minecraft.ServerVersion;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.io.NBTWriter;
import java.io.IOException;
import java.nio.file.*;
import java.util.Properties;
import java.util.zip.GZIPOutputStream;

public final class DebugWorld {

	private final Path path;

	public DebugWorld(Path path) {
		this.path = path;
	}

	public void generate(MinecraftVersion mcVersion) throws IOException, InterruptedException {
		Path r_0_0_mca =path.resolve("world/region/r.0.0.mca");
		if (Files.exists(r_0_0_mca)) {
			return;
		}

		Path versionJson = path.resolve("version.json");
		Path serverJar = path.resolve("server.jar");

		// download version.json
		if (!Files.exists(versionJson)) {
			MinecraftVersionFile.download(mcVersion, versionJson);
		}
		MinecraftVersionFile versionFile = MinecraftVersionFile.load(versionJson);

		// download server jar
		if (!Files.exists(serverJar)) {
			Download.to(versionFile.getDownloads().server().url(), serverJar);
		}

		// get dataversion for this minecraft version from server.jar
		int dataVersion;
		try (FileSystem fs = FileSystems.newFileSystem(serverJar)) {
			Path serverVersionJson = fs.getPath("version.json");
			ServerVersion serverVersion = ServerVersion.load(serverVersionJson);
			dataVersion = serverVersion.worldVersion();
		}

		// generate minimal level.dat for debug world
		generateLevelDat(mcVersion, dataVersion);

		// start server once to generate eula.txt and server.properties if they don't exist yet, and set their values to what we need
		Path eulaTxt = path.resolve("eula.txt");
		String eula = null;
		if (!Files.exists(eulaTxt) || (eula = Files.readString(eulaTxt)).contains("eula=false")) {
			Command.exec(path, "java", "-jar", "server.jar", "--nogui");
			Path serverProperties = path.resolve("server.properties");
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

		// copy chunk.dat to forceload debug world to world/data/chunk.dat
		Path worldDataDir = path.resolve("world/data");
		if (!Files.exists(worldDataDir)) {
			Files.createDirectories(worldDataDir);
		}
		Path chunksDat = worldDataDir.resolve("chunks.dat");
		FileHelper.copyFromResource("mapping/generator/chunks.dat", chunksDat);

		// start server and immediately stop, the spawn point + spawnChunkRadius set in level.dat
		// will cause the entire r.0.0.mca to generate
		if (System.getProperty("os.name").toLowerCase().contains("win")) {
			Command.redirect(path,
					"cmd", "/C", "echo", "stop", "|",
					"java", "-jar", "server.jar", "--nogui");
		} else {
			Command.redirect(path,
					"printf", "stop", "|",
					"java", "-jar", "server.jar", "--nogui");
		}
	}

	private void generateLevelDat(MinecraftVersion mcVersion, int dataVersion) throws IOException {
		Path levelDatPath = path.resolve("world/level.dat");
		Path levelDatOld = path.resolve("world/level.dat_old");
		if (Files.exists(levelDatPath) && Files.exists(levelDatOld)) {
			return;
		}

		CompoundTag levelDat = new CompoundTag();
		CompoundTag data = new CompoundTag();
		CompoundTag worldGenSettings = new CompoundTag();
		CompoundTag dimensions = new CompoundTag();
		CompoundTag overworld = new CompoundTag();
		CompoundTag generator = new CompoundTag();
		CompoundTag version = new CompoundTag();
		CompoundTag gameRules = new CompoundTag();
		generator.putString("type", "minecraft:debug");
		overworld.put("generator", generator);
		overworld.putString("type", "minecraft:overworld");
		dimensions.put("minecraft:overworld", overworld);
		worldGenSettings.putLong("seed", 0);
		worldGenSettings.put("dimensions", dimensions);
		version.putByte("Snapshot", (byte) 0);
		version.putString("Series", "main");
		version.putInt("Id", dataVersion);
		version.putString("Name", mcVersion.id());
		gameRules.putString("spawnChunkRadius", "14");
		data.put("WorldGenSettings", worldGenSettings);
		data.put("Version", version);
		data.put("GameRules", gameRules);
		data.putInt("SpawnX", 240);
		data.putInt("SpawnZ", 240);
		data.putInt("SpawnY", 0);
		data.putString("LevelName", "world");
		data.putInt("DataVersion", dataVersion);
		data.putInt("version", 19133);
		levelDat.put("Data", data);

		if (!Files.exists(levelDatPath.getParent())) {
			Files.createDirectories(levelDatPath.getParent());
		}
		try (GZIPOutputStream go = new GZIPOutputStream(Files.newOutputStream(levelDatPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
			new NBTWriter().writeNamed(go, "", levelDat);
			go.flush();
		}
		Files.copy(levelDatPath, levelDatOld, StandardCopyOption.REPLACE_EXISTING);
	}
}
