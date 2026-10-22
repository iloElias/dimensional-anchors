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

## Internal blocks

- Boundary blocks enclose each room and are not obtainable/placeable by ordinary players.
- One distinct south-wall exit/control block permits keyless exit.

## Artwork and resources

- The project owner supplies PNG sprites; the coding agent must not generate, edit, or synthesize them.
- Blank and registered dimensional keys use different sprites. Registration changes appearance; copies use the registered sprite.
- The five anchor texture paths already exist under `src/main/resources/assets/dimensionalanchors/textures/block/`: `copper_dimensional_anchor.png`, `iron_dimensional_anchor.png`, `gold_dimensional_anchor.png`, `diamond_dimensional_anchor.png`, and `netherite_dimensional_anchor.png`. Some are templates; none should be assumed final artwork or edited by the coding agent.
- `dimensional_limiter_block.png` also exists in that directory as a possible boundary-block template; confirm its intended role/final artwork with the project owner.
- Plan assets for both key states, catalyst, template, boundary block, and the single control/exit block.
- The former key-shaped anchor concept is superseded: anchors are blocks, keys are access items.
- Use the `dimensionalanchors` namespace and verify exact target-version model, item-definition, texture, language, and registry conventions before implementation.
- Project-facing names, comments, documentation, configuration descriptions, and initial UI strings are in English.
- If feasible, show a tooltip for keys whose anchor is absent; do not label the room destroyed because its contents and region are preserved.
