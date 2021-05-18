package net.querz.mcaselector.filter;

import net.querz.mcaselector.exception.ParseException;
import org.junit.Test;
import static org.junit.Assert.*;
import static net.querz.mcaselector.MCASelectorTestCase.*;

public class FilterParserTest {

	@Test
	public void testParseBiome() {
		String query = "Biome contains taiga";
		GroupFilter filter = assertThrowsNoException(() -> new FilterParser(query).parse());
		assertEquals(1, filter.getFilterValue().size());
		assertEquals(BiomeFilter.class, filter.getFilterValue().get(0).getClass());
		assertEquals(1, ((BiomeFilter) filter.getFilterValue().get(0)).getFilterValue().size());
		assertEquals(5, (int) ((BiomeFilter) filter.getFilterValue().get(0)).getFilterValue().get(0));
		assertEquals(Comparator.CONTAINS, filter.getFilterValue().get(0).getComparator());
		assertEquals(Operator.AND, filter.getFilterValue().get(0).getOperator());

		String query2 = "Biome contains taiga,plains";
		GroupFilter filter2 = assertThrowsNoException(() -> new FilterParser(query2).parse());
		assertEquals(1, filter2.getFilterValue().size());
		assertEquals(BiomeFilter.class, filter2.getFilterValue().get(0).getClass());
		assertEquals(2, ((BiomeFilter) filter2.getFilterValue().get(0)).getFilterValue().size());
		assertEquals(5, (int) ((BiomeFilter) filter2.getFilterValue().get(0)).getFilterValue().get(0));
		assertEquals(1, (int) ((BiomeFilter) filter2.getFilterValue().get(0)).getFilterValue().get(1));
		assertEquals(Comparator.CONTAINS, filter2.getFilterValue().get(0).getComparator());
		assertEquals(Operator.AND, filter2.getFilterValue().get(0).getOperator());

		String query3 = "Biome contains \"taiga,'100'\"";
		GroupFilter filter3 = assertThrowsNoException(() -> new FilterParser(query3).parse());
		assertEquals(1, filter3.getFilterValue().size());
		assertEquals(BiomeFilter.class, filter3.getFilterValue().get(0).getClass());
		assertEquals(2, ((BiomeFilter) filter3.getFilterValue().get(0)).getFilterValue().size());
		assertEquals(5, (int) ((BiomeFilter) filter3.getFilterValue().get(0)).getFilterValue().get(0));
		assertEquals(100, (int) ((BiomeFilter) filter3.getFilterValue().get(0)).getFilterValue().get(1));
		assertEquals(Comparator.CONTAINS, filter3.getFilterValue().get(0).getComparator());
		assertEquals(Operator.AND, filter3.getFilterValue().get(0).getOperator());

		String query4 = "Biome contains invalid";
		assertThrowsException(() -> new FilterParser(query4).parse(), ParseException.class);
	}

	@Test
	public void testParseDataVersion() {
		String query = "DataVersion = 1234";
		GroupFilter filter = assertThrowsNoException(() -> new FilterParser(query).parse());
		assertEquals(1, filter.getFilterValue().size());
		assertEquals(DataVersionFilter.class, filter.getFilterValue().get(0).getClass());
		assertEquals(1234, (int) ((DataVersionFilter) filter.getFilterValue().get(0)).getFilterValue());
		assertEquals(Comparator.EQUAL, filter.getFilterValue().get(0).getComparator());
		assertEquals(Operator.AND, filter.getFilterValue().get(0).getOperator());

		String query2 = "DataVersion = -1";
		assertThrowsException(() -> new FilterParser(query2).parse(), ParseException.class);

		String query3 = "DataVersion = a";
		assertThrowsException(() -> new FilterParser(query3).parse(), ParseException.class);
	}

	@Test
	public void testParseEntityAmount() {
		String query = "#Entities > 10";
		GroupFilter filter = assertThrowsNoException(() -> new FilterParser(query).parse());
		assertEquals(1, filter.getFilterValue().size());
		assertEquals(EntityAmountFilter.class, filter.getFilterValue().get(0).getClass());
		assertEquals(10, (int) ((EntityAmountFilter) filter.getFilterValue().get(0)).getFilterValue());
		assertEquals(Comparator.LARGER, filter.getFilterValue().get(0).getComparator());
		assertEquals(Operator.AND, filter.getFilterValue().get(0).getOperator());

		String query2 = "#Entities > -1";
		assertThrowsException(() -> new FilterParser(query2).parse(), ParseException.class);

		String query3 = "#Entities > a";
		assertThrowsException(() -> new FilterParser(query3).parse(), ParseException.class);
	}

	@Test
	public void testParseEntity() {
		String query = "Entities !contains shulker";
		GroupFilter filter = assertThrowsNoException(() -> new FilterParser(query).parse());
		assertEquals(1, filter.getFilterValue().size());
		assertEquals(EntityFilter.class, filter.getFilterValue().get(0).getClass());
		assertEquals(1, ((EntityFilter) filter.getFilterValue().get(0)).getFilterValue().size());
		assertEquals("minecraft:shulker", ((EntityFilter) filter.getFilterValue().get(0)).getFilterValue().get(0));
		assertEquals(Comparator.CONTAINS_NOT, filter.getFilterValue().get(0).getComparator());
		assertEquals(Operator.AND, filter.getFilterValue().get(0).getOperator());

		String query2 = "Entities intersects shulker,cow";
		GroupFilter filter2 = assertThrowsNoException(() -> new FilterParser(query2).parse());
		assertEquals(1, filter2.getFilterValue().size());
		assertEquals(EntityFilter.class, filter2.getFilterValue().get(0).getClass());
		assertEquals(2, ((EntityFilter) filter2.getFilterValue().get(0)).getFilterValue().size());
		assertEquals("minecraft:shulker", ((EntityFilter) filter2.getFilterValue().get(0)).getFilterValue().get(0));
		assertEquals("minecraft:cow", ((EntityFilter) filter2.getFilterValue().get(0)).getFilterValue().get(1));
		assertEquals(Comparator.INTERSECTS, filter2.getFilterValue().get(0).getComparator());
		assertEquals(Operator.AND, filter2.getFilterValue().get(0).getOperator());

		String query3 = "Entities contains invalid";
		assertThrowsException(() -> new FilterParser(query3).parse(), ParseException.class);
	}

	@Test
	public void testParseGroup() {
		String query = "()";
		GroupFilter filter = assertThrowsNoException(() -> new FilterParser(query).parse());
		assertEquals(1, filter.getFilterValue().size());
		assertEquals(GroupFilter.class, filter.getFilterValue().get(0).getClass());
		assertEquals(0, ((GroupFilter) filter.getFilterValue().get(0)).getFilterValue().size());
		assertEquals(Operator.AND, filter.getFilterValue().get(0).getOperator());

		String query2 = "(Entities contains cow)";
		GroupFilter filter2 = assertThrowsNoException(() -> new FilterParser(query2).parse());
		assertEquals(1, filter2.getFilterValue().size());
		assertEquals(GroupFilter.class, filter2.getFilterValue().get(0).getClass());
		assertEquals(1, ((GroupFilter) filter2.getFilterValue().get(0)).getFilterValue().size());
		assertEquals(Operator.AND, filter.getFilterValue().get(0).getOperator());

		String query3 = "(Entities contains cow AND (DataVersion = 1234 OR Biome contains taiga))";
		GroupFilter filter3 = assertThrowsNoException(() -> new FilterParser(query3).parse());
		assertEquals(1, filter3.getFilterValue().size());
		assertEquals(GroupFilter.class, filter3.getFilterValue().get(0).getClass());
		assertEquals(2, ((GroupFilter) filter3.getFilterValue().get(0)).getFilterValue().size());
		assertEquals(Operator.AND, filter3.getFilterValue().get(0).getOperator());
		assertEquals(EntityFilter.class, ((GroupFilter) filter3.getFilterValue().get(0)).getFilterValue().get(0).getClass());
		assertEquals(GroupFilter.class, ((GroupFilter) filter3.getFilterValue().get(0)).getFilterValue().get(1).getClass());
		assertEquals(2, ((GroupFilter) ((GroupFilter) filter3.getFilterValue().get(0)).getFilterValue().get(1)).getFilterValue().size());
		assertEquals(DataVersionFilter.class, ((GroupFilter) ((GroupFilter) filter3.getFilterValue().get(0)).getFilterValue().get(1)).getFilterValue().get(0).getClass());
		assertEquals(BiomeFilter.class, ((GroupFilter) ((GroupFilter) filter3.getFilterValue().get(0)).getFilterValue().get(1)).getFilterValue().get(1).getClass());
		assertEquals(Operator.OR, ((GroupFilter) ((GroupFilter) filter3.getFilterValue().get(0)).getFilterValue().get(1)).getFilterValue().get(1).getOperator());

		String query4 = "(()";
		assertThrowsException(() -> new FilterParser(query4).parse(), ParseException.class);

		String query5 = "())";
		assertThrowsNoException(() -> new FilterParser(query5).parse());
		assertThrowsException(() -> new FilterParser(query5).parseStrict(), ParseException.class);
	}

	@Test
	public void testParseInhabitedTime() {
		String query = "InhabitedTime = \"5 days 6 hours\"";
		GroupFilter filter = assertThrowsNoException(() -> new FilterParser(query).parse());
		assertEquals(1, filter.getFilterValue().size());
		assertEquals(InhabitedTimeFilter.class, filter.getFilterValue().get(0).getClass());
		assertEquals(9072000, (long) ((InhabitedTimeFilter) filter.getFilterValue().get(0)).getFilterValue());
		assertEquals(Comparator.EQUAL, filter.getFilterValue().get(0).getComparator());
		assertEquals(Operator.AND, filter.getFilterValue().get(0).getOperator());

		String query2 = "InhabitedTime < 123456";
		GroupFilter filter2 = assertThrowsNoException(() -> new FilterParser(query2).parse());
		assertEquals(1, filter2.getFilterValue().size());
		assertEquals(InhabitedTimeFilter.class, filter2.getFilterValue().get(0).getClass());
		assertEquals(123456, (long) ((InhabitedTimeFilter) filter2.getFilterValue().get(0)).getFilterValue());
		assertEquals(Comparator.SMALLER, filter2.getFilterValue().get(0).getComparator());
		assertEquals(Operator.AND, filter2.getFilterValue().get(0).getOperator());
	}

	@Test
	public void testParseLastUpdate() {
		String query = "LastUpdate = \"5 days 6 hours\"";
		GroupFilter filter = assertThrowsNoException(() -> new FilterParser(query).parse());
		assertEquals(1, filter.getFilterValue().size());
		assertEquals(LastUpdateFilter.class, filter.getFilterValue().get(0).getClass());
		assertEquals(9072000, (long) ((LastUpdateFilter) filter.getFilterValue().get(0)).getFilterValue());
		assertEquals(Comparator.EQUAL, filter.getFilterValue().get(0).getComparator());
		assertEquals(Operator.AND, filter.getFilterValue().get(0).getOperator());

		String query2 = "LastUpdate < 123456";
		GroupFilter filter2 = assertThrowsNoException(() -> new FilterParser(query2).parse());
		assertEquals(1, filter2.getFilterValue().size());
		assertEquals(LastUpdateFilter.class, filter2.getFilterValue().get(0).getClass());
		assertEquals(123456, (long) ((LastUpdateFilter) filter2.getFilterValue().get(0)).getFilterValue());
		assertEquals(Comparator.SMALLER, filter2.getFilterValue().get(0).getComparator());
		assertEquals(Operator.AND, filter2.getFilterValue().get(0).getOperator());
	}
}
