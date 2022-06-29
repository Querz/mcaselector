## Opening a World

When opening MCA Selector, it at first shows an empty map with a region and chunk grid. To see a world, use `File --> Open World` and select *the world folder* of the world you would like to open. The world folder always contains a `region` folder (and some other folders, e.g. a `poi` and since Minecraft 1.17 an `entities` folder). If the world is a single player world and the Nether and / or the End have been explored, it will ask for which dimension to be opened.

## Navigating the Map

As soon as a world or dimension has been opened, the map can be navigated using the following methods:
- Pressing and holding the middle mouse button and dragging the map (On Mac and Windows).
- Pressing and holding `Cmd` and the left mouse button and dragging the map (On Mac).
- Using `WASD` or the arrow keys to move the map (hold `Shift` to go faster).
- Using the Scroll wheel of the mouse or the "zoom" gestures of the trackpad to zoom in or out of the map.
- Using the `+` and `-` keys to zoom in or out of the map. The zoom level can be reset using `Ctrl+0` (or `Cmd+0` on Mac).
- Using `View --> Goto` to jump to a specific location.

## Grids and Layers

<table>
  <tr>
    <td>When zoomed in, MCA Selector shows the chunk grid and enables chunk selection.<br>A chunk contains all blocks in a 16x16 area.</td>
    <td>
      <p align="center">
        <a href="/Querz/mcaselector/wiki/images/Navigation/overview_zoomed_in.png">
          <img src="/Querz/mcaselector/wiki/images/Navigation/overview_zoomed_in.png" width="300">
        </a>
      </p>
    </td>
  </tr>
  <tr>
    <td>When zoomed out, MCA Selector switches to region selection mode.<br>
A region is an area of 512x512 blocks (32x32 chunks).</td>
    <td>
      <p align="center">
        <a href="/Querz/mcaselector/wiki/images/Navigation/overview_zoomed_out.png">
          <img src="/Querz/mcaselector/wiki/images/Navigation/overview_zoomed_out.png" width="300">
        </a>
      </p>
    </td>
  </tr>
  <tr>
    <td>MCA Selector renders the topmost block by default.<br>In The Nether, this is usually the Nether ceiling which is not particularly helpful.<br>To start rendering at a lower Y-level, the height slider at the top-right corner can be adjusted to start rendering below the Nether ceiling.</td>
    <td>
      <p align="center">
        <a href="/Querz/mcaselector/wiki/images/Navigation/overview_nether_layers.png">
          <img src="/Querz/mcaselector/wiki/images/Navigation/overview_nether_layers.png" width="300">
        </a>
      </p>
    </td>
  </tr>
</table>

Additional options for rendering can be found in `File --> Render Settings` (Shortcut `E`), e.g. for changing the background pattern, cave mode and different kinds of shading.