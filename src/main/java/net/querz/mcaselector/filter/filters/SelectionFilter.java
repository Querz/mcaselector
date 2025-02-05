package net.querz.mcaselector.filter.filters;

import net.querz.mcaselector.filter.*;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.util.point.Point2i;
import net.querz.mcaselector.util.property.DataProperty;
import net.querz.mcaselector.selection.Selection;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.VersionHandler;
import net.querz.nbt.IntTag;
import java.io.File;

public class SelectionFilter extends TextFilter<File> implements RegionMatcher {

	private Selection selection = new Selection();
	private final Object lock;
	private DataProperty<Boolean> loaded = new DataProperty<>(false);

	private static final Comparator[] comparators = {
			Comparator.EQUAL,
			Comparator.NOT_EQUAL
	};

	public SelectionFilter() {
		this(FilterType.SELECTION, Operator.AND, Comparator.EQUAL, null, new Object());
	}

	protected SelectionFilter(FilterType type, Operator operator, Comparator comparator, File value, Object lock) {
		super(type, operator, comparator, value);
		this.lock = lock;
		setRawValue(value == null ? "" : value.toString());
	}

	@Override
	public Comparator[] getComparators() {
		return comparators;
	}

	@Override
	public void setFilterValue(String raw) {
		File file = new File(raw.trim());
		if (file.exists() && file.isFile() && raw.endsWith(".csv")) {
			setValid(true);
			setValue(file);
			setRawValue(raw);
		} else {
			setValid(false);
			setValue(null);
		}
	}

	@Override
	public boolean contains(File value, ChunkData data) {
		if (data.region() == null || data.region().getData() == null) {
			return false;
		}

		if (!loaded.get()) {
			synchronized (lock) {
				if (!loaded.get()) {
					loadSelection(value);
				}
			}
		}

		ChunkFilter.Pos pos = VersionHandler.getImpl(data, ChunkFilter.Pos.class);
		IntTag xPos = pos.getXPos(data);
		IntTag zPos = pos.getZPos(data);
		if (xPos == null || zPos == null) {
			return false;
		}
		return selection.isChunkSelected(xPos.asInt(), zPos.asInt());
	}

	@Override
	public boolean matches(ChunkData data) {
		return switch (getComparator()) {
			case EQUAL -> contains(value, data);
			case NOT_EQUAL -> containsNot(value, data);
			default -> false;
		};
	}

	@Override
	public boolean containsNot(File value, ChunkData data) {
		return !contains(value, data);
	}

	@Override
	public boolean intersects(File value, ChunkData data) {
		throw new UnsupportedOperationException("\"intersects\" not allowed in selection filter");
	}

	@Override
	public boolean matchesRegion(Point2i region) {
		if (!loaded.get()) {
			synchronized (lock) {
				if (!loaded.get()) {
					loadSelection(value);
				}
			}
		}

		return switch (getComparator()) {
			case EQUAL -> selection.isAnyChunkInRegionSelected(region);
			case NOT_EQUAL -> !selection.isAnyChunkInRegionSelected(region);
			default -> false;
		};
	}

	@Override
	public String getFormatText() {
		return "<.csv selection file>";
	}

	protected void loadSelection(File value) {
		selection.clear();
		try {
			Selection loaded = Selection.readFromFile(value);
			selection.setSelection(loaded);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		loaded.set(true);
	}

	@Override
	public Filter<File> clone() {
		SelectionFilter clone = new SelectionFilter(getType(), getOperator(), getComparator(), getFilterValue(), lock);
		clone.selection = selection;
		clone.loaded = loaded;
		return clone;
	}

	@Override
	public String toString() {
		return "Selection " + getComparator().getQueryString() + " \"" + getRawValue().replace("\\", "\\\\") + "\"";
	}

	@Override
	public void resetTempData() {
		synchronized (lock) {
			selection.clear();
			loaded.set(false);
		}
	}
}
