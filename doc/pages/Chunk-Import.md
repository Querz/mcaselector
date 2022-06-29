Importing chunks can be easily done by opening the target world first using `File --> Open` and then merging the
chunks of a second world using `Tools --> Import chunks`. After selecting a folder containing region files, it is
possible to import the chunks with a bunch of additional options.

<p align="center">
  <img src="/Querz/mcaselector/wiki/images/Chunk-Import/import_chunks.png" alt="MCA Selector window showing the chunk import">
</p>

## Options

### Offset

Import chunks with an offset in chunk coordinates. To convert block coordinates to chunk coordinates, divide them by 16.

### Overwrite existing chunks

Deletes existing chunks and overwrites them with the imported ones.

### Apply to selection only

Import chunks into the current selection.

### Y-Offset

Shifts the imported chunks up or down in increments of 16 (the height of a chunk section). This can be used in combination
with the `Sections` option to stack chunks vertically.

### Sections

Defines one, or a range of sections to be imported. A range has the format `<from>:<to>`, inclusive.
Omitting `<from>` sets the lowest possible value, omitting `<to>` sets the highest possible value. `:`, `true` or
empty means _all_ sections. Multiple ranges or single indices can be defined by separating them with a comma. If a
chunk does not already exist at the location, it will be created containing only the specified sections of the
imported chunk.

---

**Notice**
Commands inside of command blocks will not be changed.
Maps will not be updated, because their data is not stored inside region files.

---

## Copy-Paste

It is possible to copy a selection to the system clipboard and pasting it to a different location in the same world
or into an entirely different world.
After making a selection, use `Selection --> Copy chunks` or press `Ctrl+C` (`Cmd+C` on Mac). After navigating to
the location in the world where the copied chunks need to be pasted, use `Selection --> Paste chunks` or press
`Ctrl+V` (`Cmd+V`on Mac) to display an overlay showing where the clipboard will be imported to. The overlay can be
moved around by pressing and holding the left mouse button. Press `Ctrl+V` again to import the chunks at the
selected location. This will open the [Import chunks](Chunk-Import) dialog with prefilled values depending on where
the overlay has been placed.
Copying can be cancelled by pressing `Esc`.

<p align="center">
  <img src="/Querz/mcaselector/wiki/images/Chunk-Import/copy_paste.png" alt="MCA Selector window showing copy-paste overlay">
</p>