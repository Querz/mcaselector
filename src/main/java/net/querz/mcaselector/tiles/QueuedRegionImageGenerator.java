package net.querz.mcaselector.tiles;

import java.util.*;
import java.util.concurrent.*;

/*
* every time the TileMap is moved, it will look for unloaded tiles.
* If the tile image is cached, it will load it directly, otherwise it will
* send a job to the QueuedRegionImageGenerator, which will then queue these jobs
* and execute them asynchronously.
*
* Whenever the TileMap moves, it will also tell the QueuedRegionImageGenerator
* which tiles are currently displayed and the QueuedRegionImageGenerator will then
* remove all queued jobs that would result in an image that is not displayed anymore.
* */
public class QueuedRegionImageGenerator {
	public static final int PROCESSOR_COUNT = Runtime.getRuntime().availableProcessors();

	private TileMap tileMap;
	private ThreadPoolExecutor executor;
	private List<Job> inQueue = new ArrayList<>();

	public QueuedRegionImageGenerator(int maxThreads, TileMap tileMap) {
		this.tileMap = tileMap;
		executor = new ThreadPoolExecutor(maxThreads, maxThreads,
				0L, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<>());
		Runtime.getRuntime().addShutdownHook(new Thread(() -> executor.shutdownNow()));
	}

	//removes all Points from the queue that are not in this list
	//lets already started processes finish
	//Points are region coordinates
	public void addJob(Tile tile) {
		tile.loading = true;
		Job job = new Job(tile);
		executor.execute(job);
		inQueue.add(job);
	}

	//tests if the Tiles in the Queue are still valid, and if they are not, removes them.
	public void validateJobs() {
		for (int i = 0; i < inQueue.size(); i++) {
			Job job = inQueue.get(i);
			if (!executor.getQueue().contains(job)) {
				inQueue.remove(i);
				i--;
			} else if (!job.tile.isVisible(tileMap)) {
				boolean removed = executor.getQueue().remove(job);
				if (removed) {
					job.tile.loading = false;
				}
				inQueue.remove(i);
				i--;
			}
		}
	}

	private class Job implements Runnable {
		private Tile tile;

		Job(Tile tile) {
			this.tile = tile;
		}

		@Override
		public void run() {
			System.out.println("executing job for tile " + tile.getLocation());
			if (tile.isVisible(tileMap)) {
				tile.loadImage(tileMap);
			} else {
				System.out.println("tile at " + tile.getLocation() + " not visible, skipping");
			}
		}
	}

}
