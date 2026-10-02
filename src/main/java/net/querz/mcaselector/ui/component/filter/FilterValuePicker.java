package net.querz.mcaselector.ui.component.filter;

import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.event.EventDispatcher;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Skin;
import javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;

class FilterValuePicker extends ComboBox<String> {

	private static final PseudoClass keyboardNavigation = PseudoClass.getPseudoClass("keyboard-navigation");

	private ListView<?> suggestions;
	private int suggestionIndex = -1;
	private boolean outsideSuggestions;
	private boolean advancingFocus;

	FilterValuePicker() {
		EventDispatcher dispatcher = getEventDispatcher();
		setEventDispatcher((event, tail) -> {
			if (event instanceof KeyEvent key && handleKey(key)) {
				return null;
			}
			return dispatcher.dispatchEvent(event, tail);
		});
		getEditor().textProperty().addListener((observable, oldText, text) -> clearSuggestionFocus());
		showingProperty().addListener((observable, wasShowing, showing) -> {
			if (showing) {
				clearSuggestionFocus();
			} else {
				boolean restoreFocus = suggestionIndex >= 0 && !outsideSuggestions;
				Scene scene = getScene();
				Node focusOwner = scene == null ? null : scene.getFocusOwner();
				outsideSuggestions = false;
				clearSuggestionFocus();
				if (restoreFocus && scene != null) {
					Platform.runLater(() -> {
						if (getScene() == scene && scene.getWindow() != null && scene.getWindow().isFocused()
								&& (scene.getFocusOwner() == focusOwner || scene.getFocusOwner() == null)) {
							requestFocus();
						}
					});
				}
			}
		});
	}

	@Override
	protected Skin<?> createDefaultSkin() {
		ComboBoxListViewSkin<String> skin = new ComboBoxListViewSkin<>(this);
		suggestions = (ListView<?>) skin.getPopupContent();
		suggestions.getStyleClass().add("filter-value-suggestions");
		suggestions.sceneProperty().addListener((observable, oldScene, scene) -> {
			if (scene != null) {
				EventDispatcher dispatcher = scene.getEventDispatcher();
				scene.setEventDispatcher((event, tail) -> {
					if (event instanceof KeyEvent key) {
						if (advancingFocus) {
							return event;
						}
						if (outsideSuggestions) {
							return handleOutsideKey(key) ? null : event;
						}
						if (handleKey(key)) {
							return null;
						}
					}
					return dispatcher.dispatchEvent(event, tail);
				});
			}
		});
		suggestions.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
			if (isSuggestion(event.getTarget())) {
				outsideSuggestions = false;
				clearSuggestionFocus();
				requestFocus();
			} else if (outsideSuggestions) {
				hide();
			}
		});
		suggestions.addEventFilter(ScrollEvent.SCROLL, event -> {
			if (outsideSuggestions) {
				hide();
			}
		});
		return skin;
	}

	@Override
	public void hide() {
		if (!advancingFocus) {
			super.hide();
		}
	}

	private boolean handleKey(KeyEvent event) {
		if (!isShowing() || advancingFocus || outsideSuggestions) {
			return false;
		}
		if (event.getCode() == KeyCode.ESCAPE) {
			if (event.getEventType() == KeyEvent.KEY_PRESSED) {
				hide();
				requestFocus();
			}
			return true;
		}
		if (suggestionIndex >= 0 && event.getCode() == KeyCode.ENTER) {
			if (event.getEventType() == KeyEvent.KEY_RELEASED) {
				suggestions.getSelectionModel().select(suggestionIndex);
				hide();
				requestFocus();
			}
			return true;
		}
		if (!isTab(event)) {
			return false;
		}
		if (event.getEventType() != KeyEvent.KEY_PRESSED) {
			return true;
		}
		if (getItems().isEmpty() || suggestionIndex < 0 && event.isShiftDown()) {
			hide();
			return false;
		}
		int next = suggestionIndex + (event.isShiftDown() ? -1 : 1);
		if (next < 0) {
			clearSuggestionFocus();
			requestFocus();
		} else if (next < getItems().size()) {
			focusSuggestion(next);
		} else {
			outsideSuggestions = true;
			clearSuggestionFocus();
			advancingFocus = true;
			try {
				fireEvent(event.copyFor(this, this));
			} finally {
				advancingFocus = false;
			}
		}
		return true;
	}

	private boolean handleOutsideKey(KeyEvent event) {
		if (event.getEventType() == KeyEvent.KEY_PRESSED && event.getCode() != KeyCode.SHIFT) {
			if (isTab(event) && event.isShiftDown()) {
				outsideSuggestions = false;
				requestFocus();
				focusSuggestion(getItems().size() - 1);
				return true;
			}
			hide();
		} else if (event.getEventType() == KeyEvent.KEY_TYPED && !"\t".equals(event.getCharacter())) {
			hide();
		}
		return false;
	}

	private boolean isSuggestion(Object target) {
		while (target instanceof Node node && node != suggestions) {
			if (node instanceof ListCell<?> cell) {
				return !cell.isEmpty();
			}
			target = node.getParent();
		}
		return false;
	}

	private boolean isTab(KeyEvent event) {
		return event.getCode() == KeyCode.TAB && !event.isControlDown()
				&& !event.isAltDown() && !event.isMetaDown();
	}

	private void focusSuggestion(int index) {
		suggestionIndex = index;
		suggestions.pseudoClassStateChanged(keyboardNavigation, index >= 0);
		suggestions.getFocusModel().focus(index);
		suggestions.scrollTo(index);
	}

	private void clearSuggestionFocus() {
		suggestionIndex = -1;
		if (suggestions != null) {
			suggestions.pseudoClassStateChanged(keyboardNavigation, false);
			suggestions.getFocusModel().focus(-1);
		}
	}
}
