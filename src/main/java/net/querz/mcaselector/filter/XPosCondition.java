package net.querz.mcaselector.filter;

import net.querz.nbt.CompoundTag;

public class XPosCondition extends Condition {

	private int value;

	public XPosCondition(Comparator comparator, int value) {
		super(comparator);
		this.value = value;
	}

	public XPosCondition(Operator operator, Comparator comparator, int value) {
		super(operator, comparator);
		this.value = value;
	}

	@Override
	public boolean isLarger(FilterData data) {
		System.out.println("xPos (" + ((CompoundTag) data.getChunkData().get("Level")).getInt("xPos") + ") > " + value);
		return ((CompoundTag) data.getChunkData().get("Level")).getInt("xPos") > value;
	}

	@Override
	public boolean isSmaller(FilterData data) {
		System.out.println("xPos (" + ((CompoundTag) data.getChunkData().get("Level")).getInt("xPos") + ") < " + value);
		return ((CompoundTag) data.getChunkData().get("Level")).getInt("xPos") < value;
	}

	@Override
	public boolean isEqual(FilterData data) {
		System.out.println("xPos (" + ((CompoundTag) data.getChunkData().get("Level")).getInt("xPos") + ") == " + value);
		return ((CompoundTag) data.getChunkData().get("Level")).getInt("xPos") == value;
	}

	@Override
	public String toString() {
		return "xPos " + comparator + " " + value;
	}
}
