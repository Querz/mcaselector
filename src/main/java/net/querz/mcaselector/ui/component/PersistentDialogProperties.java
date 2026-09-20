package net.querz.mcaselector.ui.component;

import javafx.application.Platform;
import javafx.scene.control.Dialog;
import net.querz.mcaselector.config.ConfigProvider;
import net.querz.mcaselector.config.GlobalConfig;
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
		ConfigProvider.GLOBAL.setDialogState(
			dialog.getClass(),
			new GlobalConfig.DialogState(getX(), getY(), getWidth(), getHeight())
		);
	}

	default void initPersistentLocationOnOpen(Dialog<?> dialog) {
		GlobalConfig.DialogState state = ConfigProvider.GLOBAL.getDialogState(dialog.getClass());
		if (state != null) {
			Platform.runLater(() -> {
				dialog.setWidth(state.width());
				dialog.setHeight(state.height());
				dialog.setX(state.x());
				dialog.setY(state.y());
			});
		}
	}
}
