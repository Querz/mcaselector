package net.querz.mcaselector.filter;

import net.querz.nbt.CompoundTag;

public class ZPosCondition extends Condition {

	private int value;

	public ZPosCondition(Comparator comparator, int value) {
		super(comparator);
		this.value = value;
	}

	public ZPosCondition(Operator operator, Comparator comparator, int value) {
		super(operator, comparator);
		this.value = value;
	}

	@Override
	public boolean isLarger(FilterData data) {
		System.out.println("zPos (" + ((CompoundTag) data.getChunkData().get("Level")).getInt("zPos") + ") > " + value);
		return ((CompoundTag) data.getChunkData().get("Level")).getInt("zPos") > value;
	}

	@Override
	public boolean isSmaller(FilterData data) {
		System.out.println("zPos (" + ((CompoundTag) data.getChunkData().get("Level")).getInt("zPos") + ") < " + value);
		return ((CompoundTag) data.getChunkData().get("Level")).getInt("zPos") < value;
	}

	@Override
	public boolean isEqual(FilterData data) {
		System.out.println("zPos (" + ((CompoundTag) data.getChunkData().get("Level")).getInt("zPos") + ") == " + value);
		return ((CompoundTag) data.getChunkData().get("Level")).getInt("zPos") == value;
	}

	@Override
	public String toString() {
		return "zPos " + comparator + " " + value;
	}
}