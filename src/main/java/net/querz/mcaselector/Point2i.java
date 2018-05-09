package net.querz.mcaselector;

import java.util.Objects;

public class Point2i implements Cloneable {
	private int x, z;

	public Point2i() {
		this.x = this.z = 0;
	}

	public Point2i(int x, int z) {
		this.x = x;
		this.z = z;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return z;
	}

	public void setX(int x) {
		this.x = x;
	}

	public void setY(int z) {
		this.z = z;
	}

	public Point2i add(int x, int z) {
		return new Point2i(this.x + x, this.z + z);
	}

	public Point2i add(Point2i p) {
		return add(p.x, p.z);
	}

	public Point2i add(int i) {
		return add(i, i);
	}

	public Point2i sub(int x, int z) {
		return new Point2i(this.x - x, this.z - z);
	}

	public Point2i sub(Point2i p) {
		return sub(p.x, p.z);
	}

	public Point2i sub(int i) {
		return sub(i, i);
	}

	public Point2i mul(int x, int z) {
		return new Point2i(this.x * x, this.z * z);
	}

	public Point2i mul(Point2i p) {
		return mul(p.x, p.z);
	}

	public Point2i mul(int i) {
		return mul(i, i);
	}

	public Point2i div(int x, int z) {
		return new Point2i(this.x / x, this.z / z);
	}

	public Point2i div(float x, float z) {
		return new Point2i((int) (this.x / x), (int) (this.z / z));
	}

	public Point2i div(Point2i p) {
		return div(p.x, p.z);
	}

	public Point2i div(int i) {
		return div(i, i);
	}

	public Point2i div(float f) {
		return div(f, f);
	}

	public Point2i shiftRight(int i) {
		return new Point2i(x >> i, z >> i);
	}

	public Point2i shiftLeft(int i) {
		return new Point2i(x << i, z << i);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof Point2i
				&& ((Point2i) other).x == x
				&& ((Point2i) other).z == z;
	}

	@Override
	public int hashCode() {
		return Objects.hash(x, z);
	}

	@Override
	public String toString() {
		return "<" + x + ", " + z + ">";
	}

	@Override
	public Point2i clone() {
		try {
			return (Point2i) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return null;
	}

}
