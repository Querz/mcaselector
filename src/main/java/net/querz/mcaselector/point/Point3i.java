package net.querz.mcaselector.point;

import java.io.Serializable;
import java.util.Objects;

public class Point3i implements Cloneable, Serializable {

	private int x, y, z;

	public Point3i() {
		this.x = this.y = this.z = 0;
	}

	public Point3i(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public int getZ() {
		return z;
	}

	public void setX(int x) {
		this.x = x;
	}

	public void setY(int y) {
		this.y = y;
	}

	public void setZ(int z) {
		this.z = z;
	}

	public Point3i add(int x, int y, int z) {
		return new Point3i(this.x + x, this.y + y, this.z + z);
	}

	public Point3i add(Point3i p) {
		return add(p.x, p.y, p.z);
	}

	public Point3i add(int i) {
		return add(i, i, i);
	}

	public Point3i sub(int x, int y, int z) {
		return new Point3i(this.x - x, this.y - y, this.z - z);
	}

	public Point3i sub(Point3i p) {
		return sub(p.x, p.y, p.z);
	}

	public Point3i sub(int i) {
		return sub(i, i, i);
	}

	public Point3i mul(int x, int y, int z) {
		return new Point3i(this.x * x, this.y * y, this.z * z);
	}

	public Point3i mul(Point3i p) {
		return mul(p.x, p.y, p.z);
	}

	public Point3i mul(int i) {
		return mul(i, i, i);
	}

	public Point3i div(int x, int y, int z) {
		return new Point3i(this.x / x, this.y / y, this.z / z);
	}

	public Point3i div(float x, float y, float z) {
		return new Point3i((int) (this.x / x), (int) (this.y / y), (int) (this.z / z));
	}

	public Point3i div(Point3i p) {
		return div(p.x, p. y, p.z);
	}

	public Point3i div(int i) {
		return div(i, i, i);
	}

	public Point3i div(float f) {
		return div(f, f, f);
	}

	public Point3i mod(int x, int y, int z) {
		return new Point3i(this.x % x, this.y % y, this.z % z);
	}

	public Point3i mod(float x, float y, float z) {
		return new Point3i((int) (this.x % x), (int) (this.y % y), (int) (this.z % z));
	}

	public Point3i mod(Point3i p) {
		return mod(p.x, p.y, p.z);
	}

	public Point3i mod(int i) {
		return mod(i, i, i);
	}

	public Point3i mod(float f) {
		return mod(f, f, f);
	}

	public Point3i and(int i) {
		return new Point3i(x & i, y & i, z & i);
	}

	public Point3i abs() {
		return new Point3i(x < 0 ? x * -1 : x, y < 0 ? y * -1 : y, z < 0 ? z * -1 : z);
	}

	public Point3i shiftRight(int i) {
		return new Point3i(x >> i, y >> i, z >> i);
	}

	public Point3i shiftLeft(int i) {
		return new Point3i(x << i, y << i, z << i);
	}

	private Point3i shift2Right(int i) {
		return new Point3i(x >> i, y, z >> i);
	}

	private Point3i shift2Left(int i) {
		return new Point3i(x << i, y, z << i);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof Point3i
				&& ((Point3i) other).x == x
				&& ((Point3i) other).y == y
				&& ((Point3i) other).z == z;
	}

	public Point3i blockToRegion() {
		return shift2Right(9);
	}

	public Point3i regionToBlock() {
		return shift2Left(9);
	}

	public Point3i regionToChunk() {
		return shift2Left(5);
	}

	public Point3i blockToChunk() {
		return shift2Right(4);
	}

	public Point3i chunkToBlock() {
		return shift2Left(4);
	}

	public Point3i chunkToRegion() {
		return shift2Right(5);
	}

	public Point2i toPoint2i() {
		return new Point2i(x, z);
	}

	public Point3i sectionToBlock() {
		return shiftLeft(4);
	}

	public Point3i blockToSection() {
		return shiftRight(4);
	}

	@Override
	public int hashCode() {
		return Objects.hash(x, y, z);
	}

	@Override
	public String toString() {
		return "<" + x + ", " + y + ", " + z + ">";
	}

	@Override
	public Point3i clone() {
		try {
			return (Point3i) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return null;
	}
}
