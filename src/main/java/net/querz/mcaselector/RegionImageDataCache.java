package net.querz.mcaselector;

import java.util.HashMap;
import java.util.Map;

public class RegionImageDataCache {
	private Map<Point2f, CachedRegionImageData> cache = new HashMap<>();

	public RegionImageDataCache(Point2f viewport) {

	}




	private class CachedRegionImageData {
		private RegionImageData data;
		private long loadTime;
	}
}
