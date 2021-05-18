package net.querz.mcaselector;

@FunctionalInterface
public interface ExceptionRunnable<E extends Exception> {

	void run() throws E;
}
