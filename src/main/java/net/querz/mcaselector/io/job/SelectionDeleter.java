package net.querz.mcaselector.io.job;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.io.JobHandler;
import net.querz.mcaselector.io.RegionDirectories;
import net.querz.mcaselector.io.SelectionData;
import net.querz.mcaselector.io.SelectionHelper;
import net.querz.mcaselector.io.mca.Region;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.progress.Progress;
import net.querz.mcaselector.progress.Timer;

public final class SelectionDeleter {

	private SelectionDeleter() {}

	public static void deleteSelection(SelectionData selection, Progress progressChannel) {
		if (selection.selection().isEmpty() && !selection.inverted()) {
			progressChannel.done("no selection");
			return;
		}

		JobHandler.clearQueues();

		progressChannel.setMessage("preparing");

		Long2ObjectOpenHashMap<LongOpenHashSet> sel = SelectionHelper.getTrueSelection(selection);

		progressChannel.setMax(sel.size());

		Point2i first = new Point2i(sel.long2ObjectEntrySet().iterator().next().getLongKey());

		progressChannel.updateProgress(FileHelper.createMCAFileName(first), 0);

		for (Long2ObjectMap.Entry<LongOpenHashSet> entry : sel.long2ObjectEntrySet()) {
			JobHandler.addJob(new MCADeleteSelectionProcessJob(FileHelper.createRegionDirectories(new Point2i(entry.getLongKey())), entry.getValue(), progressChannel));
		}
	}

	private static class MCADeleteSelectionProcessJob extends ProcessDataJob {

		private final Progress progressChannel;
		private final LongOpenHashSet selection;

		private MCADeleteSelectionProcessJob(RegionDirectories dirs, LongOpenHashSet selection, Progress progressChannel) {
			super(dirs, PRIORITY_LOW);
			this.selection = selection;
			this.progressChannel = progressChannel;
		}

		@Override
		public boolean execute() {
			// delete whole files if everything is selected
			if (selection == null) {
				// delete region
				if (getRegionDirectories().getRegion().delete()) {
					Debug.dumpf("deleted file %s", getRegionDirectories().getRegion());
				} else {
					Debug.errorf("failed to delete file %s", getRegionDirectories().getRegion());
				}

				// delete poi
				if (getRegionDirectories().getPoi().delete()) {
					Debug.dumpf("deleted file %s", getRegionDirectories().getPoi());
				} else {
					Debug.errorf("failed to delete file %s", getRegionDirectories().getPoi());
				}

				// delete entities
				if (getRegionDirectories().getEntities().delete()) {
					Debug.dumpf("deleted file %s", getRegionDirectories().getEntities());
				} else {
					Debug.errorf("failed to delete file %s", getRegionDirectories().getEntities());
				}

				progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
				return true;
			}

			byte[] regionData = loadRegionHeader();
			byte[] poiData = loadPoiHeader();
			byte[] entitiesData = loadEntitiesHeader();

			if (regionData == null && poiData == null && entitiesData == null) {
				Debug.errorf("failed to load any data from %s", getRegionDirectories().getLocationAsFileName());
				progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
				return true;
			}

			// load MCAFile
			try {
				// only load headers, we don't care for chunk contents
				Region region = Region.loadRegionHeaders(getRegionDirectories(), regionData, poiData, entitiesData);

				region.deleteChunks(selection);

				JobHandler.executeSaveData(new MCADeleteSelectionSaveJob(getRegionDirectories(), region, progressChannel));
				return false;

			} catch (Exception ex) {
				progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
				Debug.errorf("error deleting chunk indices in %s", getRegionDirectories().getLocationAsFileName());
			}
			return true;
		}
	}

	private static class MCADeleteSelectionSaveJob extends SaveDataJob<Region> {

		private final Progress progressChannel;

		private MCADeleteSelectionSaveJob(RegionDirectories dirs, Region region, Progress progressChannel) {
			super(dirs, region);
			this.progressChannel = progressChannel;
		}

		@Override
		public void execute() {
			Timer t = new Timer();
			try {
				getData().deFragment();
			} catch (Exception ex) {
				Debug.dumpException("failed to delete selected chunks from " + getRegionDirectories().getLocationAsFileName(), ex);
			}
			progressChannel.incrementProgress(getRegionDirectories().getLocationAsFileName());
			Debug.dumpf("took %s to save data for %s", t, getRegionDirectories().getLocationAsFileName());
		}
	}
}
