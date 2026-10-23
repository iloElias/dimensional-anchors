# Progress Report

**Date:** 2026-10-22  
**Last updated:** 2026-10-23 (UTC-03:00)\
**Configured version:** `0.1.0-alpha.1` (release tag: `v0.1.0-alpha.1`)  
**Stage:** Administrative anchor recovery added; authorized build and all 51 GameTests passed. Entry remains disabled; player-transition recovery and both approved entry routes are the next implementation stages.

This report distinguishes changes present in the working tree from verified gameplay.
It does not replace the [approved design](DESIGN_DECISIONS.md) or imply that the
planned room mechanics are implemented.

## Continuation handoff, 2026-10-23

- Latest checkpoint: `/dimensionalanchors recover <uuid>` is registered with
  administrator permission and a player-only inventory recipient. It gives one
  item with recorded UUID/tier/registration, never a key or player rescue.
  Unknown/conflicting/unreadable records and present anchor blocks are refused.
  A stale placed location is checked after loading the exact recorded chunk;
  full inventory fails without dropping an item or clearing its placed record.
- Recovery does not reopen entry by itself or touch occupants/room contents.
  Duplicate UUID placement is now rejected across dimensions; relocation
  consolidates records without losing registration, and obsolete lower-tier
  items cannot downgrade an anchor. Completed crafting/smithing output pickup
  records the tier even before placement; preview does not mutate it.
  Strict anchor-store loading prevents partial/corrupt recovery resets.
- Seven new recovery GameTests passed: tiers/registration/room contents and
  session preservation, real command permission/error paths, present/conflicting/
  stale records, unloaded-record inspection, repeat recovery/cross-dimension
  relocation, actual crafting/smithing output pickup, and corrupt/partial disk
  stores. A prior movement-drop fixture queried an entity-invisible newly loaded
  room chunk; it now checks uniquely named actual drops after an unauthorized
  departure into the already visible GameTest chunk.
- Final recovery validation command:
  `JAVA_HOME="$HOME/.gradle/jdks/eclipse_adoptium-25-amd64-linux.2" bash ./gradlew --no-daemon --no-parallel --max-workers=1 build runGameTestServer`.
  Result: `BUILD SUCCESSFUL in 43s`, all 51 required tests passed (50 mod and one
  vanilla). A preceding 50-test run passed after the drop-fixture correction.
  No CurseForge installation, client launch, live network/restart check or CI.
  Evidence and limitations: [TO_TEST](TO_TEST.md), TT-061 through TT-072.
- Definitions are now approved in [travel](design/TRAVEL.md): remote entry
  captures the exterior use point; matching-anchor interaction while holding a
  registered key captures a safe block-front return, fixed for that session.
  Room changes preserve the first exterior return. Interrupted travel uses
  persistent intent and verified arrival, or bounded safe-source rollback;
  ambiguous exterior state is preserved, not punished or silently cleared.
  Historical safe exterior respawn is added to the planned fallback chain.
  These player-transition/entry additions are NOT implemented by this checkpoint.
  The recovery command concerns the anchor item only; existing occupants stay
  and may exit while the block is absent, but new entry requires it placed.
- Owner reauthorized local implementation/build/test execution on 2026-10-23
  after artifact generation was cached. Use one worker and no parallel Gradle
  execution. Keep every new/changed behavior, failure path, regression and
  actual result in [TO_TEST](TO_TEST.md); preserve the entry safety gate.
- Latest checkpoint: `RoomMovementEnforcement` observes recorded occupants on
  server post-player ticks. Once a valid full-body interior baseline exists,
  unauthorized departures request vanilla generic-kill damage with the exact
  approved death message. All five tiers, wall/corner contact, three poses,
  six departure faces, dimension changes and invalid coordinates have headless
  coverage. Anchor placement is never a punishment criterion.
- Monitoring is transient and per player/session object, held by server-global
  session data. Active travel is exempt; confirmed exit clears the observation.
  Partial/throwing/redirected exits suspend punitive checks without guessing
  recovery. Ordinary invalid-key/control rejections do not disarm it.
  Ambiguous initial locations or missing/incomplete room data warn and suspend,
  rather than retroactively punish a login. Valid room reconciliation restores
  observation; logout and session cleanup remove it.
- Vanilla dimension/client-loading immunity defers damage without dropping the
  baseline. Once eligible and still outside, one damage attempt is made. A
  canceling/resurrecting hook receives diagnostics, not repeated damage loops.
  Actual pocket respawn preserves the session and arms the replacement player;
  obsolete logout/tick events cannot clear that new object's observation.
- Final movement command:
  `JAVA_HOME="$HOME/.gradle/jdks/eclipse_adoptium-25-amd64-linux.2" bash ./gradlew --no-daemon --no-parallel --max-workers=1 build runGameTestServer`.
  Result: `BUILD SUCCESSFUL in 30s`, all 44 required tests passed (43 mod and one
  vanilla). Eight new tests cover movement/death/lifecycle; exit/reconciliation
  regressions now include follow-up movement ticks. Production packaging and
  whitespace checked. No CurseForge installation, client launch or remote CI.
  Evidence and remaining manual checks: [TO_TEST](TO_TEST.md), TT-051 through TT-060.
- Current checkpoint: player-session persistence and safe-destination resolution
  are implemented; the generated exit control and server-authoritative exit
  service are now wired. The authorized exit baseline passed 30 GameTests;
  arrival-edge additions then passed all 32 (31 mod tests and one vanilla)
  together with the production build.
- Login/respawn reconciliation now preserves existing pocket occupants even
  without an anchor; neither hook expels, teleports, kills or assigns a room.
  Exterior login with an unresolved session warns and preserves it. Live vanilla
  respawn outside the pocket clears only the matching captured session, after
  vanilla placement; pocket respawn keeps it. Invalid/missing session/room/
  position states warn without destructive repair. Active travel is not disturbed.
- Four reconciliation tests passed, including actual `PlayerList.respawn`
  replacement/placement and event-bus integration. Final authorized command is
  the same one-worker build/GameTest command below; result for this checkpoint:
  `BUILD SUCCESSFUL in 32s`, all 36 required tests passed (35 mod and one vanilla).
  This does not simulate network disconnection/reconnection, process restart,
  all death/drop rules or manual client rendering.
- Owner's first exit-checkpoint build failed at `compileJava`: the new service
  used the obsolete `ServerPlayer.serverLevel()` accessor. Replaced it with the
  exact-version `level()` accessor in the service and exit tests. The new
  authorized build/GameTest runs validate that correction.
- External key charging still prepares/expands a room without entry teleport.
  No gameplay path creates a session yet; existing-session exit now invokes the
  safe-destination resolver.
- Exit now validates the current room and safe destination, reports failures,
  and retains the session when the server rejects teleportation. Successful
  exit clears only that player's matching session. The matching room key exits
  after charging; the generated control exits without a key.
- Exit hardening adds a server-thread-only, per-player transient travel guard,
  released in `finally`; post-teleport dimension/position/orientation/liveness
  and standing-safety checks; and captured-session identity checks before
  removal. Hooks cannot make an older exit delete a replacement session, even
  if its room/return values match again. No schema change is needed.
- Teleport exceptions, rejected/redirected travel, and session-cleanup conflicts
  now have explicit diagnostics without normal success or automatic retry/
  rollback. Confirmed arrivals clear velocity and fall distance. Partial
  movement can leave a retained session outside; automated reconciliation is
  still pending, not claimed as solved.
- Four additional GameTests cover cancellation/reentrancy/retry,
  exceptional or unconfirmed arrival, replacement sessions, and transient
  guards/captured snapshots. All passed in the new authorized runs. Two further
  tests cover arrival tolerances, wrapped/non-finite rotation and coordinates,
  dead/removed players and off-thread rejection. Their initial run found a test
  fixture with uninitialized yaw; explicit destination rotation fixed the
  fixture and the rerun passed all 32 tests.
- Exact final command:
  `JAVA_HOME="$HOME/.gradle/jdks/eclipse_adoptium-25-amd64-linux.2" bash ./gradlew --no-daemon --no-parallel --max-workers=1 build runGameTestServer`.
  Result: `BUILD SUCCESSFUL in 28s`, all 32 required tests passed. Production JAR
  contains travel/session classes and excludes the inspected GameTest classes.
  No installation, client launch, manual multiplayer check, or remote CI run
  was performed.
- Next: implement the approved persistent player-transition journal, conservative
  reconciliation/rollback, historical safe-spawn tracking and explicit retry
  before legitimate entry/room-change
  transactions, keeping all outstanding checks in [TO_TEST](TO_TEST.md).
  Manual/integration checks remain pending; do not expose entry before the safety gates.
- Before exposing entry, handle legitimate transitions and unresolved lifecycle
  states, and validate movement/keyless exit through the full entry pipeline.
  Do not enable a standalone entry teleport without a reliable exit path.
- Travel-service and exit-control changes remain uncommitted working-tree changes.
  Transfer those files as well as this report when moving machines; cloning only
  the last committed revision is insufficient.
- Local build permission was checkpoint-specific. Follow contribution rules
  and request authorization again for a new implementation stage.
- Start a continuation by reading this report,
  [travel](design/TRAVEL.md), and [verification](design/VERIFICATION.md), then
  inspect the actual Git status and source before editing.

## Completed in source

### Safe entry and return destination resolution

- Owner approved the bounded, non-destructive search and explicitly authorized
  local build/GameTests for this checkpoint.
- Added `SafeTravelDestination`: prefer the normal north-facing room entry,
  then search only that room's interior. For return, prefer exact saved
  coordinates/orientation, then search four blocks horizontally per axis and
  vertically, then vanilla personal respawn and bounded Overworld spawn.
- Standing collision bounds, supporting floor, vanilla dismount floor geometry
  (including slabs), fluids, dangerous/invalid-spawn blocks, portals, world
  border, build height, and room bounds are checked without changing blocks.
- Reused vanilla's personal-respawn-only helper through a focused access
  transformer, passing `consumeSpawnBlock=false`. No respawn-anchor charges are
  consumed and no automatic wider/randomized world-spawn search is invoked.
- Missing dimensions, invalid/obstructed/depleted personal respawns, and blocked
  destinations follow the fallback chain. Exhaustion throws an explicit error
  without teleporting or removing the player's session.
- Added three GameTests covering every room tier, blocked/full interiors, exact
  coordinates, slabs/hazards/portals, radius endpoints, world borders, vanilla
  bed and Nether respawn-anchor behavior, fallback priority, and unchanged
  sessions after failure.
- An initial test incorrectly expected a solid obstruction to make all nearby
  positions unsafe; standing safely above it is allowed. Corrected that
  assertion and verified actual partial-height support using the vanilla helper.
- Final authorized command
  `JAVA_HOME=/home/murilo/.gradle/jdks/eclipse_adoptium-25-amd64-linux.2 bash ./gradlew --no-daemon build runGameTestServer`
  passed all 24 GameTests (23 mod tests plus one vanilla) and production build.
- Resolver integration, player-visible failure messages, functional exit control,
  teleport/session ordering, reconciliation, and movement enforcement remain
  pending. The key still only prepares a room; no new player travel is enabled.

### Player room session-data foundation

- Owner approved continuing with session persistence while keeping teleportation
  disabled, and explicitly authorized local build/GameTests for the current
  generation baseline and this checkpoint.
- Added `ExteriorReturn`, `PlayerRoomSession`, and server-global
  `PlayerRoomSavedData`, schema version 1. Store the current room's anchor UUID
  and the exact exterior dimension, coordinates, yaw, and pitch per player.
- Entry refuses to replace an existing session. Room-to-room changes preserve
  the first exterior return. Exit completion checks the expected current room
  before removing only that player's session; new entry can then record a new
  exterior return.
- Return records reject pocket-dimension destinations, non-finite/out-of-bounds
  coordinates, and invalid rotations. These checks do not establish landing
  safety or dimension availability.
- Shared the existing strict-decoding and fail-closed saved-data loading
  safeguards with room allocation via `SavedDataSupport`. Malformed, future,
  partial, or unreadable files cannot silently reset either authoritative store.
- Added three GameTests for session lifecycle, player isolation, exact codec/disk
  round trips, cross-dimension storage, stale operations, and invalid-file
  preservation. Temporary disk-test files are removed after each test.
- No gameplay hook creates sessions yet. Key charging still prepares rooms
  without moving the player. Session/teleport ordering, safe destinations,
  working exits, login/respawn reconciliation, and movement enforcement are
  future integration tasks, not implemented behavior.

### Physical room generation and generated-boundary protection

- Owner confirmed the previous CI passed after the `Player.gameMode()` fix and approved this implementation on 2026-10-23. Separately approved the temporary trigger: a full charge with an available anchor prepares its room without teleporting.
- Added physical rooms for all five tiers within permanent reservations, with two-block-thick boundaries on all six faces and one south-wall exit block. The exit block is not functional yet.
- Repeated charges verify an existing completed boundary without recreating the room. Upgraded anchors expand on the next successful charge, keeping the floor, existing interior blocks, block entities, and contents fixed. Only old generated boundary positions are cleared; unexpected obstructions reject the operation before writes.
- Allocation schema 2 stores target tier, pending/completed generation, and previous tier during expansion. Schema 1 reservations migrate without changing slots. Invalid or partial room data is rejected, not reset.
- Generation intent and boundary provenance are saved before chunk changes. An interrupted generation can resume only at its recorded target tier. A damaged completed boundary fails explicitly rather than silently rebuilding it. Saved data and chunk writes are not an atomic transaction; hard-crash durability is not claimed.
- Server-side break cancellation applies only to persisted generated boundary positions in the pocket dimension, including both creative and survival; the client receives a corrective block update. Player-placed limiter/exit blocks in interiors, unused reservations, or other dimensions are not protected by this handler.
- Utility blocks have no loot table and block piston movement, retaining bedrock hardness/blast resistance. Commands and other mods can still bypass ordinary block-breaking protection. Movement enforcement is deferred until legitimate travel/session transitions exist.
- Preparation failures log anchor/error details, display translated failure feedback, and play the beacon failure sound. Successful preparation retains the approved chorus-fruit sound. The prototype cooldown remains unchanged, even on failure.
- Added three GameTests and extended charging, allocation migration/invalid-data, and disk tests. The void-dimension test now samples empty chunks outside the room allocation grid to remain independent of generation tests.

### Empty shared dimension and biome

- Owner confirmed the key animation matches the requested behavior, then approved this checkpoint, its initial End-based environment (including music, exploding beds, and disabled respawn anchors), and local build/GameTests. This report does not infer confirmation of the key sound or cooldown.
- Added data-driven `dimensionalanchors:pocket`, its custom dimension type, and `dimensionalanchors:pocket_void`. The actual 26.1.2 End settings are retained except dragon-fight initialization, which is disabled.
- Flat generation uses zero terrain layers, no structures/features/lakes. The custom biome has no carvers, placed features, precipitation, spawn costs, or natural spawn entries. There are no islands, End platform, portals, or dragon.
- No gameplay entry, room blocks, forced chunk loading, global entity handler, or change to the key prototype is added.
- Added a real-dimension GameTest checking environment parity, preserved vanilla End behavior, shared allocation, room-height fit, empty biome/generator settings, and three fully generated air-only chunks.
- Validation initially caught an iterable-vs-list test API mismatch, then the fact that vanilla GameTestServer discards standalone datapack dimensions. Corrected the assertion and added a test-only flat preset that preserves the three vanilla dimension definitions and includes the pocket dimension. The test compares it with the decoded production dimension resource.
- Final headless run passed all 15 tests and the production build. The test-only preset is not part of the installable JAR.
- Owner reported that the dimension checkpoint worked correctly in-game on 2026-10-23 after receiving spectator-entry instructions. This confirms the reported client checkpoint, not every environmental rule, existing-save scenario, or multiplayer/modded compatibility case.

### Registered-key charging prototype

- Owner requested a presentation/timing checkpoint before dimension work and explicitly authorized local build/GameTests for it.
- Linked keys start vanilla bow-use animation and broadcast the Nether portal contact sound (`minecraft:block.portal.trigger`) once when use begins.
- After 60 uninterrupted use ticks, check whether the linked anchor is registered and placed; if its chunk is loaded, verify the physical block and tier. Prepare or expand its room and show translated feedback, then end the use cycle. No exterior anchor chunk is forced to load and no teleportation is attempted. Generation temporarily loads the destination chunks, without persistent chunk tickets.
- Releasing early returns to idle without a completion action. No key/XP consumption is applied. Early cancellation has no cooldown; the owner then requested a 400-tick vanilla cooldown after placeholder completion, shared across all registered keys/copies per player.
- Unlinked creative-preview keys reject use with feedback; blanks are unchanged.
- Added a GameTest for both hands and survival/creative, cancellation at ticks 0/1/30/59, exact tick-60 completion, sound dispatch, resource/position preservation, and invalid-key rejection.
- Extended charging GameTests to pass only when a registered/placed record and matching loaded anchor block/tier are found, and to reject missing records, unregistered/unplaced records, and stale placement records. A stale linked key receives unavailable feedback at tick 60 without teleporting.
- Added a chorus-fruit teleport sound when the entry check passes and a beacon deactivation sound when it fails. Repeated registration attempts now play the Vault rejection sound; successful registration retains the Vault insertion sound.
- This remains a preparation checkpoint, not travel authorization. Real travel must still revalidate lifecycle state, destination safety, return sessions, and teleportation. Owner confirmed the animation matches the intended behavior; sound/cooldown client checks were not separately reported.
- Cooldown tests cover exact expiry (accounting for vanilla's cooldown tick at the end of the completing player tick), copy/other-reference bypass attempts, per-player isolation, and reuse after expiry. An initial timing assertion counted that completion tick twice; corrected the test after inspecting the vanilla tick order. The final full run passed.

### Room allocation and geometry foundation

- Owner approved 4,096 permanent reservations per world, explicit rejection of new allocations at capacity, no region reuse, and local build/GameTests for this stage.
- Added `RoomGeometry` with all five approved sizes, fixed floor, upward growth, consistent odd/even rounding, a two-block boundary on all six faces, and south-wall control and default entry positions. Maximum room shells occupy 20-by-20 footprints on a 32-block-spaced grid.
- Generated-block protection now uses persisted room geometry, not block identity alone. Player-placed limiter and exit blocks remain breakable in creative outside generated boundaries.
- Added server-global `RoomAllocationSavedData` in Overworld storage: UUID-to-slot mappings, idempotent reservations, a bounded append-only allocator, schema version 2 with version 1 migration, and no removal/reuse API.
- Strict decoding rejects malformed or partial mappings and unsupported versions. Existing unreadable data blocks allocation instead of being reset, preventing accidental reuse of preserved regions.
- Added four GameTests covering every slot/tier, all 8,386,560 reservation pairs, capacity exhaustion, cross-dimension lookup, serialization, actual disk persistence, and invalid-file preservation.
- Completing a linked-key charge with an available anchor now calls the allocator before physical generation. Registration and copying still do not allocate rooms. Safe landing and teleportation remain unimplemented.
- Existing dimension-local anchor persistence is unchanged. Global remote-anchor lookup/migration remains a prerequisite for future key travel.

### Design documentation

- Organized approved decisions into [topic documents](DESIGN_DECISIONS.md).
- Documented recipes, key usage, room sizes, shared dimension, expansion, and assets.
- Simplified the anchor lifecycle: collecting or destroying an anchor preserves its room and occupants, but suspends new entries while the anchor is absent.
- Prepared the [implementation guide](IMPLEMENTATION.md) and [verification matrix](design/VERIFICATION.md).

### Visual prototype

- Added registration code for five anchor blocks: Copper, Iron, Gold, Diamond, and Netherite.
- Added registration code for boundary and exit blocks, plus item forms for all seven blocks.
- Added catalyst, upgrade template, blank key, and registered-key preview items.
- Defined a **Dimensional Anchors** creative tab with all eleven preview entries, using the Copper anchor as its icon.
- Added English display names.
- Configured anchor sounds using the corresponding vanilla material block's sound type: Copper, Iron, Gold, Diamond, and Netherite block sounds. No custom audio files or sound events are introduced.
- Configured anchors with material block hardness and approved ore-equivalent blast resistance, requiring an iron-tier or better pickaxe to mine.
- Added pickaxe mining tags and block loot tables so the correct tool is recognized and anchors drop their corresponding item when harvested.
- Configured boundary and exit blocks with bedrock hardness and blast resistance so they cannot be broken in survival.
- Limited dimensional key stacks to one item.
- Registered the approved catalyst, anchor progression, template, Netherite smithing, and blank-key recipes.
- Internal preview blocks currently use Stone sounds.

The two key items and creative access to internal blocks are preview conveniences,
not a final implementation of key states or survival availability.

### Persistence foundation

- Added validated anchor tiers and persistent item-data records for anchor identities and registered-key references.
- Registered synchronized data components for anchor item data and key data.
- Added server-side `SavedData` records keyed by anchor UUID, including tier, registration, placement state, exterior position, and data version.
- Wired anchor placement to create a UUID when needed and persist its position.
- Wired player extraction to return the same UUID and preserve registration data.
- Upgrade recipes now preserve identity, registration, and other item components.
- First-key registration and anvil key copying are wired; teleportation is not implemented.

### Anvil key copying

- Restored the vanilla anvil-use sound on completed copies. Cancelling vanilla completion to retain the original also suppressed its sound; the custom path now emits `SOUND_ANVIL_USED` server-side at the anvil, without applying wear.
- A focused access transformer exposes the menu's `ContainerLevelAccess` so the sound uses the actual anvil location rather than the player's position.
- Owner confirmed the restored anvil sound works in-game after the fix.
- Added copying from one linked registered key and one blank key through NeoForge's anvil update and pre-completion events.
- A collected copy retains the original's UUID, name, components, and glint. The original remains in the left slot, the blank is consumed, and survival costs exactly one level without applying repair-cost escalation or anvil wear.
- Creative retains vanilla's XP exemption, but consumes the blank and preserves the original. Copying does not combine with renaming.
- Previewing/cancelling costs nothing. Shift-click with a full inventory and pickup without enough XP leave inputs unchanged. Another blank is required for every further copy, including copies made from copies.
- Copying does not access or modify saved anchor records, so an absent anchor does not prevent copying and no room/chunk is loaded or generated.
- Added two GameTests using real anvil menu clicks, including normal pickup, shift-click, repeated collection, all three anvil damage stages, preserved components/high prior repair cost, invalid pairs, no-XP/full-inventory cases, creative, and an ordinary vanilla rename regression.

### First-key registration

- Use a blank key on an unregistered placed anchor to replace it with a registered key referencing the same UUID and persist `registered=true` on the server.
- Registration works in either hand and in survival/creative. It replaces the held blank even in creative, rather than leaving an extra blank or issuing repeated registered keys.
- Further blank-key registration is rejected without consuming the item, including after extraction, upgrade, serialized reload, and relocation.
- Missing/inconsistent anchor records and malformed blanks are rejected with translated feedback and server logging; no new identity is created on failure.
- Success feedback explicitly notes that teleportation is not implemented.
- Added two registration GameTests covering all tiers, both hands/game modes, a second player's attempt, invalid inputs, key persistence/network codecs, and the registered extraction/upgrade/relocation lifecycle.
- DA-001 remains deferred. Room generation, travel, key copying, and unavailable status tooltips are not part of this change.
- Added the vanilla Vault insert-item sound on successful registration, broadcast from the server to nearby players, including the registering player. Rejected attempts do not trigger the sound.
- Registered-key items now request glint only when their registered-key component is present. Blank and unlinked creative-preview keys do not request glint.
- Owner confirmed matching key/anchor UUIDs and repeated-registration rejection after upgrades and restarting the game. A freshly extracted Gold anchor was nevertheless reported with `registered:0b`; tracked as [DA-002](KNOWN_ISSUES.md#da-002-extracted-registered-anchor-reported-with-an-unset-registration-flag), not claimed resolved by a code fix. Follow-up Copper testing confirmed `registered:0b` without a key and `registered:1b` after key registration; the original Gold report remains unexplained and was not reproduced.
- Owner confirmed key glint and silence on rejected registration. The initial Respawn Anchor sound worked but was rejected aesthetically; replaced with the Enchantment Table sound, also rejected aesthetically. The owner then auditioned and approved `minecraft:block.vault.insert_item`; registration now uses `SoundEvents.VAULT_INSERT_ITEM`.

### Netherite recipe synchronization fix

- The client log reported a `neoforge:recipe_content` encoding failure on world entry: the Netherite recipe's JSON and network unit codecs used different instances, which failed the network codec's equality check.
- Initially fixed by sharing a single recipe instance between the unit codecs.
- The upgrade regression fix below supersedes that implementation with vanilla smithing codecs; JSON/network round trips are now covered by a running test.

### Anchor upgrade and stack regressions

- Replaced the crafting matcher that rejected its own center slot with vanilla shaped recipe matching. All four upgrades retain vanilla display and placement metadata, and use vanilla recipe subclasses recognized by JEI.
- Result previews preserve existing UUIDs, registration, and other components. Pristine anchors remain unidentified until placement; previews allocate no UUID.
- Placement no longer writes identity onto the held stack. The server assigns each newly placed block its own UUID, leaving remaining survival/creative items unchanged. Failed placement consumes nothing and creates no item data.
- Replacing an upgraded anchor updates the saved tier, preventing extraction from reverting the anchor to its previous item tier.
- Added four headless regression GameTests in the separate `gameTest` source set, excluded from the production JAR, and a release-workflow gate to run them.
- Tests cover all upgrade tiers, invalid grids/materials, deterministic previews, registered/unregistered identity transfer, JSON/network codecs, display metadata, five consecutive placements in survival and creative, blocked placement, upgraded drops, and saved-data serialization.
- Existing stacks contaminated by the earlier shared-UUID bug are not automatically reassigned identities. Use freshly crafted stacks for retesting; do not duplicate an identified anchor to create independent anchors.

### Visual resources

- Added seven full-cube block models and their blockstates.
- Added client item definitions for the five anchors, the boundary and exit blocks, and four standalone items.
- Added four flat item models using the owner-supplied textures.
- Preserved the supplied PNG artwork without editing it.

### Release automation

- Changed the [workflow](../.github/workflows/build.yml) to run only on pushed tags matching `v*`, following the reference modpack's release pattern.
- Configured Java 25 builds on GitHub Actions.
- Configured release publication with installable JAR attachments, compatibility information, installation instructions, build provenance, and generated notes.
- Set the initial prototype version to `0.1.0-alpha.1`. Tags containing a hyphen are published as GitHub pre-releases; titles and descriptions are automatic.
- The expected JAR name is `dimensionalanchors-0.1.0-alpha.1.jar`, enforced by Gradle and checked by the release workflow from the mod ID and configured version.
- The release title uses the tag name. Its description combines generated compatibility/installation information with GitHub's automatic release notes; manual title or description entry is not required.
- Configured the release workflow to derive the Gradle and release-note version from the pushed tag, removing dependence on the static `mod_version` value in `gradle.properties`.
- Documented the rule against local compilation in [CONTRIBUTING](../CONTRIBUTING.md) and the [README](../README.md).

## Validation evidence

| Check                                                                                                                                                                                       | Result                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Parse asset JSON and check local model/texture references                                                                                                                                   | Passed for 28 asset JSON files at the time of the check; this does not establish registry coverage or successful game loading                                                                                                                                                                                                                                                                                                                                     |
| Parse workflow YAML, check tag-only trigger, and check shell syntax                                                                                                                         | Passed                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| Execute release-information generation with simulated GitHub environment variables                                                                                                          | Passed                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| Parse updated workflow YAML and inspect pre-release classification and configured version                                                                                                   | Passed; alpha, beta, and release-candidate tag examples contain a hyphen and are classified as pre-releases; a stable tag example is not                                                                                                                                                                                                                                                                                                                          |
| `git diff --check` on modified workflow/documentation/assets                                                                                                                                | No whitespace errors reported; Git emitted an existing invalid-attribute warning from `.gitattributes`                                                                                                                                                                                                                                                                                                                                                            |
| Initial local compilation attempt                                                                                                                                                           | Not completed: direct wrapper execution was denied; retry via Bash ended with exit code 143 during Minecraft decompilation                                                                                                                                                                                                                                                                                                                                        |
| Authorized local regression run: `JAVA_HOME=/home/murilo/.gradle/jdks/eclipse_adoptium-25-amd64-linux.2 bash ./gradlew --no-daemon runGameTestServer build`                                 | Passed; four mod regression tests and one vanilla test passed; production JAR built successfully                                                                                                                                                                                                                                                                                                                                                                  |
| Authorized local first-key registration run: `JAVA_HOME=/home/murilo/.gradle/jdks/eclipse_adoptium-25-amd64-linux.2 bash ./gradlew --no-daemon runGameTestServer build`                     | Passed; six mod GameTests and one vanilla test passed; production JAR built successfully. Error/warning logs for deliberately missing anchor data and malformed blank keys are expected rejection tests                                                                                                                                                                                                                                                           |
| Authorized sound/glint run: `JAVA_HOME=/home/murilo/.gradle/jdks/eclipse_adoptium-25-amd64-linux.2 bash ./gradlew --no-daemon runGameTestServer build`                                      | Passed; all seven GameTests and build passed, including linked/unlinked/blank/restored key foil checks. Audible playback and client rendering remain manual checks                                                                                                                                                                                                                                                                                                |
| Authorized anvil-copying run: `JAVA_HOME=/home/murilo/.gradle/jdks/eclipse_adoptium-25-amd64-linux.2 bash ./gradlew --no-daemon runGameTestServer build`                                    | Passed; eight mod GameTests and one vanilla test passed, with production JAR built. Client anvil GUI validation remains pending                                                                                                                                                                                                                                                                                                                                   |
| Authorized allocation/geometry run: `JAVA_HOME=/home/murilo/.gradle/jdks/eclipse_adoptium-25-amd64-linux.2 bash ./gradlew --no-daemon runGameTestServer build`                              | Passed; twelve mod GameTests and one vanilla test passed (13 total), including disk reload, corrupt/future/partial data rejection and all reservation-pair checks; production JAR built                                                                                                                                                                                                                                                                           |
| Authorized key-charging prototype run: `JAVA_HOME=/home/murilo/.gradle/jdks/eclipse_adoptium-25-amd64-linux.2 bash ./gradlew --no-daemon runGameTestServer build`                           | Passed; thirteen mod GameTests and one vanilla test passed (14 total), including exact 60-tick completion, early release, both hands/game modes, and server portal-trigger sound dispatch; production JAR built. Client animation/audio validation remains pending                                                                                                                                                                                                |
| Key-charging cooldown follow-up: `JAVA_HOME=/home/murilo/.gradle/jdks/eclipse_adoptium-25-amd64-linux.2 bash ./gradlew --no-daemon runGameTestServer build`                                 | Passed; all 14 GameTests and production build passed, including 400-tick shared cooldown, blocked copies/other anchor keys, player isolation, and reuse after expiry                                                                                                                                                                                                                                                                                              |
| Authorized empty-dimension run: `JAVA_HOME=/home/murilo/.gradle/jdks/eclipse_adoptium-25-amd64-linux.2 bash ./gradlew --no-daemon runGameTestServer build`                                  | Passed; fourteen mod GameTests and one vanilla test (15 total), including real pocket-dimension loading, End settings comparison, production-resource parity, and three generated air-only chunks; production JAR built                                                                                                                                                                                                                                           |
| Boundary geometry checkpoint, 2026-10-23                                                                                                                                                    | Added two-block geometry assertions. Generated-block protection awaits room generation so player-placed blocks remain breakable in creative. Not run: local compilation is prohibited by project contribution rules; CI result is pending                                                                                                                                                                                                                         |
| `git diff --check` after boundary geometry changes                                                                                                                                          | No whitespace errors reported; Git emitted the existing invalid-attribute warning from `.gitattributes`                                                                                                                                                                                                                                                                                                                                                           |
| Charge-time anchor availability checkpoint, 2026-10-23                                                                                                                                      | Added GameTest cases for valid registered/placed anchors, missing/unregistered/unplaced records, wrong-tier blocks, stale placement records, and both completion messages. Not run: local compilation is prohibited by project contribution rules; CI result is pending                                                                                                                                                                                           |
| `git diff --check` and translation JSON parse after availability changes                                                                                                                    | Passed whitespace check; English translation JSON parsed successfully. Git emitted the existing invalid-attribute warning from `.gitattributes`                                                                                                                                                                                                                                                                                                                   |
| Action-sound checkpoint, 2026-10-23                                                                                                                                                         | Added GameTest event assertions for chorus-fruit success, beacon-deactivation failure, and Vault rejection on duplicate registration. Not run: local compilation is prohibited by project contribution rules; CI and in-game audio review are pending                                                                                                                                                                                                             |
| Release CI run [39961080053](https://github.com/iloElias/dimensional-anchors/actions/runs/39961080053), 2026-10-23: `./gradlew --no-daemon -Pmod_version="0.1.1-alpha.1" runGameTestServer` | Failed at `compileGameTestJava`: the missing-anchor mock player did not implement abstract `Player.gameMode()`. Added the survival-mode override following the existing charging test pattern. GameTests did not execute; a new CI run is required to validate the fix                                                                                                                                                                                            |
| Owner-provided dedicated-server startup log, 2026-10-23, 13:38:30–13:39:27 (log timestamps)                                                                                                 | Passed startup smoke check for Dimensional Anchors `0.1.1-alpha.1`, Minecraft `26.1.2`, and NeoForge `26.1.2.122`: reached `Done (1.815s)!`, loaded 1523 recipes and 1617 advancements, and reported successful chunk saving including `dimensionalanchors:pocket`. No WARN/ERROR entries or stack traces in the supplied 113-line excerpt. This does not validate GameTests, client synchronization, sounds, key interactions, room generation, or teleportation |
| Owner's in-game report: anvil key copies and sound                                                                                                                                          | Matching original/copy UUIDs, shift-click copying, and restored anvil completion sound confirmed                                                                                                                                                                                                                                                                                                                                                                  |
| Owner's in-game report, 2026-10-23: key charging animation                                                                                                                                  | Confirmed to behave exactly as requested; no separate sound/cooldown result inferred                                                                                                                                                                                                                                                                                                                                                                              |
| Owner's in-game report, 2026-10-23: empty shared dimension                                                                                                                                  | Reported working correctly after the spectator-entry checkpoint; detailed environment, existing-save, and multiplayer checks were not individually reported                                                                                                                                                                                                                                                                                                       |
| GitHub Actions compilation and release publication                                                                                                                                          | Not run in this session                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| Owner's in-game report, 2026-10-23: full upgrade progression and extraction                                                                                                                 | Passed; UUID `00374300-47de-448e-8721-8097b10908f3` retained through Iron, Gold, Diamond, and Netherite; tier updated and registration remained false                                                                                                                                                                                                                                                                                                             |
| Owner's in-game report: save, leave, reopen, and extract                                                                                                                                    | Passed; Copper UUID `8d4343c1-258d-48cd-ada4-6f276b7e23fb` retained                                                                                                                                                                                                                                                                                                                                                                                               |
| Owner's in-game report: JEI, shift-click crafting, Netherite consumption                                                                                                                    | Recipes visible again; no shift-click duplication observed; one template, ingot, and Diamond anchor consumed                                                                                                                                                                                                                                                                                                                                                      |
| Owner's in-game report: independent/stacked anchors and fresh creative placement                                                                                                            | Distinct UUIDs confirmed; fresh creative items reusable; identity assigned only on first placement, regardless of tier                                                                                                                                                                                                                                                                                                                                            |
| Owner's in-game report: previously identified item after creative destruction                                                                                                               | Open edge case: retained item remains blocked after its placed block is destroyed; see [DA-001](KNOWN_ISSUES.md#da-001-identified-anchor-remains-unavailable-after-creative-destruction)                                                                                                                                                                                                                                                                          |
| Sound checks and dedicated-server multiplayer client synchronization for these fixes                                                                                                        | Not performed in the reported checkpoint                                                                                                                                                                                                                                                                                                                                                                                                                          |

Local compilation and headless testing were explicitly authorized by the owner for the regression, first-key registration, anvil-copying, allocation/geometry, key-charging prototype, and empty-dimension tasks. Client results above were reported by the owner, not executed by the assistant. No published release or broader mod compatibility is claimed; the normal restriction on local compilation remains.

The owner separately authorized local baseline and player-session build/GameTests
on 2026-10-23 for this checkpoint; this is not a permanent change to contribution
rules.
The safe-destination resolver received a separate local-test authorization.

The dedicated-server startup excerpt also shows RCON listening on `0.0.0.0:25575` and repeated short-lived IPv6 loopback client connections. These are informational entries, not server shutdowns; their originating process is not identified in the log. Host/container port exposure was not verified. Keep RCON restricted to trusted administration access, or disable it when unused. The INFO-level production-classpath fallback and skipped early display in headless mode are consistent with this dedicated-server launch.

### Room-generation validation status, 2026-10-23

- Owner confirmed that CI passed after the `gameMode()` fix, before these room-generation changes. No exact command, test count, or run URL was supplied for that confirmation.
- Room generation was initially untested locally under contribution rules. The
  newly authorized baseline command
  `JAVA_HOME=/home/murilo/.gradle/jdks/eclipse_adoptium-25-amd64-linux.2 bash ./gradlew --no-daemon build runGameTestServer`
  passed all 18 GameTests and built the production JAR. CI is not established by
  this local result.
- The same command after adding session persistence passed all 21 GameTests
  (20 mod tests plus one vanilla test) and the production build. The JAR includes
  session classes and excludes GameTest classes.
- Manual generation, expansion, creative break/resynchronization, audible
  sounds, normal gameplay save/restart, and multiplayer checks remain pending.
  Automated temporary-storage round trips are not manual gameplay confirmation.
- Static validation: `git diff --check` and English translation JSON parsing passed; Git still emits the pre-existing invalid-attribute warning.

## Not implemented

- Safe key entry and room-to-room travel, successful-entry cooldown, and session
  creation during entry.
- Movement enforcement, complete destruction lifecycle handling, and automatic
  recovery of ambiguous/partial travel states. Login/pocket occupant preservation
  and exterior respawn cleanup are implemented; full network/restart validation
  remains pending.

## Remaining decisions and artwork

- Deferred feature: crafting-table key + dye coloring for both blank and registered key handles, supporting all 16 vanilla dyes while preserving their distinct sprites and anchor references. Recorded in [key plans](design/KEYS.md#planned-handle-colors-deferred); not implemented.
- Recovery after permanent loss of an anchor.
- Final artwork and presentation for room blocks.
- The owner plans to make the blank and registered key sprites visually distinct.

See [open questions](design/OPEN_QUESTIONS.md) for the authoritative list.
Reported defects and investigation candidates are tracked in
[known issues](KNOWN_ISSUES.md).

## Next checkpoint

1. The owner confirmed the upgrade/identity client checkpoint. Keep the identified creative-destruction edge case tracked as DA-001; its fix is deferred.
2. Key-copy UUIDs, shift-click, and restored anvil sound have owner confirmation. Key animation and the empty dimension checkpoint are also confirmed.
3. The authorized local generation baseline and session-data checkpoint passed.
   Run CI and manually validate generation/expansion, generated-only protection,
   and save/restart preservation.
4. Bounded safe-entry/return search and fallbacks are implemented and tested.
   A server-authoritative exit service and working keyless control are now
   implemented and passed build/GameTests. Login/respawn reconciliation passed
   the 36-test checkpoint. Keep entry disabled until movement enforcement and
   remaining legitimate transition/recovery handling are ready.
5. Record additional client sound/cooldown, existing-save, and multiplayer checks when performed; do not infer results from the current confirmations.
6. Commit, push, and release/tag operations require separate requests; none were
   performed in this checkpoint.
