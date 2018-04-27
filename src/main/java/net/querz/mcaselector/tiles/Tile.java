package net.querz.mcaselector.tiles;

import javafx.scene.image.ImageView;

import java.awt.image.BufferedImage;

public class Tile {
	private BufferedImage image;

	public Tile(BufferedImage image) {
		this.image = image;
	}

	public BufferedImage getImage() {
		return image;
	}
}
