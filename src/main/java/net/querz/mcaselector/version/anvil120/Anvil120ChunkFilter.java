package net.querz.mcaselector.version.anvil120;

import net.querz.mcaselector.io.registry.StatusRegistry;
import net.querz.mcaselector.version.Helper;
import net.querz.mcaselector.version.anvil119.Anvil119ChunkFilter;
import net.querz.nbt.*;

public class Anvil120ChunkFilter extends Anvil119ChunkFilter {

	@Override
	public StringTag getStatus(CompoundTag data) {
		return Helper.tagFromCompound(data, "Status");
	}

	@Override
	public void setStatus(CompoundTag data, StatusRegistry.StatusIdentifier status) {
		if (data != null) {
			data.putString("Status", status.getStatusWithNamespace());
		}
	}

	@Override
	public boolean matchStatus(CompoundTag data, StatusRegistry.StatusIdentifier status) {
		StringTag tag = getStatus(data);
		if (tag == null) {
			return false;
		}
		return status.getStatusWithNamespace().equals(tag.getValue());
	}
}
