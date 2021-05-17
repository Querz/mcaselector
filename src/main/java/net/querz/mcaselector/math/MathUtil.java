package net.querz.mcaselector.math;

public final class MathUtil {

	public static double clamp(double a, double min, double max) {
		return Math.max(min, Math.min(max, a));
	}

	public static int clamp(int a, int min, int max) {
		return Math.max(min, Math.min(max, a));
	}
}
