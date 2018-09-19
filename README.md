# MCA Selector
#### An external tool to export or delete selected chunks and regions from a Minecraft world.
---

## Usage
### Navigation
Executing the tool, it shows an empty window with a chunk and a region grid. To actually show a world, open a folder containing Minecraft Anvil (\*.mca) files. The tool will then render a top-down view of this world that you can zoom into and zoom out of by scrolling up and down and that you can move around using the middle mouse button (`Cmd+LMB` on Mac OS).

![alt text](https://raw.githubusercontent.com/Querz/mcaselector/assets/assets/mca_selector_default.png "MCA Selector window showing chunk and region grid")

Zooming out far enough disables the selection of single chunks but lets you select entire regions.

### Selections
Upon finishing selecting chunks and regions, they can be deleted or exported using the `Selection`-menu. Exported chunks and regions are not deleted from the original world.

![alt text](https://raw.githubusercontent.com/Querz/mcaselector/assets/assets/mca_selector_selections.png "MCA Selector window showing chunk and region selection export")

A selection (not the chunks and regions themselves) can also be exported or imported and even be applied to different worlds.

### Chunk filter
The MCA Selector also contains a powerful tool to delete or export chunks and regions by conditions like the data version, the time it was last updated, how much time players have spent in this chunk and some more. Multiple of these conditions can be used to create a very specific query describing what chunks and regions should be deleted or exported.

![alt text](https://raw.githubusercontent.com/Querz/mcaselector/assets/assets/mca_selector_filter_chunks.png "MCA Selector window showing the chunk filter")

Because the conditions use internal values used by Minecraft, the following table gives a brief explanation on what they do:

| Condition | Type | Description |
| --------- | ----- | ----------- |
| Group | - | Groups multiple conditions. |
| DataVersion | int | The DataVersion tag of the chunk. 100-1343 for 1.12.2 and below, 1444 for 1.13 snapshots and above. |
| InhabitedTime | long | The total amount of time in game-ticks players have spent in that chunk. 1 second ~20 ticks. Also accepts a duration string such as `1 year 2 months 3 days 4 hours 5 minutes 6 seconds`. |
| LastUpdate | int | The time a chunk was last updated in seconds since 1970-01-01. Also accepts a timestamp in the `yyyy-MM-dd HH-mm-ss`-format such as `2018-01-02 15:03:04`. If the time is omitted, it will default to `00:00:00`. |
| xPos | int | The location of the chunk on the x-axis in chunk coordinates. |
| zPos | int | The location of the chunk on the z-axis in chunk coordinates. |
| Palette | String | A list of comma (,) separated 1.13 block names. The block names will be converted to block ids for chunks with DataVersion 1343 or below. The validation of block names can be skipped by writing them in double quotes ("). Example: `sand,"new_block",gravel`.|
| Status | String | The status of the chunk generation. Only recognized by Minecraft 1.13+ (DataVersion 1444+) |
| LightPopulated | byte | Whether the light levels for the chunk have been calculated. If this is set to 0, converting a world from 1.12.x to 1.13 will omit that chunk. Allowed values are `0` and `1`. |

**Notice**
Running the query will check the `.mca`-file's name first if the query would even apply to any chunk in this file using the xPos and zPos conditions, as long as the query is built in a way that allows doing this.

### NBT Changer
The NBT Changer modifies the world files directly by changing specific values.

You can change the following values:

| Field | Type | Description |
| ----- | ----- | ----------- |
| LightPopulated | byte | Whether the light levels for the chunk have been calculated. If this is set to 0, converting a world from 1.12.x to 1.13 will omit that chunk. Allowed values are `0` and `1` |
| DataVersion | int | Allowes to change the DataVersion of the chunks. Should be used with extreme care. |
| InhabitedTime | long | This field stores the amount of game-ticks players have spent in a chunk. Impacts the local difficulty. |
| LastUpdate | long | Stores a timestamp when this chunk was last updated in Milliseconds. |
| Status | String | The status of the chunk generation. Only recognized by Minecraft 1.13+ (DataVersion 1444+) |

For more information about the fields have a look at the chunk format description on [Minecraft Wiki](https://minecraft.gamepedia.com/Chunk_format)

### Caching
The tool creates an image for each region from the provided mca-files. These images are saved separately inside a `cache`-folder in the working directory of the program for future usage. Experience showed that a Minecraft world with a size of 10GB resulted in cached image files with a total size of 80-100MB. Caching as many regions as possible significantly improves loading times though.

### Debugging
If something is not working properly or if you want to see the exact query that is run using the chunk filter, the MCA Selector can be executed with `-debug` to print some additional information to the console.

---
## Supported Versions
The MCA Selector currently supports the following Minecraft versions:

| Minecraft Version | DataVersion | Supported |
| ----------------- | ----------- | :-------: |
| Beta 1.3 - 1.12.2 | 100 - 1343  | Yes       |
| 1.13              | 1444 - ?    | Yes       |

---

## Download and installation

[**Download Version 1.3.1**](https://github.com/Querz/mcaselector/releases/download/1.3.1/mcaselector-1.3.1-all.jar)

"Requirements":
* JRE 8+, you can get it from [HERE](http://www.oracle.com/technetwork/java/javase/downloads/jre8-downloads-2133155.html)
* A computer
* A brain

If `.jar` files are associated with java on your computer, it should launch by simply double-clicking the file (or however your OS is configured to open files using your mouse or keyboard). If not, you can try `java -jar mcaselector-1.3.1.jar` from your console. If this still doesn't work, apply "A brain" that you providently helt ready after having read the "Requirements" section carefully.
