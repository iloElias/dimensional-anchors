# Room Geometry, Expansion, and Boundaries

[Design index](../DESIGN_DECISIONS.md)

## Usable interior

Dimensions exclude walls, floor, and ceiling. These are fixed defaults for now; no dimension-size configuration is planned:

| Tier | Interior | Interior volume |
| --- | --- | --- |
| Copper | 3 x 3 x 3 | 27 |
| Iron | 5 x 5 x 5 | 125 |
| Gold | 7 x 7 x 7 | 343 |
| Diamond | 10 x 10 x 10 | 1,000 |
| Netherite | 16 x 16 x 16 | 4,096 |

## Expansion

- Keep the floor at a fixed height; expand upward and horizontally around a stable reference.
- This supersedes the original requirement for vertical symmetry. Do not remove support beneath existing floor-level constructions.
- Existing placed blocks and room contents remain at their coordinates.
- A 3-to-5 horizontal expansion adds one block to each side, preserving the centered arrangement.
- Odd/even transitions require consistent horizontal rounding. The unavoidable extra block goes to a fixed side without moving constructions. The exact coordinate formula remains technical planning.
- Upgrades are permanent; downgrades are unsupported.
- Move boundary walls and the single south-wall control block to the new limits without overwriting interior constructions or exposing neighboring regions.
- Expand within the region reserved for the maximum supported room size.

## South-wall door and entry

- The initial south wall has one dedicated door/control block, directly behind the entering player. The surrounding blocks are ordinary protected boundary blocks.
- The control/exit block is distinct from ordinary boundary blocks and requests server-authoritative exit.
- Move the control block with the south wall so it stays behind the player; entry remains inside the room, facing north.
- Orientation does not depend on the exterior facing direction.
- Safe landing checks are required if players build at the intended entry position.
- The control block cannot be broken, replaced, or moved through ordinary gameplay and must not lead into another room.
- The south wall has one dedicated control/exit block. The rest of the boundary remains ordinary protected boundary.

## Boundary protection

- Enclose all six sides with persistent, indestructible custom boundary blocks at least two blocks thick. Generated blocks drop no items. Players may break the same block types they placed in creative; generated room boundaries remain protected even in creative.
- Enforce protection server-side, not through collision or rendering alone. Distinguish generated boundary blocks from player-placed blocks; block identity and dimension alone are insufficient. Commands, non-player block changes, and modded bypasses remain separate protection cases.
- Verify manually that player-placed limiter and exit blocks can be broken in creative. Once rooms are generated, verify generated boundary blocks cannot be broken in survival or creative and drop no items. Also investigate explosions, pistons, fluids/fire, pearls/teleports, flight/knockback, ordinary-player commands, and modded bypasses.
- If a player leaves the permitted interior through unauthorized movement/teleportation, kill that player immediately with:

```text
<player> tried to access places beyond imagination
```

- Use the actual player name and supported target-version death APIs.
- Respect normal death rules, including `keepInventory`.
- Anchor collection or destruction does not cause a death message or remove occupants. This boundary-violation death applies only when a player leaves the permitted interior through unauthorized movement or teleportation.
- Normal wall movement must not cause false deaths. Process legitimate exits and internal mod teleports correctly before violation enforcement.
- Cover login, respawn, invalid coordinates, and client position manipulation without recursive or repeated death events.

## Implemented geometry and generation

- `RoomGeometry` defines usable cubic interiors of 3, 5, 7, 10, and 16 blocks.
  Physical generation uses these coordinates within the permanent reservation.
- Floor blocks are at Y=63; usable space begins at Y=64. Expansion keeps this
  floor fixed and raises the ceiling.
- For horizontal reference `(x, z)` and interior size `n`, minimum coordinates
  are `(x - floor(n/2), z - floor(n/2))`; maximums are minimums plus `n - 1`.
  Even sizes extend one extra block north/west. All successive interiors contain
  the preceding interior without moving existing coordinates.
- The shell extends two blocks beyond the interior on all six sides. The south-wall control block occupies the inner boundary layer, one block behind the entry.
- The intended entry feet position is at reference X, Y=64, and the southernmost
  interior Z. The single control position is one block south and one block above
  those feet, in the inner layer of the south wall. Future entry faces north.
- These are default coordinates, not proof of a safe landing after construction.
  Safe landing and functional exit controls remain pending. No teleportation
  uses these coordinates yet.

- Temporarily, a full charge with an available linked anchor prepares its room
  without moving the player, as approved by the owner. An upgraded anchor expands
  the existing room on the next charge; registration/copying do not generate.
- Saved generation state records target tier and completion, retaining the previous
  tier while expanding. It is flushed before writes so pending work can resume at
  the same tier. Existing completed rooms are verified, not regenerated.
- Preflight rejects unexpected blocks in coordinates needed for generation or
  expansion before writing. Existing usable interior blocks and block entities
  are untouched. Expansion removes only the previous generated shell where it
  becomes interior and relocates the one south-wall control.
- Boundary protection is derived from persisted generated geometry (including both
  shells during a pending expansion). Server-side break cancellation sends a
  corrective block update, including in creative, rather than breaking and
  replacing blocks or dropping items.
- The same utility block types placed in an interior, ungenerated region, or
  another dimension are not protected by this listener. Both utility types have
  no loot table, bedrock hardness/resistance, and blocked piston movement.
- Commands/modded bypasses and unauthorized player movement are not covered by
  this checkpoint. A completed boundary changed through such a bypass fails
  verification explicitly; generation does not silently recreate it.
- Normal persistence and pending-state recovery have regression tests awaiting CI.
  Saved data and chunks do not form an atomic crash-safe transaction.

Safe entry placement and migration remain in [open questions](OPEN_QUESTIONS.md).
