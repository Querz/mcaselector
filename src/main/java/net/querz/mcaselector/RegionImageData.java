package net.querz.mcaselector;

import java.awt.*;
import java.awt.image.BufferedImage;

public class RegionImageData {
	private BufferedImage image;
	private int zeroX, zeroZ; //the lowermost point in this region in world chunk coordinates
	private boolean[][] hasChunks; //a boolean grid that indicates whether a chunk exists

	public RegionImageData(int regionX, int regionZ) {
		this.image = image;
		this.zeroX = regionX * 32;
		this.zeroZ = regionZ * 32;

		//test if region already has an image
		// load the image
		// or create the image from the region file
	}

	public void markChunkForDeletion(int worldChunkX, int worldChunkZ) {
		hasChunks[worldChunkX - zeroX][worldChunkZ - zeroZ] = false;
	}
}
