package net.querz.mcaselector.util.property;

public class DataProperty<T> {

	private T data;

	public DataProperty() {}

	public DataProperty(T data) {
		this.data = data;
	}

	public T get() {
		return data;
	}

	public void set(T data) {
		this.data = data;
	}
}
