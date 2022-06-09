package net.querz.mcaselector.selection;

import java.awt.datatransfer.*;

public record ClipboardSelection(SelectionData data) implements Transferable, ClipboardOwner {

	public static final DataFlavor SELECTION_DATA_FLAVOR = new DataFlavor(SelectionData.class, "MCA Selector selection");

	@Override
	public DataFlavor[] getTransferDataFlavors() {
		return new DataFlavor[]{SELECTION_DATA_FLAVOR};
	}

	@Override
	public boolean isDataFlavorSupported(DataFlavor flavor) {
		return SELECTION_DATA_FLAVOR.equals(flavor);
	}

	@Override
	public SelectionData getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
		if (isDataFlavorSupported(flavor)) {
			return data;
		}
		throw new UnsupportedFlavorException(SELECTION_DATA_FLAVOR);
	}

	@Override
	public void lostOwnership(Clipboard clipboard, Transferable contents) {}
}
