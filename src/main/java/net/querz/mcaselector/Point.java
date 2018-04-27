package net.querz.mcaselector;

public class Point implements Cloneable {
	private int x, y;

	public Point() {
		this.x = this.y = 0;
	}

	public Point(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public void setX(int x) {
		this.x = x;
	}

	public void setY(int y) {
		this.y = y;
	}

	public Point add(int x, int y) {
		return new Point(this.x + x, this.y + y);
	}

	public Point add(Point p) {
		return add(p.x, p.y);
	}

	public Point add(int i) {
		return add(i, i);
	}

	public Point sub(int x, int y) {
		return new Point(this.x - x, this.y - y);
	}

	public Point sub(Point p) {
		return sub(p.x, p.y);
	}

	public Point sub(int i) {
		return sub(i, i);
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
