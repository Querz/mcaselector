package net.querz.mcaselector;

@FunctionalInterface
public interface ExceptionSupplier<T, E extends Exception> {

	T run() throws E;
}