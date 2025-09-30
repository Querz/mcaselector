package net.querz.mcaselector.ui.component;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.TextArea;

/**
 * Plain TextArea-based fallback for environments where RichTextFX is incompatible.
 * Provides the minimal API used by CodeEditor.
 */
public class FallbackScriptArea implements IScriptArea {
    private final TextArea textArea = new TextArea();
    private final SimpleStringProperty error = new SimpleStringProperty("");
    private Runnable changeListener;

    public FallbackScriptArea() {
        textArea.textProperty().addListener((obs, oldV, newV) -> {
            if (changeListener != null) {
                changeListener.run();
            }
        });
    }

    @Override
    public void setChangeListener(Runnable listener) {
        this.changeListener = listener;
    }

    @Override
    public void setText(String text) {
        textArea.setText(text);
    }

    @Override
    public String getText() {
        return textArea.getText();
    }

    @Override
    public ObservableValue<String> errorProperty() {
        return error;
    }

    @Override
    public Node getNode() {
        return textArea;
    }
}