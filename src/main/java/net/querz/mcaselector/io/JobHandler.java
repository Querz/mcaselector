package net.querz.mcaselector.io;

import net.querz.mcaselector.config.ConfigProvider;
import net.querz.mcaselector.io.job.ParseDataJob;
import net.querz.mcaselector.io.job.ProcessDataJob;
import net.querz.mcaselector.io.job.SaveDataJob;
import net.querz.mcaselector.progress.Timer;
import net.querz.mcaselector.property.DataProperty;
import net.querz.mcaselector.validation.ShutdownHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

public final class JobHandler {

	private static final Logger LOGGER = LogManager.getLogger(JobHandler.class);

	private static PausableThreadPoolExecutor processExecutor;

	private static PausableThreadPoolExecutor saveExecutor;

	private static ThreadPoolExecutor parseExecutor;

	private static final AtomicInteger allTasks = new AtomicInteger(0);

	private static final AtomicInteger runningTasks = new AtomicInteger(0);

	private static boolean trimSaveData = true;

	public static void setTrimSaveData(boolean trimSaveData) {
		LOGGER.debug("{} trimming save data", (trimSaveData ? "enabled" : "disabled"));
		JobHandler.trimSaveData = trimSaveData;
	}

	static {
		init();
		ShutdownHooks.addShutdownHook(() -> processExecutor.shutdownNow());
		ShutdownHooks.addShutdownHook(() -> saveExecutor.shutdownNow());
		ShutdownHooks.addShutdownHook(() -> parseExecutor.shutdownNow());
	}

	public static void init() {
		// first shutdown everything if there were Threads initialized already

		clearQueues();

		if (processExecutor != null) {
			processExecutor.shutdownNow();
		}
		if (saveExecutor != null) {
			saveExecutor.shutdownNow();
		}
		if (parseExecutor != null) {
			parseExecutor.shutdownNow();
		}

		processExecutor = new PausableThreadPoolExecutor(
			ConfigProvider.GLOBAL.getProcessThreads(), ConfigProvider.GLOBAL.getProcessThreads(),
			0L, TimeUnit.MILLISECONDS,
			new DynamicPriorityBlockingQueue<>(),
			new NamedThreadFactory("processPool"),
			job -> {
				int i;
				if ((i = runningTasks.incrementAndGet()) > ConfigProvider.GLOBAL.getProcessThreads() && !trimSaveData) {
					processExecutor.pause("pausing process");
				}
				LOGGER.debug("+ active jobs: {} ({} queued)", i, processExecutor.getQueue().size());
			},
			job -> {
				if (job.isDone()) {
					int i = runningTasks.decrementAndGet();
					LOGGER.debug("- active jobs: {} ({} queued)", i, processExecutor.getQueue().size());

					processExecutor.resume("freed up a task after processing");
				}
			});

		LOGGER.debug("created data processor ThreadPoolExecutor with {} threads", ConfigProvider.GLOBAL.getProcessThreads());

		saveExecutor = new PausableThreadPoolExecutor(
			ConfigProvider.GLOBAL.getWriteThreads(), ConfigProvider.GLOBAL.getWriteThreads(),
			0L, TimeUnit.MILLISECONDS,
			new LinkedBlockingDeque<>(),
			new NamedThreadFactory("savePool"),
			job -> {
				int i = runningTasks.decrementAndGet();
				LOGGER.debug("- active jobs: {} ({} queued)", i, processExecutor.getQueue().size());
				processExecutor.resume("freed up a task after saving");
			},
			job -> {});

		LOGGER.debug("created data save ThreadPoolExecutor with {} threads", ConfigProvider.GLOBAL.getWriteThreads());

		parseExecutor = new ThreadPoolExecutor(
			1, 1,
			0L, TimeUnit.MILLISECONDS,
			new DynamicPriorityBlockingQueue<>(),
			new NamedThreadFactory("parsePool"));
		LOGGER.debug("created data parser ThreadPoolExecutor with {} threads", 1);
	}

	public static void addJob(ProcessDataJob job) {
		LOGGER.debug("adding job {} for {} to executor queue", job.getClass().getSimpleName(), job.getRegionDirectories().getLocation());
		processExecutor.execute(new WrapperJob(job));
	}

	public static void executeSaveData(SaveDataJob<?> job) {
		if (runningTasks.get() <= ConfigProvider.GLOBAL.getProcessThreads() + 1) {
			saveExecutor.execute(new WrapperJob(job));
		} else {
			if (!trimSaveData) {
				processExecutor.pause("waiting for save data");
				saveExecutor.execute(new WrapperJob(job));
			} else {
				int i;
				if ((i = runningTasks.decrementAndGet()) <= ConfigProvider.GLOBAL.getProcessThreads() + 1) {
					job.cancel();
					processExecutor.resume("skipping save data");
					LOGGER.debug("too many tasks: skipping save data");
				}

				LOGGER.debug("- active jobs: {} ({} queued)", i, processExecutor.getQueue().size());
			}
		}
	}

	public static void executeParseData(ParseDataJob job) {
		parseExecutor.execute(new WrapperJob(job));
	}

	public static void validateJobs(Predicate<ProcessDataJob> p) {
		processExecutor.getQueue().removeIf(r -> {
			if (p.test((ProcessDataJob) ((WrapperJob) r).job)) {
				((WrapperJob) r).cancel();
				return true;
			}
			return false;
		});
		parseExecutor.getQueue().removeIf(r -> {
			if (p.test((ProcessDataJob) ((WrapperJob) r).job)) {
				((WrapperJob) r).cancel();
				return true;
			}
			return false;
		});
	}

	public static void clearQueues() {
		int cancelledProcessJobs = cancelExecutorQueue(processExecutor);
		int cancelledSaveJobs = cancelExecutorQueue(saveExecutor);
		int cancelledParseJobs = cancelExecutorQueue(parseExecutor);

		LOGGER.debug("cancelled {} jobs in process queue", cancelledProcessJobs);
		LOGGER.debug("cancelled {} jobs in save queue", cancelledSaveJobs);
		LOGGER.debug("cancelled {} jobs in parser queue", cancelledParseJobs);
	}

	public static void cancelParserQueue() {
		if (parseExecutor != null) {
			synchronized (parseExecutor.getQueue()) {
				parseExecutor.getQueue().removeIf(j -> {
					((WrapperJob) j).cancel();
					return true;
				});
			}
		}
	}

	private static int cancelExecutorQueue(ThreadPoolExecutor executor) {
		DataProperty<Integer> cancelled = new DataProperty<>(0);
		if (executor != null) {
			synchronized (executor.getQueue()) {
				executor.getQueue().removeIf(j -> {
					((WrapperJob) j).cancel();
					cancelled.set(cancelled.get() + 1);
					return true;
				});
			}
		}
		return cancelled.get();
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
		LOGGER.debug("took {} to cancel and flush all executors", t);
	}

	private static void flushExecutor() {
		while (allTasks.get() > 0) {
			Thread.onSpinWait();
		}
	}

	public static int getActiveJobs() {
		return allTasks.get();
	}

	private static final AtomicLong jobIDCounter = new AtomicLong(0);

	static class WrapperJob implements Runnable, Comparable<WrapperJob> {

		Job job;
		long jobID;
		boolean done = false;
		final static Object lock = new Object();

		WrapperJob(Job job) {
			jobID = jobIDCounter.incrementAndGet();
			allTasks.incrementAndGet();
			this.job = job;
		}

		@Override
		public void run() {
			try {
				job.run();
			} finally {
				synchronized (lock) {
					if (!done) {
						allTasks.decrementAndGet();
					}
					done = true;
				}
			}
		}

		public void cancel() {
			try {
				job.cancel();
			} finally {
				synchronized (lock) {
					if (!done) {
						allTasks.decrementAndGet();
					}
					done = true;
				}
			}
		}

		@Override
		public int compareTo(WrapperJob o) {
			int a = job.getPriority();
			int b = o.job.getPriority();

			if (a == b) {
				return Long.compare(jobID, o.jobID);
			}

			return Integer.compare(a, b);
		}

		@Override
		public String toString() {
			return jobID + "#" + job.toString();
		}
	}
}
