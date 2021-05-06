package net.querz.mcaselector.io.net.server.message;

import net.querz.mcaselector.io.ByteArrayReader;
import net.querz.mcaselector.io.ByteArrayWriter;
import net.querz.mcaselector.io.job.RegionImageGenerator;
import net.querz.mcaselector.io.net.event.MessageEvent;
import net.querz.mcaselector.point.Point2f;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.property.DataProperty;
import net.querz.mcaselector.tiles.Tile;
import net.querz.mcaselector.tiles.TileMap;

public class WindowInteractMessage extends MessageEvent {

	private static final int ID = 100;

	private byte[] data;

	public WindowInteractMessage(double width, double height, Point2f offset, float scale) {
		super(ID);
		ByteArrayWriter baw = new ByteArrayWriter(data = new byte[28]);
		baw.writeDouble(width);
		baw.writeDouble(height);
		baw.writePoint2f(offset);
		baw.writeFloat(scale);
	}

	public WindowInteractMessage() {
		super(ID);
	}

	public byte[] getData() {
		return data;
	}

	@Override
	public byte[] execute(ByteArrayReader ptr) {

		// size, offset, scale
		double width = ptr.readDouble();
		double height = ptr.readDouble();
		Point2f offset = ptr.readPoint2f();
		float scale = ptr.readFloat();

		DataProperty<Integer> visible = new DataProperty<>(0);
		TileMap.runOnVisibleRegions(region -> {
			visible.set(visible.get() + 1);

			RegionImageGenerator.generate(new Tile(region), null, null, null, false, null);

		}, offset, scale, width, height, new Point2f());

		System.out.printf("WindowEvent: [width=%f, height=%f, offset=%s, scale=%f, visible=%d]\n", width, height, offset, scale, visible.get());

		return null;
	}
}
