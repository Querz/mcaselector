When selecting a single chunk, the menu "Edit chunk" becomes available. It allows precise editing of the entire NBT
structure of that chunk, including poi and entities data. Names and values can be changed, added, deleted or moved
(via drag & drop).

<p align="center">
  <img src="/Querz/mcaselector/wiki/images/Chunk-Editor/edit_chunk.png" alt="MCA Selector window showing the NBT editor">
</p>

## Edit NBT tags

The editor allows easy editing of NBT tags. If a tag has a name, double-clicking the name will make it editable. To apply
a change, press `Enter` while in the editor's text field. If a tag has a value, double-clicking the value will make it editable.
If the tag has a name *and* a value, double-clicking any of them will make them editable.

Tags can be dragged and dropped as well.

Tags can be deleted by selecting them and pressing the trashcan icon in the bottom bar of the editor.

New tags can be added by selecting any tag and clicking one of the tag icons in the bottom bar. Keep in mind that the order
in which the tags are added cannot be guaranteed when the parent tag is a Compound-Tag.

To edit the editor and save the modified chunk data to disk, press `Apply`.

## Array editor

When editing a field containing a Byte-, Int- or Long-Array-Tag, instead of a simple text field to edit the value an `edit` button
becomes available that opens a dedicated editor to make it easier to edit those tags.
Specifically the Long-Array-Tag will have advanced features like bitwise splitting and a bit-overlap toggle.

## Editor Tabs

The chunk editor shows (depending on the chunk version and whether data is available or not) the chunk data from the `region`
folder, but also chunk data from `poi` and `entities`. `poi` contains "Points Of Interest", which are e.g. Villager work stations
and Nether portals. `entities` contains all entity data since Minecraft 1.17.

**Notice** When the NBT editor does not show any data, the cached top-down view might be outdated, and the chunk
might not exist anymore in the mca files. In that case, clearing the cache will re-render the regions from scratch
and show the up-to-date top-down view.