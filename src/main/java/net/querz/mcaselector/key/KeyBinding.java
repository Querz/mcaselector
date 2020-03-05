package net.querz.mcaselector.key;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public enum KeyBinding {

	MAP_UP(new KeyCodeCombination(KeyCode.W)),
	MAP_LEFT(new KeyCodeCombination(KeyCode.A)),
	MAP_DOWN(new KeyCodeCombination(KeyCode.S)),
	MAP_RIGHT(new KeyCodeCombination(KeyCode.D)),
	MAP_ZOOM_IN(new KeyCodeCombination(KeyCode.PLUS, KeyCombination.CONTROL_DOWN)),
	MAP_ZOOM_OUT(new KeyCodeCombination(KeyCode.MINUS, KeyCombination.CONTROL_DOWN));

	private KeyCodeCombination keyCodeCombination;
	private Runnable action;

	private static Map<KeyCodeCombination, KeyBinding> mapping = new HashMap<>();

	static {
		for (KeyBinding keyBinding : values()) {
			mapping.put(keyBinding.keyCodeCombination, keyBinding);
		}
	}

	KeyBinding(KeyCodeCombination keyCodeCombination) {
		this.keyCodeCombination = keyCodeCombination;
	}

	public KeyCodeCombination getCombination() {
		return keyCodeCombination;
	}

	public void registerCombination(KeyCodeCombination keyCodeCombination, Runnable action) {
		this.keyCodeCombination = keyCodeCombination;
		this.action = action;
	}

	public static void runAction(KeyCodeCombination keyCodeCombination) {
		KeyBinding keyBinding = mapping.get(keyCodeCombination);
		if (keyBinding != null) {
			keyBinding.action.run();
		}
	}

	public static void matchAndRun(KeyEvent event) {
		for (KeyBinding keyBinding : values()) {
			if (keyBinding.keyCodeCombination.match(event)) {
				keyBinding.action.run();
			}
		}
	}

	// add combination to currentCombination for every key that is pressed but is not a modifier key
	private static Set<KeyCodeCombination> currentCombinations = new HashSet<>();
	private static Set<KeyCode> currentKeys = new HashSet<>();

	public static void keyPressedAction(KeyEvent event) {

		System.out.println("p" + event.getCode());

		KeyCombination.ModifierValue shift = event.isShiftDown() ? KeyCombination.ModifierValue.DOWN : KeyCombination.ModifierValue.UP;
		KeyCombination.ModifierValue control = event.isControlDown() ? KeyCombination.ModifierValue.DOWN : KeyCombination.ModifierValue.UP;
		KeyCombination.ModifierValue alt = event.isAltDown() ? KeyCombination.ModifierValue.DOWN : KeyCombination.ModifierValue.UP;
		KeyCombination.ModifierValue meta = event.isMetaDown() ? KeyCombination.ModifierValue.DOWN : KeyCombination.ModifierValue.UP;
		KeyCombination.ModifierValue shortcut = event.isShortcutDown() ? KeyCombination.ModifierValue.DOWN : KeyCombination.ModifierValue.UP;

		boolean modifier = false;

		switch (event.getCode()) {
			case SHIFT:
			case CONTROL:
			case ALT:
			case META:
			case SHORTCUT:
				modifier = true;
		}

		if (modifier) {
			// if this is a modifier, recreate the currentCombinations set based on currentKeys
			currentCombinations.clear();
			for (KeyCode keyCode : currentKeys) {
				currentCombinations.add(new KeyCodeCombination(keyCode, shift, control, alt, meta, shortcut));
			}
		} else {
			// if this is a normal key, add it and create a combination
			currentKeys.add(event.getCode());
			currentCombinations.add(new KeyCodeCombination(event.getCode(), shift, control, alt, meta, shortcut));
		}

		System.out.println("pressed: " + currentCombinations);
	}

	public static void keyReleasedAction(KeyEvent event) {

		System.out.println("r" + event.getCode());

		KeyCombination.ModifierValue shift = event.isShiftDown() ? KeyCombination.ModifierValue.DOWN : KeyCombination.ModifierValue.UP;
		KeyCombination.ModifierValue control = event.isControlDown() ? KeyCombination.ModifierValue.DOWN : KeyCombination.ModifierValue.UP;
		KeyCombination.ModifierValue alt = event.isAltDown() ? KeyCombination.ModifierValue.DOWN : KeyCombination.ModifierValue.UP;
		KeyCombination.ModifierValue meta = event.isMetaDown() ? KeyCombination.ModifierValue.DOWN : KeyCombination.ModifierValue.UP;
		KeyCombination.ModifierValue shortcut = event.isShortcutDown() ? KeyCombination.ModifierValue.DOWN : KeyCombination.ModifierValue.UP;

		boolean modifier = false;

		switch (event.getCode()) {
			case SHIFT:
			case CONTROL:
			case ALT:
			case META:
			case SHORTCUT:
				modifier = true;
		}

		if (modifier) {
			currentCombinations.clear();
			for (KeyCode keyCode : currentKeys) {
				currentCombinations.add(new KeyCodeCombination(keyCode, shift, control, alt, meta, shortcut));
			}
		} else {
			currentKeys.remove(event.getCode());
			currentCombinations.remove(new KeyCodeCombination(event.getCode(), shift, control, alt, meta, shortcut));
		}
		System.out.println("released: " + currentCombinations);
	}
}
