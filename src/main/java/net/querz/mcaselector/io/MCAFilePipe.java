package net.querz.mcaselector.io;

import net.querz.mcaselector.Config;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.io.job.LoadDataJob;
import net.querz.mcaselector.io.job.ParseDataJob;
import net.querz.mcaselector.io.job.ProcessDataJob;
import net.querz.mcaselector.io.job.SaveDataJob;
import net.querz.mcaselector.progress.Timer;
import net.querz.mcaselector.validation.ShutdownHooks;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
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


	private static final AtomicInteger allTasks = new AtomicInteger(0);

	static {
		init();
		ShutdownHooks.addShutdownHook(() -> loadDataExecutor.shutdownNow());
		ShutdownHooks.addShutdownHook(() -> processDataExecutor.shutdownNow());
		ShutdownHooks.addShutdownHook(() -> saveDataExecutor.shutdownNow());
		ShutdownHooks.addShutdownHook(() -> dataParsingExecutor.shutdownNow());
	}

	private static class WrapperJob implements Runnable {

		Job job;
		final AtomicBoolean isDone = new AtomicBoolean(false);

		WrapperJob(Job job) {
			allTasks.incrementAndGet();
			this.job = job;
		}

		@Override
		public void run() {
			try {
				job.run();
			} finally {
				synchronized (isDone) {
					if (!isDone.get()) {
						allTasks.decrementAndGet();
					}
					isDone.set(true);
				}
			}
		}

		public void cancel() {
			try {
				job.cancel();
			} finally {
				synchronized (isDone) {
					if (!isDone.get()) {
						allTasks.decrementAndGet();
					}
					isDone.set(true);
				}
			}
		}
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
				new LinkedBlockingDeque<>(),
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
		// --> loadDataExecutor waits for processDataExecutor AND saveDataExecutor
		trimSaveDataQueue();

		while (!waitingForLoad.isEmpty()
				&& processDataExecutor.getQueue().size() + loadDataExecutor.getQueue().size() < Config.getMaxLoadedFiles()
				&& saveDataExecutor.getQueue().size() < Config.getMaxLoadedFiles()) {
			LoadDataJob job = waitingForLoad.poll();
			if (job != null) {
				Debug.dumpf("refilling data load executor queue with %s", job.getRegionDirectories().getLocationAsFileName());
				loadDataExecutor.execute(new WrapperJob(job));
			}
		}
	}

	private static void trimSaveDataQueue() {
		// caching is not a priority over processing, so we just skip caching if caching is the bottleneck
		if (saveDataExecutor.getQueue().size() > Config.getMaxLoadedFiles()) {
			@SuppressWarnings({"unchecked", "rawtypes"})
			LinkedBlockingDeque<WrapperJob> queue = (LinkedBlockingDeque) saveDataExecutor.getQueue();
			while (queue.size() > Config.getMaxLoadedFiles()) {
				WrapperJob job = queue.pollLast();
				if (job != null) {
					SaveDataJob<?> saveDataJob = (SaveDataJob<?>) job.job;
					if (saveDataJob.canSkip()) {
						job.cancel();
						RegionDirectories rd = saveDataJob.getRegionDirectories();
						Debug.dumpf("skipped SaveDataJob for " + (rd == null ? "null" : rd.getLocation()));
					} else {
						break;
					}
				} else {
					break;
				}
			}
		}
	}

	public static void addJob(LoadDataJob job) {
		trimSaveDataQueue();

		if (processDataExecutor.getQueue().size() + loadDataExecutor.getQueue().size() > Config.getMaxLoadedFiles()
				|| saveDataExecutor.getQueue().size() > Config.getMaxLoadedFiles()) {
			Debug.dumpf("adding LoadDataJob %s for %s to wait queue", job.getClass().getSimpleName(), job.getRegionDirectories().getLocationAsFileName());
			waitingForLoad.offer(job);
		} else {
			Debug.dumpf("adding LoadDataJob %s for %s to executor queue", job.getClass().getSimpleName(), job.getRegionDirectories().getLocationAsFileName());
			loadDataExecutor.execute(new WrapperJob(job));
		}
	}

	public static void executeProcessData(ProcessDataJob job) {
		processDataExecutor.execute(new WrapperJob(job));
	}

	public static void executeSaveData(SaveDataJob<?> job) {
		saveDataExecutor.execute(new WrapperJob(job));
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

		cancelExecutorQueue(loadDataExecutor);
		cancelExecutorQueue(processDataExecutor);
		cancelExecutorQueue(saveDataExecutor);
	}

	private static void cancelExecutorQueue(ThreadPoolExecutor executor) {
		if (executor != null) {
			synchronized (executor.getQueue()) {
				executor.getQueue().removeIf(j -> {
					((WrapperJob) j).cancel();
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

	public static void cancelAllJobsAndFlushAsync(Runnable callback) {
		Thread thread = new Thread(() -> {
			cancelAllJobsAndFlush();
			callback.run();
		});
		thread.start();
	}

	public static void cancelAllJobsAndFlush() {
		Timer t = new Timer();
		clearQueues();
		flushExecutor();
		clearQueues();
		flushExecutor();
		clearQueues();
		flushExecutor();
		Debug.dumpf("took %s to cancel and flush all executors", t);
	}

	private static void flushExecutor() {
		//noinspection StatementWithEmptyBody
		while (allTasks.get() > 0);
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
