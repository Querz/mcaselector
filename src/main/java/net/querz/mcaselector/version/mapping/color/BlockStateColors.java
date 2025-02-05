package net.querz.mcaselector.version.mapping.color;

import com.google.gson.annotations.SerializedName;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class BlockStateColors implements StateColors {

	@SerializedName("states")
	private final Map<BitSet, BlockColor> blockStateColors;
	@SerializedName("default")
	private BlockColor defaultColor;

	public BlockStateColors(Map<BitSet, BlockColor> blockStateColors) {
		this.blockStateColors = blockStateColors;
	}

	public BlockStateColors(BitSet state, BlockColor color) {
		this.blockStateColors = new HashMap<>();
		this.blockStateColors.put(state, color);
	}

	public BlockStateColors() {
		this.blockStateColors = new HashMap<>();
	}

	@Override
	public BlockColor getColor(BitSet state) {
		return blockStateColors.getOrDefault(state, defaultColor);
	}

	@Override
	public BlockColor getDefaultColor() {
		return defaultColor;
	}

	@Override
	public boolean hasColor(BitSet state) {
		return blockStateColors.containsKey(state);
	}

	@Override
	public void setColor(BitSet state, BlockColor color) {
		blockStateColors.put(state, color);
		if (defaultColor == null) {
			defaultColor = color;
		}
	}

	public StateColors compress() {
		BlockColor c = null;
		for (Map.Entry<BitSet, BlockColor> entry : blockStateColors.entrySet()) {
			if (c == null) {
				c = entry.getValue();
				continue;
			}
			if (!c.equals(entry.getValue())) {
				return this;
			}
		}
		return new SingleStateColors(c);
	}

	@Override
	public Iterator<Map.Entry<BitSet, BlockColor>> iterator() {
		return blockStateColors.entrySet().iterator();
	}
}
