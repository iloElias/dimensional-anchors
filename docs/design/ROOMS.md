# Room Geometry, Expansion, and Boundaries

[Design index](../DESIGN_DECISIONS.md)

## Usable interior

Dimensions exclude walls, floor, and ceiling. These defaults remain server-configurable:

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
- Move boundary walls and the south-wall door to the new limits without overwriting interior constructions or exposing neighboring regions.
- Expand within the region reserved for the maximum supported room size.

## South-wall door and entry

- The initial south wall is a 3 x 3 door/control area, directly behind the entering player.
- The control/exit function is distinct from ordinary boundary blocks and requests server-authoritative exit.
- Move entry position with the south-wall door so entry remains in front of it, facing north.
- Orientation does not depend on the exterior facing direction.
- Safe landing checks are required if players build at the intended entry position.
- The door/control area cannot be broken, replaced, or moved through ordinary gameplay and must not lead into another room.
- Whether every block of the 3 x 3 area is interactive or only a dedicated control block remains open.

## Boundary protection

- Enclose all six sides with persistent, indestructible custom boundary blocks that drop no items and are unavailable for ordinary players to obtain/place.
- Enforce protection server-side, not through collision or rendering alone.
- Investigate mining/replacement, explosions, pistons, fluids/fire, pearls/teleports, flight/knockback, ordinary-player commands, and modded bypasses.
- If a player leaves the permitted interior through unauthorized movement/teleportation, kill that player immediately with:

```text
<player> tried to access places beyond imagination
```

- Use the actual player name and supported target-version death APIs.
- Respect normal death rules, including `keepInventory`.
- This reason is distinct from anchor destruction, whose message is `The universe forgot <player>`.
- Normal wall movement must not cause false deaths. Process legitimate exits and internal mod teleports correctly before violation enforcement.
- Cover login, respawn, invalid coordinates, and client position manipulation without recursive or repeated death events.

Configuration maximums, changes to existing rooms, geometry formulas, and safe entry placement remain in [open questions](OPEN_QUESTIONS.md).
