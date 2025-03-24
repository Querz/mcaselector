package net.querz.mcaselector.version;

import org.atteo.classindex.IndexAnnotated;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@IndexAnnotated
public @interface MCVersionImplementation {

	/**
	 * Represents the minimum supported DataVersion of this implementation.
	 * @return the DataVersion, as int.
	 */
	int value();
}
