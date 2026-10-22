# Travel, Return, and Offline Players

[Design index](../DESIGN_DECISIONS.md)

## Normal entry and exit

- Only charged registered-key use permits entry; block interaction does not.
- Entry always faces north, with the south-wall door/control area behind the player.
- Store one server-side exterior return destination per player/session, including dimension and position. Preserve it across disconnection and restart.
- Normal exit returns to the last location outside pocket rooms before entry, not the anchor's location or another player's return point.
- Room-to-room travel preserves that one exterior destination. `world -> A -> B -> exit` returns to the world, not A.
- Extraction/destruction of A after the player has moved into B does not affect that player. The currently occupied room determines exceptional outcomes.
- Using the currently occupied room's key exits after normal charging and subject to cooldown.
- The south-wall control/exit block permits exit without a key, including after losing the key inside or while key use is unavailable. It does not grant reentry.

## Return priority

| Event | Primary destination/outcome | If primary destination is invalid |
| --- | --- | --- |
| Normal exit | Recorded last exterior position and dimension | Valid personal respawn, then Overworld world spawn |
| Extraction, player online | Anchor position and dimension at removal | Valid personal respawn, then Overworld world spawn |
| Extraction, player offline; anchor still extracted on reconnect | Same recorded extraction return on reconnect | Valid personal respawn, then Overworld world spawn |
| Extraction, player offline; anchor placed again before reconnect | Resume inside the same room | Not applicable |
| Destruction, player online inside | Death under the [destruction rules](ANCHORS.md) | Normal death/respawn handling |
| Destruction, player offline inside | Return alive to personal respawn on reconnect | Overworld world spawn |

- Extraction overrides normal key-entry return points.
- If an offline occupant's anchor is placed again before they reconnect, they resume inside the same preserved room, including if the anchor was relocated in the exterior world.
- If the anchor remains extracted at reconnect, return the player to the exterior location recorded at its latest extraction. If that location is invalid, use valid personal respawn, then Overworld world spawn.
- Re-evaluate current room lifecycle at reconnect: a placed, live anchor restores the room session; an extracted anchor returns the player outside; a destroyed anchor uses the nonlethal destruction fallback.
- If the room is destroyed while its occupant is offline, destruction takes precedence over a pending extraction return: return them alive to personal respawn, then Overworld world spawn.
- After an extracted-anchor return, the player can subsequently enter again with a valid key.
- A destroyed room's disconnected occupants receive the approved nonlethal exception rather than a delayed destruction death.
- Personal respawn can be invalid because it is missing, obstructed, depleted, or in an unavailable dimension. Use vanilla personal-respawn resolution before world spawn.
- Persist applicable outcomes so reconnecting cannot restore deleted rooms or bypass pending returns.

## Safety and planning

- Validate entry landing and return destinations; arbitrary saved coordinates are not automatically safe merely because respawn uses vanilla rules.
- Handle blocked destinations, unavailable dimensions, unloaded chunks, invalid saved data, disconnections, and restarts.
- Never report an unsafe return as successful or strand a player silently.
- Nearby safe-position search bounds and spawn obstruction handling still require technical planning.
