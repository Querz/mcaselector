package net.querz.mcaselector.headless;

@FunctionalInterface
public interface ExceptionConsumer<T, E extends Exception> {

	void accept(T t) throws E;
}
