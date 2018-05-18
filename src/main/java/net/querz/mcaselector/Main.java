package net.querz.mcaselector;

import javafx.scene.paint.Color;
import net.querz.mcaselector.anvil112.Anvil112TextureColorMapping;
import net.querz.mcaselector.util.Helper;

public class Main {

	private static int foliageColor = 0x5bab47;

	private static byte toByteColor(double d) {
		byte c = (byte) (d * 256);
		System.out.println("byte color: " + c);
		return c;
	}

	public static void main(String[] args) {
		System.out.println(Helper.getWorkingDir());

		Color c = new Color(0.592157, 0.592157, 0.592157, 1);

		int nr = (foliageColor >> 16) * toByteColor(c.getRed()) / 255;
		int ng = (foliageColor >> 8 & 0x00FF) * toByteColor(c.getGreen()) / 255;
		int nb = (foliageColor & 0x0000FF) * toByteColor(c.getBlue()) / 255;

		System.out.printf("color: %d %d %d", nr, ng, nb);

		Anvil112TextureColorMapping m = new Anvil112TextureColorMapping();
		m.getRGB(0);

		Window.launch(Window.class, args);
	}
}
