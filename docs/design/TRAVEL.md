# Travel, Return, and Offline Players

[Design index](../DESIGN_DECISIONS.md)

## Normal entry and exit

- Entry is authorized by a valid registered key: either charged remote use or
  interaction with its matching placed anchor while holding that key.
- This supersedes the previous prohibition on anchor-interaction entry. Both
  entry paths are implemented (2026-10-23) through `PlayerTravelService` and
  the transition journal; in-game validation is pending (TT-082).
- Entry always faces north, with the south-wall door/control area behind the player.
- Store one server-side exterior return destination per player/session, including dimension and position. Preserve it across disconnection and restart.
- Charged remote entry records exact exterior position, dimension and orientation
  when entry is committed. Normal exit returns there, not to another player's
  position or the anchor's current location.
- Anchor-interaction entry records a safe point in front of the block according
  to its facing. Capture it at entry; later relocation/destruction does not
  change that session's return. The anchor has no facing state, so the clicked
  horizontal side is used (top/bottom clicks use the player's side); the
  return faces the anchor and falls back to the player's position if no safe
  point is found nearby.
- The key cooldown (400 ticks) applies only after a successful remote key
  teleport (entry, room change or exit). Anchor interaction, the exit control
  and failed charges do not apply it. It is added after arrival, since a
  dimension change recreates the client player and loses earlier cooldowns.
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
- The bounded search below is approved and implemented; normal exit invokes
  it and reports failure. The exit/hardening checkpoint passed local GameTests;
  client and broader integration checks remain pending.

## Approved interrupted-transition policy, 2026-10-23

These definitions were approved before implementation. The anchor recovery
command is a separate feature; it never repairs or moves player sessions.
The following player-transition work is still pending:

- Persist a unique operation ID, transition type, source location and room,
  expected destination and room, original exterior return, and operation stage
  before committing travel. Compare actual loaded player/session state on
  restart; a journal alone does not make player data and saved data atomic.
- Outside with a retained session: finalize cleanup only when the journal and a
  verified safe expected arrival prove the exit. Without that evidence, retain
  the session, diagnose the ambiguity, and deny new entry; do not retroactively
  kill or pull the player back.
- Interrupted travel: unchanged source cancels the attempt; proven safe expected
  arrival completes it. For an intermediate failed state, attempt the previously
  captured safe source, without an unbounded teleport/retry loop.
- Failed A-to-B travel returns to a revalidated safe point in A and retains the
  original exterior return. If no safe source-room point exists, try that
  exterior return and its fallback chain. Never overwrite a session changed by
  another operation or infer a new room from proximity.
- Pocket occupants without a valid session are not assigned arbitrary rooms.
  After authoritative data loads, attempt safe exterior rescue through the
  player's spawn fallback chain, with explicit diagnostics.
- If recovery also fails, preserve the records, suspend punitive movement checks
  for the failed transition, report the issue, and block new travel. No
  success-shaped fallback, blind data deletion, or repeated automatic teleport.
  A new explicit recovery attempt must remain possible without requiring a key
  to leave; the player-facing trigger is part of the pending recovery wiring.
- Safe-return priority: recorded exterior point and its approved nearby search;
  valid current personal respawn; last exterior spawn actually used safely by
  this player and recorded by the mod; bounded safe Overworld spawn search.
  Revalidate each candidate. The historical spawn is not an arbitrary walking
  position and cannot be invented for players predating tracking.
- Existing anchor absence rules are unchanged: occupants may stay and exit;
  no new entry until an anchor is placed again.

## Implemented exit transition journal, 2026-10-23

- `PlayerRoomSavedData` optionally stores one `PlayerTransition` per player
  (`transitions`, still data version 1; older saves load with none). It records
  operation ID, type (`exit` only), source room, expected destination
  (dimension, position, rotation), original exterior return and stage
  (`prepared` before teleport, `teleported` after the server accepts it).
- Exit records the transition before teleporting, removes it with the session
  on confirmed arrival, cancels it when the teleport is rejected without
  movement, and retains it when the outcome is ambiguous.
- Login/respawn: a player in the source room cancels the transition; an
  exterior player within one block of the expected destination at a safe
  position completes the exit; any other state keeps the session and journal
  and reports a diagnostic. No teleport is performed by reconciliation.
- Entry (`entry`) and room change (`room_change`) are journaled the same way.
  The session is created or switched only after confirmed safe arrival. At
  login, a player at the expected arrival inside the target room completes the
  transition; an entry whose player never reached the pocket is cancelled; a
  room change whose player is still in the source room is cancelled; anything
  else is retained and reported.
- Entry is denied while a transition is unresolved, outside the pocket with a
  leftover session, or inside the pocket without a session.
- Pending: intermediate-state rescue, historical spawn tracking and the
  explicit player-facing retry.

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
  same room. Login and post-placement respawn reconciliation now run on the
  server event bus; neither hook teleports or expels pocket occupants.
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
- Exit integration now removes a session only after confirmed safe arrival and
  only if the captured in-memory session is still current. Entry integration,
  complete network reconnect/restart validation and
  recovery of partial transitions remain pending. No hard-crash atomicity
  between player position and saved data is claimed.

## Login and respawn reconciliation

- Owner clarified on 2026-10-23: do not expel anyone from a room. An absent
  anchor affects new entry only, never the existing occupant's session.
- Login in the recorded, completed room preserves the session and exact player
  state, without checking anchor placement, generating blocks, or adding tickets.
  Disconnect does not clear anything; the server-global store survives player
  object replacement.
- An exterior login with a leftover session is ambiguous. Preserve it and report
  an administrative diagnostic; do not infer successful exit or teleport the
  player back. Recovery of this state remains pending.
- A live exterior respawn ends only that player's captured session after vanilla
  has placed the replacement player. Do not change vanilla respawn destination,
  inventory, death drops, respawn-block consumption, or other occupants.
- A pocket respawn preserves the session. Missing sessions, invalid coordinates,
  incomplete/missing rooms, or position outside the recorded interior produce
  explicit feedback and server diagnostics, without movement, session deletion,
  inferred room assignment, or automatic death.
- Reconciliation shares the per-player travel guard, refuses to interfere with
  active travel, and releases only its own guard in `finally`. Dead/removed
  players are not reconciled. No periodic retry, clone-time cleanup, or
  dimension-change cleanup is installed.
- Valid pocket reconciliation also establishes a full-body movement baseline.
  It does not punish an already inconsistent location. Monitoring uses player
  and session identity; old-player logout/tick events do not clear a respawn
  replacement's observation.

## Movement and legitimate transitions

- Server post-player ticks enforce departures only after trustworthy occupancy
  of the recorded room has been observed. The boundary check uses the current
  full-body box, not just the feet block, and ignores anchor placement. See
  [rooms](ROOMS.md#implemented-movement-enforcement) for bounds and death rules.
- The travel guard suppresses movement checks throughout resolution and
  teleport hooks. Confirmed safe exit removes its matching session and monitor.
  Partial movement, throwing hooks, unconfirmed arrival or cleanup conflicts
  suspend punitive monitoring with explicit feedback rather than killing the
  player for the mod's failed transition.
- A rejected request with no uncertain movement does not disable enforcement.
  Vanilla loading immunity postpones damage, preserving the baseline until a
  later eligible tick. A death-canceling/resurrecting hook is diagnosed once
  rather than receiving recursive/repeated lethal damage.
- Suspended monitoring is transient safety state, not an exterior-session
  recovery mechanism or travel authorization. Login outside still warns and
  preserves the session; a valid interior reconciliation can restore monitoring.
  Future entry/room-change transactions must explicitly coordinate observation
  and guard/session ownership before those gameplay paths are enabled.

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
- The current resolver, before the approved historical-spawn addition, uses this
  chain: if the saved dimension is unavailable or no safe nearby return exists, try
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
  player session. The exit service shows failure feedback and keeps the session
  when resolution fails.
- The generated south-wall control and the current room's matching key can now
  perform server-authoritative exit. The service verifies the player's pocket
  dimension, recorded room allocation and generation, actual position inside
  that room, and (for control use) the exact generated control block. Exit
  resolves the safe destination immediately before teleportation and removes
  only the matching player's session after the server confirms the teleport.
  Unsafe destinations and rejected teleports report failure and retain the
  session. The control works without a key, including if the anchor is absent.
- Exit is restricted to the server thread and protected by a per-player,
  server-local guard released in `finally`; the guard is not persisted and
  another player's travel is independent. After teleportation, verify actual
  dimension, coordinates (within 0.000001 blocks), orientation (within 0.0001
  degrees, allowing equivalent wrapped yaw), liveness, and standing safety
  before removing the captured session. Confirmed arrivals reset fall distance
  and velocity.
- Captured-session identity rejects replacement/recreated sessions and room
  changes away and back during hooks. Do not remove or restore a replacement
  session. Teleport exceptions, false success, unsafe/redirected arrivals, and
  cleanup conflicts are logged and reported without normal success or a success
  cooldown. No automatic second teleport/rollback is attempted: a hook may have
  moved the player or modified the session before failing. Recovery of an
  exterior player with a retained session remains pending reconciliation, and
  is a prerequisite for enabling entry. Exit/hardening and arrival-edge tests
  passed in the authorized 32-GameTest run; this does not establish arbitrary
  third-party hook compatibility or live client behavior.
- Charging a key for an unoccupied room still only prepares it; charging a key
  for the currently occupied room exits. Entry and room-to-room travel remain
  disabled. Full lifecycle validation, partial-transition recovery and movement
  enforcement remain prerequisites for enabling entry, and no hard-crash atomicity
  between player position and saved data is claimed.
