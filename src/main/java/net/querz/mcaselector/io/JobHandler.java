package net.querz.mcaselector.io;

import net.querz.mcaselector.Config;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.io.job.ParseDataJob;
import net.querz.mcaselector.io.job.ProcessDataJob;
import net.querz.mcaselector.io.job.SaveDataJob;
import net.querz.mcaselector.progress.Timer;
import net.querz.mcaselector.validation.ShutdownHooks;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

public final class JobHandler {

	private static PausableThreadPoolExecutor processExecutor;

	private static PausableThreadPoolExecutor saveExecutor;

	private static ThreadPoolExecutor parseExecutor;

	private static final AtomicInteger allTasks = new AtomicInteger(0);

	private static final AtomicInteger runningTasks = new AtomicInteger(0);

	private static boolean trimSaveData = true;

	public static void setTrimSaveData(boolean trimSaveData) {
		Debug.dump((trimSaveData ? "enabled" : "disabled") + " trimming save data");
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
			Config.getProcessThreads(), Config.getProcessThreads(),
			0L, TimeUnit.MILLISECONDS,
			new LinkedBlockingQueue<>(),
			new NamedThreadFactory("processPool"),
			job -> {
				int i;
				if ((i = runningTasks.incrementAndGet()) > Config.getProcessThreads() && !trimSaveData) {
					processExecutor.pause("pausing process");
				}
				Debug.dumpf("+ active jobs: %d (%d queued)", i, processExecutor.getQueue().size());
			},
			job -> {
				if (job.isDone()) {
					int i = runningTasks.decrementAndGet();
					Debug.dumpf("- active jobs: %d (%d queued)", i, processExecutor.getQueue().size());

					processExecutor.resume("freed up a task after processing");
				}
			});

		Debug.dumpf("created data processor ThreadPoolExecutor with %d threads", Config.getProcessThreads());

		saveExecutor = new PausableThreadPoolExecutor(
			Config.getWriteThreads(), Config.getWriteThreads(),
			0L, TimeUnit.MILLISECONDS,
			new LinkedBlockingDeque<>(),
			new NamedThreadFactory("savePool"),
			job -> {
				int i = runningTasks.decrementAndGet();
				Debug.dumpf("- active jobs: %d (%d queued)", i, processExecutor.getQueue().size());
				processExecutor.resume("freed up a task after saving");
			},
			job -> {});

		Debug.dumpf("created data save ThreadPoolExecutor with %d threads", Config.getWriteThreads());

		parseExecutor = new ThreadPoolExecutor(
			1, 1,
			0L, TimeUnit.MILLISECONDS,
			new LinkedBlockingQueue<>(),
			new NamedThreadFactory("parsePool"));
		Debug.dumpf("created data parser ThreadPoolExecutor with %d threads", 1);
	}

	public static void addJob(ProcessDataJob job) {
		Debug.dumpf("adding job %s for %s to executor queue", job.getClass().getSimpleName(), job.getRegionDirectories().getLocationAsFileName());
		processExecutor.execute(new WrapperJob(job));
	}

	public static void executeSaveData(SaveDataJob<?> job) {
		if (runningTasks.get() <= Config.getProcessThreads() + 1) {
			saveExecutor.execute(new WrapperJob(job));
		} else {
			if (!trimSaveData) {
				processExecutor.pause("waiting for save data");
				saveExecutor.execute(new WrapperJob(job));
			} else {
				int i;
				if ((i = runningTasks.decrementAndGet()) <= Config.getProcessThreads() + 1) {
					job.cancel();
					processExecutor.resume("skipping save data");
					Debug.dumpf("too many tasks: skipping save data");
				}

				Debug.dumpf("- active jobs: %d (%d queued)", i, processExecutor.getQueue().size());
			}
		}
	}

	public static void executeParseData(ParseDataJob job) {
		parseExecutor.execute(job);
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
			if (p.test((ProcessDataJob) r)) {
				((Job) r).cancel();
				return true;
			}
			return false;
		});
	}

	public static void clearQueues() {
		cancelExecutorQueue(processExecutor);
		cancelExecutorQueue(saveExecutor);
		cancelExecutorQueue(parseExecutor);
	}

	public static void cancelParserQueue() {
		if (parseExecutor != null) {
			synchronized (parseExecutor.getQueue()) {
				parseExecutor.getQueue().removeIf(j -> {
					((Job) j).cancel();
					return true;
				});
			}
		}
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
		Debug.dumpf("took %s to cancel and flush all executors", t);
	}

	private static void flushExecutor() {
		//noinspection StatementWithEmptyBody
		while (allTasks.get() > 0);
	}

	public static int getActiveJobs() {
		return processExecutor.getQueue().size() +
			processExecutor.getActiveCount() +
			saveExecutor.getQueue().size() +
			saveExecutor.getActiveCount();
	}

	static class WrapperJob implements Runnable {

		Job job;
		boolean done = false;
		final static Object lock = new Object();

		WrapperJob(Job job) {
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
	}
}
