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
import java.util.regex.Pattern;
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

		// copy mapping/generator/level.dat to world/level.dat
		Path worldDir = path.resolve("world");
		if (!Files.exists(worldDir)) {
			Files.createDirectories(worldDir);
		}
		Path levelDat = worldDir.resolve("level.dat");
		FileHelper.copyFromResource("mapping/generator/level.dat", levelDat);
		Path levelDatOld = worldDir.resolve("level.dat_old");
		FileHelper.copyFromResource("mapping/generator/level.dat", levelDatOld);

		// copy mapping/generator/chunk_tickets.dat to world/dimensions/minecraft/overworld/data/minecraft/chunk_tickets.dat
		Path overworldDataDir = worldDir.resolve("dimensions/minecraft/overworld/data/minecraft");
		if (!Files.exists(overworldDataDir)) {
			Files.createDirectories(overworldDataDir);
		}
		Path chunkTicketsDat = overworldDataDir.resolve("chunk_tickets.dat");
		FileHelper.copyFromResource("mapping/generator/chunk_tickets.dat", chunkTicketsDat);

		// copy mapping/generator/world_gen_settings.dat to world/data/minecraft/world_gen_settings.dat
		Path worldDataDir = worldDir.resolve("data/minecraft");
		if (!Files.exists(worldDataDir)) {
			Files.createDirectories(worldDataDir);
		}
		Path worldGenSettingsDat = worldDataDir.resolve("world_gen_settings.dat");
		FileHelper.copyFromResource("mapping/generator/world_gen_settings.dat", worldGenSettingsDat);


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

		// start server and immediately stop, the spawn point + spawnChunkRadius set in level.dat
		// will cause the entire r.0.0.mca to generate
		Pattern serverDoneLoadingPattern = Pattern.compile("\\[Server thread/INFO]: Done \\(.+\\)! For help, type \"help\"");
		Command.exec(path, (line, process) -> {
			if (serverDoneLoadingPattern.matcher(line).find()) {
				Command.sendToProcess(process, "stop\n");
			}
		}, "java", "-jar", "server.jar", "--nogui");
	}
}
