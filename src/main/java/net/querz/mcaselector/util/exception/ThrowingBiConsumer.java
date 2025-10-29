package net.querz.mcaselector.util.exception;

@FunctionalInterface
public interface ThrowingBiConsumer<T, U, E extends Throwable> {

	void accept(T t, U u) throws E;
}
