package net.querz.mcaselector.util;

import javafx.scene.input.KeyCode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class KeyActivator extends TimerTask {

	private Timer timer = new Timer();

	private Set<KeyCode> pressedButtons = new HashSet<>();

	private Map<KeyCode, List<Runnable>> actions = new HashMap<>();

	private Runnable globalAction;

	public KeyActivator() {
		timer.schedule(this, 0L, 33L);
		Runtime.getRuntime().addShutdownHook(new Thread(timer::cancel));
	}

	@Override
	public void run() {
		for (KeyCode pressedButton : pressedButtons) {
			List<Runnable> actionList = actions.get(pressedButton);
			if (actionList != null) {
				for (Runnable runnable : actionList) {
					runnable.run();
				}
			}
		}
		if (globalAction != null && pressedButtons.size() > 0) {
			globalAction.run();
		}
	}

	public void pressKey(KeyCode key) {
		pressedButtons.add(key);
	}

	public void releaseKey(KeyCode key) {
		pressedButtons.remove(key);
	}

	public void registerAction(KeyCode key, Runnable action) {
		List<Runnable> actionList = actions.getOrDefault(key, new ArrayList<>());
		actionList.add(action);
		actions.put(key, actionList);
	}

	public void registerGlobalAction(Runnable action) {
		globalAction = action;
	}
}
