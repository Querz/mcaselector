package net.querz.mcaselector.ui.dialog;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.IndexedCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Skin;
import javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.scene.control.skin.VirtualFlow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.css.PseudoClass;
import javafx.stage.Window;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

final class ReplaceBlocksAutocomplete {

	private static final PseudoClass HIGHLIGHTED = PseudoClass.getPseudoClass("autocomplete-highlighted");
	private static final String HIGHLIGHT_INDEX = "replace-blocks-autocomplete-highlight-index";

	private ReplaceBlocksAutocomplete() {}

	static boolean isPopupKey(KeyCode code) {
		return code == KeyCode.UP || code == KeyCode.DOWN || code == KeyCode.PAGE_UP
				|| code == KeyCode.PAGE_DOWN || code == KeyCode.ENTER || code == KeyCode.TAB;
	}

	record HorizontalPopupBounds(double x, double width) {}

	static HorizontalPopupBounds constrainHorizontalBounds(double popupX, double popupWidth,
			double comboX, double comboWidth, double ownerX, double ownerWidth, double margin) {
		double safeMargin = Math.max(0, margin);
		double safeLeft = ownerX + safeMargin;
		double safeRight = ownerX + ownerWidth - safeMargin;
		if (safeRight <= safeLeft) {
			return new HorizontalPopupBounds(safeLeft, 0);
		}

		double width = Math.min(Math.max(popupWidth, comboWidth), safeRight - safeLeft);
		double x = popupX;
		boolean comboFits = comboX >= safeLeft && comboX + comboWidth <= safeRight;
		if (comboFits) {
			x = comboX;
			width = Math.min(width, safeRight - comboX);
		} else {
			x = Math.max(safeLeft, safeRight - width);
		}
		x = Math.max(safeLeft, Math.min(x, safeRight - width));
		return new HorizontalPopupBounds(x, width);
	}

	static int selectionTarget(KeyCode code, int selectedIndex, int itemCount, int visibleRows) {
		if (itemCount <= 0) {
			return -1;
		}
		int current = selectedIndex < 0 ? 0 : selectedIndex;
		return switch (code) {
			case UP -> Math.max(0, selectedIndex < 0 ? itemCount - 1 : current - 1);
			case DOWN -> Math.min(itemCount - 1, selectedIndex < 0 ? 0 : current + 1);
			case PAGE_UP -> Math.max(0, current - Math.max(1, visibleRows - 1));
			case PAGE_DOWN -> Math.min(itemCount - 1, current + Math.max(1, visibleRows - 1));
			default -> -1;
		};
	}

	static boolean focusSuggestion(ComboBox<?> comboBox, int index) {
		ListView<?> popup = popupContent(comboBox);
		if (popup == null || index < -1 || index >= popup.getItems().size()) {
			return false;
		}
		popup.getSelectionModel().clearSelection();
		popup.getFocusModel().focus(-1);
		applyHighlight(popup, index);
		if (index < 0) {
			return true;
		}
		VisibleRange range = visibleRange(popup);
		if (range == null || needsReveal(index, range.first(), range.last())) {
			popup.scrollTo(index);
			Platform.runLater(() -> {
				if (Objects.equals(popup.getProperties().get(HIGHLIGHT_INDEX), index)) {
					applyHighlight(popup, index);
				}
			});
		}
		return true;
	}

	static int visibleRowCount(ComboBox<?> comboBox, int fallback) {
		ListView<?> popup = popupContent(comboBox);
		VisibleRange range = popup == null ? null : visibleRange(popup);
		return range == null ? Math.max(1, fallback) : pageSize(range.first(), range.last(), fallback);
	}

	private static VisibleRange visibleRange(ListView<?> popup) {
		if (!(popup.lookup(".virtual-flow") instanceof VirtualFlow<?> flow)) {
			return null;
		}
		IndexedCell<?> first = flow.getFirstVisibleCell();
		IndexedCell<?> last = flow.getLastVisibleCell();
		if (first == null || last == null) {
			return null;
		}
		int firstFullyVisible = first.getIndex();
		int lastFullyVisible = last.getIndex();
		Bounds viewportBounds = flow.getBoundsInLocal();
		Bounds firstBounds = flow.sceneToLocal(first.localToScene(first.getBoundsInLocal()));
		Bounds lastBounds = flow.sceneToLocal(last.localToScene(last.getBoundsInLocal()));
		if (!isCellFullyVisible(firstBounds, viewportBounds)) {
			firstFullyVisible++;
		}
		if (!isCellFullyVisible(lastBounds, viewportBounds)) {
			lastFullyVisible--;
		}
		return new VisibleRange(firstFullyVisible, lastFullyVisible);
	}

	static boolean needsReveal(int target, int firstVisible, int lastVisible) {
		return target < firstVisible || target > lastVisible;
	}

	static int pageSize(int firstVisible, int lastVisible, int fallback) {
		int visible = lastVisible - firstVisible + 1;
		return visible > 0 ? visible : Math.max(1, fallback);
	}

	static boolean isCellFullyVisible(Bounds cellBounds, Bounds viewportBounds) {
		return cellBounds.getMinY() >= viewportBounds.getMinY() - 0.5
				&& cellBounds.getMaxY() <= viewportBounds.getMaxY() + 0.5;
	}

	static List<String> suggestions(List<String> names, String query, boolean allowEmpty) {
		String text = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
		if (text.isEmpty()) {
			return allowEmpty ? List.copyOf(names) : List.of();
		}
		return names.stream().filter(name -> matches(name, text)).toList();
	}

	private static boolean matches(String name, String query) {
		if (name.contains(query)) {
			return true;
		}
		int namespace = name.indexOf(':');
		return namespace >= 0 && name.substring(namespace + 1).contains(query);
	}

	static boolean isArrowTarget(Object target) {
		Node node = target instanceof Node n ? n : null;
		while (node != null) {
			if (node.getStyleClass().contains("arrow-button")) {
				return true;
			}
			node = node.getParent();
		}
		return false;
	}

	static boolean isCellHighlighted(int cellIndex, int highlightIndex, boolean empty) {
		return !empty && cellIndex == highlightIndex;
	}

	static boolean shouldClearExplicitCatalog(boolean explicitCatalog, boolean showing, String editorText) {
		return explicitCatalog && !showing && (editorText == null || editorText.isBlank());
	}

	static void clearEmptyExplicitCatalog(ComboBox<?> comboBox) {
		String editorText = comboBox.getEditor() == null ? null : comboBox.getEditor().getText();
		if (!shouldClearExplicitCatalog(true, comboBox.isShowing(), editorText)) {
			return;
		}
		comboBox.getItems().clear();
		comboBox.getSelectionModel().clearSelection();
		comboBox.setValue(null);
	}

	static void clearHighlight(ComboBox<?> comboBox) {
		ListView<?> popup = popupContent(comboBox);
		if (popup != null) {
			applyHighlight(popup, -1);
		}
	}

	private static void applyHighlight(ListView<?> popup, int index) {
		popup.getProperties().put(HIGHLIGHT_INDEX, index);
		for (Node node : popup.lookupAll(".list-cell")) {
			if (node instanceof IndexedCell<?> cell) {
				cell.pseudoClassStateChanged(HIGHLIGHTED,
						isCellHighlighted(cell.getIndex(), index, cell.isEmpty()));
			}
		}
	}

	static void installPopupKeyFilter(ComboBox<?> comboBox, EventHandler<KeyEvent> handler) {
		PopupPositionTracker positionTracker = new PopupPositionTracker(comboBox);
		comboBox.addEventHandler(ComboBoxBase.ON_SHOWN, event -> {
			focusSuggestion(comboBox, -1);
			positionTracker.attach();
		});
		comboBox.addEventHandler(ComboBoxBase.ON_HIDDEN, event -> positionTracker.detach());
		comboBox.skinProperty().addListener((observable, oldSkin, newSkin) -> installPopupKeyFilter(newSkin, handler));
		installPopupKeyFilter(comboBox.getSkin(), handler);
	}

	static void configure(ComboBox<?> comboBox) {
		comboBox.skinProperty().addListener((observable, oldSkin, newSkin) -> configure(newSkin));
		configure(comboBox.getSkin());
	}

	private static void configure(Skin<?> skin) {
		if (skin instanceof ComboBoxListViewSkin<?> comboBoxSkin
				&& comboBoxSkin.getPopupContent() instanceof ListView<?> popup
				&& !popup.getStyleClass().contains("replace-blocks-builder-dropdown")) {
			popup.getStyleClass().add("replace-blocks-builder-dropdown");
		}
	}

	private static void installPopupKeyFilter(Skin<?> skin, EventHandler<KeyEvent> handler) {
		if (skin instanceof ComboBoxListViewSkin<?> comboBoxSkin
				&& comboBoxSkin.getPopupContent() instanceof ListView<?> popup) {
			popup.sceneProperty().addListener((observable, oldScene, newScene) -> {
				if (oldScene != null) oldScene.removeEventFilter(KeyEvent.KEY_PRESSED, handler);
				if (newScene != null) newScene.addEventFilter(KeyEvent.KEY_PRESSED, handler);
			});
			Scene scene = popup.getScene();
			if (scene != null) scene.addEventFilter(KeyEvent.KEY_PRESSED, handler);
		}
	}

	private static ListView<?> popupContent(ComboBox<?> comboBox) {
		if (comboBox != null && comboBox.getSkin() instanceof ComboBoxListViewSkin<?> skin
				&& skin.getPopupContent() instanceof ListView<?> popup) {
			return popup;
		}
		return null;
	}

	private static final class PopupPositionTracker {
		private static final double EPSILON = 0.5;
		private static final double WINDOW_MARGIN = 4;
		private static final int MAX_STABILIZATION_PASSES = 4;
		private final ComboBox<?> comboBox;
		private final InvalidationListener geometryListener = observable -> stabilize();
		private ListView<?> popupContent;
		private Window popupWindow;
		private Window ownerWindow;
		private boolean stabilizing;
		private boolean stabilizationPending;

		private PopupPositionTracker(ComboBox<?> comboBox) {
			this.comboBox = comboBox;
		}

		private void attach() {
			detach();
			popupContent = popupContent(comboBox);
			if (popupContent == null || popupContent.getScene() == null) {
				popupContent = null;
				return;
			}
			popupWindow = popupContent.getScene().getWindow();
			if (popupWindow == null) {
				popupContent = null;
				return;
			}
			ownerWindow = comboBox.getScene() == null ? null : comboBox.getScene().getWindow();
			popupContent.heightProperty().addListener(geometryListener);
			popupContent.widthProperty().addListener(geometryListener);
			popupWindow.heightProperty().addListener(geometryListener);
			popupWindow.widthProperty().addListener(geometryListener);
			popupWindow.xProperty().addListener(geometryListener);
			popupWindow.yProperty().addListener(geometryListener);
			if (ownerWindow != null) {
				ownerWindow.xProperty().addListener(geometryListener);
				ownerWindow.yProperty().addListener(geometryListener);
				ownerWindow.widthProperty().addListener(geometryListener);
			}
			stabilize();
		}

		private void detach() {
			if (popupContent != null) popupContent.heightProperty().removeListener(geometryListener);
			if (popupContent != null) popupContent.widthProperty().removeListener(geometryListener);
			if (popupWindow != null) {
				popupWindow.heightProperty().removeListener(geometryListener);
				popupWindow.widthProperty().removeListener(geometryListener);
				popupWindow.xProperty().removeListener(geometryListener);
				popupWindow.yProperty().removeListener(geometryListener);
			}
			if (ownerWindow != null) {
				ownerWindow.xProperty().removeListener(geometryListener);
				ownerWindow.yProperty().removeListener(geometryListener);
				ownerWindow.widthProperty().removeListener(geometryListener);
			}
			popupContent = null;
			popupWindow = null;
			ownerWindow = null;
			stabilizationPending = false;
		}

		private void stabilize() {
			if (stabilizing) {
				stabilizationPending = true;
				return;
			}
			int remainingPasses = MAX_STABILIZATION_PASSES;
			do {
				stabilizationPending = false;
				if (popupWindow == null || popupContent == null || !comboBox.isShowing()) return;
				Bounds comboBounds = comboBox.localToScreen(comboBox.getBoundsInLocal());
				if (comboBounds == null) return;
				stabilizeHorizontal(comboBounds);
				Bounds popupBounds = popupContent.localToScreen(popupContent.getLayoutBounds());
				if (popupBounds == null) continue;
				double correction = popupBounds.getCenterY() < comboBounds.getCenterY()
						? comboBounds.getMinY() - popupBounds.getMaxY()
						: comboBounds.getMaxY() - popupBounds.getMinY();
				if (Math.abs(correction) <= EPSILON) continue;
				stabilizing = true;
				try {
					popupWindow.setY(popupWindow.getY() + correction);
				} finally {
					stabilizing = false;
				}
			} while (stabilizationPending && --remainingPasses > 0);
		}

		private void stabilizeHorizontal(Bounds comboBounds) {
			if (ownerWindow == null || ownerWindow.getWidth() <= 0 || popupWindow.getWidth() <= 0) return;
			HorizontalPopupBounds constrained = constrainHorizontalBounds(
					popupWindow.getX(), popupWindow.getWidth(), comboBounds.getMinX(), comboBounds.getWidth(),
					ownerWindow.getX(), ownerWindow.getWidth(), WINDOW_MARGIN);
			if (Math.abs(constrained.width() - popupWindow.getWidth()) <= EPSILON
					&& Math.abs(constrained.x() - popupWindow.getX()) <= EPSILON) return;
			stabilizing = true;
			try {
				if (Math.abs(constrained.width() - popupWindow.getWidth()) > EPSILON) {
					popupWindow.setWidth(constrained.width());
				}
				if (Math.abs(constrained.x() - popupWindow.getX()) > EPSILON) {
					popupWindow.setX(constrained.x());
				}
			} finally {
				stabilizing = false;
			}
		}
	}

	private record VisibleRange(int first, int last) {}
}
