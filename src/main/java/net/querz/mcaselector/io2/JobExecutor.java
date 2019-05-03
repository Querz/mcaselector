package net.querz.mcaselector.io2;

import net.querz.mcaselector.Config;
import net.querz.mcaselector.util.Debug;
import java.io.File;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class JobExecutor {

	private static ThreadPoolExecutor loadExecutor;
	private static ThreadPoolExecutor saveExecutor;

	// put all jobs into this queue fist. loadExecutor will be refilled once saveExecutor
	private static Queue<Runnable> queuedLoadJobs = new LinkedBlockingQueue<>();

	private static int maxLoadedFiles = 50;

	static {
		new JobExecutor();
	}

	private JobExecutor() {
		init(Config.getLoadThreads(), Config.getWriteThreads());
		Runtime.getRuntime().addShutdownHook(new Thread(() -> loadExecutor.shutdownNow()));
		Runtime.getRuntime().addShutdownHook(new Thread(() -> saveExecutor.shutdownNow()));
	}

	public static void init(int loadThreads, int saveThreads) {
		if (loadExecutor != null) {
			loadExecutor.shutdownNow();
		}

		if (saveExecutor != null) {
			saveExecutor.shutdownNow();
		}

		loadExecutor = new ThreadPoolExecutor(
				loadThreads, loadThreads,
				0L, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<>() // we only ever read from one file at a time
		);
		Debug.dumpf("created load ThreadPoolExecutor with %d threads", loadThreads);

		saveExecutor = new ThreadPoolExecutor(
				saveThreads, saveThreads,
				0L, TimeUnit.MILLISECONDS,
				new BlockingFileAccessDeque()
		);
		Debug.dumpf("created save ThreadPoolExecutor with %d threads", saveThreads);
	}

	public static void addLoadDataJob(LoadDataJob loadDataJob) {
		queuedLoadJobs.add(loadDataJob);
		refillLoadExecutor();
	}

	public static int getCurrentlyLoadedFiles() {
		return Config.getLoadThreads() +
				Config.getWriteThreads() +
				loadExecutor
						.getQueue()
						.size() +
				saveExecutor
						.getQueue()
						.size();
	}

	public static void addSaveDataJob(SaveDataJob<?> saveDataJob) {
		System.out.println("adding SaveDataJob for file: " + saveDataJob.get());
		saveExecutor.execute(new Job<File>(saveDataJob.get()){
			public void run() {
				saveDataJob.execute();
				refillLoadExecutor();
			}
		});
	}

	private static void refillLoadExecutor() {
		int currentlyLoadedFiles = getCurrentlyLoadedFiles();
		while (currentlyLoadedFiles < maxLoadedFiles && !queuedLoadJobs.isEmpty()) {
			loadExecutor.execute(queuedLoadJobs.poll());
			currentlyLoadedFiles++;
		}
	}
}