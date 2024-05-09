package net.querz.mcaselector.validation;

public interface BeforeAfterCallback {

	void before();

	void after();

	boolean valid();
}
