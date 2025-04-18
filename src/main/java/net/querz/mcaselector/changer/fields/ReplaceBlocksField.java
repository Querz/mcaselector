package net.querz.mcaselector.changer.fields;

import net.querz.mcaselector.changer.Field;
import net.querz.mcaselector.changer.FieldType;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.VersionHandler;
import net.querz.mcaselector.version.mapping.registry.BlockRegistry;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.io.snbt.ParseException;
import net.querz.nbt.io.snbt.SNBTParser;
import java.util.HashMap;
import java.util.Map;

public class ReplaceBlocksField extends Field<Map<String, ChunkFilter.BlockReplaceData>> {

	public ReplaceBlocksField() {
		super(FieldType.REPLACE_BLOCKS);
	}

	@Override
	public boolean parseNewValue(String s) {

		// format: <from=to>[,<from=to>,...]
		// from format: minecraft:<block-name>
		//              <block-name>
		//              '<custom-block-name-with-namespace>'
		// to format:   minecraft:<block-name>
		//              <block-name>
		//              '<custom-block-name-with-namespace>'
		//              <snbt-string-block-state>
		//              <to>;<snbt-string-tile-entity>

		Map<String, ChunkFilter.BlockReplaceData> newValue = new HashMap<>();

		String trimmed = s.trim();

		while (!trimmed.isEmpty()) {
			String[] fromTo = trimmed.split("=", 2);
			if (fromTo.length != 2) {
				return super.parseNewValue(s);
			}

			String from = fromTo[0].trim();
			if (from.startsWith("'") && from.endsWith("'") && from.length() > 2) {
				from = from.substring(1, from.length() - 1);
			} else if (!from.startsWith("minecraft:")) {
				from = "minecraft:" + from;
				if (!BlockRegistry.isValidName(from)) {
					return super.parseNewValue(s);
				}
			}

			String to = fromTo[1].trim();
			int read = 0;
			CompoundTag toState = null;
			String toName = null;
			if (to.startsWith("{")) {
				// block state
				try {
					SNBTParser parser = new SNBTParser(to);
					toState = (CompoundTag) parser.parse(true);
					read += parser.getReadChars() - 1;
				} catch (ParseException ex) {
					return super.parseNewValue(s);
				}
			} else if (to.startsWith("'")) {
				// quoted
				int i = 1;
				while (i < to.length() && to.charAt(i) != '\'') {
					i++;
				}
				toName = to.substring(1, Math.max(i, to.length()));
				if (!toName.endsWith("'")) {
					return super.parseNewValue(s);
				}
				toName = toName.substring(0, toName.length() - 1);
				if (toName.isEmpty()) {
					return super.parseNewValue(s);
				}
				read += i + 1;
			} else {
				// minecraft block
				// read everything until , or ;
				int i = 0;
				while (i < to.length()) {
					if (to.charAt(i) == ',' || to.charAt(i) == ';') {
						break;
					}
					i++;
				}
				toName = to.substring(0, i);
				if (!toName.startsWith("minecraft:")) {
					toName = "minecraft:" + toName;
				}
				if (!BlockRegistry.isValidName(toName)) {
					return super.parseNewValue(s);
				}
				read += i;
			}

			to = to.substring(read).trim();

			CompoundTag toTile = null;
			if (to.startsWith(";")) {
				to = to.substring(1).trim();
				if (to.isEmpty()) {
					return super.parseNewValue(s);
				}
				try {
					SNBTParser parser = new SNBTParser(to);
					toTile = (CompoundTag) parser.parse(true);
					int readTile = parser.getReadChars();
					to = to.substring(readTile - 1);
				} catch (ParseException ex) {
					return super.parseNewValue(s);
				}
			}

			ChunkFilter.BlockReplaceData data;
			if (toName != null && toTile != null) {
				data = new ChunkFilter.BlockReplaceData(toName, toTile);
			} else if (toName != null) {
				data = new ChunkFilter.BlockReplaceData(toName);
			} else if (toState != null && toTile != null) {
				data = new ChunkFilter.BlockReplaceData(toState, toTile);
			} else if (toState != null) {
				data = new ChunkFilter.BlockReplaceData(toState);
			} else {
				return super.parseNewValue(s);
			}
			newValue.put(from, data);

			to = to.trim();

			if (to.startsWith(",")) {
				trimmed = to.substring(1).trim();
			} else if (!to.isEmpty()) {
				return super.parseNewValue(s);
			} else {
				break;
			}
		}

		if (newValue.isEmpty()) {
			return super.parseNewValue(s);
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
		VersionHandler.getImpl(data, ChunkFilter.Blocks.class).replaceBlocks(data, getNewValue());
		ChunkFilter.Heightmap heightmap = VersionHandler.getImpl(data, ChunkFilter.Heightmap.class);
		heightmap.worldSurface(data);
		heightmap.oceanFloor(data);
		heightmap.motionBlocking(data);
		heightmap.motionBlockingNoLeaves(data);
	}

	@Override
	public void force(ChunkData data) {
		change(data);
	}

	@Override
	public String toString() {
		return getType().toString() + " = \"" + valueToString() + "\"";
	}

	@Override
	public String valueToString() {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (Map.Entry<String, ChunkFilter.BlockReplaceData> entry : getNewValue().entrySet()) {
			if (first) {
				first = false;
			} else {
				sb.append(", ");
			}
			String from = entry.getKey();
			if (!from.startsWith("minecraft:")) {
				sb.append("'").append(from).append("'");
			} else {
				sb.append(entry.getKey());
			}
			sb.append("=");
			sb.append(escapeString(entry.getValue().toString()));
		}
		return sb.toString();
	}

	private String escapeString(String s) {
		return s.replace("\\", "\\\\").replace("\"", "\\\"");
	}
}
