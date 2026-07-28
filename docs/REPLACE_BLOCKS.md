# ReplaceBlocks

ReplaceBlocks changes matching blocks in selected chunks. It is destructive: close Minecraft, make a backup, and test unfamiliar rules on a copied world first.

## Builder and advanced text

The Builder generates the same text format used by the existing advanced field. Builder rules can be edited as text, and valid advanced rules can be reopened in the Builder when their controls are representable. Advanced state or tile SNBT that has no dedicated control is preserved as fallback text.

Common source forms:

- `literal(minecraft:stone)` matches that block ID exactly.
- `regex(minecraft:.*_log)` intentionally uses a Java regular expression.
- `{Name:"minecraft:stone",Properties:{...}}` is an exact stored block-state match.
- `props({Name:"minecraft:oak_stairs",Properties:{facing:"north"}})` matches the named block and only the listed properties.
- `tile(source)` and `no_tile(source)` require or exclude a block entity at the source position.
- `y(-64..64, source)` restricts the source Y coordinate; either bound may be omitted.
- `biome(minecraft:plains;minecraft:forest, source)` restricts modern 1.18+ biome cells.

Targets may be a block ID, block-state SNBT, or a target followed by block-entity SNBT, for example `minecraft:barrel;{id:"minecraft:barrel"}`. Multiple rules are comma-separated.

Legacy bare or quoted sources remain Java regular expressions for compatibility. Prefer `literal(...)` or `regex(...)` in new rules so intent is explicit.

## Catalogue compatibility

The Builder bundles block-state catalogues for Java 1.18.2, 1.20.6, 1.21.9, 1.21.11, and 26.2. The newest catalogue is selected by default; selection remains manual. Catalogues supply suggestions and property values only, and the displayed Java/DataVersion label identifies the active catalogue.

An empty Builder switches catalogue immediately. A non-empty Builder asks first: Cancel keeps the old catalogue and all current work, while Confirm selects the new catalogue and fully resets fields, properties, Extra NBT, Y/biome filters, rules, selections, generated text, validation, and open popups.

A syntactically valid future or modded resource ID remains accepted even when it is absent from the active catalogue; the Builder shows a non-blocking warning and provides no property suggestions for it. Target warnings are stronger because an unsupported target ID can produce a world that Minecraft cannot load as intended.

Built-in and custom presets remain versionless ReplaceBlocks text. Switching catalogue clears the current preset selection and Builder content but never deletes saved presets. Applying a custom preset still appends valid rules; an exact source or target ID outside the active catalogue produces a non-blocking advisory, while regex sources are not guessed.

The catalogue is not world-version detection and does not guarantee that an ID exists in the target world. MCA Selector does not rename or migrate block IDs between Minecraft versions. Verify renamed/removed IDs yourself.

## Preview and execution safety

Preview is non-mutating. It reports affected chunks/sections, per-rule matches, overlaps, block-entity changes, unsupported chunks, and errors. Preview counts are the recommended check before execution, but they do not replace a backup.

Replacing air can complete missing sections and greatly increase world size. Replacing blocks that carry block entities can remove or rewrite their data. Modern execution invalidates affected lighting and heightmaps; existing adjacent chunks may also be marked for relighting.

Tile, Y, and biome conditions fail closed on unsupported pre-1.18 formats. If a ReplaceBlocks-only operation fails in any chunk, MCA Selector aborts saving that region and logs the failing chunk coordinate. Mixed legacy field changes preserve the historical per-chunk behavior.

After a risky change, load only a copied world in the intended Minecraft version, save/reload it, and inspect the game log for chunk, palette, block-entity, lighting, or heightmap errors.
