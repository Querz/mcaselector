# MCA Selector
[![total downloads](https://img.shields.io/github/downloads/Querz/mcaselector/total.svg)](https://github.com/Querz/mcaselector/releases) [![paypal](https://img.shields.io/badge/donate-PayPal-green.svg)](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=3PV2GDWZL8HCA)


#### An external tool to export or delete selected chunks and regions from a world save of Minecraft Java Edition.
---

<!--toc-start-->
* [Usage](#usage)
  * [Navigation](#navigation)
  * [Selections](#selections)
  * [Chunk filter](#chunk-filter)
  * [NBT Changer](#nbt-changer)
  * [Chunk Editor](#chunk-editor)
  * [Chunk import](#chunk-import)
  * [Caching](#caching)
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
    * [Configuration parameters](#configuration-parameters)
  * [Filter query](#filter-query)
  * [Change values](#change-values)
* [Checkout and building](#checkout-and-building)
* [Translation](#translation)
* [Download and installation](#download-and-installation)
    * [If you have Java from Oracle installed on your system](#if-you-have-java-from-oracle-installed-on-your-system)
    * [If you have Minecraft Java Edition installed on your system](#if-you-have-minecraft-java-edition-installed-on-your-system)
    * [If you are using OpenJDK](#if-you-are-using-openjdk)
<!--toc-end-->

---

## Usage
### Navigation
Executing the tool, it shows an empty window with a chunk and a region grid. To actually show a world, open a folder containing Minecraft Anvil (\*.mca) files. The tool will then render a top-down view of this world that you can zoom into and zoom out of by scrolling up and down and that you can move around using the middle mouse button (`Cmd+LMB` on Mac OS) or using `WASD`.

![alt text](https://gist.githubusercontent.com/Querz/5e08c4ab863c2ad8b5da146dc4188ecb/raw/302f2ec9ea76913c1fdd217415d623bb776e4e77/mca_selector_default.png "MCA Selector window showing chunk and region grid")

Zooming out far enough disables the selection of single chunks but lets you select entire regions.

### Selections
Upon finishing selecting chunks and regions, they can be deleted or exported using the `Selection`-menu. Exported chunks and regions are not deleted from the original world.

![alt text](https://gist.githubusercontent.com/Querz/5e08c4ab863c2ad8b5da146dc4188ecb/raw/d708eafb98e62a99e26735c166dc8fdcecf17185/mca_selector_selections.png "MCA Selector window showing chunk and region selection export")

A selection (not the chunks and regions themselves) can also be exported or imported and even be applied to different worlds.

### Chunk filter
The MCA Selector also contains a powerful tool to delete or export chunks and regions by conditions like the data version, the time it was last updated, how much time players have spent in this chunk and some more. Multiple of these conditions can be used to create a very specific query describing what chunks and regions should be deleted or exported.

![alt text](https://gist.githubusercontent.com/Querz/5e08c4ab863c2ad8b5da146dc4188ecb/raw/a977a7b2b4844123262003268992dcf0b80c337c/mca_selector_filter_chunks.png "MCA Selector window showing the chunk filter")

Because the conditions use internal values used by Minecraft, the following table gives a brief explanation on what they do:

| Condition | Type | Description |
| --------- | ----- | ----------- |
| Group | - | Groups multiple conditions. |
| DataVersion | int | The DataVersion tag of the chunk. 100-1343 for 1.12.2 and below, 1444 for 1.13 snapshots and above. |
| InhabitedTime | long | The total amount of time in game-ticks players have spent in that chunk. 1 second ~20 ticks. Also accepts a duration string such as `1 year 2 months 3 days 4 hours 5 minutes 6 seconds`. |
| LastUpdate | int | The time a chunk was last updated in seconds since 1970-01-01. Also accepts a timestamp in the `yyyy-MM-dd HH-mm-ss`-format such as `2018-01-02 15:03:04`. If the time is omitted, it will default to `00:00:00`. |
| xPos | int | The location of the chunk on the x-axis in chunk coordinates. |
| zPos | int | The location of the chunk on the z-axis in chunk coordinates. |
| Palette | String | A list of comma (,) separated 1.13 block names. The block names will be converted to block ids for chunks with DataVersion 1343 or below. The validation of block names can be skipped by writing them in single quotes ('). Example: `sand,'new_block',gravel`.|
| Status | String | The status of the chunk generation. Only recognized by Minecraft 1.13+ (DataVersion 1444+) |
| LightPopulated | byte | Whether the light levels for the chunk have been calculated. If this is set to 0, converting a world from 1.12.x to 1.13 will omit that chunk. Allowed values are `0` and `1`. |
| Biome | String | One or multiple biome names, separated by comma (,). For a reference of biome names, have a look at the [Wiki](https://minecraft.gamepedia.com/Java_Edition_data_values#Biomes). |
| Entities | String | One or multiple entity names, separated by comma (,). For a reference of entity names, have a look at the [Wiki](https://minecraft.gamepedia.com/Java_Edition_data_values#Entities). |
| #Entities | int | The total amount of entities in that chunk. |
| #TileEntities | int | The total amount of tile entities in that chunk. |

Fields that allow multiple comma separated values act the same as multiple consecutive filters of the same type with single values connected with the `AND` operator.

**Notice**
Running the query will check the `.mca`-file's name first if the query would even apply to any chunk in this file using the xPos and zPos conditions, as long as the query is built in a way that allows doing this.

### NBT Changer
The NBT Changer modifies the world files directly by changing specific values.

![alt text](https://gist.githubusercontent.com/Querz/5e08c4ab863c2ad8b5da146dc4188ecb/raw/a977a7b2b4844123262003268992dcf0b80c337c/mca_selector_change_nbt.png "MCA Selector window showing the nbt changer")

You can change the following values:

| Field | Type | Description |
| ----- | ----- | ----------- |
| LightPopulated | byte | Whether the light levels for the chunk have been calculated. If this is set to `0`, converting a world from 1.12.x to 1.13 will omit that chunk. Allowed values are `0` and `1` |
| DataVersion | int | Allows to change the DataVersion of the chunks. Should be used with extreme care. |
| InhabitedTime | long | This field stores the amount of game-ticks players have spent in a chunk. Impacts the local difficulty. |
| LastUpdate | long | Stores a timestamp when this chunk was last updated in Milliseconds. |
| Status | String | The status of the chunk generation. Only recognized by Minecraft 1.13+ (DataVersion 1444+) |
| Biome | String | A biome name. This sets all biomes of this chunk to a single biome. For a reference of biome names, have a look at the [Wiki](https://minecraft.gamepedia.com/Java_Edition_data_values#Biomes). |
| DeleteEntities | boolean | If set to `1` or `true`, all entities in that chunk will be deleted. |

Once the field is highlighted in green, the value is considered valid and will be changed. A gray field, no matter its content, will be ignored.

For more information about the fields have a look at the chunk format description on [Minecraft Wiki](https://minecraft.gamepedia.com/Chunk_format)

### Chunk Editor
When selecting a single chunk, the menu "Edit chunks" becomes available. It allows precise editing of the entire NBT structure of that chunk. Names and values can be changed, added, deleted or moved (drag & drop).

![alt text](https://gist.githubusercontent.com/Querz/5e08c4ab863c2ad8b5da146dc4188ecb/raw/a977a7b2b4844123262003268992dcf0b80c337c/mca_selector_edit_chunk.png "MCA Selector window showing the NBT editor")

**Notice** When the NBT editor does not show any data, the cached top-down view might be outdated and the chunk might not exist anymore in the mca files. In that case, clearing the cache will rerender the regions from scratch and show the up-to-date top-down view.

### Chunk import
Importing chunks can be easily done by opening the target world first using `File --> Open` and then merging the chunks of a second world using `Tools --> Import chunks`. After selecting a folder containing region files, it is possible to import the chunks with a specific offset. Doing so automatically adjusts relevant values to the new location of the chunk.

![alt text](https://gist.githubusercontent.com/Querz/5e08c4ab863c2ad8b5da146dc4188ecb/raw/a977a7b2b4844123262003268992dcf0b80c337c/mca_selector_import_chunks.png "MCA Selector window showing the chunk import")

**Notice**
Commands inside of command blocks will not be changed.
Maps will not be updated, because their data is not stored inside region files.

### Caching
The tool creates an image for each region from the provided mca-files. These images are saved separately inside a `cache`-folder in the working directory of the program for future usage. Experience showed that a Minecraft world with a size of 10GB resulted in cached image files with a total size of 80-100MB. Caching as many regions as possible significantly improves loading times though.

### Debugging
If something is not working properly or if you want to see the exact query that is run using the chunk filter, debugging can be enabled in the settings. It will print useful information about what the program is currently doing to the console.

---
## Supported Versions
The MCA Selector currently supports the following Minecraft versions:

| Minecraft Version | DataVersion | Supported |
| ----------------- | ----------- | :-------: |
| Beta 1.3 - 1.12.2 | 100 - 1343  | Yes       |
| 1.13 - 1.13.2     | 1444 - 1631 | Yes       |
| 1.14 - 1.14.4     | 1901 - 1976 | Yes       |
| 1.15 - 1.15.2     | 2200 - 2230 | Yes       |
| 20w07a            | 2506 - ?    | Prerelease |

---
## Headless mode

The MCA Selector can be run in a headless mode without showing the UI. Use the program parameter `--headless` to do so.
Headless mode can be run in different modes:

| Mode | Parameter | Description |
| ---- | --------- | ----------- |
| Create selection | `--mode select` | Create a selection from a filter query and save it as a CSV file. |
| Export chunks | `--mode export` | Export chunks based on a filter query and/or a selection. |
| Import chunks | `--mode import` | Import chunks with an optional offset. |
| Delete chunks | `--mode delete` | Delete chunks based on a filter query and/or a selection. |
| Change NBT | `--mode change` | Changes NBT values in an entire world or only in chunks based on a selection. |
| Cache images | `--mode cache` | Generates the cache images for an entire world. |

### Mandatory and optional parameters

#### Create selection

| Parameter | Description | Mandatory |
| --------- | ----------- | :-------: |
| `--world <directory>` | The world for which to create the selection. | Yes |
| `--output <csv-file>` | The CSV-file to save the selection to. | Yes |
| `--query <filter-query>` | The filter query to use to create a selection. | Yes |
| `--radius <positive number>` | The radius for adjacent chunks to be selected | No, default `0` |

#### Export chunks

| Parameter | Description | Mandatory |
| --------- | ----------- | :-------: |
| `--world <directory>` | The world to export chunks from. | Yes |
| `--output <directory>` | The destination of the exported chunks. The directory MUST be empty. | Yes |
| `--query <filter-query>` | The filter query to use to export the chunks. | Yes if `--input` is not set, otherwise No |
| `--input <csv-file>` | The csv-file to load a selection from. | Yes if `--query` is not set, otherwise No |

#### Import chunks

| Parameter | Description | Mandatory |
| --------- | ----------- | :-------: |
| `--world <directory>` | The world to import chunks to. | Yes |
| `--input <directory>` | The world to import the chunks from. | Yes |
| `--offset-x <number>` | The offset in chunks in x-direction. | No, default `0` |
| `--offset-z <number>` | The offset in chunks in z-direction. | No, default `0` |
| `--overwrite` | Whether to overwrite existing chunks. | No, default `false` |
| `--selection` | A specific selection where to import chunks to. | No |

#### Delete chunks

| Parameter | Description | Mandatory |
| --------- | ----------- | :-------: |
| `--world <directory>` | The world to delete chunks from. | Yes |
| `--query <filter-query>` | The filter query to use to delete the chunks. | Yes if `--input` is not set, otherwise No |
| `--input <csv-file>` | The csv-file to load a selection from. | Yes if `--query` is not set, otherwise No |

#### Change NBT

| Parameter | Description | Mandatory |
| --------- | ----------- | :-------: |
| `--world <directory>` | The world in which NBT values should be changed. | Yes |
| `--query <values>` | The values to be changed. | Yes |
| `--input <csv-file>` | The csv-file to load a selection from. | No |
| `--force` | Whether the value should be created if the key doesn't exist. | No, default `false` |

#### Cache images

| Parameter | Description | Mandatory |
| --------- | ----------- | :-------: |
| `--world <directory>` | The world to create cache images from. | Yes |
| `--output <directory>` | Where the cache files fille be saved. | Yes |
| `--zoom-level <1\|2\|4>` | The zoom level for which to generate the images. | No, generates images for all zoom levels if not specified |

#### Configuration parameters

| Parameter | Description | Mandatory |
| --------- | ----------- | :-------: |
| `--debug` | Enables debug messages. | No |
| `--read-threads <number>` | The amount of Threads to be used for reading files. | No, default `1` |
| `--process-threads <number>` | The amounts of Threads to be used for processing data. | No, default is the amount of processor cores |
| `--write-threads <number>` | The amount of Threads to be used for writing data to disk. | No, default `4` |
| `--max-loaded-files <number>` | The maximum amount of simultaneously loaded files. | No, default is 1.5 * amount of processor cores |


### Filter query

A filter query is a text representation of the chunk filter that can be created in the UI of the program. When the debug mode is enabled, it will be printed into the console.
Example:
```
--query "xPos >= 10 AND xPos <= 20 AND Palette contains \"sand,water\""
```
This will select all chunks that contain sand and water blocks and their x-position ranges from 10 to 20.
As shown, double quotes (") must be escaped with a backslash.

### Change values

The query for changing NBT values in chunks looks slightly different to the filter query. It is a comma (,) separated list of assignments.
Example:
```
--query "LightPopulated = 1, Status = empty"
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
./gradlew build minifyCss shadowJar
```
On Windows, run
```
gradlew.bat build minifyCss shadowJar
```

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
* French (France) (thanks to [@SkytAsul](https://github.com/SkytAsul) and [@DoctaEnkoda](https://github.com/DoctaEnkoda) for translating)
* Swedish (Sweden) (thanks to [@TechnicProblem](https://github.com/TechnicProblem) for translating)

If you would like to contribute a translation, you can find the language files in [resources/lang/](https://github.com/Querz/mcaselector/tree/master/src/main/resources/lang). The files are automatically detected and shown as the respective language option in the settings dropdown menu once they are placed in this folder.

---

## Download and installation

[**Download Version 1.9.4**](https://github.com/Querz/mcaselector/releases/download/1.9.4/mcaselector-1.9.4.jar)

"Requirements":
* Either:
  * JRE 8+, you can get it from [HERE](http://www.oracle.com/technetwork/java/javase/downloads/jre8-downloads-2133155.html)
  * A Minecraft Java Edition installation
* A computer
* A brain

#### If you have Java from Oracle installed on your system

Most likely, `.jar` files are associated with java on your computer, it should therefore launch by simply double clicking the file (or however your OS is configured to open files using your mouse or keyboard). If not, you can try `java -jar mcaselector-1.9.4.jar` from your console. If this doesn't work, you might want to look into how to modify the `PATH` variable on your system to tell your system that java is an executable program.

#### If you have Minecraft Java Edition installed on your system

Minecraft Java Edition comes with a JRE that you can use to start the MCA Selector, so there is no need to install another version of java on your system. On Windows, that java version is usually located in `C:\Program Files (x86)\Minecraft\runtime\jre-x64\bin\` and once inside this folder you can simply run `java.exe -jar <path-to-mcaselector-1.9.4.jar>`. On Mac OS you should find it in `Applications/Minecraft.app/Contents/runtime/jre-x64/1.8.0_74/bin` where you can execute `./java -jar <path-to-mcaselector-1.9.4.jar>`.

#### If you are using OpenJDK

If you are using a distribution of OpenJDK, you have to make sure that it comes with JavaFX, as it is needed to run the MCA Selector. Some distributions like AdoptOpenJDK (shipped with most Linux distributions) do not ship with JavaFX by default. On Debian distributions, an open version of JavaFX is contained in the `openjfx` package. This or some other installation of JavaFX is required to run the `.jar`.
##
If none of these instructions work, apply "A brain" that you providently held ready after having read the "Requirements" section carefully.
