package net.querz.mcaselector.validation;

import java.util.PriorityQueue;

/*
* collects and handles shutdown hooks with priorities.
* the lower the priority number, the later the task is executed,
* e.g. priority 5 will be executed after priority 7 (like a count down)
* */
public final class ShutdownHooks {

	private static final PriorityQueue<ShutdownJob> hooks = new PriorityQueue<>();

	static {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			for (ShutdownJob hook : hooks) {
				hook.run();
			}
		}));
	}

	public static ShutdownJob addShutdownHook(Runnable r) {
		return addShutdownHook(r, Integer.MAX_VALUE);
	}

	public static ShutdownJob addShutdownHook(Runnable r, int priority) {
		ShutdownJob j = new ShutdownJob(r, priority);
		hooks.offer(j);
		return j;
	}

	public static boolean removeShutdownHook(ShutdownJob j) {
		return hooks.remove(j);
	}

	public static class ShutdownJob implements Runnable, Comparable<ShutdownJob> {

		private final int priority;
		private final Runnable runnable;

		private ShutdownJob(Runnable runnable, int priority) {
			this.runnable = runnable;
			this.priority = priority;
		}

		@Override
		public int compareTo(ShutdownJob o) {
			return Integer.compare(o.priority, priority);
		}

		@Override
		public void run() {
			runnable.run();
		}
	}
}
