package net.querz.mcaselector.ui.component.filter;

import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import net.querz.mcaselector.filter.TextFilter;
import java.io.File;

public class FileFilterBox extends TextFilterBox {

	public FileFilterBox(FilterBox parent, TextFilter<?> filter, boolean root) {
		super(parent, filter, root);
		input.setOnDragOver(this::onDragOver);
		input.setOnDragDropped(this::onDragDropped);
	}

	private void onDragDropped(DragEvent event) {
		Dragboard db = event.getDragboard();
		if (db.hasFiles()) {
			for (File file : db.getFiles()) {
				if (file.exists()) {
					setText(file.getAbsolutePath());
					break;
				}
			}
			event.setDropCompleted(true);
		}
		event.consume();
	}

	private void onDragOver(DragEvent event) {
		if (event.getGestureSource() != this && event.getDragboard().hasFiles()) {
			Dragboard db = event.getDragboard();
			if (db.hasFiles()) {
				for (File file : db.getFiles()) {
					if (file.exists()) {
						event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
						event.consume();
						break;
					}
				}
			}
		}
	}
}
