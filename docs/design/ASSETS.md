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
- The distinct south-wall exit/control block or area permits keyless exit.
- Exact final display names and the fully interactive 3 x 3 door versus a dedicated control block need definition.

## Artwork and resources

- The project owner supplies PNG sprites; the coding agent must not generate, edit, or synthesize them.
- Blank and registered dimensional keys use different sprites. Registration changes appearance; copies use the registered sprite.
- Plan assets for all five anchor variants, both key states, catalyst, template, boundary block, and door/control appearance as needed.
- The former key-shaped anchor concept is superseded: anchors are blocks, keys are access items.
- Use the `dimensionalanchors` namespace and verify exact target-version model, item-definition, texture, language, and registry conventions before implementation.
- Project-facing names, comments, documentation, configuration descriptions, and initial UI strings are in English.
- A destroyed-room key tooltip (`Linked room destroyed`) is conditional on technical feasibility and reliable server-synchronized status.
