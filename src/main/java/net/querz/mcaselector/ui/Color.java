package net.querz.mcaselector.ui;

public class Color {

	public static final Color BLACK = new Color(0, 0, 0, 1);
	public static final Color DARK_GRAY = new Color(0.6627451f, 0.6627451f, 0.6627451f, 1);
	public static final Color TRANSPARENT = new Color(0, 0, 0, 0);

	private javafx.scene.paint.Color handle;
	private final double r, g, b, a;

	private final int i;
	private String web;

	public Color(String web) {
		this.web = web;
		handle = javafx.scene.paint.Color.web(web);
		this.r = handle.getRed();
		this.g = handle.getGreen();
		this.b = handle.getBlue();
		this.a = handle.getOpacity();
		int ri = (int) Math.round(this.r * 255.0);
		int gi = (int) Math.round(this.g * 255.0);
		int bi = (int) Math.round(this.b * 255.0);
		int ai = (int) Math.round(this.a * 255.0);
		this.i = ai << 24 | ri << 16 | gi << 8 | bi;
	}

	public Color(double r, double g, double b, double a) {
		this.r = r;
		this.g = g;
		this.b = b;
		this.a = a;
		int ri = (int) Math.round(this.r * 255.0);
		int gi = (int) Math.round(this.g * 255.0);
		int bi = (int) Math.round(this.b * 255.0);
		int ai = (int) Math.round(this.a * 255.0);
		this.i = ai << 24 | ri << 16 | gi << 8 | bi;
	}

	public Color(javafx.scene.paint.Color handle) {
		this.handle = handle;
		this.r = handle.getRed();
		this.g = handle.getGreen();
		this.b = handle.getBlue();
		this.a = handle.getOpacity();
		int ri = (int) Math.round(this.r * 255.0);
		int gi = (int) Math.round(this.g * 255.0);
		int bi = (int) Math.round(this.b * 255.0);
		int ai = (int) Math.round(this.a * 255.0);
		this.i = ai << 24 | ri << 16 | gi << 8 | bi;
	}

	public javafx.scene.paint.Color makeJavaFXColor() {
		if (handle == null) {
			if (web != null) {
				handle = javafx.scene.paint.Color.web(web);
				web = null;
			} else {
				handle = new javafx.scene.paint.Color(r, g, b, a);
			}
		}
		return handle;
	}

	public int toInt() {
		return i;
	}

	public static int shade(int color, int amount) {
		int a = (color >> 24) & 0xFF;
		int r = (color >> 16) & 0xFF;
		int g = (color >> 8) & 0xFF;
		int b = color & 0xFF;

		int r2 = clampByte(r + amount) << 16;
		int g2 = clampByte(g + amount) << 8;
		int b2 = clampByte(b + amount);
		return a << 24 | r2 | g2 | b2;
	}

	private static int clampByte(int b) {
		if (b < 0) {
			return 0;
		}
		return Math.min(b, 255);
	}

	public static int blend(int color, int other, float ratio) {
		if (ratio > 1) {
			ratio = 1;
		} else if (ratio < 0) {
			ratio = 0;
		}

		float iRatio = 1.0f - ratio;

		int aA = color >> 24 & 0xFF;
		int aR = color >> 16 & 0xFF;
		int aG = color >> 8 & 0xFF;
		int aB = color & 0xFF;

		int bA = other >> 24 & 0xFF;
		int bR = other >> 16 & 0xFF;
		int bG = other >> 8 & 0xFF;
		int bB = other & 0xFF;

		int a = (int) (aA * iRatio + bA * ratio);
		int r = (int) (aR * iRatio + bR * ratio);
		int g = (int) (aG * iRatio + bG * ratio);
		int b = (int) (aB * iRatio + bB * ratio);

		return a << 24 | r << 16 | g << 8 | b;
	}

	@Override
	public String toString() {
		if (handle == null) {
			if (web != null) {
				return web;
			} else {
				int r = (int) Math.round(this.r * 255.0);
				int g = (int) Math.round(this.g * 255.0);
				int b = (int) Math.round(this.b * 255.0);
				int a = (int) Math.round(this.a * 255.0);
				return String.format("0x%02x%02x%02x%02x" , r, g, b, a);
			}
		}
		return handle.toString();
	}
}
