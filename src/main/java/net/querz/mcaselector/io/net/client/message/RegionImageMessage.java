package net.querz.mcaselector.io.net.client.message;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import net.querz.mcaselector.io.ByteArrayReader;
import net.querz.mcaselector.io.MCAFilePipe;
import net.querz.mcaselector.io.job.RegionImageGenerator;
import net.querz.mcaselector.io.net.event.MessageEvent;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.tiles.Tile;
import net.querz.mcaselector.tiles.TileMap;
import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class RegionImageMessage extends MessageEvent {

	private static final int ID = 101;

	private TileMap tileMap;
	private byte[] data;

	public RegionImageMessage(Point2i region, Image img) {
		super(ID);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (DataOutputStream dos = new DataOutputStream(baos)) {
			dos.writeInt(region.getX());
			dos.writeInt(region.getZ());
			ImageIO.write(SwingFXUtils.fromFXImage(img, null), "png", dos);
		} catch (IOException e) {
			e.printStackTrace();
		}
		data = baos.toByteArray();
	}

	public RegionImageMessage(TileMap tileMap) {
		super(ID);
		this.tileMap = tileMap;
	}

	@Override
	public byte[] getData() {
		return data;
	}

	@Override
	public byte[] execute(ByteArrayReader ptr) {
		Point2i region = ptr.readPoint2i();
		Image img = null;

		if (ptr.hasNext()) {
			try {
				img = SwingFXUtils.toFXImage(ImageIO.read(ptr), null);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		Tile tile = tileMap.getTile(region);
		if (tile != null) {
			// TODO: scaling
			tile.setImage(img);
			tile.setLoaded(true);
			tileMap.update();
		}

		RegionImageGenerator.MCAImageSaveCacheJob saveJob = new RegionImageGenerator.MCAImageSaveCacheJob(img, region, null, null, false, null);
		MCAFilePipe.executeSaveData(saveJob);

		return null;
	}
}
