package net.querz.mcaselector.version.mapping.generator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import net.querz.mcaselector.io.mca.RegionChunk;
import net.querz.mcaselector.io.mca.RegionMCAFile;
import net.querz.mcaselector.version.Helper;
import net.querz.mcaselector.version.mapping.minecraft.MinecraftVersion;
import net.querz.mcaselector.version.mapping.util.DebugWorld;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.ListTag;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class HeightmapConfig {

	@SerializedName("world_surface") private Set<String> worldSurface;
	@SerializedName("ocean_floor") private Set<String> oceanFloor;
	@SerializedName("motion_blocking") private Set<String> motionBlocking;
	@SerializedName("leaves") private Set<String> leaves;

	private static final String WORLD_SURFACE = "WORLD_SURFACE";
	private static final String OCEAN_FLOOR = "OCEAN_FLOOR";
	private static final String MOTION_BLOCKING = "MOTION_BLOCKING";
	private static final String MOTION_BLOCKING_NO_LEAVES = "MOTION_BLOCKING_NO_LEAVES";

	private static final Gson GSON = new GsonBuilder()
			.setPrettyPrinting()
			.create();

	public HeightmapConfig() {
		worldSurface = new HashSet<>();
		oceanFloor = new HashSet<>();
		motionBlocking = new HashSet<>();
		leaves = new HashSet<>();
	}

	public static HeightmapConfig load(Path path) throws IOException {
		return GSON.fromJson(Files.newBufferedReader(path), HeightmapConfig.class);
	}

	public void save(Path path) throws IOException {
		String json = GSON.toJson(this);
		Files.writeString(path, json);
	}

	public void generate(MinecraftVersion version, Path tmp) throws IOException, InterruptedException {
		// generate debug world
		DebugWorld debugWorld = new DebugWorld(tmp);
		debugWorld.generate(version);

		// read r.0.0.mca
		Path region_0_0 = tmp.resolve("world/region/r.0.0.mca");
		RegionMCAFile region = new RegionMCAFile(region_0_0.toFile());
		region.load(false);
		Set<String> mbnl = new HashSet<>();
		for (int i = 0; i < 1024; i++) {
			RegionChunk chunk = region.getChunk(i);

			// get section 4
			ListTag sections = Helper.tagFromCompound(chunk.getData(), "sections");
			if (sections == null) {
				continue;
			}
			CompoundTag section = null;
			for (CompoundTag s : sections.iterateType(CompoundTag.class)) {
				int y = Helper.numberFromCompound(s, "Y", -5).intValue();
				if (y == 4) {
					section = s;
					break;
				}
			}
			if (section == null) {
				continue;
			}

			for (int x = 1; x < 16; x += 2) {
				for (int z = 1; z < 16; z += 2) {
					CompoundTag block = getBlockAt(section, x, 6, z);
					int worldSurface = getHeightmapDataAt(chunk.getData(), x, z, WORLD_SURFACE);
					int oceanFloor = getHeightmapDataAt(chunk.getData(), x, z, OCEAN_FLOOR);
					int motionBlocking = getHeightmapDataAt(chunk.getData(), x, z, MOTION_BLOCKING);
					int motionBlockingNoLeaves = getHeightmapDataAt(chunk.getData(), x, z, MOTION_BLOCKING_NO_LEAVES);
					String name = block.getString("Name");
					if (worldSurface > 125) {
						this.worldSurface.add(name);
					}
					if (oceanFloor > 125) {
						this.oceanFloor.add(name);
					}
					if (motionBlocking > 125) {
						this.motionBlocking.add(name);
					}
					if (motionBlockingNoLeaves > 125) {
						mbnl.add(name);
					}
				}
			}
		}

		for (String id : this.motionBlocking) {
			if (!mbnl.contains(id)) {
				this.leaves.add(id);
			}
		}
	}

	private int getHeightmapDataAt(CompoundTag root, int x, int z, String heightmapID) {
		int index = z * 16 + x;
		CompoundTag heightmaps = root.getCompound("Heightmaps");
		long[] data = heightmaps.getLongArray(heightmapID);
		int dataIndex = Math.floorDiv(index, 7);
		int startBit = (index % 7) * 9;
		return (int) ((data[dataIndex] >> startBit) & 0x1FF);
	}

	private CompoundTag getBlockAt(CompoundTag section, int x, int y, int z) {
		ListTag palette = Helper.tagFromCompound(Helper.tagFromCompound(section, "block_states"), "palette");
		long[] data = Helper.longArrayFromCompound(Helper.tagFromCompound(section, "block_states"), "data");
		int paletteIndex = getPaletteIndex(x & 0xF, y & 0xF, z & 0xF, data);
		return palette.getCompound(paletteIndex);
	}

	private int getPaletteIndex(int x, int y, int z, long[] data) {
		int bits = data == null ? 0 : data.length >> 6;
		if (bits == 0) {
			return 0;
		}
		int clean = ((2 << (bits - 1)) - 1);
		int index = y * 256 + z * 16 + x;
		int indexesPerLong = (int) (64D / bits);
		int blockStatesIndex = index / indexesPerLong;
		int startBit = (index % indexesPerLong) * bits;
		return (int) (data[blockStatesIndex] >> startBit) & clean;
	}
}
