package net.querz.mcaselector.changer.fields;

import net.querz.mcaselector.changer.Field;
import net.querz.mcaselector.changer.FieldType;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.io.mca.CompressionType;

public class CompressionField extends Field<CompressionType> {

    public CompressionField() {
        super(FieldType.COMPRESSION);
    }

    @Override
    public CompressionType getOldValue(ChunkData root) {
        return root.region().getCompressionType();
    }

    @Override
    public boolean parseNewValue(String s) {
        for (CompressionType t : CompressionType.values()) {
            if (t.toString().equalsIgnoreCase(s) || ("" + t.getByte()).equals(s)) {
                this.setNewValue(t);
                return true;
            }
        }
        return super.parseNewValue(s);
    }

    @Override
    public void change(ChunkData data) {
		force(data);
    }

    @Override
    public void force(ChunkData data) {
		if (data.region() != null) {
			data.region().setCompressionType(getNewValue());
		}
		if (data.poi() != null) {
			data.poi().setCompressionType(getNewValue());
		}
		if (data.entities() != null) {
			data.entities().setCompressionType(getNewValue());
		}
    }
}
