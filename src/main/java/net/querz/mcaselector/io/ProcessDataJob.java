package net.querz.mcaselector.io;

public abstract class ProcessDataJob extends Job {

	private final byte[] regionData, poiData, entitiesData;

	public ProcessDataJob(RegionDirectories dirs, byte[] regionData, byte[] poiData, byte[] entitiesData) {
		super(dirs);
		this.regionData = regionData;
		this.poiData = poiData;
		this.entitiesData = entitiesData;
	}

	public byte[] getRegionData() {
		return regionData;
	}

	public byte[] getPoiData() {
		return poiData;
	}

	public byte[] getEntitiesData() {
		return entitiesData;
	}

	@Override
	public void run() {
		MCAFilePipe.refillDataLoadExecutorQueue();
		execute();
	}

	public abstract void execute();
}