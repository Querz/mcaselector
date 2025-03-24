package net.querz.mcaselector.changer.fields;

import net.querz.mcaselector.changer.Field;
import net.querz.mcaselector.changer.FieldType;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.VersionHandler;
import net.querz.mcaselector.version.mapping.registry.BiomeRegistry;

public class BiomeField extends Field<BiomeRegistry.BiomeIdentifier> {

	public BiomeField() {
		super(FieldType.BIOME);
	}

	@Override
	public String toString() {
		return "Biome = " + getNewValue();
	}

	@Override
	public boolean parseNewValue(String s) {
		// name or 'name' or id or 'id'

		String low = s.toLowerCase();
		boolean quoted = false;
		if (low.startsWith("'") && low.endsWith("'") && low.length() > 1) {
			low = low.substring(1, low.length() - 1);
			quoted = true;
		} else if (!low.matches("^[0-9]+$") && !low.startsWith("minecraft:")) {
			low = "minecraft:" + low;
		}

		if (low.matches("^[0-9]+$")) {
			try {
				int id = Integer.parseInt(low);
				if (quoted || BiomeRegistry.isValidID(id)) {
					setNewValue(new BiomeRegistry.BiomeIdentifier(id));
					return true;
				}
			} catch (NumberFormatException ex) {
				// do nothing
			}
		} else if (low.equals("-1")) {
			setNewValue(new BiomeRegistry.BiomeIdentifier(-1));
			return true;
		} else if (quoted || BiomeRegistry.isValidName(low)) {
			setNewValue(new BiomeRegistry.BiomeIdentifier(low));
			return true;
		}
		return super.parseNewValue(s);
	}

	@Override
	public BiomeRegistry.BiomeIdentifier getOldValue(ChunkData data) {
		return null;
	}

	@Override
	public void change(ChunkData data) {
		VersionHandler.getImpl(data, ChunkFilter.Biomes.class).changeBiome(data, getNewValue());
	}

	@Override
	public void force(ChunkData data) {
		VersionHandler.getImpl(data, ChunkFilter.Biomes.class).forceBiome(data, getNewValue());
	}
}
