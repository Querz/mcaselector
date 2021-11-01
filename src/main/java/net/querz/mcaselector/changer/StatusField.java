package net.querz.mcaselector.changer;

import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.filter.BiomeFilter;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.VersionController;
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
		ChunkFilter chunkFilter = VersionController.getChunkFilter(data.getRegion().getData().getInt("DataVersion"));
		StringTag status = chunkFilter.getStatus(data.getRegion().getData());
		return status == null ? null : status.getValue();
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
		ChunkFilter chunkFilter = VersionController.getChunkFilter(data.getRegion().getData().getInt("DataVersion"));
		StringTag tag = chunkFilter.getStatus(data.getRegion().getData());
		if (tag != null) {
			tag.setValue(getNewValue());
		}
	}

	@Override
	public void force(ChunkData data) {
		ChunkFilter chunkFilter = VersionController.getChunkFilter(data.getRegion().getData().getInt("DataVersion"));
		chunkFilter.setStatus(data.getRegion().getData(), getNewValue());
	}
}
