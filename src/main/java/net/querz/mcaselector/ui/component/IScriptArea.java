package net.querz.mcaselector.ui.component;

import javafx.scene.Node;
import javafx.beans.value.ObservableValue;

/**
 * Minimal interface to abstract the script editor area, allowing
 * a graceful fallback when RichTextFX is not available or incompatible.
 */
public interface IScriptArea {
    void setChangeListener(Runnable listener);
    void setText(String text);
    String getText();
    ObservableValue<String> errorProperty();
    Node getNode();
}