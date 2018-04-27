package net.querz.mcaselector;

public class Point implements Cloneable {
	private float x, y;

	public Point() {
		this.x = this.y = 0;
	}

	public Point(float x, float y) {
		this.x = x;
		this.y = y;
	}

	public float getX() {
		return x;
	}

	public float getY() {
		return y;
	}

	public void setX(float x) {
		this.x = x;
	}

	public void setY(float y) {
		this.y = y;
	}

	public Point add(float x, float y) {
		return new Point(this.x + x, this.y + y);
	}

	public Point add(Point p) {
		return add(p.x, p.y);
	}

	public Point add(float i) {
		return add(i, i);
	}

	public Point sub(float x, float y) {
		return new Point(this.x - x, this.y - y);
	}

	public Point sub(Point p) {
		return sub(p.x, p.y);
	}

	public Point sub(float i) {
		return sub(i, i);
	}

	public Point mul(float x, float y) {
		return new Point(this.x * x, this.y * y);
	}

	public Point mul(Point p) {
		return mul(p.x, p.y);
	}

	public Point mul(float i) {
		return mul(i, i);
	}

	public Point div(float x, float y) {
		return new Point(this.x / x, this.y / y);
	}

	public Point div(Point p) {
		return div(p.x, p.y);
	}

	public Point div(float i) {
		return div(i, i);
	}

	@Override
	public String toString() {
		return "<" + x + ", " + y + ">";
	}

	@Override
	public Point clone() {
		try {
			return (Point) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return null;
	}
}
