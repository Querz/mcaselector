package net.querz.mcaselector.version;

import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.point.Point2i;
import java.util.Random;

public interface EntityRelocator {

	Random random = new Random();

	boolean relocateEntities(ChunkData data, Point2i offset);
}
