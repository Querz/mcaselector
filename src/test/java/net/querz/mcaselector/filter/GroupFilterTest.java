package net.querz.mcaselector.filter;

import net.querz.mcaselector.point.Point2i;
import org.junit.Test;
import static org.junit.Assert.*;

public class GroupFilterTest {

	@Test
	public void testAppliesToRegion() {
		// impossible selection
		GroupFilter gf = new GroupFilter(false);
		gf.addFilter(new XPosFilter(Operator.AND, Comparator.SMALLER, -10));
		gf.addFilter(new XPosFilter(Operator.AND, Comparator.LARGER, 10));
		assertFalse(gf.appliesToRegion(new Point2i(1, 0)));
		assertFalse(gf.appliesToRegion(new Point2i(0, 0)));
		assertFalse(gf.appliesToRegion(new Point2i(-1, 0)));
		assertFalse(gf.appliesToRegion(new Point2i(-2, 0)));

		// vertical stripe
		gf = new GroupFilter(false);
		gf.addFilter(new XPosFilter(Operator.AND, Comparator.LARGER, -10));
		gf.addFilter(new XPosFilter(Operator.AND, Comparator.SMALLER, 10));
		assertFalse(gf.appliesToRegion(new Point2i(1, 0)));
		assertTrue(gf.appliesToRegion(new Point2i(0, 0)));
		assertTrue(gf.appliesToRegion(new Point2i(-1, 0)));
		assertFalse(gf.appliesToRegion(new Point2i(-2, 0)));

		// everything but vertical stripe
		gf = new GroupFilter(false);
		gf.addFilter(new XPosFilter(Operator.AND, Comparator.SMALLER, -10));
		gf.addFilter(new XPosFilter(Operator.OR, Comparator.LARGER, 10));
		assertTrue(gf.appliesToRegion(new Point2i(1, 0)));
		assertTrue(gf.appliesToRegion(new Point2i(0, 0)));
		assertTrue(gf.appliesToRegion(new Point2i(-1, 0)));
		assertTrue(gf.appliesToRegion(new Point2i(-2, 0)));

		// vertical stripe through negation
		gf = new GroupFilter(true);
		gf.addFilter(new XPosFilter(Operator.AND, Comparator.SMALLER, -10));
		gf.addFilter(new XPosFilter(Operator.OR, Comparator.LARGER, 10));
		assertFalse(gf.appliesToRegion(new Point2i(1, 0)));
		assertTrue(gf.appliesToRegion(new Point2i(0, 0)));
		assertTrue(gf.appliesToRegion(new Point2i(-1, 0)));
		assertFalse(gf.appliesToRegion(new Point2i(-2, 0)));

		// everything but vertical stripe through negation
		gf = new GroupFilter(true);
		gf.addFilter(new XPosFilter(Operator.AND, Comparator.LARGER, -10));
		gf.addFilter(new XPosFilter(Operator.AND, Comparator.SMALLER, 10));
		assertTrue(gf.appliesToRegion(new Point2i(1, 0)));
		assertTrue(gf.appliesToRegion(new Point2i(0, 0)));
		assertTrue(gf.appliesToRegion(new Point2i(-1, 0)));
		assertTrue(gf.appliesToRegion(new Point2i(-2, 0)));
	}
}
