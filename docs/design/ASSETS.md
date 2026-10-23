# Item/Block Catalog and Assets

[Design index](../DESIGN_DECISIONS.md)

## Player-facing catalog

| Content | Role |
| --- | --- |
| Copper Dimensional Anchor | Initial anchor block and inventory item |
| Iron Dimensional Anchor | First upgrade |
| Gold Dimensional Anchor | Second upgrade |
| Diamond Dimensional Anchor | Third upgrade |
| Netherite Dimensional Anchor | Final upgrade |
| Blank dimensional key | Initial registration and copying ingredient |
| Registered dimensional key | Room access and charged exit |
| Dimensional Catalyst | Initial anchor, key, and template ingredient |
| Dimensional Upgrade Template | Consumed Netherite smithing template |

Blank/registered keys are gameplay states; separate sprites do not mandate separate registered item types.

### Deferred colored-key visuals

Future crafting-table key-and-dye coloring supports all 16 vanilla dyes for both
key states. Only the handle changes color; the blank/registered sprite distinction
must remain visible for every color (32 colored visual variants in total).
This does not require 32 registered items or 32 standalone PNGs; layered tinting
versus individual textures must be assessed before implementation. No artwork or
runtime resources are added by this plan. See [keys](KEYS.md#planned-handle-colors-deferred).

## Internal blocks

- Boundary blocks enclose each room and are not obtainable/placeable by ordinary players.
- One distinct south-wall exit/control block permits keyless exit.

## Artwork and resources

- The project owner supplies PNG sprites; the coding agent must not generate, edit, or synthesize them.
- Blank and registered dimensional keys use different sprites. Both files exist, but currently contain identical image data; the registered-key sprite is awaiting the planned visual change. Registration changes appearance; copies use the registered sprite.
- The five anchor textures exist under `src/main/resources/assets/dimensionalanchors/textures/block/`: `copper_dimensional_anchor.png`, `iron_dimensional_anchor.png`, `gold_dimensional_anchor.png`, `diamond_dimensional_anchor.png`, and `netherite_dimensional_anchor.png`. Some existing artwork may be a template; do not assume any is final or edit it.
- `dimensional_limiter_block.png` exists as the room-boundary texture, and `dimensional_exit_block.png` exists for the dedicated exit/control block. Confirm whether either needs final artwork.
- Item textures exist for the catalyst (`dimensional_catalyst.png`) and upgrade template (`dimensional_upgrade_template.png`).
- `chestplate.png` is also present in the item texture folder but does not correspond to an approved catalog entry.
- All listed texture files are 16 x 16 PNGs.
- Basic model resources are now present under `assets/dimensionalanchors/`: cube models and blockstates for the five anchors and two internal blocks, plus client item definitions and flat item models for the catalyst, upgrade template, and both key states.
- Anchor client item definitions and model filenames currently assume each block item uses the same registry path as its texture. Confirm those IDs when the content is registered. The block models use the entire texture on every face and are initial full-cube models, not custom 3D designs.
- The former key-shaped anchor concept is superseded: anchors are blocks, keys are access items.
- Use the `dimensionalanchors` namespace and verify exact target-version model, item-definition, texture, language, and registry conventions before implementation.
- Project-facing names, comments, documentation, configuration descriptions, and initial UI strings are in English.
- If feasible, show a tooltip for keys whose anchor is absent; do not label the room destroyed because its contents and region are preserved.
