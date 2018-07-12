package net.querz.mcaselector.version.anvil112;

import net.querz.mcaselector.util.Debug;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.ListTag;
import net.querz.nbt.Tag;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Anvil112ChunkFilter implements ChunkFilter {

	private Map<String, BlockData[]> mapping = new HashMap<>();

	public Anvil112ChunkFilter() {
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
		ListTag sections = (ListTag) ((CompoundTag) data.get("Level")).get("Sections");
		int c = 0;
		nameLoop:
		for (String name : names) {
			BlockData[] bd = mapping.get(name);
			if (bd == null) {
				Debug.dump("No mapping found for " + name);
				continue;
			}
			for (Tag t : sections.getValue()) {
				byte[] blocks = ((CompoundTag) t).getBytes("Blocks");
				byte[] blockData = ((CompoundTag) t).getBytes("Data");

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

	private class BlockData {
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
}
