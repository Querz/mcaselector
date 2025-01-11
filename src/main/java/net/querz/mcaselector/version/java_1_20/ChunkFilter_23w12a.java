package net.querz.mcaselector.version.java_1_20;

import net.querz.mcaselector.io.mca.ChunkData;
import net.querz.mcaselector.io.registry.StatusRegistry;
import net.querz.mcaselector.version.Helper;
import net.querz.mcaselector.version.MCVersionImplementation;
import net.querz.mcaselector.version.java_1_18.ChunkFilter_21w43a;
import net.querz.nbt.*;

public class ChunkFilter_23w12a {

	@MCVersionImplementation(3207)
	public static class Status extends ChunkFilter_21w43a.Status {

		@Override
		public void setStatus(ChunkData data, StatusRegistry.StatusIdentifier status) {
			if (Helper.getRegion(data) != null) {
				Helper.getRegion(data).putString("Status", status.getStatusWithNamespace());
			}
		}

		@Override
		public boolean matchStatus(ChunkData data, StatusRegistry.StatusIdentifier status) {
			StringTag tag = getStatus(data);
			if (tag == null) {
				return false;
			}
			return status.getStatusWithNamespace().equals(tag.getValue());
		}
	}
}
