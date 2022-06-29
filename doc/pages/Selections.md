## Create a selection

A selection can be created in multiple ways.

<p align="center">
  <img src="/Querz/mcaselector/wiki/images/Selections/selections.png" alt="MCA Selector window showing chunk and region selections">
</p>

### Using the mouse buttons

* There are two selection modes. When zooming in until the chunk grid can be seen, individual chunks can be 
  selected or deselected. When zooming out until only the region grid can be seen, entire regions can be selected 
  or deselected.
* Click the left mouse button to select a chunk or region. Hold and drag to select multiple chunks or regions. To 
  deselect, click the right mouse button. Hold and drag to deselect multiple chunks or regions.

### Using filters

* See [Chunk Filter](Chunk-Filter.md).

### Inverting an existing selection.

* Use `Selection --> Invert` to invert a selection.
* A selection can also be inverted on a per-region basis *only* if a region contains any form of selection (either the 
  entire region is selected or at least one chunk in the region is selected). Use `Selection --> Invert selected regions` to achieve this.
  This is not a toggle though, so inverting a fully selected region will fully deselect it, and it will not be 
  reselected after inverting selected regions again.

---

## Save and load selections

A selection can be saved using `Selection --> Export selection`. It can be loaded using `Selection --> Import 
selection`.

---

## Selection file format

The selection file format is a simple csv format.
* In case the selection is inverted, the first line in the file is the string `inverted`. All listed chunks and regions
  below will then be treated as "not selected" but everything else as "selected".
* All values in a single line are separated by semicolon.
* When a full region is selected, it is represented by a single line only containing the region coordinate. E.g. if 
  selecting the region `5 | -3` the line would be `5;-3` only.
* A single selected chunk is represented by a single line containing the region coordinate followed by the absolute 
  chunk coordinate. E.g. selecting the chunk `3 | -36` results in the line `0;-2;3;-36`.

---

## What can I do with selections?

* A selection can be used to *delete* selected chunks using `Selection --> Delete selected chunks`.
* It can be used to *export* selected chunks using `Selection --> Export selected chunks`. When choosing a target 
  folder, it will create a new `region`, `poi` and `entities` folder with the appropriate files.
* By using `Selection --> Copy chunks` the selection can be *copied* to the clipboard. It can then be *pasted* into either 
  the same world or a completely different world using `Selection --> Paste chunks`. Move the pasted chunks around 
  to the desired target location, then use `Selection --> Paste chunks` again to open the [Chunk Import](Chunk-Import)
  dialog for more options.
* *Bulk-change values* in those chunks. See [NBT Changer](NBT-Changer) for more information.
* Export all selected chunks as an *image* using `Selection --> Export as image`. When empty chunks are selected, they 
  are exported without the background color but with transparency instead to simplify further editing of the 
  resulting image.
* Clear all cache associated with the selected chunks. This includes all rendered images of regions used for the 
  top-down view and all accumulated data for all overlays.

---

## Special cases

### Selecting only one chunk

* Edit this chunks data manually (see [Chunk Editor](Chunk-Editor)).
* The [NBT Changer](NBT-Changer) now shows this chunks values as a preview for some fields.

### Selecting exactly two chunks

* Using `Tools --> Swap chunks`, the two selected chunks can be swapped. This can be useful for when chunk 
  corruption occurred while Minecraft tried to save the world, like mentioned e.g. in [this issue](https://bugs.mojang.com/browse/MC-178029).
