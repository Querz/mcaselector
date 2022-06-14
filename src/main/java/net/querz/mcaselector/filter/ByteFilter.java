package net.querz.mcaselector.filter;

public abstract class ByteFilter extends NumberFilter<Byte> {

	protected byte value;

	public ByteFilter(FilterType type, Operator operator, Comparator comparator, byte value) {
		super(type, operator, comparator);
		this.value = value;
		setRawValue(value + "");
	}

	@Override
	protected Byte getFilterNumber() {
		return value;
	}

	@Override
	protected void setFilterNumber(Byte value) {
		this.value = value;
	}

	@Override
	public void setFilterValue(String raw) {
		if (raw == null) {
			value = 0;
			setValid(false);
		} else {
			try {
				value = Byte.parseByte(raw.replace(" ", ""));
				setValid(true);
				setRawValue(raw);
			} catch (NumberFormatException ex) {
				value = 0;
				setValid(false);
			}
		}
	}

	@Override
	protected boolean isEqual(Byte a, Byte b) {
		return a.byteValue() == b.byteValue();
	}

	@Override
	protected boolean isNotEqual(Byte a, Byte b) {
		return a.byteValue() != b.byteValue();
	}

	@Override
	protected boolean isLargerThan(Byte a, Byte b) {
		return a > b;
	}

	@Override
	protected boolean isSmallerThan(Byte a, Byte b) {
		return a < b;
	}

	@Override
	protected boolean isLargerEqual(Byte a, Byte b) {
		return a >= b;
	}

	@Override
	protected boolean isSmallerEqual(Byte a, Byte b) {
		return a <= b;
	}

	@Override
	public String getFormatText() {
		return "+/-byte";
	}
}
