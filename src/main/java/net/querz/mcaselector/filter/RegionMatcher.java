package net.querz.mcaselector.filter;

import net.querz.mcaselector.util.point.Point2i;

public interface RegionMatcher {

	MatchType matchesRegion(Point2i region);

	enum MatchType {
		FULLY,
		PARTIALLY,
		NONE
	}
}
