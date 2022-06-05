package net.querz.mcaselector.selection;

import net.querz.mcaselector.point.Point2i;
import org.junit.Test;
import static org.junit.Assert.*;

public class TestChunkSet {

	@Test
	public void getMinMax() {
		ChunkSet s = new ChunkSet();
		Point2i i = new Point2i(3, 2);
		s.set(i.asChunkIndex());
		assertEquals(3, s.getMinX(31));
		assertEquals(2, s.getMinZ(31));
		assertEquals(3, s.getMaxX(0));
		assertEquals(2, s.getMaxZ(0));

		s = new ChunkSet();
		i = new Point2i(2, 3);
		s.set(i.asChunkIndex());
		assertEquals(2, s.getMinX(31));
		assertEquals(3, s.getMinZ(31));
		assertEquals(2, s.getMaxX(0));
		assertEquals(3, s.getMaxZ(0));
	}

	@Test
	public void forEach() {
		ChunkSet s = new ChunkSet();
		Point2i i = new Point2i(5, 5);
		s.set(i.asChunkIndex());
		System.out.println(s);
		s.forEach(p -> System.out.println(new Point2i(p)));
	}
}
