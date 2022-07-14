package net.querz.mcaselector.changer.fields;

import net.querz.mcaselector.changer.Field;
import net.querz.mcaselector.changer.FieldType;
import net.querz.mcaselector.filter.filters.BiomeFilter;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.version.ChunkFilter;
import net.querz.mcaselector.version.VersionController;
import net.querz.nbt.StringTag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class StatusField extends Field<String> {

	private static final Logger LOGGER = LogManager.getLogger(StatusField.class);

	private static final Set<String> validStatus = new HashSet<>();

	static {
		try (BufferedReader bis = new BufferedReader(
				new InputStreamReader(Objects.requireNonNull(BiomeFilter.class.getClassLoader().getResourceAsStream("mapping/all_status.txt"))))) {
			String line;
			while ((line = bis.readLine()) != null) {
				validStatus.add(line);
			}
		} catch (IOException ex) {
			LOGGER.error("error reading mapping/all_status.txt for StatusFilter", ex);
		}
	}

	public StatusField() {
		super(FieldType.STATUS);
	}

	@Override
	public String getOldValue(ChunkData data) {
		ChunkFilter chunkFilter = VersionController.getChunkFilter(data.getDataVersion());
		StringTag status = chunkFilter.getStatus(data.region().getData());
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
		ChunkFilter chunkFilter = VersionController.getChunkFilter(data.getDataVersion());
		StringTag tag = chunkFilter.getStatus(data.region().getData());
		if (tag != null) {
			chunkFilter.setStatus(data.region().getData(), getNewValue());
		}
	}

	@Override
	public void force(ChunkData data) {
		ChunkFilter chunkFilter = VersionController.getChunkFilter(data.getDataVersion());
		chunkFilter.setStatus(data.region().getData(), getNewValue());
	}
}
