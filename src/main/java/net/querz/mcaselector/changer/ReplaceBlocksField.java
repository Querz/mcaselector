package net.querz.mcaselector.changer;

import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.progress.Timer;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.VersionController;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class ReplaceBlocksField extends Field<Map<String, ChunkFilter.BlockReplaceData>> {

	private static final Set<String> validNames = new HashSet<>();
	private static final Map<Integer, String> validIDs = new HashMap<>();

	static {
		try (BufferedReader bis = new BufferedReader(
				new InputStreamReader(Objects.requireNonNull(ReplaceBlocksField.class.getClassLoader().getResourceAsStream("mapping/all_block_names.txt"))))) {
			String line;
			while ((line = bis.readLine()) != null) {
				validNames.add("minecraft:" + line);
			}
		} catch (IOException ex) {
			Debug.dumpException("error reading mapping/all_block_names.txt", ex);
		}

		try (BufferedReader bis = new BufferedReader(
				new InputStreamReader(Objects.requireNonNull(ReplaceBlocksField.class.getClassLoader().getResourceAsStream("mapping/block_name_to_id.txt"))))) {
			String line;
			while ((line = bis.readLine()) != null) {
				String[] split = line.split(";");
				validIDs.put(Integer.parseInt(split[1]), "minecraft:" + split[0]);
			}
		} catch (IOException ex) {
			Debug.dumpException("error reading mapping/block_name_to_id.txt", ex);
		}
	}

	public ReplaceBlocksField() {
		super(FieldType.REPLACE_BLOCKS);
	}

	@Override
	public boolean parseNewValue(String s) {
		String low = s.toLowerCase();

		// format: <from=to>[,<from=to>,...]

		Map<String, ChunkFilter.BlockReplaceData> newValue = new HashMap<>();

		String[] pairs = low.replaceAll(" ", "").split(",");
		for (String pair : pairs) {
			String[] fromTo = pair.split("=");
			if (fromTo.length != 2) {
				return super.parseNewValue(s);
			}
			String from = fromTo[0];
			String to = fromTo[1];
			boolean fromQuoted = false, toQuoted = false;
			if (from.startsWith("'") && from.endsWith("'") && from.length() > 1) {
				from = from.substring(1, from.length() - 1);
				fromQuoted = true;
			}
			if (to.startsWith("'") && to.endsWith("'") && to.length() > 1) {
				to = to.substring(1, to.length() - 1);
				toQuoted = true;
			}

			String key = "", value = "";

			if (from.matches("^[0-9]+$")) {
				try {
					int id = Integer.parseInt(from);
					if (fromQuoted || validIDs.containsKey(id)) {
						key = validIDs.get(id);
					}
				} catch (NumberFormatException ex) {
					return super.parseNewValue(s);
				}
			} else if (fromQuoted) {
				key = from;
			} else {
				if (!from.startsWith("minecraft:")) {
					from = "minecraft:" + from;
				}
				if (validNames.contains(from)) {
					key = from;
				} else {
					return super.parseNewValue(s);
				}
			}

			if (to.matches("^[0-9]+$")) {
				try {
					int id = Integer.parseInt(to);
					if (toQuoted || validIDs.containsKey(id)) {
						value = validIDs.get(id);
					}
				} catch (NumberFormatException ex) {
					return super.parseNewValue(s);
				}
			} else if (toQuoted) {
				value = to;
			} else {
				if (!to.startsWith("minecraft:")) {
					to = "minecraft:" + to;
				}
				if (validNames.contains(to)) {
					value = to;
				} else {
					return super.parseNewValue(s);
				}
			}

			newValue.put(key, new ChunkFilter.BlockReplaceData(value));
		}

		setNewValue(newValue);
		return true;
	}

	@Override
	public Map<String, ChunkFilter.BlockReplaceData> getOldValue(ChunkData data) {
		return null;
	}

	@Override
	public void change(ChunkData data) {
		VersionController.getChunkFilter(data.getRegion().getData().getInt("DataVersion")).replaceBlocks(data.getRegion().getData(), getNewValue());
	}

	@Override
	public void force(ChunkData data) {
		change(data);
	}

	@Override
	public String toString() {
		return getType().toString() + " = \"" + getNewValue().entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining(", ")) + "\"";
	}

	@Override
	public String valueToString() {
		return getNewValue().entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining(", "));
	}
}
