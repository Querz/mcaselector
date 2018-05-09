package net.querz.mcaselector.tiles;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import net.querz.mcaselector.Point2f;
import net.querz.mcaselector.Point2i;

public abstract class Tile {
	private int size;
	private Point2i location;
	protected Image image;

	public Tile(int size, Point2i location) {
		this.size = size;
		this.location = location;
	}

	public Image getImage() {
		return image;
	}

	public Point2i getLocation() {
		return location;
	}

	public int getSize() {
		return size;
	}

	public boolean isLoaded() {
		return image != null;
	}

	public void unload() {
		image.cancel();
		image = null;
	}

	public void draw(GraphicsContext ctx, float scale, Point2f offset) {
//		System.out.print("drawing " + location + " ");
		ctx.drawImage(getImage(), offset.getX(), offset.getY(), size / scale, size / scale);
	}

	public abstract void loadImage();
}
