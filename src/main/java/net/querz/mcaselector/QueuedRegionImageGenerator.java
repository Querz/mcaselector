package net.querz.mcaselector;

import net.querz.mcaselector.util.Point2i;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class QueuedRegionImageGenerator {
//	private static final int maxThreads = Runtime.getRuntime().availableProcessors();
	private int maxThreads;

	private ThreadPoolExecutor executor;

	private List<Point2i> queue = new ArrayList<>();

	public QueuedRegionImageGenerator(int maxThreads) {
		this.maxThreads = maxThreads;
		executor = new ThreadPoolExecutor(maxThreads, maxThreads,
				0L, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<>());
		Runtime.getRuntime().addShutdownHook(new Thread(() -> executor.shutdownNow()));
	}

	//removes all Points from the queue that are not in this list
	//lets already started processes finish
	//Points are region coordinates
	public void syncQueue(List<Point2i> visibleRegions) {
		executor.getQueue().clear();
	}

}
