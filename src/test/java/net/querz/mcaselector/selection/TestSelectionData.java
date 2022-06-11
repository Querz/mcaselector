package net.querz.mcaselector.selection;

import net.querz.mcaselector.point.Point2i;
import org.junit.Test;
import static org.junit.Assert.*;

public class TestSelectionData {

	@Test
	public void getMinMax() {
		Selection s = new Selection();
		s.addRegion(new Point2i(0, 0).asLong());
		SelectionData d = new SelectionData(s, null);
		assertEquals(new Point2i(0, 0), d.getMin());
		assertEquals(new Point2i(31, 31), d.getMax());
	}
}
