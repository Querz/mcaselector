MCA Selector can be run in CLI mode without showing the UI. Using any command line parameter automatically
switches to CLI mode and doesn't start the GUI.
A cheat-sheet with all commands can be found [here](https://gist.githubusercontent.com/Querz/5e08c4ab863c2ad8b5da146dc4188ecb/raw/efc4b6990bce79ed4512e9a0a363c64afd7b7444/commands-diagram.png).

## Modes

| Mode             | Parameter       | Description                                                                   |
|------------------|-----------------|-------------------------------------------------------------------------------|
| Create selection | `--mode select` | Create a selection from a filter query and save it as a CSV file.             |
| Export chunks    | `--mode export` | Export chunks based on a filter query and/or a selection.                     |
| Import chunks    | `--mode import` | Import chunks with an optional offset.                                        |
| Delete chunks    | `--mode delete` | Delete chunks based on a filter query and/or a selection.                     |
| Change NBT       | `--mode change` | Changes NBT values in an entire world or only in chunks based on a selection. |
| Cache images     | `--mode cache`  | Generates the cache images for an entire world.                               |
| Generate image   | `--mode image`  | Generates a single image based on a selection.                                |

## Mandatory and optional parameters

### Mode: Create selection

| Parameter                    | Description                                                             |    Mandatory    |
|------------------------------|-------------------------------------------------------------------------|:---------------:|
| `--world <directory>`        | The world folder for which to the create the selection.                 |       Yes       |
| `--region <directory>`       | The world's region folder to override the region folder from `--world`. |       No        |
| `--poi <directory>`          | The world's poi folder.                                                 |       No        |
| `--entities <directory>`     | The world's entities folder.                                            |       No        |
| `--query <filter-query>`     | The filter query to use to create a selection.                          |       Yes       |
| `--output <csv-file>`        | The CSV-file to save the selection to.                                  |       Yes       |
| `--radius <positive number>` | The radius for adjacent chunks to be selected                           | No, default `0` |

### Mode: Export chunks

| Parameter                       | Description                                                                  |                   Mandatory                   |
|---------------------------------|------------------------------------------------------------------------------|:---------------------------------------------:|
| `--world <directory>`           | The world folder from which to export.                                       |                      Yes                      |
| `--region <directory>`          | The world's region folder to override the region folder from `--world`.      |                      No                       |
| `--poi <directory>`             | The source world's poi folder.                                               |                      No                       |
| `--entities <directory>`        | The source world's entities folder.                                          |                      No                       |
| `--output <directory>`          | The target world folder of the exported chunks. The directory MUST be empty. |                      Yes                      |
| `--output-region <directory>`   | The target region folder to override the region folder created by `--output` |                      No                       |
| `--output-poi <directory>`      | The target poi folder.                                                       |                      No                       |
| `--output-entities <directory>` | The target entities folder.                                                  |                      No                       |
| `--query <filter-query>`        | The filter query to use to export the chunks.                                | Yes if `--selection` is not set, otherwise No |
| `--selection <csv-file>`        | The csv-file to load a selection from.                                       |   Yes if `--query` is not set, otherwise No   |

### Mode: Import chunks

| Parameter                            | Description                                                                                                                                                                                                                                                                                                  |        Mandatory         |
|--------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|:------------------------:|
| `--world <directory>`                | The target world folder to which to import.                                                                                                                                                                                                                                                                  |           Yes            |
| `--region <directory>`               | The target world's region folder to override the region folder from `--world`.                                                                                                                                                                                                                               |            No            |
| `--poi <directory>`                  | The target world's poi folder.                                                                                                                                                                                                                                                                               |            No            |
| `--entities <directory>`             | The target world's entities folder.                                                                                                                                                                                                                                                                          |            No            |
| `--source-world <directory>`         | The source world from which to import chunks.                                                                                                                                                                                                                                                                |           Yes            |
| `--source-region <directory>`        | The source world's region folder to override the region folder from `--source-world`.                                                                                                                                                                                                                        |            No            |
| `--source-poi <directory>`           | The source world's poi folder.                                                                                                                                                                                                                                                                               |            No            |
| `--source-entities <directory>`      | The source world's entities folder                                                                                                                                                                                                                                                                           |            No            |
| `--x-offset <number>`                | The offset in chunks in x-direction.                                                                                                                                                                                                                                                                         |     No, default `0`      |
| `--y-offset <number>`                | The offset in sections in y-direction.                                                                                                                                                                                                                                                                       |     No, default `0`      |
| `--z-offset <number>`                | The offset in chunks in z-direction.                                                                                                                                                                                                                                                                         |     No, default `0`      |
| `--overwrite`                        | Whether to overwrite existing chunks.                                                                                                                                                                                                                                                                        |   No, default `false`    |
| `--selection <csv-file>`             | A specific selection where to import chunks to.                                                                                                                                                                                                                                                              |            No            |
| `--source-selection <csv-file>`      | A specific selection for the source world listing what chunks to import into the target world.                                                                                                                                                                                                               |            No            |
| `--sections <range or number[,...]>` | One or a range of section indices. A range has the format `<from>:<to>`, inclusive. Omitting `<from>` sets the lowest possible value, omitting `<to>` sets the highest possible value. `:` or `true` means _all_ sections. Multiple ranges or single indices can be defined by separating them with a comma. | No, default all sections |

### Mode: Delete chunks

| Parameter                | Description                                                             |                 Mandatory                 |
|--------------------------|-------------------------------------------------------------------------|:-----------------------------------------:|
| `--world <directory>`    | The world folder from which to delete chunks.                           |                    Yes                    |
| `--region <directory>`   | The world's region folder to override the region folder from `--world`. |                    No                     |
| `--poi <directory>`      | The world's poi folder.                                                 |                    No                     |
| `--entities <directory>` | The world's entities folder.                                            |                    No                     |
| `--query <filter-query>` | The filter query to use to delete the chunks.                           | Yes if `--input` is not set, otherwise No |
| `--selection <csv-file>` | The csv-file to load a selection from.                                  | Yes if `--query` is not set, otherwise No |

### Mode: Change NBT

| Parameter                | Description                                                             |      Mandatory      |
|--------------------------|-------------------------------------------------------------------------|:-------------------:|
| `--world <directory>`    | The world folder for which to change NBT fields.                        |         Yes         |
| `--region <directory>`   | The world's region folder to override the region folder from `--world`. |         No          |
| `--entities <directory>` | The world's entities folder.                                            |         No          |
| `--fields <values>`      | The values to be changed.                                               |         Yes         |
| `--selection <csv-file>` | The csv-file to load a selection from.                                  |         No          |
| `--force`                | Whether the value should be created if the key doesn't exist.           | No, default `false` |

### Mode: Cache images

**Notice**
This requires a working JavaFX installation.

| Parameter                         | Description                                                             |                         Mandatory                         |
|-----------------------------------|-------------------------------------------------------------------------|:---------------------------------------------------------:|
| `--world <directory>`             | The world folder for which to generate the cache files.                 |                            Yes                            |
| `--region <directory>`            | The world's region folder to override the region folder from `--world`. |                            No                             |
| `--output <directory>`            | Where the cache files will be saved.                                    |                            Yes                            |
| `--zoom-level <1 or 2 or 4 or 8>` | The zoom level for which to generate the images.                        | No, generates images for all zoom levels if not specified |

### Mode: Generate image

**Notice**
This requires a working JavaFX installation.

| Parameter                                | Description                                                                                                    |                      Mandatory                      |
|------------------------------------------|----------------------------------------------------------------------------------------------------------------|:---------------------------------------------------:|
| `--world <directory>`                    | The world folder from which to generate the image.                                                             |                         Yes                         |
| `--region <directory>`                   | The world's region folder to override the region folder from `--world`.                                        |                         No                          |
| `--output <png-file>`                    | Where the generated image will be saved.                                                                       |                         Yes                         |
| `--selection <csv-file>`                 | The csv-file from which to a load a selection.                                                                 |                         Yes                         |
| `--render-height <-64 - 319>`            | The Y level for which to render the image.                                                                     |                  No, default `319`                  |
| `--render-caves`                         | Whether to render in cave mode. Does not work together with `--render-layer-only`.                             |                 No, default `false`                 |
| `--render-layer-only`                    | Whether to render the current layer only, specified be `--render-height`.                                      |                 No, default `false`                 |
| `--render-shade <true or false>`         | Whether to shade the image for height. Does not work together with `--render-caves` and `--render-layer-only`. |                 No, default `true`                  |
| `--render-water-shade <true or false>`   | Whether to shade the water for depth. Does not work together with `--render-caves` and `--render-layer-only`.  |                 No, default `true`                  |
| `--overlay-type <overlay>`               | The overlay to render on top of the image.                                                                     |                         No                          |
| `--overlay-min-value <string or number>` | The minimum value to be used for the overlay gradient.                                                         | Yes, if `--overlay-type` is specified, otherwise No |
| `--overlay-max-value <string or number>` | The maximum value to be used for the overlay gradient.                                                         | Yes, if `--overlay-type` is specified, otherwise No |
| `--overlay-data <string>`                | Additional data used for some overlay types, e.g. the `Custom` type.                                           | Yes, if the overlay type requires it, otherwise No  |
| `--overlay-min-hue <float>`              | The minimum hue used for the overlay gradient, ranging from `0.0` to `1.0`.                                    |              No, default `0.66666667`               |
| `--overlay-max-hue <float>`              | The maximum hue used for the overlay gradient, ranging from `0.0` to `1.0`.                                    |                  No, default `0.0`                  |


### Configuration parameters

These parameters can be used in combination with all modes.

| Parameter                     | Description                                                |                                  Mandatory                                   |
|-------------------------------|------------------------------------------------------------|:----------------------------------------------------------------------------:|
| `--debug`                     | Enables debug messages to be printed to the log file.      |                                      No                                      |
| `--process-threads <number>`  | The amounts of Threads to be used for processing data.     | No, default is the `amount of processor cores - 2`, minimum `1`, maximum `4` |
| `--write-threads <number>`    | The amount of Threads to be used for writing data to disk. |         No, default ìs the `amount of processor cores`, maximum `4`          |

## Filter query

A filter query is a text representation of the chunk filter that can be created in the UI of the program. It is
shown in the text field below the query editor of the Chunk filter tool.
Example:
```
--query "xPos >= 10 AND xPos <= 20 AND Palette contains \"sand,water\""
```
This will select all chunks that contain sand and water blocks and their x-position ranges from 10 to 20.
As shown, double quotes (") must be escaped with a backslash.

Groups are represented with a pair of parentheses (`(...)`). As with the chunk filter, logical operators are
evaluated from left to right, and `AND` has a higher precedence than `OR`. In other words, `a AND b OR c AND d AND
e` is the same as `(a AND b) OR (c AND d AND e)`. It differs from `c AND d AND e OR a AND b` not by the result, but
by the sequence of tests performed due to [short-circuiting](https://en.wikipedia.org/wiki/Short-circuit_evaluation).

## Change values

The query for changing NBT values in chunks looks slightly different to the filter query. It is a comma (,)
separated list of assignments.
Example:
```
--fields "LightPopulated = 1, Status = empty"
```
This will set the field "LightPopulated" to "1" and "Status" to "empty". Just like the filter query, the query to
change values is printed to the console when using the UI in debug mode.
