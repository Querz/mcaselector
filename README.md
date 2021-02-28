# MCA Selector
[![total downloads](https://img.shields.io/github/downloads/Querz/mcaselector/total.svg)](https://github.com/Querz/mcaselector/releases) [![paypal](https://img.shields.io/badge/donate-PayPal-green.svg)](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=3PV2GDWZL8HCA) [![chat](https://img.shields.io/discord/722924391805223113?logo=discord)](https://discord.gg/h942U8U)


#### An external tool to export or delete selected chunks and regions from a world save of Minecraft Java Edition.
---

<!--toc-start-->
* [Usage](#usage)
  * [Video Tutorials](#video-tutorials)
  * [Navigation](#navigation)
  * [Selections](#selections)
  * [Chunk filter](#chunk-filter)
  * [NBT Changer](#nbt-changer)
  * [Chunk Editor](#chunk-editor)
  * [Chunk import](#chunk-import)
  * [Copy and Paste](#copy-and-paste)
  * [Swapping chunks](#swapping-chunks)
  * [Caching](#caching)
  * [Generating an image](#generating-an-image)
  * [Debugging](#debugging)
* [Supported Versions](#supported-versions)
* [Headless mode](#headless-mode)
  * [Mandatory and optional parameters](#mandatory-and-optional-parameters)
    * [Create selection](#create-selection)
    * [Export chunks](#export-chunks)
    * [Import chunks](#import-chunks)
    * [Delete chunks](#delete-chunks)
    * [Change NBT](#change-nbt)
    * [Cache images](#cache-images)
    * [Generate image from selection](#generate-image-from-selection)
    * [Configuration parameters](#configuration-parameters)
  * [Filter query](#filter-query)
  * [Change values](#change-values)
* [Checkout and building](#checkout-and-building)
* [Translation](#translation)
* [Download and installation](#download-and-installation)
  * [What works on most systems](#what-works-on-most-systems)
  * [If you have Minecraft Java Edition installed](#if-you-have-minecraft-java-edition-installed)
  * [If you receive a JavaFX error](#if-you-receive-a-javafx-error)
<!--toc-end-->

---

## Usage
### Video Tutorials
For people who prefer watching a video to understand how the MCA Selector works, there some very good tutorials on Youtube explaining the basics:

* [How To Reset Chunks In Your Minecraft World!](https://www.youtube.com/watch?v=1xx8fwynlRs) by [Farzy](https://www.youtube.com/channel/UCVtz3s3FUxVxBgPl2OWtIJQ)
* [How To Clear Unwanted Chunks In Minecraft 1.16 | MCASelector Tutorial](https://www.youtube.com/watch?v=ADDTXGRJo20) by [Muriako](https://www.youtube.com/channel/UCpt-MjKkc5X4W7bUFV3Dwrw)
* [Preparing Your World for the Nether Update! - The Minecraft Survival Guide](https://www.youtube.com/watch?v=1fiyVvoD9jQ) starting at [2:52](https://www.youtube.com/watch?v=1fiyVvoD9jQ&t=2m52s) by [Pixlriffs](https://www.youtube.com/channel/UCgGjBqZZtAjxfpGSba7d6ww)
* [How To Reset The End Dimension! - The Minecraft Survival Guide](https://www.youtube.com/watch?v=p-2gFkJl_Lo) starting at [8:45](https://www.youtube.com/watch?v=p-2gFkJl_Lo&t=8m45s) by [Pixlriffs](https://www.youtube.com/channel/UCgGjBqZZtAjxfpGSba7d6ww)

### Navigation
Executing the tool, it shows an empty window with a chunk and a region grid. To actually show a world, open a folder containing Minecraft Anvil (\*.mca) files. The tool will then render a top-down view of this world that you can zoom into and zoom out of by scrolling up and down and that you can move around using the middle mouse button (`Cmd+LMB` on Mac OS) or using `WASD`.

<p align="center">
  <img src="https://gist.githubusercontent.com/Querz/5e08c4ab863c2ad8b5da146dc4188ecb/raw/88e1887eb441c1f0f60af6eb3dfcf7917b16544a/default.png" alt="MCA Selector window showing chunk and region grid">
</p>

Zooming out far enough disables the selection of single chunks but lets you select entire regions.

### Selections
Upon finishing selecting chunks and regions, they can be deleted or exported using the `Selection`-menu. Exported chunks and regions are not deleted from the original world.

<p align="center">
  <img src="https://gist.githubusercontent.com/Querz/5e08c4ab863c2ad8b5da146dc4188ecb/raw/88e1887eb441c1f0f60af6eb3dfcf7917b16544a/selections.png" alt="MCA Selector window showing chunk and region selection export">
</p>

A selection (not the chunks and regions themselves) can also be exported or imported and even be applied to different worlds.

### Chunk filter
The MCA Selector also contains a powerful tool to delete or export chunks and regions by conditions like the data version, the time it was last updated, how much time players have spent in this chunk and some more. Multiple of these conditions can be used to create a very specific query describing what chunks and regions should be deleted or exported.

<p align="center">
  <img src="https://gist.githubusercontent.com/Querz/5e08c4ab863c2ad8b5da146dc4188ecb/raw/88e1887eb441c1f0f60af6eb3dfcf7917b16544a/filter_chunks.png" alt="MCA Selector window showing the chunk filter">
</p>

Because the conditions use internal values used by Minecraft, the following table gives a brief explanation on what they do:

| Condition | Type | Description |
| --------- | ----- | ----------- |
| Group | - | Groups multiple conditions. |
| Not Group | - | A negated group. |
| DataVersion | int | The DataVersion tag of the chunk. 100-1343 for 1.12.2 and below, 1444 for 1.13 snapshots and above. |
| InhabitedTime | long | The total amount of time in game-ticks players have spent in that chunk. 1 second ~20 ticks. Also accepts a duration string such as `1 year 2 months 3 days 4 hours 5 minutes 6 seconds`. |
| LastUpdate | int | The time a chunk was last updated in seconds since 1970-01-01. Also accepts a timestamp in the `yyyy-MM-dd HH-mm-ss`-format such as `2018-01-02 15:03:04`. If the time is omitted, it will default to `00:00:00`. |
| xPos | int | The location of the chunk on the x-axis in chunk coordinates. |
| zPos | int | The location of the chunk on the z-axis in chunk coordinates. |
| Palette | String | A list of comma (,) separated 1.13 block names. The block names will be converted to block ids for chunks with DataVersion 1343 or below. The validation of block names can be skipped by writing them in single quotes ('). Example: `sand,'new_block',gravel`.|
| Status | String | The status of the chunk generation. Only recognized by Minecraft 1.13+ (DataVersion 1444+) |
| LightPopulated | byte | Whether the light levels for the chunk have been calculated. If this is set to 0, converting a world from 1.12.x to 1.13 will omit that chunk. Allowed values are `0` and `1`. |
| Biome | String/int | One or multiple biome names and IDs, separated by comma (,). For a reference of biome names and IDs, have a look at the [Wiki](https://minecraft.gamepedia.com/Java_Edition_data_values#Biomes). Custom biomes can be specified by using single quotes (') around a biome ID. |
| Entities | String | One or multiple entity names, separated by comma (,). For a reference of entity names, have a look at the [Wiki](https://minecraft.gamepedia.com/Java_Edition_data_values#Entities). Custom entities are supported, though they must be declared in single quotes (') and with their namespace id. |
| Structures | String | One or multiple structure names, separated by comma (,). A list of all valid structure names can be found [here](https://github.com/Querz/mcaselector/blob/master/src/main/resources/mapping/all_structures.txt). Custom structures are supported, though they must be declared in single quotes ('). |
| #Entities | int | The total amount of entities in that chunk. |
| #TileEntities | int | The total amount of tile entities in that chunk. |

Fields that allow multiple comma separated values act the same as multiple consecutive filters of the same type with single values connected with the `AND` operator.

A string representation of the query is printed in a text field below the query editor. When entering a query into this field directly, press `Enter` to parse it into the query editor.

**Notice**
Running the query will check the `.mca`-file's name first if the query would even apply to any chunk in this file using the xPos and zPos conditions, as long as the query is built in a way that allows doing this.

### NBT Changer
The NBT Changer modifies the world files directly by changing specific values.

<p align="center">
  <img src="https://gist.githubusercontent.com/Querz/5e08c4ab863c2ad8b5da146dc4188ecb/raw/88e1887eb441c1f0f60af6eb3dfcf7917b16544a/change_nbt.png" alt="MCA Selector window showing the nbt changer">
</p>

You can change the following values:

| Field | Type | Description |
| ----- | ----- | ----------- |
| LightPopulated | byte | Whether the light levels for the chunk have been calculated. If this is set to `0`, converting a world from 1.12.x to 1.13 will omit that chunk. Allowed values are `0` and `1` |
| DataVersion | int | Allows to change the DataVersion of the chunks. Should be used with extreme care. |
| InhabitedTime | long | This field stores the amount of game-ticks players have spent in a chunk. Impacts the local difficulty. |
| LastUpdate | long | Stores a timestamp when this chunk was last updated in Milliseconds. |
| Status | String | The status of the chunk generation. Only recognized by Minecraft 1.13+ (DataVersion 1444+) |
| Biome | String/int | A biome name or ID. This sets all biomes of this chunk to a single biome. For a reference of biome names and IDs, have a look at the [Wiki](https://minecraft.gamepedia.com/Java_Edition_data_values#Biomes). Custom biomes can be specified by using single quotes (') around a biome ID. |
| ReplaceBlocks | String | A comma separated list of block replacements in the format `<block-name>=<block-name\|block-nbt>[;<tile-nbt>]`. Custom block names can be specified by surrounding them with single quotes. |
| DeleteEntities | boolean | If set to `1` or `true`, all entities in that chunk will be deleted. |
| DeleteSections | boolean | One or a range of section indices. A range has the format `<from>:<to>`, inclusive. Omitting `<from>` sets the lowest possible value, omitting `<to>` sets the highest possible value. `:` or `true` means _all_ sections. Multiple ranges or single indices can be defined by separating them with a comma. |

Once the field is highlighted in green, the value is considered valid and will be changed. A gray field, no matter its content, will be ignored.

A string representation of the to be changed fields is printed in a text field below the editor. When entering a field string into this field directly, press `Enter` to parse it into the editor.

For more information about the fields have a look at the chunk format description on [Minecraft Wiki](https://minecraft.gamepedia.com/Chunk_format)

### Chunk Editor
When selecting a single chunk, the menu "Edit chunks" becomes available. It allows precise editing of the entire NBT structure of that chunk, including poi and entities data. Names and values can be changed, added, deleted or moved (via drag & drop).

<p align="center">
  <img src="https://gist.githubusercontent.com/Querz/5e08c4ab863c2ad8b5da146dc4188ecb/raw/88e1887eb441c1f0f60af6eb3dfcf7917b16544a/edit_chunk.png" alt="MCA Selector window showing the NBT editor">
</p>

**Notice** When the NBT editor does not show any data, the cached top-down view might be outdated, and the chunk might not exist anymore in the mca files. In that case, clearing the cache will re-render the regions from scratch and show the up-to-date top-down view.

### Chunk import
Importing chunks can be easily done by opening the target world first using `File --> Open` and then merging the chunks of a second world using `Tools --> Import chunks`. After selecting a folder containing region files, it is possible to import the chunks with a bunch of additional options.

<p align="center">
  <img src="https://gist.githubusercontent.com/Querz/5e08c4ab863c2ad8b5da146dc4188ecb/raw/88e1887eb441c1f0f60af6eb3dfcf7917b16544a/import_chunks.png" alt="MCA Selector window showing the chunk import">
</p>

Options:
* **Offset** imports chunks with an offset in chunk coordinates. To convert block coordinates to chunk coordinates, divide them by 16.
* **Overwrite existing chunks** deletes existing chunks and overwrites them with the imported ones.
* **Apply to selection only** imports chunks into the current selection.
* **Sections** defines one, or a range of sections to be imported. A range has the format `<from>:<to>`, inclusive. Omitting `<from>` sets the lowest possible value, omitting `<to>` sets the highest possible value. `:`, `true` or empty means _all_ sections. Multiple ranges or single indices can be defined by separating them with a comma. If a chunk does not already exist at the location, it will be created containing only the specified sections of the imported chunk.

**Notice**
Commands inside of command blocks will not be changed.
Maps will not be updated, because their data is not stored inside region files.

### Copy and Paste
It is possible to copy a selection to the system clipboard and pasting it to a different location in the same world or into an entirely different world.
After making a selection, use `Selection --> Copy chunks` or press `Ctrl+C` (`Cmd+C` on Mac). After navigating to the location in the world where the copied chunks need to be pasted, use `Selection --> Paste chunks` or press `Ctrl+V` (`Cmd+V`on Mac) to display an overlay showing where the clipboard will be imported to. The overlay can be moved around by pressing and holding the left mouse button. Press `Ctrl+V` again to import the chunks at the selected location. This will open the [Import chunks](#chunk-import) dialog with prefilled values depending on where the overlay has been placed.
Copying can be cancelled by pressing `Esc`.

<p align="center">
  <img src="https://gist.githubusercontent.com/Querz/5e08c4ab863c2ad8b5da146dc4188ecb/raw/88e1887eb441c1f0f60af6eb3dfcf7917b16544a/copy_paste.png" alt="MCA Selector window showing copy-paste overlay">
</p>

### Swapping chunks
When exactly two chunks are selected, they can be swapped using `Tools --> Swap chunks`. This is useful for corrupted region files when Minecraft failed to correctly save the region file index, resulting in scrambled chunks.

### Caching
The tool creates an image for each region from the provided mca-files. These images are saved separately inside the respective operating system's specific cache folders. Experience showed that a Minecraft world with a size of 10GB resulted in cached image files with a total size of 80-100MB. Caching as many regions as possible significantly improves loading times.
* Windows: `%LOCALAPPDATA%\mcaselector\cache` if `%LOCALAPPDATA%` is set, otherwise `<parent directory of mcaselector.jar>\mcaselector\cache`
* MacOS: `~/Library/Caches/mcaselector`
* Linux: `$XDG_CACHE_HOME/mcaselector` if `$XDG_CACHE_HOME` is set or the first directory that contains a folder `mcaselector` in `$XDG_CACHE_DIRS` or a new directory called `mcaselector` in the first entry in `$XDG_CACHE_DIRS` is created or if neither `$XDG_CACHE_HOME` nor `$XDG_CACHE_DIRS` is set `~/.cache/mcaselector`.

### Generating an image
MCA Selector is capable of generating an image based on a selection.

### Debugging
If something is not working properly or if you want to see what exactly the MCA Selector is doing, debugging can be enabled in the settings. Informative messages and errors will be printed to the console as well as to a log file.
The log file is stored in the following directory and is overwritten after each new start of the program:
* Windows: `%LOCALAPPDATA%\mcaselector\debug.log` if `%LOCALAPPDATA%` is set, otherwise `<parent directory of mcaselector.jar>\mcaselector\debug.log`
* MacOS: `~/Library/Logs/mcaselector/debug.log`
* Linux: `$XDG_DATA_HOME/mcaselector/debug.log` if `$XDG_DATA_HOME` is set or the first directory that contains a file `mcaselector/debug.log` in `$XDG_DATA_DIRS` or `mcaselector` in the first entry in `$XDG_DATA_DIRS` or if neither `$XDG_DATA_HOME` nor `$XDG_DATA_DIRS` is set `~/.local/share/mcaselector/debug.log`.

Alternatively, the location of the log file can be viewed by clicking on the link in the settings dialog.

---
## Supported Versions
The MCA Selector currently supports the following Minecraft versions:

| Minecraft Version | DataVersion |
| ----------------- | ----------- |
| 1.2.1 - 1.12.2    | None - 1343 |
| 1.13 - 1.13.2     | 1444 - 1631 |
| 1.14 - 1.14.4     | 1901 - 1976 |
| 1.15 - 1.15.2     | 2200 - 2230 |
| 1.16 - 1.16.5     | 2566 - 2586 |
| Snapshot 21w08a   | 2697 |

There is no guarantee for worlds generated in a Snapshot version to work, even if it is specified in the table above. This only represents the current development status towards the next Minecraft release. Old Snapshots of past Minecraft releases are not supported.

---
## Headless mode

MCA Selector can be run in a headless mode without showing the UI. Using any command line parameter automatically switches to headless mode.
A cheat-sheet with all commands can be found [here](https://gist.githubusercontent.com/Querz/5e08c4ab863c2ad8b5da146dc4188ecb/raw/efc4b6990bce79ed4512e9a0a363c64afd7b7444/commands-diagram.png).

| Mode | Parameter | Description |
| ---- | --------- | ----------- |
| Create selection | `--mode select` | Create a selection from a filter query and save it as a CSV file. |
| Export chunks | `--mode export` | Export chunks based on a filter query and/or a selection. |
| Import chunks | `--mode import` | Import chunks with an optional offset. |
| Delete chunks | `--mode delete` | Delete chunks based on a filter query and/or a selection. |
| Change NBT | `--mode change` | Changes NBT values in an entire world or only in chunks based on a selection. |
| Cache images | `--mode cache` | Generates the cache images for an entire world. |
| Generate image | `--mode image` | Generates a single image based on a selection. |

### Mandatory and optional parameters

#### Create selection

| Parameter | Description | Mandatory |
| --------- | ----------- | :-------: |
| `--region <directory>` | The world's region folder for which to create the selection. | Yes |
| `--poi <directory>` | The world's poi folder. | No |
| `--entities <directory>` | The world's entities folder. | No |
| `--query <filter-query>` | The filter query to use to create a selection. | Yes |
| `--output <csv-file>` | The CSV-file to save the selection to. | Yes |
| `--radius <positive number>` | The radius for adjacent chunks to be selected | No, default `0` |

#### Export chunks

| Parameter | Description | Mandatory |
| --------- | ----------- | :-------: |
| `--region <directory>` | The source world's region folder to export chunks from. | Yes |
| `--poi <directory>` | The source world's poi folder. | No, but needs `--output-poi` if specified |
| `--entities <directory>` | The source world's entities folder. | No, but needs `--output-entities` if specified |
| `--output-region <directory>` | The target region folder of the exported chunks. The directory MUST be empty. | Yes |
| `--output-poi <directory>` | The target poi folder. | Yes if `--poi` is specified |
| `--output-entities <directory>` | The target entities folder. | Yes if `--entities` is specified |
| `--query <filter-query>` | The filter query to use to export the chunks. | Yes if `--selection` is not set, otherwise No |
| `--selection <csv-file>` | The csv-file to load a selection from. | Yes if `--query` is not set, otherwise No |

#### Import chunks

| Parameter | Description | Mandatory |
| --------- | ----------- | :-------: |
| `--region <directory>` | The target world's region folder to import chunks to. | Yes |
| `--poi <directory>` | The target world's poi folder. | No, but needs `--input-poi` if specified |
| `--entities <directory>` | The target world's entities folder. | No, but needs `--input-entities` if specified |
| `--input-region <directory>` | The source region folder to import the chunks from. | Yes |
| `--input-poi <directory>` | The source poi folder. | Yes if `--poi` is specified |
| `--input-entities <directory>` | The source entities folder | Yes if `--entities` is specified |
| `--offset-x <number>` | The offset in chunks in x-direction. | No, default `0` |
| `--offset-z <number>` | The offset in chunks in z-direction. | No, default `0` |
| `--overwrite` | Whether to overwrite existing chunks. | No, default `false` |
| `--selection <csv-file>` | A specific selection where to import chunks to. | No |
| `--sections <range\|number[,...]>` | One or a range of section indices. A range has the format `<from>:<to>`, inclusive. Omitting `<from>` sets the lowest possible value, omitting `<to>` sets the highest possible value. `:` or `true` means _all_ sections. Multiple ranges or single indices can be defined by separating them with a comma. | No, default all sections |

#### Delete chunks

| Parameter | Description | Mandatory |
| --------- | ----------- | :-------: |
| `--region <directory>` | The world's region folder to delete chunks from. | Yes |
| `--poi <directory>` | The world's poi folder. | No |
| `--entities <directory>` | The world's entities folder. | No |
| `--query <filter-query>` | The filter query to use to delete the chunks. | Yes if `--input` is not set, otherwise No |
| `--selection <csv-file>` | The csv-file to load a selection from. | Yes if `--query` is not set, otherwise No |

#### Change NBT

| Parameter | Description | Mandatory |
| --------- | ----------- | :-------: |
| `--region <directory>` | The world's region folder in which NBT values should be changed. | Yes |
| `--poi <directory>` | The world's poi folder. | No |
| `--entities <directory>` | The world's entities folder. | No |
| `--fields <values>` | The values to be changed. | Yes |
| `--selection <csv-file>` | The csv-file to load a selection from. | No |
| `--force` | Whether the value should be created if the key doesn't exist. | No, default `false` |

#### Cache images

**Notice**
This requires a working JavaFX installation.

| Parameter | Description | Mandatory |
| --------- | ----------- | :-------: |
| `--region <directory>` | The world to create cache images from. | Yes |
| `--output <directory>` | Where the cache files will be saved. | Yes |
| `--zoom-level <1\|2\|4\|8>` | The zoom level for which to generate the images. | No, generates images for all zoom levels if not specified |

#### Generate image from selection

**Notice**
This requires a working JavaFX installation

| Parameter | Description | Mandatory |
| --------- | ----------- | :-------: |
| `--region <directory>` | The world to create cache images from. | Yes |
| `--output <png-file>` | Where the generated image will be saved. | Yes |
| `--selection <csv-file>` | The csv-file to a load a selection from. | Yes |

#### Configuration parameters

| Parameter | Description | Mandatory |
| --------- | ----------- | :-------: |
| `--debug` | Enables debug messages. | No |
| `--read-threads <number>` | The amount of Threads to be used for reading files. | No, default `1` |
| `--process-threads <number>` | The amounts of Threads to be used for processing data. | No, default is the `amount of processor cores - 2`, minimum `1` |
| `--write-threads <number>` | The amount of Threads to be used for writing data to disk. | No, default ìs the `amount of processor cores`, maximum `4` |
| `--max-loaded-files <number>` | The maximum amount of simultaneously loaded files. | No, default is the `maximum heap size in GB * 2`, minimum `1` |

### Filter query

A filter query is a text representation of the chunk filter that can be created in the UI of the program. It is shown in the text field below the query editor of the Chunk filter tool.
Example:
```
--query "xPos >= 10 AND xPos <= 20 AND Palette contains \"sand,water\""
```
This will select all chunks that contain sand and water blocks and their x-position ranges from 10 to 20.
As shown, double quotes (") must be escaped with a backslash.

Groups are represented with a pair of parentheses (`(...)`). As with the chunk filter, logical operators are evaluated from left to right, and `AND` has a higher precedence than `OR`. In other words, `a AND b OR c AND d AND e` is the same as `(a AND b) OR (c AND d AND e)`. It differs from `c AND d AND e OR a AND b` not by the result, but by the sequence of tests performed due to [short-circuiting](https://en.wikipedia.org/wiki/Short-circuit_evaluation).

### Change values

The query for changing NBT values in chunks looks slightly different to the filter query. It is a comma (,) separated list of assignments.
Example:
```
--filter "LightPopulated = 1, Status = empty"
```
This will set the field "LightPopulated" to "1" and "Status" to "empty". Just like the filter query, the query to change values is printed to the console when using the UI in debug mode.

---
## Checkout and building

To checkout master:
```
git clone https://github.com/Querz/mcaselector.git
```
To build a standalone jar file on Mac OS or Linux using the Gradle Wrapper, run
```
./gradlew build
```
On Windows, run
```
gradlew.bat build
```

The resulting jar files can be found in `build/libs/`.

---
## Translation
The UI language of the MCA Selector can be dynamically changed in the settings.
The following languages are available:

* English (UK)
* German (Germany)
* Chinese (China) (thanks to [@LovesAsuna](https://github.com/LovesAsuna) for translating)
* Chinese (Taiwan) (thanks to [@hugoalh](https://github.com/hugoalh) for translating)
* Czech (Czech Republic) (thanks to [@mkyral](https://github.com/mkyral) for translating)
* Spanish (Spain) (thanks to [@NathanielFreeman](https://github.com/NathanielFreeman) for translating)
* Portuguese (Brazil) (thanks to [@cr1st0ph3r](https://github.com/cr1st0ph3r) for translating)
* Portuguese (Portugal) (thanks to [@D3W10](https://github.com/D3W10) for translating)
* French (France) (thanks to [@SkytAsul](https://github.com/SkytAsul) and [@DoctaEnkoda](https://github.com/DoctaEnkoda) for translating)
* Swedish (Sweden) (thanks to [@TechnicProblem](https://github.com/TechnicProblem) for translating)
* Russian (Russia) (thanks to [@Quarktal](https://github.com/Quarktal) for translating)
* Dutch (Netherlands) (thanks to [@Clijmart](https://github.com/Clijmart) for translating)
* Italian (Italy) (thanks to @valeilsimpaticone for translating)
* Polish (Poland) (thanks to [@Marcinolak](https://github.com/Marcinolak) for thranslating)

If you would like to contribute a translation, you can find the language files in [resources/lang/](https://github.com/Querz/mcaselector/tree/master/src/main/resources/lang). The files are automatically detected and shown as the respective language option in the settings dropdown menu once they are placed in this folder.

---

## Download and installation

**MCA Selector modifies and deletes chunks in your Minecraft world. Please make backups of your world before using.**

---

[**Download Version 1.14.1 (Windows Installer)**](https://github.com/Querz/mcaselector/releases/download/1.14.1/MCA_Selector_Setup.exe)

"Requirements":
* Windows 7-10 64bit
* At least 6 GB of RAM

---

[**Download Version 1.14.1 (Universal)**](https://github.com/Querz/mcaselector/releases/download/1.14.1/mcaselector-1.14.1.jar)


"Requirements":
* Either:
  * 64bit JRE 8+, you can get it from [HERE](https://www.java.com/en/download/windows-64bit.jsp)
  * A Minecraft Java Edition installation
* A computer
  * At least 6 GB of RAM. If lower, more RAM has to manually be assigned to the JVM using the `-Xmx` argument. Assigning 4 GB is recommended.
* A brain

### What works on most systems
Most likely, `.jar` files are associated with java on your computer, it should therefore launch by simply double clicking the file (or however your OS is configured to open files using your mouse or keyboard). If not, you can try `java -jar mcaselector-1.14.1.jar` from your console. If this doesn't work, you might want to look into how to modify the `PATH` variable on your system to tell your system that java is an executable program.

### If you have Minecraft Java Edition installed
Minecraft Java Edition comes with a JRE that you can use to start the MCA Selector, so there is no need to install another version of Java on your system.

For Windows:
* Hold `Shift` and Right-click on an empty space and select `Open PowerShell window here` (`Open Command window here` on Windows 8 and earlier).
* Type `& "C:\Program Files (x86)\Minecraft\runtime\jre-x64\bin\java" -jar `, then drag and drop the `mcaselector-1.14.1.jar into the console` and press `Enter`. In the Command window (Windows 8 and earlier), the command starts with `"C:\Program Files (x86)\Minecraft\runtime\jre-x64\bin\java" -jar ` instead and the path to `mcaselector-1.14.1.jar` must be typed or copied and pasted into the console manually.

For MacOS:
* Press `Cmd+Space`, type `Terminal` and press `Enter`.
* Type `~/Library/Application\ Support/minecraft/runtime/jre-x64/jre.bundle/Contents/Home/bin/java -jar ` (with a space at the end), then drag and drop the `mcaselector-1.14.1.jar` into the console and press `Enter`.

**WARNING:** For MacOS 10.14+ (Mojave) It is NOT recommended to use the JRE provided by Minecraft (1.8.0_74), because it contains a severe bug that causes JavaFX applications to crash when they lose focus while a dialog window (such as the save-file-dialog) is open (see the bug report [here](https://bugs.openjdk.java.net/browse/JDK-8211304)). This bug has been fixed in Java 1.8.0_201 and above.

### If you receive a JavaFX error
"When I run `mcaselector-1.14.1.jar`, an error dialog appears that looks like this:"

<p align="center">
  <img src="https://gist.githubusercontent.com/Querz/5e08c4ab863c2ad8b5da146dc4188ecb/raw/88e1887eb441c1f0f60af6eb3dfcf7917b16544a/missing_javafx.png" alt="Popup dialog stating a missing JavaFX installation">
</p>

Open the console or terminal on your OS.

For Windows:
* Hold `Shift` and Right-click on an empty space on your desktop and select `Open PowerShell here` (`Open Command window here` on Windows 8 and earlier).

For MacOS:
* Press `Cmd+Space`, type `Terminal` and press `Enter`.

Type the command `java -version` and press `Enter`. If the output shows that your java command is linked to `java version "1.8.0"`, you can simply run MCA Selector through the console.

For Windows and MacOS:
* Type `java -jar ` (with a space at the end) and drag and drop the `mcaselector-1.14.1.jar` into the console and hit `Enter`.

For Linux:
* Run `java -jar <path to mcaselector-1.14.1.jar` where you replace everything in `<>`.

If the output shows a Java version higher than Java 8, please find and download the appropriate JavaFX version from [Here](https://gluonhq.com/products/javafx/). If you know what to do, do it. If you don't, follow these steps:

For Windows:
* Download "JavaFX Windows SDK" for your Java version from [here](https://gluonhq.com/products/javafx/).
* Unzip the `.zip`-file with your program of choice, then navigate into the unzipped folder.
* Hold `Shift` and Right-click on an empty space in that folder and select `Open PowerShell window here` (`Open Command window here` on Windows 8 and earlier). Type `java --module-path ` (with a space at the end), then drag and drop the `lib`-folder into the console. Continue to type ` --add-modules ALL-MODULE-PATH -jar ` (with a space at the beginning and the end), then drag and drop the `mcaselector-1.14.1.jar` into the console and hit `Enter`.

For MacOS:
* Download "JavaFX Mac OS X SDK" for your Java version from [here](https://gluonhq.com/products/javafx/).
* Double-click the `.zip`-file to unpack, then navigate into the unzipped folder.
* Press `Cmd+Space`, type `Terminal` and press `Enter`. Type `java --module-path ` (with a space at the end), then drag and drop the `lib`-folder into the console. Continue to type `--add-modules ALL-MODULE-PATH -jar ` (with a space at the end), then drag and drop the `mcaselector-1.14.1.jar` into the console and hit `Enter`.

For Linux:
* Download "JavaFX Linux SDK" for your Java version from [here](https://gluonhq.com/products/javafx/).
* Unzip the `.zip`-file with your program of choice.
* Open the command prompt and run `java --module-path <path to unzipped folder>/lib --add-modules ALL-MODULE-PATH -jar <path to mcaselector-1.14.1.jar>` where you replace everything in `<>` with the appropriate paths.
* Some distributions like AdoptOpenJDK (shipped with most Linux distributions) do not ship with JavaFX by default. On Debian, an open version of JavaFX is contained in the `openjfx` package. This or some other installation of JavaFX is required to run the `mcaselector-1.14.1.jar`.

To avoid having to go through this process every time to start MCA Selector, the resulting command can be copied into a `.bat`-file on Windows or `.sh`-file on MacOS and Linux and can then be executed by double-clicking the `.bat`-file on Windows or running `sh <file>.sh` in the terminal / console on MacOS or Linux where `<file>` must be replaced by the name of the `.sh`-file.

---

If none of these instructions work, apply "A brain" that you providently held ready after having read the "Requirements" section carefully. Or ask your question on [Discord](https://discord.gg/h942U8U).
