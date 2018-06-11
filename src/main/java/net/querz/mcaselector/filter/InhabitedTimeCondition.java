package net.querz.mcaselector.filter;

import net.querz.nbt.CompoundTag;

public class InhabitedTimeCondition extends Condition {

	private long value;

	public InhabitedTimeCondition(Comparator comparator, long value) {
		super(comparator);
		this.value = value;
	}

	public InhabitedTimeCondition(Operator operator, Comparator comparator, long value) {
		super(operator, comparator);
		this.value = value;
	}

	@Override
	public boolean isLarger(FilterData data) {
		System.out.println("InhabitedTime (" + ((CompoundTag) data.getChunkData().get("Level")).getLong("InhabitedTime") + ") > " + value);
		return ((CompoundTag) data.getChunkData().get("Level")).getLong("InhabitedTime") > value;
	}

	@Override
	public boolean isSmaller(FilterData data) {
		System.out.println("InhabitedTime (" + ((CompoundTag) data.getChunkData().get("Level")).getLong("InhabitedTime") + ") < " + value);
		return ((CompoundTag) data.getChunkData().get("Level")).getLong("InhabitedTime") < value;
	}

	@Override
	public boolean isEqual(FilterData data) {
		System.out.println("InhabitedTime (" + ((CompoundTag) data.getChunkData().get("Level")).getLong("InhabitedTime") + ") == " + value);
		return ((CompoundTag) data.getChunkData().get("Level")).getLong("InhabitedTime") == value;
	}


	@Override
	public String toString() {
		return "InhabitedTime " + comparator + " " + value;
	}
}
