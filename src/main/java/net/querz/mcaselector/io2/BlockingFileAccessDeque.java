package net.querz.mcaselector.io2;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

// This Queue handles the order of jobs that might try to access the same files at the same time
// by dynamically changing their execution order.
//
class BlockingFileAccessDeque extends LinkedBlockingDeque<Runnable> {

	private Set<File> blockedFiles = ConcurrentHashMap.newKeySet();
	private Map<File, Queue<Job<File>>> waitlist = new HashMap<>();

	private void addToWaitlist(Job<File> job) {
		Queue<Job<File>> waitJobs = waitlist.getOrDefault(job.get(), new LinkedBlockingQueue<>());
		waitJobs.add(job);
		waitlist.put(job.get(), waitJobs);
	}

	private Job<File> pollFromWaitlist(File file) {
		Queue<Job<File>> waitJobs = waitlist.get(file);
		if (waitJobs == null) {
			return null;
		}

		Job<File> job = waitJobs.poll();
		if (waitJobs.isEmpty()) {
			waitlist.remove(file);
		}
		return job;
	}

	class BlockingJob extends Job<File> {
		Job<File> parent;

		BlockingJob(Job<File> parent) {
			super(parent.get());
			this.parent = parent;
		}

		@Override
		public void run() {
			blockedFiles.add(get());
			try {
				parent.run();
			} finally {
				blockedFiles.remove(get());
				Job<File> waitJob = pollFromWaitlist(get());
				if (waitJob != null) {
					// add job from waitlist to front of the queue
					addFirst(waitJob);
				}
			}
		}

		// we want this wrapper to be recognized as the Job we just wrapped
		@Override
		public int hashCode() {
			return parent.hashCode();
		}

		@Override
		public boolean equals(Object other) {
			return parent.equals(other);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean add(Runnable job) {
		return super.add(new BlockingJob((Job<File>) job));
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean offer(Runnable job) {
		return super.offer(new BlockingJob((Job<File>) job));
	}

	// returns a job whose file is not marked as blocked
	@Override
	public Runnable poll() {
		Runnable queuedJob;
		while ((queuedJob = super.poll()) != null && blockedFiles.contains(((BlockingJob) queuedJob).get())) {
			addToWaitlist(((BlockingJob) queuedJob));
		}
		return queuedJob;
	}

	@Override
	public Runnable poll(long timeout, TimeUnit timeUnit) throws InterruptedException {
		Runnable queuedJob;
		while ((queuedJob = super.poll(timeout, timeUnit)) != null && blockedFiles.contains(((BlockingJob) queuedJob).get())) {
			addToWaitlist(((BlockingJob) queuedJob));
		}
		return queuedJob;
	}

	@Override
	public Runnable take() throws InterruptedException {
		Runnable queuedJob;
		while (blockedFiles.contains(((BlockingJob) (queuedJob = super.take())).get())) {
			addToWaitlist(((BlockingJob) queuedJob));
		}
		return queuedJob;
	}
}