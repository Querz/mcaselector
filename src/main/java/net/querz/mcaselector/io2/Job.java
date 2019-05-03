package net.querz.mcaselector.io2;

// a job does stuff
public abstract class Job<T> implements Runnable {

	private T t;

	public Job(T t) {
		this.t = t;
	}

	public T get() {
		return t;
	}
}