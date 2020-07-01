package net.querz.mcaselector.tiles;

import net.querz.mcaselector.debug.Debug;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;

public class TileMapSelection implements Transferable, ClipboardOwner {

	public static final DataFlavor SELECTION_DATA_FLAVOR = new DataFlavor(Selection.class, "MCA Selector selection");
	private final Selection selection;

	public TileMapSelection(Selection selection) {
		this.selection = selection;
	}

	@Override
	public DataFlavor[] getTransferDataFlavors() {
		return new DataFlavor[] {SELECTION_DATA_FLAVOR};
	}

	@Override
	public boolean isDataFlavorSupported(DataFlavor flavor) {
		return SELECTION_DATA_FLAVOR.equals(flavor);
	}

	@Override
	public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
		if (isDataFlavorSupported(flavor)) {
			return selection;
		}
		throw new UnsupportedFlavorException(SELECTION_DATA_FLAVOR);
	}

	@Override
	public void lostOwnership(Clipboard clipboard, Transferable contents) {
		Debug.dump("Selection lost ownership");
	}
}
