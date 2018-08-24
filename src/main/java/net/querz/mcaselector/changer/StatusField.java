package net.querz.mcaselector.changer;

import net.querz.nbt.CompoundTag;
import net.querz.nbt.StringTag;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class StatusField extends Field<String> {

	private final static Set<String> validStatus = new HashSet<>();

	static {
		validStatus.addAll(Arrays.asList("empty", "base", "carved", "liquid_carved", "decorated", "lighted", "mobs_spawned", "finalized", "fullchunk", "postprocessed"));
	}

	public StatusField() {
		super(FieldType.STATUS);
	}

	@Override
	public boolean parseNewValue(String s) {
		if (validStatus.contains(s)) {
			setNewValue(s);
			return true;
		}
		return super.parseNewValue(s);
	}

	@Override
	public void change(CompoundTag root) {
		StringTag tag = root.getCompoundTag("Level").getStringTag("Status");
		if (tag != null) {
			tag.setValue(getNewValue());
		}
	}

	@Override
	public void force(CompoundTag root) {
		root.getCompoundTag("Level").putString("Status", getNewValue());
	}
}
