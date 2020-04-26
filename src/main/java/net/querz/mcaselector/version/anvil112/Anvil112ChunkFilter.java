package net.querz.mcaselector.version.anvil112;

import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.ListTag;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import static net.querz.mcaselector.validation.ValidationHelper.*;

public class Anvil112ChunkFilter implements ChunkFilter {

	private Map<String, BlockData[]> mapping = new HashMap<>();

	public Anvil112ChunkFilter() {
		// noinspection ConstantConditions
		try (BufferedReader bis = new BufferedReader(
				new InputStreamReader(Anvil112ChunkFilter.class.getClassLoader().getResourceAsStream("block-id-mapping.csv")))) {
			String line;
			while ((line = bis.readLine()) != null) {
				line = line.trim();
				if (line.startsWith("#")) {
					continue;
				}
				String[] elements = line.split(";");
				if (elements.length != 3 && !line.endsWith(";")) {
					Debug.error("invalid line in block id mapping file: \"" + line + "\"");
					continue;
				}

				int id;
				Set<Byte> data = new HashSet<>();
				try {
					id = Integer.parseInt(elements[1]);
					String[] stringBytes;
					if (elements.length == 2 || (stringBytes = elements[2].split(",")).length == 0) {
						for (int i = 0; i < 16; i++) {
							data.add((byte) i);
						}
					} else {
						for (String stringByte : stringBytes) {
							data.add(Byte.parseByte(stringByte));
						}
					}
				} catch (NumberFormatException ex) {
					Debug.error("unable to parse block id or data in block id mapping file: \"" + line + "\"");
					continue;
				}

				String[] names = elements[0].split(",");
				for (String name : names) {
					BlockData blockData = new BlockData(id, data);
					BlockData[] array = mapping.get(name);
					if (array != null) {
						BlockData[] newArray = new BlockData[array.length + 1];
						System.arraycopy(array, 0, newArray, 0, array.length);
						newArray[newArray.length - 1] = blockData;
						array = newArray;
					} else {
						array = new BlockData[1];
						array[0] = blockData;
					}
					mapping.put(name, array);
				}
			}

		} catch (IOException ex) {
			throw new RuntimeException("Unable to open block id mapping file");
		}
	}

	@Override
	public boolean matchBlockNames(CompoundTag data, String... names) {
		ListTag<CompoundTag> sections = withDefault(() -> data.getCompoundTag("Level").getListTag("Sections").asCompoundTagList(), null);
		if (sections == null) {
			return false;
		}
		int c = 0;
		nameLoop:
		for (String name : names) {
			BlockData[] bd = mapping.get(name);
			if (bd == null) {
				Debug.dump("No mapping found for " + name);
				continue;
			}
			for (CompoundTag t : sections) {
				byte[] blocks = withDefault(() -> t.getByteArray("Blocks"), null);
				if (blocks == null) {
					continue;
				}
				byte[] blockData = withDefault(() -> t.getByteArray("Data"), null);
				if (blockData == null) {
					continue;
				}

				for (int i = 0; i < blocks.length; i++) {
					short b = (short) (blocks[i] & 0xFF);
					for (BlockData d : bd) {
						if (d.id == b) {
							byte dataByte = (byte) (i % 2 == 0 ? blockData[i / 2] & 0x0F : (blockData[i / 2] >> 4) & 0x0F);
							if (d.data.contains(dataByte)) {
								c++;
								continue nameLoop;
							}
						}
					}
				}
			}
		}
		return names.length == c;
	}

	private static class BlockData {
		int id;
		Set<Byte> data;

		BlockData(int id, Set<Byte> data) {
			this.id = id;
			this.data = data;
		}

		@Override
		public String toString() {
			return "{" + id + ":" + Arrays.toString(data.toArray()) + "}";
		}
	}

	@Override
	public boolean matchBiomeIDs(CompoundTag data, int... ids) {
		if (!data.containsKey("Level") || withDefault(() -> data.getCompoundTag("Level").getByteArray("Biomes"), null) == null) {
			return false;
		}
		filterLoop: for (int filterID : ids) {
			for (byte dataID : data.getCompoundTag("Level").getByteArray("Biomes")) {
				if (filterID == dataID) {
					continue filterLoop;
				}
			}
			return false;
		}
		return true;
	}

	@Override
	public void changeBiome(CompoundTag data, int id) {
		if (!data.containsKey("Level") || withDefault(() -> data.getCompoundTag("Level").getByteArray("Biomes"), null) == null) {
			return;
		}
		Arrays.fill(data.getCompoundTag("Level").getByteArray("Biomes"), (byte) id);
	}

	@Override
	public void forceBiome(CompoundTag data, int id) {
		if (data.containsKey("Level")) {
			byte[] biomes = new byte[256];
			Arrays.fill(biomes, (byte) id);
			data.getCompoundTag("Level").putByteArray("Biomes", biomes);
		}
	}
}
