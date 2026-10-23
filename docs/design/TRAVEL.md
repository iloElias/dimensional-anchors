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
- The bounded search below is approved and implemented; integrating it with
  normal exit and reporting failure to the player remains pending.

## Implemented session-data foundation

- `PlayerRoomSavedData` stores sessions by player UUID in the Overworld's
  `dimensionalanchors:player_rooms` saved data, shared across dimensions.
  Schema version 1 records the current room's anchor UUID and one exterior
  destination (dimension, exact coordinates, yaw, and pitch).
- Entering creates a session only if none exists; an unexpected second entry
  fails instead of overwriting the original return. Changing rooms preserves
  that return. Completing exit removes only the matching player's session and
  rejects a stale exit for a room the player no longer occupies.
- Sessions are not removed because an anchor is absent or because a player
  disconnects. Disk reload preserves them, including separate occupants of the
  same room. There are no login, respawn, or teleport hooks yet.
- Return data rejects the pocket dimension, non-finite coordinates/rotation,
  coordinates outside vanilla spawnable bounds, and invalid pitch. These are
  structural checks, not proof of a safe destination. Other dimension IDs are
  retained even when unavailable, so future return handling can use explicit
  fallback rather than erase the session.
- Strict decoding rejects corrupt, unsupported-version, and partially readable
  stores without replacing them. The same saved-data safeguards are shared with
  permanent room allocation.
- This is a persistence API, not travel authorization or a teleport transaction.
  No current gameplay path creates or changes a session. In particular, charging
  a key only prepares the room, and administrator-command entry does not create
  a session. A future travel service must validate lifecycle, actual location,
  landing safety, and destination availability before using this API.
- Integration of safe entry/return resolution, exit control behavior,
  disconnect/reconnect reconciliation, death handling, and
  ordering/rollback between session writes and teleportation remain pending.
  No hard-crash atomicity between player position and saved data is claimed.

## Implemented safe-destination resolver

- Owner approved a non-destructive search: try the intended room entry first,
  then other standing positions strictly within that room's usable interior.
  Entry always faces north. A fully obstructed room fails rather than clearing
  blocks, placing a platform, or selecting another reservation.
- Exterior return tries the exact saved position first, preserving orientation.
  If blocked, search at most four block coordinates horizontally on each axis
  and four vertically around the saved block position (a 9-by-9-by-9 box).
  Alternatives are ordered by distance, then Y/Z/X for deterministic ties.
- The resolver reuses vanilla safe-dismount floor geometry, including slabs,
  then checks the player's standing bounding box, collision, supporting ground,
  dimension build height, and world border. Reject fluids, vanilla dangerous
  blocks in the body/support footprint, invalid-spawn blocks, and portal blocks.
  This is not a guarantee against arbitrary modded hazards or future world
  changes; destinations must be resolved immediately before actual travel.
- If the saved dimension is unavailable or no safe nearby return exists, try
  vanilla personal-respawn resolution without consuming a respawn-anchor charge.
  A missing, obstructed, depleted, or unavailable personal respawn falls back
  to a bounded search around the Overworld's configured world-spawn position.
  Never use the pocket dimension as a fallback.
- A focused access transformer exposes vanilla's personal-respawn-only helper
  and its result type. This preserves vanilla/modded respawn-block hooks without
  invoking the public method's automatic, potentially wider world-spawn search.
- Search loads only destination-area chunks on demand and adds no persistent
  tickets. Room search considers at most 4,096 block candidates; each exterior
  search considers at most 729. Personal respawn is queried locally at its block.
- All unsafe outcomes raise an explicit error after fallbacks are exhausted.
  Resolution does not teleport, consume resources, change blocks, or remove a
  player session. The future travel service must show failure feedback and keep
  the session when resolution fails.
- `SafeTravelDestination` is not wired into key use or the exit control yet.
  Room-generation client checks, session/teleport transaction handling,
  reconciliation after reconnect/death, and movement enforcement are still
  prerequisites for enabling playable travel.
