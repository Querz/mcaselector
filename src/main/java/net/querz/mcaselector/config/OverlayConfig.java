package net.querz.mcaselector.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.querz.mcaselector.config.adapter.OverlayAdapter;
import net.querz.mcaselector.logging.GsonNamingStrategy;
import net.querz.mcaselector.overlay.Overlay;
import java.util.Arrays;
import java.util.List;

public class OverlayConfig extends Config {

	private static final Gson gsonInstance;

	static {
		GsonBuilder builder = new GsonBuilder();
		builder.registerTypeHierarchyAdapter(Overlay.class, new OverlayAdapter());
		gsonInstance = builder.create();
	}

	private List<Overlay> overlays = null;

	public void setOverlays(List<Overlay> overlays) {
		this.overlays = overlays;
	}

	public List<Overlay> getOverlays() {
		return overlays;
	}

	@Override
	public void save() {
		save(gsonInstance, BASE_OVERLAYS_FILE);
	}

	@Override
	protected String save(Gson gson) {
		return gson.toJson(overlays);
	}

	public static OverlayConfig load() {
		String json = loadString(BASE_OVERLAYS_FILE);
		if (json == null) {
			return new OverlayConfig();
		}
		Overlay[] overlays = gsonInstance.fromJson(json, Overlay[].class);
		OverlayConfig cfg = new OverlayConfig();
		cfg.overlays = Arrays.asList(overlays);
		return cfg;
	}

	private static final Gson toStringGsonInstance;

	static {
		GsonBuilder builder = new GsonBuilder();
		builder.serializeNulls();
		builder.setFieldNamingStrategy(new GsonNamingStrategy());
		builder.registerTypeAdapter(Overlay.class, new OverlayAdapter());
		toStringGsonInstance = builder.create();
	}

	@Override
	public String toString() {
		return toStringGsonInstance.toJson(overlays);
	}
}
