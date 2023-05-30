package net.querz.mcaselector.logging;

import com.google.gson.FieldNamingStrategy;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class GsonNamingStrategy implements FieldNamingStrategy {

	@Override
	public String translateName(Field f) {
		if ((f.getModifiers() & Modifier.TRANSIENT) > 0) {
			return "t_" + f.getName();
		}
		return f.getName();
	}
}
