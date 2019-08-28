package net.querz.mcaselector.util;

public class Color {

	public static final Color BLACK = new Color(0, 0, 0, 1);
	public static final Color DARKGRAY = new Color(0.6627451f, 0.6627451f, 0.6627451f, 1);
	public static final Color TRANSPARENT = new Color(0, 0, 0, 0);

	private javafx.scene.paint.Color handle;
	private double r, g, b, a;
	private String web;

	public Color(String web) {
		this.web = web;
	}

	public Color(double r, double g, double b, double a) {
		this.r = r;
		this.g = g;
		this.b = b;
		this.a = a;
	}

	public Color(javafx.scene.paint.Color handle) {
		this.handle = handle;
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

	@Override
	public String toString() {
		if (handle == null) {
			if (web != null) {
				return web;
			} else {
				int r = (int)Math.round(this.r * 255.0);
				int g = (int)Math.round(this.g * 255.0);
				int b = (int)Math.round(this.b * 255.0);
				int a = (int)Math.round(this.a * 255.0);
				return String.format("0x%02x%02x%02x%02x" , r, g, b, a);
			}
		}
		return handle.toString();
	}
}
