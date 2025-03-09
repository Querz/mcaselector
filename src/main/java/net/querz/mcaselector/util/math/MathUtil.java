package net.querz.mcaselector.util.math;

public final class MathUtil {

	public static double clamp(double a, double min, double max) {
		return Math.max(min, Math.min(max, a));
	}

	public static float clamp(float a, float min, float max) {
		return Math.max(min, Math.min(max, a));
	}

	public static int clamp(int a, int min, int max) {
		return Math.max(min, Math.min(max, a));
	}
}
