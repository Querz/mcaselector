package net.querz.mcaselector.text;

import org.junit.Test;
import static org.junit.Assert.*;
import static net.querz.mcaselector.MCASelectorTestCase.*;

public class TextHelperTest {

	@Test
	public void testParseDuration() {
		assertEquals(5, TextHelper.parseDuration("5 seconds"));
		assertEquals(5, TextHelper.parseDuration("5 secs"));
		assertEquals(5, TextHelper.parseDuration("5 sec"));
		assertEquals(5, TextHelper.parseDuration("5 s"));
		assertEquals(5, TextHelper.parseDuration("5seconds"));
		assertEquals(5, TextHelper.parseDuration("5secs"));
		assertEquals(5, TextHelper.parseDuration("5sec"));
		assertEquals(5, TextHelper.parseDuration("5s"));
		assertEquals(300, TextHelper.parseDuration("5 minutes"));
		assertEquals(300, TextHelper.parseDuration("5 mins"));
		assertEquals(300, TextHelper.parseDuration("5 min"));
		assertEquals(300, TextHelper.parseDuration("5minutes"));
		assertEquals(300, TextHelper.parseDuration("5mins"));
		assertEquals(300, TextHelper.parseDuration("5min"));
		assertEquals(18000, TextHelper.parseDuration("5 hours"));
		assertEquals(18000, TextHelper.parseDuration("5 hour"));
		assertEquals(18000, TextHelper.parseDuration("5 h"));
		assertEquals(18000, TextHelper.parseDuration("5hours"));
		assertEquals(18000, TextHelper.parseDuration("5hour"));
		assertEquals(18000, TextHelper.parseDuration("5h"));
		assertEquals(432000, TextHelper.parseDuration("5 days"));
		assertEquals(432000, TextHelper.parseDuration("5 day"));
		assertEquals(432000, TextHelper.parseDuration("5 d"));
		assertEquals(432000, TextHelper.parseDuration("5days"));
		assertEquals(432000, TextHelper.parseDuration("5day"));
		assertEquals(432000, TextHelper.parseDuration("5d"));
		assertEquals(12960000, TextHelper.parseDuration("5 months"));
		assertEquals(12960000, TextHelper.parseDuration("5 month"));
		assertEquals(12960000, TextHelper.parseDuration("5months"));
		assertEquals(12960000, TextHelper.parseDuration("5month"));
		assertEquals(157680000, TextHelper.parseDuration("5 years"));
		assertEquals(157680000, TextHelper.parseDuration("5 year"));
		assertEquals(157680000, TextHelper.parseDuration("5 y"));
		assertEquals(157680000, TextHelper.parseDuration("5years"));
		assertEquals(157680000, TextHelper.parseDuration("5year"));
		assertEquals(157680000, TextHelper.parseDuration("5y"));
		assertEquals(300, TextHelper.parseDuration("5 minutes "));
		assertEquals(300, TextHelper.parseDuration(" 5 minutes"));
		assertEquals(300, TextHelper.parseDuration(" 5 minutes "));
		assertEquals(73893781, TextHelper.parseDuration("2 years 4months 5 d 6h 1s 3 mins"));
		assertThrowsRuntimeException(() -> TextHelper.parseDuration("invalid"), IllegalArgumentException.class);
		assertThrowsRuntimeException(() -> TextHelper.parseDuration("5 minutes invalid"), IllegalArgumentException.class);
		assertThrowsRuntimeException(() -> TextHelper.parseDuration("5 mini"), IllegalArgumentException.class);
		assertThrowsRuntimeException(() -> TextHelper.parseDuration("5 minutes invalid 5 seconds"), IllegalArgumentException.class);
	}
}
