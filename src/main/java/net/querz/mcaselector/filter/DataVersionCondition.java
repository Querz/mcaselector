package net.querz.mcaselector.filter;

public class DataVersionCondition extends Condition {

	private int value;

	public DataVersionCondition(Comparator comparator, int value) {
		super(comparator);
		this.value = value;
	}

	public DataVersionCondition(Operator operator, Comparator comparator, int value) {
		super(operator, comparator);
		this.value = value;
	}

	@Override
	public boolean isLarger(FilterData data) {
		System.out.println("DataVersion (" + data.getChunkData().getInt("DataVersion") + ") > " + value);
		return data.getChunkData().getInt("DataVersion") > value;
	}

	@Override
	public boolean isSmaller(FilterData data) {
		System.out.println("DataVersion (" + data.getChunkData().getInt("DataVersion") + ") < " + value);
		return data.getChunkData().getInt("DataVersion") < value;
	}

	@Override
	public boolean isEqual(FilterData data) {
		System.out.println("DataVersion (" + data.getChunkData().getInt("DataVersion") + ") == " + value);
		return data.getChunkData().getInt("DataVersion") == value;
	}

	@Override
	public String toString() {
		return "DataVersion " + comparator + " " + value;
	}
}
