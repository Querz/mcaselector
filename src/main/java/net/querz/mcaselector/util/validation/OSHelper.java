package net.querz.mcaselector.util.validation;

public final class OSHelper {

	public static final OSType OS_TYPE;

	static {
		String osName = System.getProperty("os.name").toLowerCase();
		if (osName.contains("mac")) {
			OS_TYPE = OSType.MAC;
		} else if (osName.contains("windows")) {
			OS_TYPE = OSType.WINDOWS;
		} else {
			OS_TYPE = OSType.OTHER;
		}
	}

	private OSHelper() {}

	public static boolean isWindows() {
		return OS_TYPE == OSType.WINDOWS;
	}

	public static boolean isMac() {
		return OS_TYPE == OSType.MAC;
	}

	public static boolean isOther() {
		return OS_TYPE == OSType.OTHER;
	}

	public enum OSType {
		MAC,
		WINDOWS,
		OTHER,
	}
}
