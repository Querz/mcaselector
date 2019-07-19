package net.querz.mcaselector.headless;

import net.querz.mcaselector.changer.Field;
import net.querz.mcaselector.filter.GroupFilter;
import java.util.List;

public class Main {

	public static void main(String[] args) throws ParseException {
		GroupFilter g = new FilterParser("xPos == 100 OR zPos != 4 AND Status == empty AND LightPopulated == 1 OR (DataVersion > 1 OR (xPos <= \"17\")) AND xPos == 2").parse();

		System.out.println(g);

		String change = "DataVersion = 2, Status = base";

		List<Field<?>> fields = new ChangeParser(change).parse();
		fields.forEach(f -> System.out.println(f + " = " + f.getNewValue()));
	}
}
