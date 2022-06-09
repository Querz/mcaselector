package net.querz.mcaselector.cli;

public final class CLIHelper {

	private CLIHelper() {}

	public static boolean hasJavaFX() {
		try  {
			Class.forName("javafx.scene.paint.Color");
			return true;
		}  catch (ClassNotFoundException e) {
			return false;
		}
	}
}
