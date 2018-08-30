package net.querz.mcaselector.io;


import net.querz.mcaselector.util.Debug;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * This will be an abstract version of the QueuedRegionImageGenerator so it can be used
 * for more stuff than just creating images.
 *
 * It has a thread to read files, a threadpool to work with the files and a thread
 * to write the files back to disk.
 *
 * Writing can also be skipped if it is not needed, as we can create our own worker job.
 *
 * */
public final class MCAFilePipe {

	public static final int THREAD_COUNT = Runtime.getRuntime().availableProcessors();
//	public static final int THREAD_COUNT = 2;

	public static final int MAX_LOADED_FILES = THREAD_COUNT + (THREAD_COUNT / 2);

	public static final int MAX_WRITE_THREADS = 4;

	public static final int MAX_READ_THREADS = 1;

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
		loadDataExecutor = new ThreadPoolExecutor(
				MAX_READ_THREADS, MAX_READ_THREADS,
				0L, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<>());
		Debug.dump("created data load ThreadPoolExecutor");
		processDataExecutor = new ThreadPoolExecutor(
				THREAD_COUNT, THREAD_COUNT,
				0L, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<>());
		Debug.dumpf("created image creation ThreadPoolExecutor with %d threads", THREAD_COUNT);
		saveDataExecutor = new ThreadPoolExecutor(
				MAX_WRITE_THREADS, MAX_WRITE_THREADS,
				0L, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<>());
		Debug.dump("created image cache ThreadPoolExecutor");
		Runtime.getRuntime().addShutdownHook(new Thread(() -> loadDataExecutor.shutdownNow()));
		Runtime.getRuntime().addShutdownHook(new Thread(() -> processDataExecutor.shutdownNow()));
		Runtime.getRuntime().addShutdownHook(new Thread(() -> saveDataExecutor.shutdownNow()));
	}

	static void refillDataLoadExecutorQueue() {
		//should only refill if processDataExecutor and loadDataExecutor don't have more than MAX_LOADED_FILES
		//should only refill if saveDataExecutor is not jamming the other executors
		//--> loadDataExecutor waits for processDataExecutor AND saveDataExecutor
		while (!waitingForLoad.isEmpty()
				&& processDataExecutor.getQueue().size() + loadDataExecutor.getQueue().size() < MAX_LOADED_FILES
				&& saveDataExecutor.getQueue().size() < MAX_LOADED_FILES) {
			LoadDataJob job = waitingForLoad.poll();
			if (job != null) {
				Debug.dumpf("refilling data load executor queue with %s", job.getFile().getAbsolutePath());
				loadDataExecutor.execute(job);
			}
		}
	}

	public static void addJob(LoadDataJob job) {
		if (processDataExecutor.getQueue().size() + loadDataExecutor.getQueue().size() > MAX_LOADED_FILES
				|| saveDataExecutor.getQueue().size() > MAX_LOADED_FILES) {
			Debug.dumpf("adding DataLoadJob for %s to wait queue", job.getFile().getName());
			waitingForLoad.offer(job);
		} else {
			Debug.dumpf("adding DataLoadJob for %s to executor queue", job.getFile().getName());
			loadDataExecutor.execute(job);
		}
	}

	static void executeLoadData(LoadDataJob job) {
		loadDataExecutor.execute(job);
	}

	static void executeProcessData(ProcessDataJob job) {
		processDataExecutor.execute(job);
	}

	static void executeSaveData(SaveDataJob<?> job) {
		saveDataExecutor.execute(job);
	}

	public static void validateLoadDataJobs(Predicate<Runnable> p) {
		loadDataExecutor.getQueue().removeIf(p);
	}

	public static void validateProcessDataJobs(Predicate<Runnable> p) {
		processDataExecutor.getQueue().removeIf(p);
	}

	public static void validateSaveDataJobs(Predicate<Runnable> p) {
		saveDataExecutor.getQueue().removeIf(p);
	}
}
