package net.querz.mcaselector.util.validation;

public interface BeforeAfterCallback {

	void before();

	void after();

	boolean valid();
}
