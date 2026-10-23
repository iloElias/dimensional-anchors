# Workflow and Verification Requirements

[Design index](../DESIGN_DECISIONS.md)

## Approval gates

The project targets Minecraft 26.1.2, NeoForge 26.1.2.122, Java 25, and Gradle with ModDevGradle.

1. **Research and planning:** inspect the project and verify exact-version APIs, registry/resource formats, dimensions, recipes, movement/death handling, and persistence. Deliver architecture, data model, resource plan, boundary analysis, multiplayer/failure/performance plan, test strategy, open-question recommendations, and a staged roadmap.
2. **Approved foundation:** implement only after explicit approval, reporting changed files, decisions, and actual validation.
3. **Incremental features:** work in reviewable steps; stop when unresolved behavior affects implementation.
4. **Verification:** run approved focused tests/builds, inspect results, report risks and manual checks.

The original Stage 1 restriction on code changes, builds, resource creation, and Minecraft launches remains. The owner's requests to organize and update design documentation authorize those documentation edits only, not gameplay implementation or execution.

## Future test matrix

These requirements consolidate the original and subsequent plans. They are not claims of implemented or passing tests.

### Identity, keys, and recipes

- Create independent anchors and verify distinct identities and room isolation.
- Reject access before first registration; register once; reject subsequent blank keys even after relocation/restart.
- Permit any valid-key holder without owner/invitation checks; reject blank/unrelated keys and all block-entry routes.
- Verify approved recipe grids, counts, outputs, and Netherite template consumption.
- Upgrade all tiers while preserving registration, identity, region, room contents, block data, and key validity; unregistered anchors remain unregistered.
- Enforce maximum key stack size one and distinct blank/registered sprites, including registered copies.
- Copy in an anvil while preserving the original, consuming one blank and exactly one level only upon collection, without anvil wear.
- Test shift-click, repeated output interaction, and copying from copies without free copies or escalating costs.
- Permit unlimited copies of keys to unavailable rooms without bypassing the unavailable state. Check the unavailable-anchor tooltip if implemented.
- Losing every key denies entry without resetting registration or deleting the room; keyless south-wall exit remains available.

### Travel and lifecycle

- Verify teleport exactly at 60 charge ticks, no teleport/penalty on early release, and 400-tick shared cooldown after success.
- Use the current room's key to exit; use other keys to travel between rooms while retaining one exterior return point.
- Remote access works with the anchor chunk unloaded; neither anchor nor inactive room stays permanently loaded.
- Check north-facing entry and door behind the player for every tier.
- Check normal return position/dimension, personal-respawn then Overworld-spawn fallbacks, blocked destinations, and unavailable dimensions.
- Persist per-player return/session state across disconnect and server restart.
- Extract occupied anchors with qualifying pickaxes; preserve all occupants, non-player entities, and contents.
- Prevent all breaking progress with inadequate tools; permit any player to register, extract, or destroy.
- Preserve room position, contents, identity, and keys when replacing the anchor.
- Verify connected and disconnected occupants remain inside after collection, explosion-drop, or block/item destruction; they can exit without a key.
- Suspend new key entry while the anchor is absent and re-enable it only after recovery/replacement.
- Verify block/item destruction never deletes room blocks, entities, contents, or its reserved region.
- Compare each tier's blast resistance with its approved reference block, using Ancient Debris for Netherite.
- Verify anchor removal does not trigger an anchor-destruction death message, death, respawn, inventory drop, entity cleanup, or region deletion.
- Prevent duplicated/stale keys from bypassing the unavailable state; protect neighboring rooms.

### Geometry, protection, and performance

- Verify all five interior sizes, fixed-floor upward growth, consistent horizontal rounding, and unchanged existing builds near boundaries.
- Move the south-wall door and north-facing entry together; handle blocked landing positions.
- Reserve non-overlapping maximum-size regions and generate only on first access.
- Verify the two-block boundary thickness on all six faces and ensure room
  reservations contain it without overlap.
- Verify player-placed limiter and exit blocks remain breakable in creative.
  Once room generation exists, verify generated boundary blocks reject player
  break attempts in survival and creative without drops; do not infer generated
  status from block identity alone. Continue testing commands, explosions,
  pistons, fluids/fire, and modded bypasses separately.
- Exercise boundary mining/replacement, explosions, piston/fluid/fire mechanics, pearls, other teleports, flight, knockback, commands, and relevant modded behavior.
- Verify exact boundary death message `<player> tried to access places beyond imagination`, normal death consequences, no false wall deaths, and no repeated/recursive deaths.
- Distinguish legitimate exits/internal teleports from violations; test login, respawn, invalid coordinates, and client position manipulation.
- Check dedicated-server multiplayer, fixed tier dimensions, no unbounded world scans/ticking/allocation, and no unnecessary inactive-room chunk loading.

Do not claim compatibility or a passed check without executing and inspecting the relevant evidence. Documentation organization alone requires no game build.

## Deferred-validation workflow, 2026-10-23

The owner initially deferred builds/tests while memory was constrained, then
reauthorized local build and test execution on 2026-10-23 after artifact caching.
Use one Gradle worker and no parallel execution.
[TO_TEST](../TO_TEST.md) is the central tabular backlog:
add every changed behavior, failure path, regression risk, and pending automated
or manual check in the same change. Keep unimplemented prerequisites distinct
from implemented-but-unverified behavior. Record execution evidence only when
the owner requests checks; source inspection and written tests are not passes.
This workflow does not waive unresolved design decisions or entry safety gates.

## Implemented anchor regression tests

The focused headless suite is in
[AnchorRegressionTests](../../src/gameTest/java/com/iloelias/dimensionalanchors/AnchorRegressionTests.java).
It exercises real registered recipes and server placement, rather than substituting
JSON parsing or source-shape checks for gameplay assertions.

Run on CI, or locally only with explicit owner authorization:

```sh
bash ./gradlew --no-daemon runGameTestServer build
```

Use Java 25. The release workflow runs the GameTests before publication; test
classes are excluded from the installable JAR. See [progress](../PROGRESS.md)
for commands actually run and results.

Covered: all four upgrades, valid/invalid ingredients and grids, identity and
registration transfer, deterministic result previews, vanilla/JEI display
metadata, JSON/network codec round trips, five-item survival/creative placement,
failed placement, upgraded extraction, and saved-data round trips.

Client checkpoint (not established by headless tests):

1. Check Copper, Iron, Gold, Diamond, and Netherite recipes in JEI. The last four
   must show the approved upgrade ingredients and one result.
2. Craft five new Copper anchors, place them consecutively in survival, and
   verify that the held count decreases by one without inventory rollback.
3. Collect the placed anchors and compare `dimensionalanchors:anchor_data`:
   each independent anchor must have a distinct `anchor_id`.
4. Upgrade an identified anchor through every tier, place and collect it after
   each upgrade, and verify unchanged UUID/registration and the new tier.
5. Repeat fresh-item placement in creative; the held stack must remain reusable
   and must not receive the placed block's UUID.

Use a disposable test world and fresh stacks. Items already contaminated with
duplicated identity by an older build are not automatically repaired.

### First-key registration checkpoint

The suite also checks registration on all five anchor tiers, both hands, and
survival/creative. It verifies one-time replacement, a second player's rejected
attempt, key JSON/network serialization, invalid/missing data rejection, and
preservation of registration through extraction, upgrade, serialized reload, and
relocation. Deliberate invalid-state tests produce expected warning/error logs.

Pending in-game confirmation:

1. Place a new anchor, then right-click it with a blank key. Check that the item
   becomes a registered key with `dimensionalanchors:registered_key` referencing
   the anchor UUID. No room or teleportation is expected.
2. Try another blank key on that anchor. It must remain unchanged and a rejection
   message must appear.
3. Extract the anchor: it must retain its UUID and now have `registered:1b`.
4. Upgrade and relocate it, then save and reopen the world. A second blank key
   must still be rejected, and the original key reference must remain unchanged.
5. Repeat registration with the off hand and in creative. Only the hand used
   changes; creative registration also replaces its blank key.

### Anvil copying checkpoint

Automated tests use real `AnvilMenu` pickup/shift-click operations to verify the
retained original, single blank consumption, one-level cost, no wear on all
anvil damage stages, unchanged components/UUID/glint, copying from a copy, fixed
cost despite high prior repair cost, no repeated free output, no-XP rejection,
full-inventory rejection, preview cancellation, invalid inputs, creative XP
exemption, and unchanged vanilla renaming outside the copy operation.

Pending in-game confirmation:

1. In survival, put a registered key on the left and a blank on the right.
   Previewing must consume nothing; collecting costs one level and one blank,
   leaving the original in place and producing a copy with identical UUID/glint.
2. Click the output again without adding another blank: no additional copy.
3. Repeat with shift-click, then with no XP and with a full inventory.
   Failed collections must consume neither XP nor blank keys.
4. Remove the anchor and copy its key again. Check that copying changes no room
   state and does not reset registration.
5. Use a copy as the next original and verify the same fixed cost and no anvil
   damage. Repeat after a game restart.

### Allocation and geometry foundation

Four additional GameTests in
[RoomAllocationTests](../../src/gameTest/java/com/iloelias/dimensionalanchors/RoomAllocationTests.java)
cover:

- Exact interior dimensions and fixed floor for every tier in all 4,096 slots;
  nested upgrade bounds, consistent odd/even rounding, standing space, and
  south-wall control coordinates.
- Pairwise non-overlap of all maximum-room reservations (8,386,560 pairs).
- Idempotent reservation, all 4,096 allocations, explicit exhaustion without
  mutation, existing-room lookup at capacity, and the next slot after reload.
- One global allocator across the server's available dimensions.
- JSON codec and actual compressed-NBT disk write/reload through
  `SavedDataStorage`, preserving UUID mappings.
- Invalid UUIDs/slots, duplicated or missing slots, unsupported versions,
  partial decoding, and corrupt files. Existing invalid files must not be
  overwritten or silently replaced by an empty allocator.

Temporary disk-test files are removed after testing. Deliberately malformed
saved-data cases produce expected error logs. These tests do not establish
physical room generation, safe landing after construction, protection, or
travel behavior. There is no new in-game room interaction to test at this stage.

### Registered-key charging prototype

[KeyChargingTests](../../src/gameTest/java/com/iloelias/dimensionalanchors/KeyChargingTests.java)
uses real player item-use ticks in both hands and survival/creative:

- Idle/start state, 60-tick duration, bow-use animation, and automatic completion
  on tick 60 rather than on release.
- Cancellation at ticks 0, 1, 30, and 59, with no completion message or cooldown.
- One completion message per use cycle; no movement, dimension change, or key/XP
  consumption. Completion applies a 400-tick cooldown shared across copies and
  different anchor references, isolated per player. Verify rejection until
  expiry, reuse after expiry, and no cooldown on early release.
- One server-side Nether portal trigger sound event per started charge; no
  extra portal-trigger sound at completion or for invalid keys.
- Explicit unlinked-preview rejection and unchanged blank-key behavior.
- At charge completion, verify positive feedback for a registered, placed
  anchor whose block/tier match; verify unavailable feedback for missing,
  unregistered, unplaced, or stale records. The check must not force-load an
  unloaded anchor chunk. Check the chorus-fruit sound on success and beacon
  deactivation sound on failure.
- Verify repeated registration attempts play the Vault rejection sound while
  successful registration retains the Vault insertion sound.
- This is not proof of travel. Once rooms are implemented, separately verify
  full lifecycle revalidation, room availability, safe destination, and actual
  teleportation at completion.

Manual client checkpoint:

1. Hold a registered key without interacting: it keeps its ordinary item pose.
2. Hold use while aiming into the air: the bow-use pose starts and the Nether
   portal contact sound plays.
3. Continue for three seconds: the completion message appears without releasing.
   A valid placed anchor prepares its room; a missing or stale anchor is
   reported unavailable. Obstructions or damaged generated boundaries report
   preparation failure with server log details. No teleport is expected; a
   20-second vanilla cooldown overlay appears.
4. Release earlier: the key returns to idle with no completion message or penalty.
5. Repeat using the offhand and check first-/third-person presentation.
6. During cooldown, switch to another copy or another registered key: use must
   remain blocked. After 20 seconds, charging becomes available again.

Headless tests verify animation selection, sound dispatch, and the charge-time
anchor availability result; they do not establish rendered animation, audible
client playback, room access, or teleportation.

### Empty shared dimension checkpoint

[PocketDimensionTests](../../src/gameTest/java/com/iloelias/dimensionalanchors/PocketDimensionTests.java)
checks a loaded server dimension, production-resource/test-preset parity,
End-equivalent dimension settings except dragon-fight initialization, unchanged
real End dragon-fight availability, maximum-room vertical fit, global allocation
lookup, the flat generator's empty layers/structure overrides, exclusive custom
biome selection, no carvers/features/precipitation/spawn entries, and three
generated chunks outside the allocation grid containing
only air and no structure starts.

The GameTest server discards standalone dimension entries, so its flat preset
is extended only in the `gameTest` resource source set. Verify that the
production JAR contains the three dimension/biome resources but no test classes
or `data/minecraft/worldgen/world_preset/flat.json`.

Client checkpoint, in a disposable world with commands enabled:

1. Confirm the world loads without registry errors and the dimension is offered
   by `/execute in` completion. Check a fresh world and a reopened existing
   disposable world; headless preset tests do not establish existing-save loading.
2. Switch to spectator **before** entering; this dimension has no floor or exit.
   Record an exterior return position. For example:

   ```text
   /gamemode spectator
   /execute in dimensionalanchors:pocket run tp @s 16 80 16
   ```

3. Check the void terrain, End-like sky/lighting and biome. This is a visual
   check, not evidence of complete room protection or gameplay.
4. Return by operator command to a known safe exterior position, for example
   `/execute in minecraft:overworld run tp @s <x> <y> <z>`, before changing mode.
   Do not test survival entry into the void.
5. Registered-key use must remain the charging/message/cooldown prototype.
   It must not teleport into the empty dimension.

No claim of client rendering, existing-save migration, modded spawning
compatibility, playable rooms, or safe entry is established by these headless
checks.

### Physical generation and generated-boundary checkpoint

[RoomGenerationTests](../../src/gameTest/java/com/iloelias/dimensionalanchors/RoomGenerationTests.java)
adds three headless tests for all five initial room sizes, empty interiors,
all shell coordinates and one control, repeated generation, all tier expansions,
preserved chest contents and player-placed interior blocks, downgrade rejection,
obstruction rejection, pending generation/expansion recovery, damaged completed
boundary rejection, and survival/creative break-event cancellation with client
resynchronization. Loot queries must return no drops. Player-placed utility blocks
in interiors, outside shells, and outside the pocket dimension must not receive
generated protection.

Allocation tests additionally cover version 1 migration to version 2, strict
room-state decoding without partial resets, and actual disk reload of pending
and completed room records and their protection provenance. Charging tests assert
that early release and unavailable anchors do not generate rooms, successful
completion prepares the room, and preparation failure gives explicit feedback,
failure audio, no teleport, and the unchanged prototype cooldown. The deliberate
obstruction case logs an expected room-preparation error.

These generation tests passed in an explicitly authorized local baseline run
(18 total GameTests), and again with the session-data additions (21 total).
CI remains a separate check. Event cancellation and loot queries do not simulate
actual client prediction or prove audible playback.

Manual checkpoint in a disposable world, after CI passes:

1. Place an anchor, register a blank key, and fully charge it in the air. Expect
   room-prepared feedback and the approved success sound, with no teleport.
2. Use spectator mode and an administrator command to inspect the pocket.
   In a fresh world, the first room's interior is centered horizontally at
   X=16/Z=16 and starts at Y=64. For slot `s`, center X is
   `(s % 64) * 32 + 16` and center Z is `floor(s / 64) * 32 + 16`.
   Verify tier size, two layers on every face, and one inner south-wall control.
3. Attempt to break generated inner/outer boundary layers and the control in
   creative and survival. Expect them to remain/resynchronize with no drops.
   Place the same utility blocks in the interior and outside the shell, then
   break them in creative: these player-placed blocks must remain breakable.
4. Build inside the usable interior, including a chest with contents. Recharge
   after cooldown, then save and restart normally; verify the same reservation,
   construction, contents, and generated-boundary protection.
5. Extract, upgrade, replace the anchor, and charge again. Check every upgrade,
   unchanged floor/construction coordinates, removed old walls/control where
   they become interior, and the relocated single south-wall control. Include
   wall-mounted blocks: ordinary neighbor physics still applies when supports
   move, beyond the direct-write/content-preservation assertions.
6. Repeat with two players/independent anchors and confirm rooms remain separate.
   Test explosions and pistons against boundaries. Commands and modded bypasses
   are separate cases, not evidence that ordinary breaking is unprotected.

The control now exits only with an existing recorded session. Command-based
entry does not create one; keep an administrator return path and do not enable
ordinary player entry. Later checkpoints below cover safe landing, sessions,
exit and observed movement. Hard-crash transactional recovery is not established.

### Player room session-data checkpoint

[PlayerRoomTests](../../src/gameTest/java/com/iloelias/dimensionalanchors/PlayerRoomTests.java)
adds three GameTests:

- Per-player entry, refusal to overwrite an existing exterior return,
  room-to-room changes preserving that return, idempotent current-room selection,
  stale-exit rejection, matching exit removal, and new exterior entry after exit.
- Exact coordinates, yaw/pitch, dimension, and current room across codec and
  compressed-NBT disk reload; separate players sharing a room remain independent.
- Global storage across server dimensions; dirty-state updates; malformed UUIDs,
  missing fields, invalid coordinates/rotations, pocket return destinations,
  unsupported versions, corrupt files, and rejection of partial decoding without
  overwriting existing session files.

The session-data GameTests do not teleport players or simulate a network
disconnect/reconnect. Disk reload proves persistence, not login reconciliation.
There is no new manual gameplay path for sessions in this checkpoint; key use
continues to prepare rooms without teleporting. Manual room-generation checks
above remain necessary.

### Safe-destination resolver checkpoint

[SafeTravelTests](../../src/gameTest/java/com/iloelias/dimensionalanchors/SafeTravelTests.java)
adds three GameTests:

- Default north-facing entry for all five room tiers, alternate interior landing
  for obstructed entry, and explicit failure without block removal when every
  interior position is filled.
- Exact saved-position preservation, vanilla slab floor resolution, harmful
  floors/body blocks, liquids, portal rejection, world-border rejection,
  inclusion of four-block horizontal/vertical limits, and rejection beyond them.
- Return priority: saved position before vanilla bed; obstructed/missing bed or
  unavailable saved dimension falls back to Overworld spawn; charged Nether
  respawn anchor is resolved without charge consumption, depleted anchor is
  rejected, and total destination failure preserves the existing session.

Tests call the resolver without teleporting. They do not establish network
transition correctness, live reconnect recovery, all modded hazard behavior, or
the player's eventual failure notification. Temporary world-spawn/border changes
in tests are restored. The game world used is the disposable GameTest world.

### Server-authoritative exit checkpoint

[TravelServiceTests](../../src/gameTest/java/com/iloelias/dimensionalanchors/TravelServiceTests.java)
checks that the generated control exits without a key, restores the exact safe
exterior position and orientation, and removes only the player's matching
session after teleport success. A rejected server teleport must leave the player
inside, retain the session, and show explicit failure feedback.

The hardening additions include four more GameTests:

- `canceled_reentrant_exit`: cancel real NeoForge dimension travel, reject nested
  key/control exits, preserve session and motion on cancellation, then retry
  successfully with zero velocity/fall distance.
- `exceptional_exit_teleports`: controlled player overrides simulate exceptions
  before/after movement, false success, rejection after movement, redirection,
  wrong rotation/dimension, and destination obstruction introduced during travel.
  No normal success or session removal is allowed; the guard must release.
- `replaced_exit_sessions`: preserve another occupant and changed/recreated/
  removed sessions, including a room change away and back to identical values.
  Cleanup conflicts have explicit feedback without overwriting hook changes.
- `player_travel_snapshots`: guard isolation/release and non-persistence;
  captured-session identity, not just equal room/return values, authorizes removal.

The controlled overrides are not proof of every third-party hook or crash
outcome. Partial movement is diagnosed without a second teleport; recovery after
such failures remains part of pending lifecycle reconciliation.

Two additional tests cover `arrival_validation_limits` (exact/inside/outside
position tolerance, total-distance rather than per-axis tolerance, inclusive
rotation endpoints and neighboring floats, wrapped yaw, non-finite coordinates/
rotation, dead/removed arrivals) and `travel_thread_liveness` (off-thread
rejection before session/position changes, dead/removed travel, guard release).
Non-finite rotations are emulated through test getters because vanilla setters
discard them. The initial exact-arrival fixture omitted initialization of the
constructor's yaw; it was corrected before the successful rerun.

Authorized final command on 2026-10-23:
`JAVA_HOME="$HOME/.gradle/jdks/eclipse_adoptium-25-amd64-linux.2" bash ./gradlew --no-daemon --no-parallel --max-workers=1 build runGameTestServer`.
Result: build passed, all 32 GameTests passed (31 mod, one vanilla). Expected
exception/corrupt-store diagnostics are exercised rejection paths, not failing
tests. This does not establish live client synchronization, arbitrary mod hooks,
manual installation or remote CI; see [TO_TEST](../TO_TEST.md).

Key charging still does not enter a room or create a session. Only a key matching
the currently recorded room can request its normal charged exit. Room-to-room
travel remains disabled; the later movement checkpoint does not establish those
transitions or manual gameplay.

### Login/respawn reconciliation checkpoint

[PlayerRoomReconciliationTests](../../src/gameTest/java/com/iloelias/dimensionalanchors/PlayerRoomReconciliationTests.java)
adds four tests:

- `reconcile_room_occupants`: absent-anchor occupants retain the same session,
  position and velocity across repeated login/pocket-respawn events.
- `reconcile_inconsistent_locations`: exterior stale session, absent session,
  missing allocation, wrong room and non-finite position warn without
  teleportation or destructive session changes.
- `reconcile_exterior_respawn`: active travel is not disturbed, inactive players
  retain sessions, live exterior respawn clears only its session, and repeated
  events without a session are no-ops.
- `reconcile_vanilla_respawn`: actual `PlayerList.respawn` creates a new player
  with the same UUID, follows vanilla's resolved exterior position/orientation,
  and clears the old room session through the registered event listener.

Authorized command on 2026-10-23:
`JAVA_HOME="$HOME/.gradle/jdks/eclipse_adoptium-25-amd64-linux.2" bash ./gradlew --no-daemon --no-parallel --max-workers=1 build runGameTestServer`.
Result: `BUILD SUCCESSFUL in 32s`, all 36 tests passed (35 mod and one vanilla).
No live disconnect/reconnect, process restart, manual multiplayer or exhaustive
death/drop/keep-inventory checks are inferred. Exterior stale-login recovery
remains deliberately unresolved; no pocket occupant is automatically expelled.

### Observed-occupant movement checkpoint

[RoomMovementTests](../../src/gameTest/java/com/iloelias/dimensionalanchors/RoomMovementTests.java)
adds eight tests:

- `movement_room_faces`: all five tiers, standing/crouching/swimming boxes at
  interior faces/corners, collision-rounding tolerance, and no placed anchor.
- `movement_departures`: all six full-body faces beyond tolerance, neighboring
  reservation, dimension change and non-finite position after valid occupancy.
  Death retains the session for normal post-placement respawn reconciliation.
- `movement_vanilla_death`: actual outbound death packet contains the exact
  approved text; creative with keepInventory retains items, survival without it
  creates the expected actual item drops without duplication.
- `movement_canceled_death`: a real death hook cancels/restores health and posts
  nested ticks; exactly one attempt occurs, without recursive/repeated death.
- `movement_session_lifecycle`: active travel is exempt; logout forgets the old
  baseline; ambiguous occupancy suspends rather than kills or silently rearms;
  valid room login establishes a new baseline.
- `movement_invalid_exit_attempts`: unrelated keys and invalid controls cannot
  disable enforcement.
- `movement_loading_immunity`: actual cross-dimension teleport remains protected
  during vanilla loading, but acknowledgment permits subsequent enforcement.
- `movement_pocket_respawn`: actual vanilla replacement stays in its forced
  pocket respawn and keeps the session. Obsolete old-player logout/tick events
  cannot erase the replacement's baseline before its unauthorized departure.

Exit/reconciliation regressions also post movement ticks before/after legitimate
control/key exits, failed/partial/throwing teleports and inconsistent login.
The first 42-test run compiled but five death cases failed: fixtures were still
under vanilla client/dimension loading immunity. Acknowledgment was added to
fixtures and production enforcement now defers damage without discarding the
baseline. The next 43-test run exposed a fixture querying an already-reset
combat tracker after death; assertions now inspect the emitted death packet.
All 43 then passed. Identity-scoped old-player cleanup and the actual pocket
respawn regression subsequently passed in the final 44-test run.

Authorized command on 2026-10-23:
`JAVA_HOME="$HOME/.gradle/jdks/eclipse_adoptium-25-amd64-linux.2" bash ./gradlew --no-daemon --no-parallel --max-workers=1 build runGameTestServer`.
Result: `BUILD SUCCESSFUL in 30s`, all 44 required tests passed (43 mod and one
vanilla). Production JAR contains the movement implementation and excludes
GameTest classes; whitespace check passed apart from the pre-existing
`.gitattributes` warning. No client launch, CurseForge replacement, network
reconnect/restart, manual multiplayer or remote CI was performed.

This is post-player-tick enforcement, not proof of packet-level manipulation
handling, every movement mechanic, unloaded-chunk behavior or arbitrary mod
compatibility. Failed-travel recovery remains unresolved; entry/room-to-room
travel stay disabled. Track the remaining checks in [TO_TEST](../TO_TEST.md).

### Administrative anchor recovery checkpoint

[AnchorRecoveryTests](../../src/gameTest/java/com/iloelias/dimensionalanchors/AnchorRecoveryTests.java)
adds seven tests through the actual registered command dispatcher:

- `recover_anchor_tiers`: five tiers and both registration states; item count
  and component identity; preserved occupant session, room reservation and
  sentinel room contents. Recovery itself does not make a room available.
- `recover_command_failures`: normal-player permission denial, malformed and
  unknown UUIDs, console rejection and full-inventory refusal without clearing
  a stale placed record or dropping an item.
- `recover_placed_conflicts`: present block refusal, multiple placed records
  refusal and successful recovery of an actually missing block's stale record.
- `recover_unloaded_record`: initially absent chunk is loaded before a missing
  block is declared recoverable. This does not simulate saving an existing
  block and unloading/restarting; that remains a separate manual check.
- `recover_relocated_duplicates`: repeated recovery can leave spare items but
  cannot place another copy, including across dimensions. Extraction/relocation
  consolidates identity metadata; old-dimension recovery cannot bypass it.
- `recover_unplaced_upgrades`: actual crafting and smithing output pickup
  updates the persistent tier before placement, while preview does not; reject
  an obsolete lower-tier item and recover the upgraded tier.
- `recover_anchor_storage`: recorded tier survives disk reload; strict loading
  refuses partial/corrupt authoritative data without changing the file.

The first two 50-test runs compiled and passed the recovery tests but failed the
existing `movement_vanilla_death` actual-drop query. Unique item markers alone
did not fix its zero count. The fixture now performs an unauthorized departure
into the already entity-visible GameTest chunk before death and queries that
chunk, rather than assuming newly generated room chunks expose entities
synchronously. The next 50-test run passed, followed by the final 51-test run
with explicit unloaded-record and room-content coverage.

Exact authorized command on 2026-10-23:
`JAVA_HOME="$HOME/.gradle/jdks/eclipse_adoptium-25-amd64-linux.2" bash ./gradlew --no-daemon --no-parallel --max-workers=1 build runGameTestServer`.
Result: `BUILD SUCCESSFUL in 43s`, all 51 required tests passed (50 mod and one
vanilla). Production packaging/whitespace inspected separately. No installed
client, CurseForge replacement, live multiplayer/restart or remote CI.

The command restores recorded identity/tier/registration, not unknown historical
item upgrades or custom names. Existing stores remain per dimension; resolution
checks loaded server dimensions and refuses conflicting placed records. No
player-session recovery, transition journal, historical-spawn tracking or entry
route is implemented by this checkpoint. See [approved recovery policy](TRAVEL.md#approved-interrupted-transition-policy-2026-10-23)
and [TO_TEST](../TO_TEST.md).

### Item tooltip checkpoint

`item_descriptions` invokes real `ItemStack.getTooltipLines` and the registered
NeoForge tooltip event with a null player, all five anchor tiers, blank/linked/
unlinked keys, normal/advanced flags and an explicit Shift flag. It checks
description order, exact dimensions, gray styling, two separate key instructions,
the truthful entry-disabled notice, conditional UUID visibility, no generated
UUID for pristine anchors, and unchanged vanilla stone tooltip.

Command:
`JAVA_HOME="$HOME/.gradle/jdks/eclipse_adoptium-25-amd64-linux.2" bash ./gradlew --no-daemon --no-parallel --max-workers=1 build runGameTestServer`.
Result on 2026-10-23: `BUILD SUCCESSFUL in 33s`, all 52 required tests passed
(51 mod and one vanilla). Actual keyboard modifier input, rendered layout,
creative search and recipe-viewer presentation remain manual checks. No
installation, client launch or remote CI was performed.
