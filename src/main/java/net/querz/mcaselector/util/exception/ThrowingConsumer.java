package net.querz.mcaselector.util.exception;

@FunctionalInterface
public interface ThrowingConsumer<T, E extends Throwable> {

	void accept(T t) throws E;
}
