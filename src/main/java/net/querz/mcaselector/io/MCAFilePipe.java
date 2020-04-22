package net.querz.mcaselector.io;

import net.querz.mcaselector.debug.Debug;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public final class MCAFilePipe {

	public static final int DEFAULT_THREAD_COUNT = Runtime.getRuntime().availableProcessors();

	public static final int DEFAULT_MAX_LOADED_FILES = DEFAULT_THREAD_COUNT + (DEFAULT_THREAD_COUNT / 2);

	public static final int DEFAULT_MAX_WRITE_THREADS = 4;

	public static final int DEFAULT_MAX_READ_THREADS = 1;

	private static int maxLoadedFiles = DEFAULT_MAX_READ_THREADS;

	//loading mca files into memory should occur single threaded
	private static ThreadPoolExecutor loadDataExecutor;

	//calculating the image from the data should be distributed to multiple threads
	private static ThreadPoolExecutor processDataExecutor;

	//saving the cache files may take relatively long, so we do this separately but still single threaded because it's a hdd access
	private static ThreadPoolExecutor saveDataExecutor;

	private static Queue<LoadDataJob> waitingForLoad = new LinkedBlockingQueue<>();

	static {
		new MCAFilePipe();
	}

	private MCAFilePipe() {
		init(DEFAULT_MAX_READ_THREADS, DEFAULT_THREAD_COUNT, DEFAULT_MAX_WRITE_THREADS, DEFAULT_MAX_LOADED_FILES);
		Runtime.getRuntime().addShutdownHook(new Thread(() -> loadDataExecutor.shutdownNow()));
		Runtime.getRuntime().addShutdownHook(new Thread(() -> processDataExecutor.shutdownNow()));
		Runtime.getRuntime().addShutdownHook(new Thread(() -> saveDataExecutor.shutdownNow()));
	}

	public static void init(int readThreads, int processThreads, int writeThreads, int maxLoadedFiles) {
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
				readThreads, readThreads,
				0L, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<>());
		Debug.dumpf("created data load ThreadPoolExecutor with %d threads", readThreads);
		processDataExecutor = new ThreadPoolExecutor(
				processThreads, processThreads,
				0L, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<>());
		Debug.dumpf("created data processor ThreadPoolExecutor with %d threads", processThreads);
		saveDataExecutor = new ThreadPoolExecutor(
				writeThreads, writeThreads,
				0L, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<>());
		Debug.dumpf("created data save ThreadPoolExecutor with %d threads", writeThreads);
		MCAFilePipe.maxLoadedFiles = maxLoadedFiles;
	}

	static void refillDataLoadExecutorQueue() {
		//should only refill if processDataExecutor and loadDataExecutor don't have more than MAX_LOADED_FILES
		//should only refill if saveDataExecutor is not jamming the other executors
		//--> loadDataExecutor waits for processDataExecutor AND saveDataExecutor
		while (!waitingForLoad.isEmpty()
				&& processDataExecutor.getQueue().size() + loadDataExecutor.getQueue().size() < maxLoadedFiles
				&& saveDataExecutor.getQueue().size() < maxLoadedFiles) {
			LoadDataJob job = waitingForLoad.poll();
			if (job != null) {
				Debug.dumpf("refilling data load executor queue with %s", job.getFile().getAbsolutePath());
				loadDataExecutor.execute(job);
			}
		}
	}

	public static void addJob(LoadDataJob job) {
		if (processDataExecutor.getQueue().size() + loadDataExecutor.getQueue().size() > maxLoadedFiles
				|| saveDataExecutor.getQueue().size() > maxLoadedFiles) {
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
		if (waitingForLoad != null) {
			waitingForLoad.clear();
		}
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
}
