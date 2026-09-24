package net.querz.mcaselector.io.job;

import javafx.scene.image.Image;
import net.querz.mcaselector.config.ConfigProvider;
import net.querz.mcaselector.io.JobHandler;
import net.querz.mcaselector.io.RegionDirectories;
import net.querz.mcaselector.io.db.CacheHandler;
import net.querz.mcaselector.io.mca.RegionMCAFile;
import net.querz.mcaselector.selection.ChunkSet;
import net.querz.mcaselector.tile.Tile;
import net.querz.mcaselector.tile.TileImage;
import net.querz.mcaselector.util.progress.Timer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.IOException;
import java.util.UUID;
import java.util.function.Supplier;

public final class RegionHeaderImageGeneratorJob extends ProcessDataJob {

	private static final Logger LOGGER = LogManager.getLogger(RegionHeaderImageGeneratorJob.class);

	public static void generate(Tile tile, HeaderImageGeneratorCallback callback, Supplier<Integer> prioritySupplier) {
		LOGGER.debug("adding job {}, tile:{}", RegionHeaderImageGeneratorJob.class.getSimpleName(), tile);
		JobHandler.addJob(new RegionHeaderImageGeneratorJob(tile, ConfigProvider.WORLD.getWorldUUID(), callback, prioritySupplier));
	}

	private final Tile tile;
	private final UUID uuid;
	private final HeaderImageGeneratorCallback callback;
	private final Supplier<Integer> prioritySupplier;

	private RegionHeaderImageGeneratorJob(Tile tile, UUID uuid, HeaderImageGeneratorCallback callback, Supplier<Integer> prioritySupplier) {
		super(new RegionDirectories(tile.getLocation(), null, null, null), PRIORITY_LOW);
		this.tile = tile;
		this.uuid = uuid;
		this.callback = callback;
		this.prioritySupplier = prioritySupplier;
	}

	@Override
	public boolean execute() {
		RegionMCAFile region = new RegionMCAFile(tile.getMCAFile());
		try {
			Timer t = new Timer();
			region.loadOffsets();
			LOGGER.debug("took {} to read mca file offsets {}", t, region.getFile().getName());
		} catch (IOException e) {
			LOGGER.warn("failed to load mca file offsets {}", region.getFile().getName());
			callback.accept(null, uuid);
			return true;
		}

		ChunkSet chunks = region.offsetsToChunkSet();
		Image image = TileImage.generateImageFromChunkSet(chunks);
		callback.accept(image, uuid);
		CacheHandler.setChunks(tile.getLocation(), chunks);
		return true;
	}

	@Override
	public int getPriority() {
		if (prioritySupplier == null) {
			return super.getPriority();
		}
		return super.getBasePriority() + prioritySupplier.get();
	}

	@FunctionalInterface
	public interface HeaderImageGeneratorCallback {
		void accept(Image image, UUID world);
	}
}
