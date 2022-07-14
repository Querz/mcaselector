package net.querz.mcaselector.filter.filters;

import net.querz.mcaselector.filter.Comparator;
import net.querz.mcaselector.filter.FilterType;
import net.querz.mcaselector.filter.Operator;
import net.querz.mcaselector.filter.TextFilter;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.VersionController;
import net.querz.nbt.StringTag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class StatusFilter extends TextFilter<String> {

	private static final Logger LOGGER = LogManager.getLogger(StatusFilter.class);

	private static final Set<String> validStatus = new HashSet<>();
	private static final Comparator[] comparators = {
			Comparator.EQUAL,
			Comparator.NOT_EQUAL,
	};

	static {
		try (BufferedReader bis = new BufferedReader(
				new InputStreamReader(Objects.requireNonNull(BiomeFilter.class.getClassLoader().getResourceAsStream("mapping/all_status.txt"))))) {
			String line;
			while ((line = bis.readLine()) != null) {
				validStatus.add(line);
			}
		} catch (IOException ex) {
			LOGGER.error("error reading mapping/all_status.txt for StatusFilter", ex);
		}
	}

	public StatusFilter() {
		this(Operator.AND, Comparator.EQUAL, null);
	}

	private StatusFilter(Operator operator, Comparator comparator, String value) {
		super(FilterType.STATUS, operator, comparator, value);
		setRawValue(value);
	}

	@Override
	public Comparator[] getComparators() {
		return comparators;
	}

	@Override
	public String getFormatText() {
		return "empty,...";
	}

	@Override
	public boolean matches(ChunkData data) {
		return switch (getComparator()) {
			case EQUAL -> isEqual(value, data);
			case NOT_EQUAL -> !isEqual(value, data);
			default -> false;
		};
	}

	public boolean isEqual(String value, ChunkData data) {
		if (data.region() == null || data.region().getData() == null) {
			return false;
		}
		ChunkFilter chunkFilter = VersionController.getChunkFilter(data.getDataVersion());
		StringTag tag = chunkFilter.getStatus(data.region().getData());
		return tag != null && value.equals(tag.getValue());
	}

	@Override
	public boolean contains(String value, ChunkData data) {
		throw new UnsupportedOperationException("\"contains\" not allowed in StatusFilter");
	}

	@Override
	public boolean containsNot(String value, ChunkData data) {
		throw new UnsupportedOperationException("\"!contains\" not allowed in StatusFilter");
	}

	@Override
	public boolean intersects(String value, ChunkData data) {
		throw new UnsupportedOperationException("\"intersects\" not allowed in StatusFilter");
	}

	@Override
	public void setFilterValue(String raw) {
		if (validStatus.contains(raw)) {
			setValue(raw);
			setRawValue(raw);
			setValid(true);
		} else {
			setValue(null);
			setValid(false);
		}
	}

	@Override
	public String toString() {
		return "Status " + getComparator().getQueryString() + " " + getFilterValue();
	}

	@Override
	public StatusFilter clone() {
		return new StatusFilter(getOperator(), getComparator(), value);
	}
}
