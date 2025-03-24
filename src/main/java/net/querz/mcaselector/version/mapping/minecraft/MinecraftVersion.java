package net.querz.mcaselector.version.mapping.minecraft;

import com.google.gson.annotations.SerializedName;

public record MinecraftVersion (
		@SerializedName("id") String id,
		@SerializedName("releaseTime") String releaseTime,
		@SerializedName("time") String time,
		@SerializedName("type") Type type,
		@SerializedName("url") String url) {

	public enum Type {
		@SerializedName("snapshot") SNAPSHOT,
		@SerializedName("release") RELEASE,
		@SerializedName("old_alpha") OLD_ALPHA,
		@SerializedName("old_beta") OLD_BETA
	}

	public boolean before(MinecraftVersion other) {
		return releaseTime.compareTo(other.releaseTime) < 0;
	}
}
