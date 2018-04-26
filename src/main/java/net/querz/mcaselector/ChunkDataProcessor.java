package net.querz.mcaselector;

import net.querz.nbt.CompoundTag;
import java.awt.image.BufferedImage;

public interface ChunkDataProcessor {
	BufferedImage drawImage(CompoundTag data, ColorMapping colorMapping);
}
