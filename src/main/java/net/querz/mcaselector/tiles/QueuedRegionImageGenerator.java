package net.querz.mcaselector.tiles;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import net.querz.mcaselector.util.Debug;
import net.querz.mcaselector.util.Timer;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * This loads files one after the other in one Thread and creates the image in others.
 * Loading lots of files at the same time significantly slows down loading times, so we
 * load mca files and save cache files single threaded.
 * Therefore we can load the entire file into memory before accessing its data to minimize the time
 * the program needs access to the file on the hard drive.
 *
 * Cache files are loaded directly, because loading them is pretty fast.
 *
 * The ThreadPoolExecutor that is responsible for creating the images also limits the amount of files
 * that can be loaded into memory. There can ever be a maximum of the amount of processor cores +50%
 * files loaded into memory before they are processed
 * */
public class QueuedRegionImageGenerator {

	public static final int THREAD_COUNT = Runtime.getRuntime().availableProcessors();
//	public static final int THREAD_COUNT = 2;

	public static final int MAX_LOADED_FILES = THREAD_COUNT + (THREAD_COUNT / 2);

	//loading mca files into memory should occur single threaded
	private ThreadPoolExecutor dataLoadExecutor;

	//calculating the image from the data should be distributed to multiple threads
	private ThreadPoolExecutor createImageExecutor;

	//saving the cache files may take relatively long, so we do this separately but still single threaded because it's a hdd access
	private ThreadPoolExecutor cacheImageExecutor;

	private Queue<DataLoadJob> waitingForLoad = new LinkedBlockingQueue<>();

	private TileMap tileMap;

	public QueuedRegionImageGenerator(TileMap tileMap) {
		this.tileMap = tileMap;
		dataLoadExecutor = new ThreadPoolExecutor(
				1, 1,
				0L, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<>());
		Debug.dump("created data load ThreadPoolExecutor");
		createImageExecutor = new ThreadPoolExecutor(
				THREAD_COUNT, THREAD_COUNT,
				0L, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<>());
		Debug.dumpf("created image creation ThreadPoolExecutor with %d threads", THREAD_COUNT);
		cacheImageExecutor = new ThreadPoolExecutor(
				1, 1,
				0L, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<>());
		Debug.dump("created image cache ThreadPoolExecutor");
		Runtime.getRuntime().addShutdownHook(new Thread(() -> createImageExecutor.shutdownNow()));
		Runtime.getRuntime().addShutdownHook(new Thread(() -> dataLoadExecutor.shutdownNow()));
		Runtime.getRuntime().addShutdownHook(new Thread(() -> cacheImageExecutor.shutdownNow()));
	}

	public void addJob(Tile tile) {
		tile.setLoading(true);
		DataLoadJob job = new DataLoadJob(tile);
		if (createImageExecutor.getQueue().size() + dataLoadExecutor.getQueue().size() > MAX_LOADED_FILES) {
			Debug.dumpf("adding DataLoadJob for %s to wait queue", tile.getLocation());
			waitingForLoad.offer(job);
		} else {
			Debug.dumpf("adding DataLoadJob for %s to executor queue", tile.getLocation());
			dataLoadExecutor.execute(job);
		}
	}

	//tests if the Tiles in the Queue are still valid, and if they are not, removes them.
	public void validateJobs() {
		dataLoadExecutor.getQueue().removeIf(this::notVisible);
		createImageExecutor.getQueue().removeIf(this::notVisible);
		//not removing jobs from the pool that saves cache files
	}

	private boolean notVisible(Runnable r) {
		if (!((Job) r).tile.isVisible(tileMap)) {
			Debug.dumpf("removing %s for tile %s from queue", r.getClass().getSimpleName(), ((Job) r).tile.getLocation());
			return true;
		}
		return false;
	}

	private void refillDataLoadExecutorQueue() {
		while (!waitingForLoad.isEmpty() && createImageExecutor.getQueue().size() + dataLoadExecutor.getQueue().size() < MAX_LOADED_FILES) {
			DataLoadJob job = waitingForLoad.poll();
			if (job != null) {
				Debug.dumpf("refilling data load executor queue with %s", job.tile.getLocation());
				dataLoadExecutor.execute(job);
			}
		}
	}

	private abstract class Job implements Runnable {

		Tile tile;

		Job(Tile tile) {
			this.tile = tile;
		}
	}

	private class DataLoadJob extends Job {

		DataLoadJob(Tile tile) {
			super(tile);
		}

		@Override
		public void run() {
			refillDataLoadExecutorQueue();
			//try to load cached file first
			tile.loadFromCache(tileMap);

			if (!tile.isLoaded()) {
				tile.setLoading(true);

				Timer t = new Timer();

				File file = tile.getMCAFile();
				long length = file.length();

				if (length > 0) {
					int read;
					byte[] data = new byte[(int) length];
					try (FileInputStream fis = new FileInputStream(file)) {
						read = fis.read(data);
					} catch (IOException ex) {
						ex.printStackTrace();
						return;
					}
					Debug.dumpf("read %d bytes from %s in %s", read, file.getAbsolutePath(), t);

					createImageExecutor.execute(new CreateImageJob(tile, data));
					return;
				}
			}
		}
	}

	private class CreateImageJob extends Job {

		byte[] rawData;

		CreateImageJob(Tile tile, byte[] rawData) {
			super(tile);
			this.rawData = rawData;
		}

		@Override
		public void run() {
			refillDataLoadExecutorQueue();

			//creates image from raw data
			Image image = tile.generateImage(tileMap, rawData);
			if (image != null) {
				cacheImageExecutor.execute(new CacheImageJob(tile, image));
			}
		}
	}

	private class CacheImageJob extends Job {

		Image image;

		CacheImageJob(Tile tile, Image image) {
			super(tile);
			this.image = image;
		}

		@Override
		public void run() {
			Timer t = new Timer();

			//save image to cache
			File cacheFile = tile.getCacheFile();
			if (!cacheFile.getParentFile().exists() && !cacheFile.getParentFile().mkdirs()) {
				Debug.errorf("failed to create cache directory for %s", cacheFile.getAbsolutePath());
			}
			try {
				ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", cacheFile);
			} catch (IOException e) {
				e.printStackTrace();
			}

			Debug.dumpf("took %s to cache image of %s to %s", t, tile.getMCAFile().getName(), cacheFile.getName());
		}
	}
}
