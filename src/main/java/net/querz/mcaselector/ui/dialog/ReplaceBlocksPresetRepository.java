package net.querz.mcaselector.ui.dialog;

import net.querz.mcaselector.config.GlobalConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

final class ReplaceBlocksPresetRepository {

	private final List<GlobalConfig.ReplaceBlocksUserPreset> presets;
	private final BooleanSupplier writer;

	ReplaceBlocksPresetRepository(List<GlobalConfig.ReplaceBlocksUserPreset> presets, BooleanSupplier writer) {
		this.presets = presets;
		this.writer = writer;
	}

	boolean save(String name, String value) {
		List<GlobalConfig.ReplaceBlocksUserPreset> snapshot = new ArrayList<>(presets);
		GlobalConfig.ReplaceBlocksUserPreset replacement = new GlobalConfig.ReplaceBlocksUserPreset(name, value);
		int existing = find(name);
		if (existing < 0) {
			presets.add(replacement);
		} else {
			presets.set(existing, replacement);
		}
		return persistOrRollback(snapshot);
	}

	boolean delete(String name) {
		List<GlobalConfig.ReplaceBlocksUserPreset> snapshot = new ArrayList<>(presets);
		presets.removeIf(preset -> preset != null && name.equals(preset.name()));
		return persistOrRollback(snapshot);
	}

	private int find(String name) {
		for (int i = 0; i < presets.size(); i++) {
			GlobalConfig.ReplaceBlocksUserPreset preset = presets.get(i);
			if (preset != null && name.equals(preset.name())) {
				return i;
			}
		}
		return -1;
	}

	private boolean persistOrRollback(List<GlobalConfig.ReplaceBlocksUserPreset> snapshot) {
		try {
			if (writer.getAsBoolean()) {
				return true;
			}
		} catch (RuntimeException ignored) {
			// The UI reports the failure after the in-memory state has been restored.
		}
		presets.clear();
		presets.addAll(snapshot);
		return false;
	}
}
