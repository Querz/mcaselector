package net.querz.mcaselector.io;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

class PausableThreadPoolExecutor extends ThreadPoolExecutor {

	private static final Logger LOGGER = LogManager.getLogger(PausableThreadPoolExecutor.class);

	private boolean isPaused;
	private final ReentrantLock pauseLock = new ReentrantLock();
	private final Condition unpaused = pauseLock.newCondition();
	private Consumer<Job> beforeExecute, afterExecute;

	public PausableThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
	}

	public PausableThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, Consumer<Job> beforeExecute, Consumer<Job> afterExecute) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
		this.beforeExecute = beforeExecute;
		this.afterExecute = afterExecute;
	}

	public PausableThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
	}

	public PausableThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
	}

	@Override
	protected void beforeExecute(Thread t, Runnable r) {
		super.beforeExecute(t, r);
		pauseLock.lock();
		try {
			while (isPaused) {
				unpaused.await();
			}
		} catch (InterruptedException ex) {
			t.interrupt();
		} finally {
			pauseLock.unlock();
		}
		beforeExecute.accept(((JobHandler.WrapperJob) r).job);
	}

	@Override
	protected void afterExecute(Runnable r, Throwable t) {
		super.afterExecute(r, t);
		if (t != null) {
			// mark this job as done when it threw an exception
			((JobHandler.WrapperJob) r).job.done();
		}
		afterExecute.accept(((JobHandler.WrapperJob) r).job);
	}

	public void pause(String msg) {
		pauseLock.lock();
		try {
			if (!isPaused) {
				LOGGER.debug("paused process executor: {}", msg);
			}
			isPaused = true;
		} finally {
			pauseLock.unlock();
		}
	}

	public void resume(String msg) {
		pauseLock.lock();
		try {
			if (isPaused) {
				LOGGER.debug("resumed process executor: {}", msg);
			}
			isPaused = false;
			unpaused.signalAll();
		} finally {
			pauseLock.unlock();
		}
	}
}