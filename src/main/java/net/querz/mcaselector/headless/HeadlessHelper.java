package net.querz.mcaselector.headless;

public final class HeadlessHelper {

	private HeadlessHelper() {}

	public static boolean hasJavaFX() {
		try  {
			Class.forName("javafx.scene.paint.Color");
			return true;
		}  catch (ClassNotFoundException e) {
			return false;
		}
	}
}
