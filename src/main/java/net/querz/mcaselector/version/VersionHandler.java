package net.querz.mcaselector.version;

import net.querz.mcaselector.io.mca.ChunkData;
import org.atteo.classindex.ClassIndex;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public final class VersionHandler {

	private VersionHandler() {}

	private static final Map<Class<?>, TreeMap<Integer, Object>> implementations = new HashMap<>();

	public static void init() {
		// initialize all implementations
		for (Class<?> clazz : ClassIndex.getAnnotated(MCVersionImplementation.class)) {
			Class<?> interfaceClass = null;
			Class<?> superClass = clazz;
			while (interfaceClass == null && superClass != null) {
				Class<?>[] interfaces = superClass.getInterfaces();
				if (interfaces.length > 0) {
					interfaceClass = interfaces[0];
				}
				superClass = superClass.getSuperclass();
			}
			if (interfaceClass == null) {
				throw new RuntimeException("could not find interface for " + clazz);
			}

			implementations.compute(interfaceClass, (k, v) -> {
				if (v == null) {
					v = new TreeMap<>();
				}
				try {
					v.put(clazz.getAnnotation(MCVersionImplementation.class).value(), clazz.getConstructor().newInstance());
				} catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
					throw new RuntimeException(e);
				}
				return v;
			});
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> T getImpl(int dataVersion, Class<T> clazz) {
		TreeMap<Integer, Object> implementation = implementations.get(clazz);
		if (implementation == null) {
			throw new IllegalArgumentException("no implementation for " + clazz);
		}
		Map.Entry<Integer, Object> e = implementation.floorEntry(dataVersion);
		if (e == null) {
			throw new IllegalArgumentException("no implementation for " + clazz + " with version " + dataVersion);
		}
		if (!clazz.isAssignableFrom(e.getValue().getClass())) {
			throw new IllegalArgumentException("wrong implementation for " + clazz + " with version " + dataVersion + ": " + e.getValue().getClass());
		}
		return (T) e.getValue();
	}

	public static <T> T getImpl(ChunkData data, Class<T> clazz) {
		if (data == null) {
			throw new IllegalArgumentException("chunk data is null");
		}
		int dataVersion = 0;
		if (data.region() != null && data.region().getData() != null) {
			dataVersion = Helper.getDataVersion(data.region().getData());
		} else if (data.entities() != null && data.entities().getData() != null) {
			dataVersion = Helper.getDataVersion(data.entities().getData());
		} else if (data.poi() != null && data.poi().getData() != null) {
			dataVersion = Helper.getDataVersion(data.poi().getData());
		}
		return getImpl(dataVersion, clazz);
	}
}
