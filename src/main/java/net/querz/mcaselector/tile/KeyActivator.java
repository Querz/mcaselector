package net.querz.mcaselector.tile;

import javafx.scene.input.KeyCode;
import net.querz.mcaselector.validation.ShutdownHooks;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

public class KeyActivator extends TimerTask {

	private final Set<KeyCode> pressedButtons = EnumSet.noneOf(KeyCode.class);
	private final Set<KeyCode> pressedActionKeys = EnumSet.noneOf(KeyCode.class);
	private final Map<KeyCode, List<Consumer<Set<KeyCode>>>> actions = new EnumMap<>(KeyCode.class);
	private Runnable globalAction;

	public KeyActivator() {
		Timer timer = new Timer();
		timer.schedule(this, 0L, 33L);
		ShutdownHooks.addShutdownHook(timer::cancel);
	}

	@Override
	public void run() {
		int executed = 0;
		for (KeyCode pressedButton : pressedButtons) {
			List<Consumer<Set<KeyCode>>> actionList = actions.get(pressedButton);
			if (actionList != null) {
				for (Consumer<Set<KeyCode>> consumer : actionList) {
					consumer.accept(pressedActionKeys);
					executed++;
				}
			}
		}
		if (globalAction != null && executed > 0) {
			globalAction.run();
		}
	}

	public void pressActionKey(KeyCode key) {
		pressedActionKeys.add(key);
	}

	public void releaseActionKey(KeyCode key) {
		pressedActionKeys.remove(key);
	}

	public void pressKey(KeyCode key) {
		pressedButtons.add(key);
	}

	public void releaseKey(KeyCode key) {
		pressedButtons.remove(key);
	}

	public void releaseAllKeys() {
		pressedButtons.clear();
		pressedActionKeys.clear();
	}

	public void registerAction(KeyCode key, Consumer<Set<KeyCode>> action) {
		List<Consumer<Set<KeyCode>>> actionList = actions.getOrDefault(key, new ArrayList<>());
		actionList.add(action);
		actions.put(key, actionList);
	}

	public void registerGlobalAction(Runnable action) {
		globalAction = action;
	}

	public static boolean isArrowKey(KeyCode code) {
		return switch (code) {
			case UP, RIGHT, DOWN, LEFT -> true;
			default -> false;
		};
	}
}
