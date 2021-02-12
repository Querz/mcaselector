package net.querz.mcaselector.version.anvil112;

import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.ListTag;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static net.querz.mcaselector.validation.ValidationHelper.*;

public class Anvil112ChunkFilter implements ChunkFilter {

	private final Map<String, BlockData[]> mapping = new HashMap<>();

	public Anvil112ChunkFilter() {
		// noinspection ConstantConditions
		try (BufferedReader bis = new BufferedReader(
				new InputStreamReader(Anvil112ChunkFilter.class.getClassLoader().getResourceAsStream("mapping/block_name_to_id.txt")))) {
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
					Debug.dumpException(String.format("unable to parse block id or data in block id mapping file: \"%s\"", line), ex);
					continue;
				}

				String[] names = elements[0].split(",");
				for (String name : names) {
					String fullName = "minecraft:" + name;
					BlockData blockData = new BlockData(id, data);
					BlockData[] array = mapping.get(fullName);
					if (array != null) {
						BlockData[] newArray = new BlockData[array.length + 1];
						System.arraycopy(array, 0, newArray, 0, array.length);
						newArray[newArray.length - 1] = blockData;
						array = newArray;
					} else {
						array = new BlockData[1];
						array[0] = blockData;
					}
					mapping.put(fullName, array);
				}
			}

		} catch (IOException ex) {
			throw new RuntimeException("failed to open mapping/block_name_to_id.txt");
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
				Debug.dump("no mapping found for " + name);
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

	@Override
	public void replaceBlocks(CompoundTag data, Map<String, String> replace) {
		ListTag<CompoundTag> sections = withDefault(() -> data.getCompoundTag("Level").getListTag("Sections").asCompoundTagList(), null);
		if (sections == null) {
			return;
		}

		// handle the special case when someone wants to replace air with something else
		if (replace.containsKey("minecraft:air")) {
			Map<Integer, CompoundTag> sectionMap = new HashMap<>();
			List<Integer> heights = new ArrayList<>(18);
			for (CompoundTag section : sections) {
				sectionMap.put((int) section.getByte("Y"), section);
				heights.add((int) section.getByte("Y"));
			}

			for (int y = 0; y < 16; y++) {
				if (!sectionMap.containsKey(y)) {
					sectionMap.put(y, createEmptySection(y));
					heights.add(y);
				} else {
					CompoundTag section = sectionMap.get(y);
					if (!section.containsKey("Blocks") || !section.containsKey("Data")) {
						sectionMap.put(y, createEmptySection(y));
					}
				}
			}

			heights.sort(Integer::compareTo);
			sections.clear();

			for (int height : heights) {
				sections.add(sectionMap.get(height));
			}
		}

		for (CompoundTag section : sections) {
			for (Map.Entry<String, String> entry : replace.entrySet()) {
				BlockData[] bd = mapping.get(entry.getKey());
				BlockData bdr = mapping.get(entry.getValue())[0];

				byte[] blocks = section.getByteArray("Blocks");
				byte[] blockData = section.getByteArray("Data");

				blockLoop:
				for (int i = 0; i < blocks.length; i++) {
					byte dataByte = blockData[i / 2];
					byte dataBits = (byte) (i % 2 == 0 ? dataByte & 0x0F : (dataByte >> 4) & 0x0F);
					for (BlockData d : bd) {
						if (d.id == blocks[i] && d.data.contains(dataBits)) {
							blocks[i] = (byte) bdr.id;
							byte newDataBits = bdr.data.iterator().next();
							blockData[i / 2] = (byte) (i % 2 == 0 ? (dataByte & 0xF0) + newDataBits : (dataByte & 0x0F) + (newDataBits << 4));
							continue blockLoop;
						}
					}
				}
			}
		}

		// delete tile entities with that name
		ListTag<CompoundTag> tileEntities = catchClassCastException(() -> data.getCompoundTag("Level").getListTag("TileEntities").asCompoundTagList());
		if (tileEntities != null) {
			for (int i = 0; i < tileEntities.size(); i++) {
				CompoundTag tileEntity = tileEntities.get(i);
				String id = catchClassCastException(() -> tileEntity.getString("id"));
				if (replace.containsKey(id)) {
					tileEntities.remove(i);
					i--;
				}
			}
		}
	}

	protected CompoundTag createEmptySection(int y) {
		CompoundTag newSection = new CompoundTag();
		newSection.putByte("Y", (byte) y);
		newSection.putByteArray("Blocks", new byte[4096]);
		newSection.putByteArray("Data", new byte[2048]);
		return newSection;
	}
}
