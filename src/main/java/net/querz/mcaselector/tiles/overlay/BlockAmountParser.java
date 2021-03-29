package net.querz.mcaselector.tiles.overlay;

import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.filter.PaletteFilter;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.version.VersionController;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class BlockAmountParser extends OverlayParser {

	private static final int MIN_VALUE = 0;
	private static final int MAX_VALUE = 98304; // 384 * 16 * 16

	private static final Set<String> validNames = new HashSet<>();

	static {
		try (BufferedReader bis = new BufferedReader(
				new InputStreamReader(Objects.requireNonNull(PaletteFilter.class.getClassLoader().getResourceAsStream("mapping/all_block_names.txt"))))) {
			String line;
			while ((line = bis.readLine()) != null) {
				validNames.add(line);
			}
		} catch (IOException ex) {
			Debug.dumpException("error reading mapping/all_block_names.txt", ex);
		}
	}

	public BlockAmountParser() {
		super(OverlayType.BLOCK_AMOUNT);
		setMultiValues(new String[0]);
	}

	@Override
	public int parseValue(ChunkData chunkData) {
		return VersionController.getChunkFilter(chunkData.getRegion().getData().getInt("DataVersion")).getBlockAmount(chunkData.getRegion().getData(), multiValues());
	}

	@Override
	public String name() {
		return "Blocks";
	}

	@Override
	public boolean setMin(String raw) {
		setRawMin(raw);
		try {
			int value = Integer.parseInt(raw);
			if (value < MIN_VALUE || value > MAX_VALUE) {
				return setMin((Integer) null);
			}
			return setMin(value);
		} catch (NumberFormatException ex) {
			return setMin((Integer) null);
		}
	}

	@Override
	public boolean setMax(String raw) {
		setRawMax(raw);
		try {
			int value = Integer.parseInt(raw);
			if (value < MIN_VALUE || value > MAX_VALUE) {
				return setMax((Integer) null);
			}
			return setMax(value);
		} catch (NumberFormatException ex) {
			return setMax((Integer) null);
		}
	}

	@Override
	public boolean setMultiValues(String raw) {
		if (raw == null) {
			setMultiValues(new String[0]);
			return false;
		}
		setRawMultiValues(raw);
		String[] split = raw.split(",");
		List<String> result = new ArrayList<>(split.length);
		for (String s : split) {
			String block = s.trim();
			if (!block.startsWith("minecraft:")) {
				block = "minecraft:" + block;
			}
			result.add(block);
		}
		setMultiValues(result.toArray(new String[0]));
		return true;
	}
}
