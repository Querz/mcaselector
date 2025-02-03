package net.querz.mcaselector.exception;

@FunctionalInterface
public interface ThrowingFunction <T, R, E extends Throwable> {

	R apply(T t) throws E;
}
