package net.querz.mcaselector.tiles;

import net.querz.mcaselector.util.Debug;
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
	private Set<Job> inQueue = ConcurrentHashMap.newKeySet();

	public QueuedRegionImageGenerator(int maxThreads, TileMap tileMap) {
		Debug.dump("creating QueuedRegionImageGenerator with " + maxThreads + " Threads");
		this.tileMap = tileMap;
		executor = new ThreadPoolExecutor(maxThreads, maxThreads,
				0L, TimeUnit.MILLISECONDS,
				new PriorityBlockingQueue<>(10, this::compareJobs));
		Runtime.getRuntime().addShutdownHook(new Thread(() -> executor.shutdownNow()));
	}

	//removes all Points from the queue that are not in this list
	//lets already started processes finish
	//Points are region coordinates
	public void addJob(Tile tile) {
		tile.setLoading(true);
		Job job = new Job(tile);
		executor.execute(job);
		inQueue.add(job);
	}

	//tests if the Tiles in the Queue are still valid, and if they are not, removes them.
	public void validateJobs() {
		Iterator<Job> jobs = inQueue.iterator();
		while (jobs.hasNext()) {
			Job job = jobs.next();
			if (!executor.getQueue().contains(job)) {
				jobs.remove();
			} else if (!job.tile.isVisible(tileMap)) {
				boolean removed = executor.getQueue().remove(job);
				if (removed) {
					job.tile.setLoading(false);
				}
				jobs.remove();
			}
		}
	}

	private int compareJobs(Runnable a, Runnable b) {
		if (a instanceof Job && b instanceof Job) {
			return ((Job) a).highPriority ? ((Job) b).highPriority ? 0 : 1 : -1;
		}
		return 0;
	}

	private class Job implements Runnable {
		
		private Tile tile;
		private boolean highPriority = true;

		Job(Tile tile) {
			this.tile = tile;
		}

		@Override
		public void run() {
			inQueue.remove(this);
			Debug.dump("executing job for region at " + tile.getLocation());
			if (tile.isVisible(tileMap)) {
				if (highPriority) {
					tile.loadFromCache(tileMap);
					if (!tile.isLoaded()) {
						highPriority = false;
						inQueue.add(this);
						executor.execute(this);
					}
				} else {
					tile.loadImage(tileMap);
				}
			} else {
				Debug.dump("skipping tile at " + tile.getLocation() + ", not visible");
			}
		}
	}
}
