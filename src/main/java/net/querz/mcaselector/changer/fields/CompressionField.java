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
            if (t.toString().equalsIgnoreCase(s)) {
                this.setNewValue(t);
                return true;
            }
        }
        return super.parseNewValue(s);
    }

    @Override
    public void change(ChunkData root) {
        if (root.region() != null) {
            root.region().setCompressionType(getNewValue());
        }
        if (root.poi() != null) {
            root.poi().setCompressionType(getNewValue());
        }
        if (root.entities() != null) {
            root.entities().setCompressionType(getNewValue());
        }
    }

    @Override
    public void force(ChunkData root) {
        change(root);
    }
}
