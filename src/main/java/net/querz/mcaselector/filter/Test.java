package net.querz.mcaselector.filter;

import net.querz.mcaselector.filter.structure.Comparator;
import net.querz.mcaselector.filter.structure.DataVersionFilter;
import net.querz.mcaselector.filter.structure.FilterData;
import net.querz.mcaselector.filter.structure.GroupFilter;
import net.querz.mcaselector.filter.structure.InhabitedTimeFilter;
import net.querz.mcaselector.filter.structure.Operator;
import net.querz.mcaselector.filter.structure.XPosFilter;
import net.querz.mcaselector.filter.structure.ZPosFilter;
import net.querz.nbt.CompoundTag;

public class Test {

	public static void main(String[] args) {

		GroupFilter gf = new GroupFilter();
		gf.addFilter(new DataVersionFilter(Operator.AND, Comparator.LT, 1343));
		GroupFilter inner = new GroupFilter();
		inner.addFilter(new XPosFilter(Operator.AND, Comparator.ST, 100));
		inner.addFilter(new ZPosFilter(Operator.AND, Comparator.LT, -100));
		inner.addFilter(new InhabitedTimeFilter(Operator.OR, Comparator.EQ, 100));
		gf.addFilter(inner);

		CompoundTag data = new CompoundTag("Section");
		data.setInt("DataVersion", 1344);
		CompoundTag level = new CompoundTag("Level");
		level.setInt("xPos", 100);
		level.setLong("InhabitedTime", 99);
		data.set(level);

		FilterData fd = new FilterData((int) (System.currentTimeMillis() / 1000), data);

		System.out.println(gf.matches(fd));

		System.out.println(gf.toString(fd));

		System.out.println("\u2195");
	}
}
