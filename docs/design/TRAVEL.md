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
| Anchor extracted/destroyed while player is inside | Stay in the preserved room | Not applicable; player can exit normally |
| Player reconnects while room's anchor is absent | Resume existing room session; no new entry | Not applicable |
| Room's anchor is recovered/replaced | Existing occupants remain; new key entries resume | Not applicable |

- Collecting or destroying the anchor does not interrupt or teleport players already inside. They may use the room's normal key exit or south-wall control block.
- While the anchor is absent, suspend new entries by key. This also applies when the block or dropped item is destroyed; destruction does not delete the room.
- An occupant who disconnects inside resumes their existing session on reconnect. If the anchor remains absent, they can leave but cannot newly enter. If the anchor was recovered, the room is available for normal key entry again.
- No pending extraction return or destruction-death outcome is created by anchor removal under this simplified design.
- Personal respawn can be invalid because it is missing, obstructed, depleted, or in an unavailable dimension. Use vanilla personal-respawn resolution before world spawn.
- Persist the player's room session and anchor availability so reconnecting cannot enter through an absent anchor or lose access to an existing room session.

## Safety and planning

- Validate entry landing and return destinations; arbitrary saved coordinates are not automatically safe merely because respawn uses vanilla rules.
- Handle blocked destinations, unavailable dimensions, unloaded chunks, invalid saved data, disconnections, and restarts for normal exit only.
- Never report an unsafe return as successful or strand a player silently.
- Nearby safe-position search bounds and spawn obstruction handling still require technical planning for normal exit.
