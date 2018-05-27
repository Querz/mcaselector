# MCA Selector
#### An external tool to export or delete selected chunks and regions from a Minecraft world.
---
## Usage
### Navigation
Executing the tool, it shows an empty window with a chunk and a region grid. To actually show a world, open a folder containing Minecraft Anvil (\*.mca) files. The tool will then render a top-down view of this world that you can zoom into and zoom out of by scrolling up and down and that you can move around using the middle mouse button (```Cmd+LMB``` on Mac OS).

Zooming out far enough disables the selection of single chunks but lets you select entire regions.

### Selections
Upon finishing selecting chunks and regions, they can be deleted or exported using the ```Selection```-menu. Exported chunks and regions are not deleted from the original world.
For the sake of performance, the tool does not actually delete chunks from mca-files, it only removes the chunk indices and lets Minecraft handle the generation of new chunks and defragmentation of the mca-files.

A selection (not the chunks and regions themselves) can also be exported or imported and even be applied to different worlds.

### Caching
The tool creates an image for each region from the provided mca-files. These images are saved separately inside a ```cache```-folder in the working directory of the program for future usage. Experience showed that a Minecraft world with a size of 10GB resulted in cached image files with a total size of 80-100MB. Caching as many regions as possible significantly improves loading times though.
