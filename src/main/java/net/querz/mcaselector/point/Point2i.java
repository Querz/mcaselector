package net.querz.mcaselector.point;

import java.io.Serializable;
import java.util.Objects;

public class Point2i implements Cloneable, Serializable {

	private int x, z;

	public Point2i() {
		this.x = this.z = 0;
	}

	public Point2i(int x, int z) {
		this.x = x;
		this.z = z;
	}

	public Point2i(long l) {
		this.x = (int) (l >> 32);
		this.z = (int) l;
	}

	public Point2i(int chunkIndex) {
		this.x = chunkIndex & 0x1F;
		this.z = chunkIndex >> 5;
	}

	public Point2i(short chunkIndex) {
		this.x = chunkIndex & 0x1F;
		this.z = chunkIndex >> 5;
	}

	public int getX() {
		return x;
	}

	public int getZ() {
		return z;
	}

	public void setX(int x) {
		this.x = x;
	}

	public void setZ(int z) {
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

	public Point2i mod(int x, int z) {
		return new Point2i(this.x % x, this.z % z);
	}

	public Point2i mod(float x, float z) {
		return new Point2i((int) (this.x % x), (int) (this.z % z));
	}

	public Point2i mod(Point2i p) {
		return mod(p.x, p.z);
	}

	public Point2i mod(int i) {
		return mod(i, i);
	}

	public Point2i mod(float f) {
		return mod(f, f);
	}

	public Point2i and(int i) {
		return new Point2i(x & i, z & i);
	}

	public Point2i abs() {
		return new Point2i(x < 0 ? x * -1 : x, z < 0 ? z * -1 : z);
	}

	public Point2i shiftRight(int i) {
		return new Point2i(x >> i, z >> i);
	}

	public Point2i shiftLeft(int i) {
		return new Point2i(x << i, z << i);
	}

	public Point2f toPoint2f() {
		return new Point2f((float) x, (float) z);
	}

	public Point3i toPoint3i() {
		return new Point3i(x, 0, z);
	}

	public Point3i toPoint3i(int y) {
		return new Point3i(x, y, z);
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

	public Point2i blockToRegion() {
		return shiftRight(9);
	}

	public Point2i regionToBlock() {
		return shiftLeft(9);
	}

	public Point2i regionToChunk() {
		return shiftLeft(5);
	}

	public Point2i blockToChunk() {
		return shiftRight(4);
	}

	public Point2i chunkToBlock() {
		return shiftLeft(4);
	}

	public Point2i chunkToRegion() {
		return shiftRight(5);
	}

	public long asLong() {
		return (long) x << 32 | z & 0xFFFFFFFFL;
	}

	// converts this absolute chunk coordinate into a relative chunk coordinate
	public Point2i asRelativeChunk() {
		return new Point2i(x & 0x1F, z & 0x1F);
	}

	// returns a short containing relative chunk coordinates (0|0 to 31|31).
	// only the first 1024 bits are populated which allows easy looping without having to convert.
	public short asChunkIndex() {
		Point2i n = asRelativeChunk();
		return (short) (n.z << 5 | n.x & 0x1F);
	}
}
