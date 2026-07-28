package net.querz.mcaselector.changer.fields;

import net.querz.mcaselector.changer.Field;
import net.querz.mcaselector.changer.FieldType;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.VersionHandler;
import java.util.LinkedHashMap;
import java.util.Map;

public class ReplaceBlocksField extends Field<Map<ChunkFilter.BlockReplaceSource, ChunkFilter.BlockReplaceData>> {

	public ReplaceBlocksField() {
		super(FieldType.REPLACE_BLOCKS);
	}

	@Override
	public boolean parseNewValue(String value) {
		ReplaceBlocksParser.Result result = ReplaceBlocksParser.parse(value);
		if (!(result instanceof ReplaceBlocksParser.Success success)) {
			return super.parseNewValue(value);
		}
		Map<ChunkFilter.BlockReplaceSource, ChunkFilter.BlockReplaceData> replacements = new LinkedHashMap<>();
		for (ReplaceBlocksParser.ParsedRule rule : success.rules()) {
			replacements.put(rule.source(), rule.target());
		}
		if (replacements.isEmpty()) {
			return super.parseNewValue(value);
		}
		setNewValue(replacements);
		return true;
	}

	@Override
	public Map<ChunkFilter.BlockReplaceSource, ChunkFilter.BlockReplaceData> getOldValue(ChunkData data) {
		return null;
	}

	@Override
	public void change(ChunkData data) {
		applyReplacement(data);
	}

	private boolean applyReplacement(ChunkData data) {
		if (!VersionHandler.getImpl(data, ChunkFilter.Blocks.class).replaceBlocks(data, getNewValue())) {
			return false;
		}
		VersionHandler.getImpl(data, ChunkFilter.LightPopulated.class).setLightPopulated(data, (byte) 0);
		ChunkFilter.Heightmap heightmap = VersionHandler.getImpl(data, ChunkFilter.Heightmap.class);
		heightmap.worldSurface(data);
		heightmap.oceanFloor(data);
		heightmap.motionBlocking(data);
		heightmap.motionBlockingNoLeaves(data);
		return true;
	}

	@Override
	public void force(ChunkData data) {
		applyReplacement(data);
	}

	@Override
	public boolean applyWithResult(ChunkData data, boolean force) {
		return applyReplacement(data);
	}

	@Override
	public String toString() {
		return getType().toString() + " = \"" + escapeString(valueToString()) + "\"";
	}

	@Override
	public String valueToString() {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (Map.Entry<ChunkFilter.BlockReplaceSource, ChunkFilter.BlockReplaceData> entry : getNewValue().entrySet()) {
			if (first) {
				first = false;
			} else {
				sb.append(", ");
			}
			sb.append(entry.getKey());
			sb.append("=");
			sb.append(entry.getValue());
		}
		return sb.toString();
	}

	private String escapeString(String value) {
		return value.replace("\\", "\\\\").replace("\"", "\\\"");
	}
}
