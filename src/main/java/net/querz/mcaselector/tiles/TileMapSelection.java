package net.querz.mcaselector.tiles;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;

public class TileMapSelection implements Transferable, ClipboardOwner {

	public static final DataFlavor selectionDataFlavor = new DataFlavor(Selection.class, "MCA Selector selection");
	private Selection selection;

	public TileMapSelection(Selection selection) {
		this.selection = selection;
	}

	@Override
	public DataFlavor[] getTransferDataFlavors() {
		return new DataFlavor[] {selectionDataFlavor};
	}

	@Override
	public boolean isDataFlavorSupported(DataFlavor flavor) {
		return selectionDataFlavor.equals(flavor);
	}

	@Override
	public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
		if (isDataFlavorSupported(flavor)) {
			return selection;
		}
		throw new UnsupportedFlavorException(selectionDataFlavor);
	}

	@Override
	public void lostOwnership(Clipboard clipboard, Transferable contents) {
		System.out.println("Selection lost ownership");
	}
}
