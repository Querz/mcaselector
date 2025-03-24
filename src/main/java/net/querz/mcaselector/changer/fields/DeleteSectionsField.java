package net.querz.mcaselector.changer.fields;

import net.querz.mcaselector.changer.Field;
import net.querz.mcaselector.changer.FieldType;
import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.util.range.Range;
import net.querz.mcaselector.util.range.RangeParser;
import net.querz.mcaselector.version.*;
import java.util.List;
import java.util.StringJoiner;

public class DeleteSectionsField extends Field<List<Range>> {

	public DeleteSectionsField() {
		super(FieldType.DELETE_SECTIONS);
	}

	@Override
	public List<Range> getOldValue(ChunkData data) {
		return null;
	}

	@Override
	public boolean parseNewValue(String s) {
		if (getNewValue() != null) {
			getNewValue().clear();
		}

		setNewValue(RangeParser.parseRanges(s, ","));
		if (getNewValue() != null && !getNewValue().isEmpty()) {
			return true;
		}
		return super.parseNewValue(s);
	}

	@Override
	public void change(ChunkData data) {
		VersionHandler.getImpl(data, ChunkFilter.Sections.class).deleteSections(data, getNewValue());
		VersionHandler.getImpl(data, ChunkFilter.Entities.class).deleteEntities(data, getNewValue());
		ChunkFilter.Heightmap heightmap = VersionHandler.getImpl(data, ChunkFilter.Heightmap.class);
		heightmap.worldSurface(data);
		heightmap.oceanFloor(data);
		heightmap.motionBlocking(data);
		heightmap.motionBlockingNoLeaves(data);
	}

	@Override
	public void force(ChunkData data) {
		change(data);
	}

	@Override
	public String toString() {
		if (getNewValue().size() == 1 && getNewValue().get(0).isMaxRange()) {
			return getType().toString() + " = true";
		}
		StringJoiner sj = new StringJoiner(", ");
		getNewValue().forEach(r -> sj.add(r.toString()));
		return getType().toString() + " = \"" + sj + "\"";
	}

	@Override
	public String valueToString() {
		if (getNewValue().size() == 1 && getNewValue().get(0).isMaxRange()) {
			return "true";
		}
		StringJoiner sj = new StringJoiner(", ");
		getNewValue().forEach(r -> sj.add(r.toString()));
		return sj.toString();
	}
}
