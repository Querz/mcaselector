package net.querz.mcaselector.ui.dialog;

import net.querz.mcaselector.config.GlobalConfig;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ReplaceBlocksPresetRepositoryTest {

	@Test
	void rollsBackSaveWhenConfigWriteFails() {
		List<GlobalConfig.ReplaceBlocksUserPreset> presets = new ArrayList<>(List.of(
				new GlobalConfig.ReplaceBlocksUserPreset("Existing", "stone=dirt")));
		ReplaceBlocksPresetRepository repository = new ReplaceBlocksPresetRepository(presets, () -> false);

		assertFalse(repository.save("Existing", "stone=air"));
		assertEquals(List.of(new GlobalConfig.ReplaceBlocksUserPreset("Existing", "stone=dirt")), presets);
	}

	@Test
	void rollsBackDeleteWhenConfigWriteFails() {
		List<GlobalConfig.ReplaceBlocksUserPreset> presets = new ArrayList<>(List.of(
				new GlobalConfig.ReplaceBlocksUserPreset("Existing", "stone=dirt")));
		ReplaceBlocksPresetRepository repository = new ReplaceBlocksPresetRepository(presets, () -> false);

		assertFalse(repository.delete("Existing"));
		assertEquals(1, presets.size());
		assertEquals("Existing", presets.getFirst().name());
	}

	@Test
	void rollsBackWhenConfigWriterThrows() {
		List<GlobalConfig.ReplaceBlocksUserPreset> presets = new ArrayList<>();
		ReplaceBlocksPresetRepository repository = new ReplaceBlocksPresetRepository(presets, () -> {
			throw new IllegalStateException("expected");
		});

		assertFalse(repository.save("New", "stone=dirt"));
		assertTrue(presets.isEmpty());
	}
}
