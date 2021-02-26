package net.querz.mcaselector.changer;

import net.querz.mcaselector.io.mca.ChunkData;

public abstract class Field<T> {

	private final FieldType type;
	private T newValue;

	public Field(FieldType type) {
		this(type, null);
	}

	public Field(FieldType type, T newValue) {
		this.newValue = newValue;
		this.type = type;
	}

	public boolean needsChange() {
		return newValue != null;
	}

	public void setNewValue(T newValue) {
		this.newValue = newValue;
	}

	@SuppressWarnings("unchecked")
	public void setNewValueRaw(Object newValue) {
		if (newValue == null) {
			this.newValue = null;
		} else {
			this.newValue = (T) newValue;
		}
	}

	public T getNewValue() {
		return newValue;
	}

	public FieldType getType() {
		return type;
	}

	public abstract T getOldValue(ChunkData root);

	@Override
	public String toString() {
		return type.toString() + " = " + valueToString();
	}

	public String valueToString() {
		return newValue.toString();
	}

	//returns true if the value has been correctly parsed and value is not null
	//returns false if value is null
	public boolean parseNewValue(String s) {
		setNewValue(null);
		return false;
	}

	public abstract void change(ChunkData root);

	public abstract void force(ChunkData root);
}
