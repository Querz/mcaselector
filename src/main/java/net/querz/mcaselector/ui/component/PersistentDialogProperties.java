package net.querz.mcaselector.ui.component;

import javafx.application.Platform;
import javafx.scene.control.Dialog;
import net.querz.mcaselector.util.point.Point2i;
import java.util.HashMap;
import java.util.Map;

public interface PersistentDialogProperties {

	@SuppressWarnings("rawtypes")
	Map<Class<? extends Dialog>, Point2i> lastWindowSize = new HashMap<>();
	@SuppressWarnings("rawtypes")
	Map<Class<? extends Dialog>, Point2i> lastWindowLocation = new HashMap<>();

	double getWidth();
	double getHeight();
	double getX();
	double getY();

	default void initPersistentLocationOnClose(Dialog<?> dialog) {
		lastWindowSize.put(dialog.getClass(), new Point2i((int) getWidth(), (int) getHeight()));
		lastWindowLocation.put(dialog.getClass(), new Point2i((int) getX(), (int) getY()));
	}

	default void initPersistentLocationOnOpen(Dialog<?> dialog) {
		Point2i lastSize = lastWindowSize.get(dialog.getClass());
		Point2i lastLocation = lastWindowLocation.get(dialog.getClass());

		if (lastSize != null && lastLocation != null && lastSize.getX() != 0 && lastSize.getZ() != 0) {
			Platform.runLater(() -> {
				dialog.setWidth(lastSize.getX());
				dialog.setHeight(lastSize.getZ());
				dialog.setX(lastLocation.getX());
				dialog.setY(lastLocation.getZ());
			});
		}
	}
}
