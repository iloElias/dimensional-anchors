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
  extra sound at completion or for invalid keys.
- Explicit unlinked-preview rejection and unchanged blank-key behavior.

Manual client checkpoint:

1. Hold a registered key without interacting: it keeps its ordinary item pose.
2. Hold use while aiming into the air: the bow-use pose starts and the Nether
   portal contact sound plays.
3. Continue for three seconds: the completion message appears without releasing.
   No teleport is expected; a 20-second vanilla cooldown overlay appears.
4. Release earlier: the key returns to idle with no completion message or penalty.
5. Repeat using the offhand and check first-/third-person presentation.
6. During cooldown, switch to another copy or another registered key: use must
   remain blocked. After 20 seconds, charging becomes available again.

Headless tests verify animation selection and sound dispatch, not rendered
animation or audible client playback.

### Empty shared dimension checkpoint

[PocketDimensionTests](../../src/gameTest/java/com/iloelias/dimensionalanchors/PocketDimensionTests.java)
checks a loaded server dimension, production-resource/test-preset parity,
End-equivalent dimension settings except dragon-fight initialization, unchanged
real End dragon-fight availability, maximum-room vertical fit, global allocation
lookup, the flat generator's empty layers/structure overrides, exclusive custom
biome selection, no carvers/features/precipitation/spawn entries, and three
generated chunks (including the far edge of the allocation grid) containing
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
