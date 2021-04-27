package net.querz.mcaselector.io;

import net.querz.mcaselector.Config;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.io.job.LoadDataJob;
import net.querz.mcaselector.io.job.ParseDataJob;
import net.querz.mcaselector.io.job.ProcessDataJob;
import net.querz.mcaselector.io.job.SaveDataJob;
import net.querz.mcaselector.validation.ShutdownHooks;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public final class MCAFilePipe {

	// loading mca files into memory should occur single threaded
	private static ThreadPoolExecutor loadDataExecutor;

	// calculating the image from the data should be distributed to multiple threads
	private static ThreadPoolExecutor processDataExecutor;

	// saving the cache files may take relatively long, so we do this separately but still single threaded because it's a hdd access
	private static ThreadPoolExecutor saveDataExecutor;

	// a separate thread pool to parse data independently from the other thread pools
	private static ThreadPoolExecutor dataParsingExecutor;

	private static final Queue<LoadDataJob> waitingForLoad = new LinkedBlockingQueue<>();

	static {
		init();
		ShutdownHooks.addShutdownHook(() -> loadDataExecutor.shutdownNow());
		ShutdownHooks.addShutdownHook(() -> processDataExecutor.shutdownNow());
		ShutdownHooks.addShutdownHook(() -> saveDataExecutor.shutdownNow());
		ShutdownHooks.addShutdownHook(() -> dataParsingExecutor.shutdownNow());
	}

	public static void init() {
		// first shutdown everything if there were Threads initialized already
		clearQueues();
		if (loadDataExecutor != null) {
			loadDataExecutor.shutdownNow();
		}
		if (processDataExecutor != null) {
			processDataExecutor.shutdownNow();
		}
		if (saveDataExecutor != null) {
			saveDataExecutor.shutdownNow();
		}
		if (dataParsingExecutor != null) {
			dataParsingExecutor.shutdownNow();
		}
		loadDataExecutor = new ThreadPoolExecutor(
				Config.getLoadThreads(), Config.getLoadThreads(),
				0L, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<>(),
				new NamedThreadFactory("loadPool"));
		Debug.dumpf("created data load ThreadPoolExecutor with %d threads", Config.getLoadThreads());
		processDataExecutor = new ThreadPoolExecutor(
				Config.getProcessThreads(), Config.getProcessThreads(),
				0L, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<>(),
				new NamedThreadFactory("processPool"));
		Debug.dumpf("created data processor ThreadPoolExecutor with %d threads", Config.getProcessThreads());
		saveDataExecutor = new ThreadPoolExecutor(
				Config.getWriteThreads(), Config.getWriteThreads(),
				0L, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<>(),
				new NamedThreadFactory("savePool"));
		Debug.dumpf("created data save ThreadPoolExecutor with %d threads", Config.getWriteThreads());
		dataParsingExecutor = new ThreadPoolExecutor(
				1, 1,
				0L, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<>(),
				new NamedThreadFactory("parsePool"));
		Debug.dumpf("created data parser ThreadPoolExecutor with %d threads", 1);
	}

	public static void refillDataLoadExecutorQueue() {
		// should only refill if processDataExecutor and loadDataExecutor don't have more than MAX_LOADED_FILES
		// should only refill if saveDataExecutor is not jamming the other executors
		// --> loadDataExecutor waits for processDataExecutor AND saveDataExecutor
		while (!waitingForLoad.isEmpty()
				&& processDataExecutor.getQueue().size() + loadDataExecutor.getQueue().size() < Config.getMaxLoadedFiles()
				&& saveDataExecutor.getQueue().size() < Config.getMaxLoadedFiles()) {
			LoadDataJob job = waitingForLoad.poll();
			if (job != null) {
				Debug.dumpf("refilling data load executor queue with %s", job.getRegionDirectories().getLocationAsFileName());
				loadDataExecutor.execute(job);
			}
		}
	}

	public static void addJob(LoadDataJob job) {
		if (processDataExecutor.getQueue().size() + loadDataExecutor.getQueue().size() > Config.getMaxLoadedFiles()
				|| saveDataExecutor.getQueue().size() > Config.getMaxLoadedFiles()) {
			Debug.dumpf("adding LoadDataJob %s for %s to wait queue", job.getClass().getSimpleName(), job.getRegionDirectories().getLocationAsFileName());
			waitingForLoad.offer(job);
		} else {
			Debug.dumpf("adding LoadDataJob %s for %s to executor queue", job.getClass().getSimpleName(), job.getRegionDirectories().getLocationAsFileName());
			loadDataExecutor.execute(job);
		}
	}

	public static void executeProcessData(ProcessDataJob job) {
		processDataExecutor.execute(job);
	}

	public static void executeSaveData(SaveDataJob<?> job) {
		saveDataExecutor.execute(job);
	}

	public static void executeParseData(ParseDataJob job) {
		dataParsingExecutor.execute(job);
	}

	public static void validateJobs(Predicate<LoadDataJob> p) {
		waitingForLoad.removeIf(r -> {
			if (p.test(r)) {
				r.cancel();
				return true;
			}
			return false;
		});
		dataParsingExecutor.getQueue().removeIf(r -> {
			if (p.test((LoadDataJob) r)) {
				((Job) r).cancel();
				return true;
			}
			return false;
		});
	}

	public static void clearQueues() {
		synchronized (waitingForLoad) {
			waitingForLoad.removeIf(j -> {
				j.cancel();
				return true;
			});
		}
		if (loadDataExecutor != null) {
			synchronized (loadDataExecutor.getQueue()) {
				loadDataExecutor.getQueue().removeIf(j -> {
					((Job) j).cancel();
					return true;
				});
			}
		}
		if (processDataExecutor != null) {
			synchronized (processDataExecutor.getQueue()) {
				processDataExecutor.getQueue().removeIf(j -> {
					((Job) j).cancel();
					return true;
				});
			}
		}
		if (saveDataExecutor != null) {
			synchronized (saveDataExecutor.getQueue()) {
				saveDataExecutor.getQueue().removeIf(j -> {
					((Job) j).cancel();
					return true;
				});
			}
		}
	}

	public static void clearParserQueue() {
		if (dataParsingExecutor != null) {
			synchronized (dataParsingExecutor.getQueue()) {
				dataParsingExecutor.getQueue().removeIf(j -> {
					((Job) j).cancel();
					return true;
				});
			}
		}
	}

	public static void cancelAllJobs(Runnable callback) {
		clearQueues();
		Thread thread = new Thread(() -> {
			for (;;) {
				if (loadDataExecutor.getActiveCount() == 0
						&& processDataExecutor.getActiveCount() == 0
						&& saveDataExecutor.getActiveCount() == 0) {
					break;
				}
			}
			callback.run();
		});
		thread.start();
	}

	public static int getActiveJobs() {
		return waitingForLoad.size() +
			loadDataExecutor.getQueue().size() +
			loadDataExecutor.getActiveCount() +
			processDataExecutor.getQueue().size() +
			processDataExecutor.getActiveCount() +
			saveDataExecutor.getQueue().size() +
			saveDataExecutor.getActiveCount();
	}
}
