package net.querz.mcaselector.exception;

@FunctionalInterface
public interface ExceptionConsumer<T, E extends Exception> {

	void accept(T t) throws E;
}
