package net.querz.mcaselector.changer;

import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.filter.BiomeFilter;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.validation.ValidationHelper;
import net.querz.nbt.tag.StringTag;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class StatusField extends Field<String> {

	private static final Set<String> validStatus = new HashSet<>();

	static {
		try (BufferedReader bis = new BufferedReader(
				new InputStreamReader(Objects.requireNonNull(BiomeFilter.class.getClassLoader().getResourceAsStream("mapping/all_status.txt"))))) {
			String line;
			while ((line = bis.readLine()) != null) {
				validStatus.add(line);
			}
		} catch (IOException ex) {
			Debug.dumpException("error reading mapping/all_status.txt for StatusFilter", ex);
		}
	}

	public StatusField() {
		super(FieldType.STATUS);
	}

	@Override
	public String getOldValue(ChunkData data) {
		return ValidationHelper.withDefault(() -> data.getRegion().getData().getCompoundTag("Level").getString("Status"), null);
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
	public void change(ChunkData data) {
		StringTag tag = data.getRegion().getData().getCompoundTag("Level").getStringTag("Status");
		if (tag != null) {
			tag.setValue(getNewValue());
		}
	}

	@Override
	public void force(ChunkData data) {
		data.getRegion().getData().getCompoundTag("Level").putString("Status", getNewValue());
	}
}
