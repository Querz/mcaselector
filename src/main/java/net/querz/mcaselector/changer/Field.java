package net.querz.mcaselector.changer;

import net.querz.nbt.tag.CompoundTag;

public abstract class Field<T> {

	private FieldType type;
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

	public T getNewValue() {
		return newValue;
	}

	public FieldType getType() {
		return type;
	}

	public abstract T getOldValue(CompoundTag root);

	@Override
	public String toString() {
		return type.toString() + " = " + newValue;
	}

	//returns true if the value has been correctly parsed and value is not null
	//returns false if value is null
	public boolean parseNewValue(String s) {
		setNewValue(null);
		return false;
	}

	public abstract void change(CompoundTag root);

	public abstract void force(CompoundTag root);
}
