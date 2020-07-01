package net.querz.mcaselector.io;

import net.querz.mcaselector.Config;
import net.querz.mcaselector.debug.Debug;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public final class MCAFilePipe {

	//loading mca files into memory should occur single threaded
	private static ThreadPoolExecutor loadDataExecutor;

	//calculating the image from the data should be distributed to multiple threads
	private static ThreadPoolExecutor processDataExecutor;

	//saving the cache files may take relatively long, so we do this separately but still single threaded because it's a hdd access
	private static ThreadPoolExecutor saveDataExecutor;

	private static final Queue<LoadDataJob> waitingForLoad = new LinkedBlockingQueue<>();

	static {
		init();
		Runtime.getRuntime().addShutdownHook(new Thread(() -> loadDataExecutor.shutdownNow()));
		Runtime.getRuntime().addShutdownHook(new Thread(() -> processDataExecutor.shutdownNow()));
		Runtime.getRuntime().addShutdownHook(new Thread(() -> saveDataExecutor.shutdownNow()));
	}

	public static void init() {
		//first shutdown everything if there were Threads initialized already
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
		loadDataExecutor = new ThreadPoolExecutor(
				Config.getLoadThreads(), Config.getLoadThreads(),
				0L, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<>());
		Debug.dumpf("created data load ThreadPoolExecutor with %d threads", Config.getLoadThreads());
		processDataExecutor = new ThreadPoolExecutor(
				Config.getProcessThreads(), Config.getProcessThreads(),
				0L, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<>());
		Debug.dumpf("created data processor ThreadPoolExecutor with %d threads", Config.getProcessThreads());
		saveDataExecutor = new ThreadPoolExecutor(
				Config.getWriteThreads(), Config.getWriteThreads(),
				0L, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<>());
		Debug.dumpf("created data save ThreadPoolExecutor with %d threads", Config.getWriteThreads());
	}

	static void refillDataLoadExecutorQueue() {
		//should only refill if processDataExecutor and loadDataExecutor don't have more than MAX_LOADED_FILES
		//should only refill if saveDataExecutor is not jamming the other executors
		//--> loadDataExecutor waits for processDataExecutor AND saveDataExecutor
		while (!waitingForLoad.isEmpty()
				&& processDataExecutor.getQueue().size() + loadDataExecutor.getQueue().size() < Config.getMaxLoadedFiles()
				&& saveDataExecutor.getQueue().size() < Config.getMaxLoadedFiles()) {
			LoadDataJob job = waitingForLoad.poll();
			if (job != null) {
				Debug.dumpf("refilling data load executor queue with %s", job.getFile().getAbsolutePath());
				loadDataExecutor.execute(job);
			}
		}
	}

	public static void addJob(LoadDataJob job) {
		if (processDataExecutor.getQueue().size() + loadDataExecutor.getQueue().size() > Config.getMaxLoadedFiles()
				|| saveDataExecutor.getQueue().size() > Config.getMaxLoadedFiles()) {
			Debug.dumpf("adding LoadDataJob %s for %s to wait queue", job.getClass().getSimpleName(), job.getFile().getName());
			waitingForLoad.offer(job);
		} else {
			Debug.dumpf("adding LoadDataJob %s for %s to executor queue", job.getClass().getSimpleName(), job.getFile().getName());
			loadDataExecutor.execute(job);
		}
	}

	static void executeProcessData(ProcessDataJob job) {
		processDataExecutor.execute(job);
	}

	static void executeSaveData(SaveDataJob<?> job) {
		saveDataExecutor.execute(job);
	}

	public static void validateJobs(Predicate<LoadDataJob> p) {
		waitingForLoad.removeIf(p);
	}

	public static void clearQueues() {
		waitingForLoad.clear();
		if (loadDataExecutor != null) {
			loadDataExecutor.getQueue().clear();
		}
		if (processDataExecutor != null) {
			processDataExecutor.getQueue().clear();
		}
		if (saveDataExecutor != null) {
			saveDataExecutor.getQueue().clear();
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
