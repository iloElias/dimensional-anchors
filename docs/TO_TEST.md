# TO_TEST

Central validation backlog. Updated: 2026-10-23.

## Working agreement

- Local implementation/build/test execution was reauthorized on 2026-10-23
  after artifact caching. Use one Gradle worker with parallel execution disabled.
  Client launches, installation and remote CI results remain separate checks.
- Add every new or changed behavior, regression risk, failure path, and deferred
  validation here in the same change as its implementation. Writing automated
  tests does not mean running them or passing validation.
- Keep stable IDs. Record the exact command or manual steps, result, date,
  version/commit or working-tree identity, and evidence when a check is performed.
  Never mark a check passed solely from source inspection or an older checkpoint.
- Status: **Pending** = implementation exists but needs validation;
  **Blocked** = prerequisites are missing; **Failed** = observed failure awaiting
  successful rerun; **Passed** = executed with recorded evidence.
- A backlog entry does not authorize unresolved design decisions or release an
  entry safety gate. Do not expose entry before reliable exit, legitimate
  transitions, movement enforcement, and reconnect/death reconciliation.
- Use disposable worlds and back up existing saves before later manual checks.

## Baseline and current evidence

The earlier session/resolver checkpoint passed 24 GameTests and a production
build; it does not validate the newer exit-service changes. The owner's first
exit build failed because `ServerPlayer.serverLevel()` does not exist in the
target version. The service and test calls were changed to `level()`; the
correction now passes the authorized build. Exit baseline: 30 GameTests passed.
After adding arrival-edge tests, the first run found an uninitialized yaw in the
new test fixture; explicit rotation initialization fixed it and the final run
passed all 32 GameTests (31 mod and one vanilla) and the production build.

Evidence E-2026-10-23-EXIT32: current uncommitted exit/hardening/arrival-edge
working tree, configured version `0.1.0-alpha.1`; exact command
`JAVA_HOME="$HOME/.gradle/jdks/eclipse_adoptium-25-amd64-linux.2" bash ./gradlew --no-daemon --no-parallel --max-workers=1 build runGameTestServer`;
result `BUILD SUCCESSFUL in 28s`, all 32 required tests passed. Expected
exception/corrupt-store logs are negative test cases. Production JAR inspection
found session/travel classes and no inspected GameTest classes. No CurseForge
installation, client check or remote CI was performed.

Evidence E-2026-10-23-RECONCILE36: current uncommitted reconciliation working tree,
configured version `0.1.0-alpha.1`; same exact command as EXIT32 above.
Result: `BUILD SUCCESSFUL in 32s`, all 36 tests passed (35 mod and one vanilla).
Four new tests cover event-bus login/respawn integration and actual vanilla
respawn replacement. No network reconnect/process restart, installation or
manual client test was performed. Pocket occupants are never expelled by these
hooks; absent anchor placement only restricts new entry.

Evidence E-2026-10-23-MOVEMENT44: current uncommitted movement/exit/reconciliation
working tree, version `0.1.0-alpha.1`; exact command
`JAVA_HOME="$HOME/.gradle/jdks/eclipse_adoptium-25-amd64-linux.2" bash ./gradlew --no-daemon --no-parallel --max-workers=1 build runGameTestServer`.
Result: `BUILD SUCCESSFUL in 30s`, all 44 required tests passed (43 mod and one
vanilla). Eight new movement tests plus extended exit/reconciliation checks.
Initial 42-test run failed five death cases due to loading immunity; the 43-test
rerun failed one post-death tracker assertion. Fixtures now acknowledge loading,
production defers immune damage without discarding its baseline, and exact death
text is checked in the emitted packet. A 43-test run then passed before adding
the final identity/actual-pocket-respawn regression. Production packaging and
whitespace inspected; no installation, client launch or remote CI.

Evidence E-2026-10-23-RECOVER51: current uncommitted administrative recovery
working tree, version `0.1.0-alpha.1`; exact command
`JAVA_HOME="$HOME/.gradle/jdks/eclipse_adoptium-25-amd64-linux.2" bash ./gradlew --no-daemon --no-parallel --max-workers=1 build runGameTestServer`.
Result: `BUILD SUCCESSFUL in 43s`, all 51 required tests passed (50 mod and one
vanilla), including seven new recovery tests and all prior regressions.
Two initial 50-test runs failed the existing movement death-drop query (zero
visible drops); named-item isolation alone did not fix it. Moving that death
fixture to the already entity-visible GameTest chunk retained real packet,
inventory and spawned-item assertions; the next 50-test run and final 51-test
run passed. No installation, client launch, real reconnect/restart or CI.
Player-transition journal/rescue/retry and both entry paths remain unimplemented.

See [verification](design/VERIFICATION.md) for detailed historical coverage,
[progress](PROGRESS.md) for the handoff, and [known issues](KNOWN_ISSUES.md) for
reported defects. Existing historical confirmations remain in those documents;
the rows below track outstanding validation, not a claim that all features exist.

## Build and test installation

| ID | Check / procedure | Expected result | Status | Latest evidence |
| --- | --- | --- | --- | --- |
| TT-001 | Run `bash ./gradlew --no-daemon --no-parallel --max-workers=1 build` after the accessor fix. | Production sources compile and the production JAR is produced. | Passed | E-2026-10-23-EXIT32: build passed; artifact inspected. |
| TT-002 | Run `bash ./gradlew --no-daemon --no-parallel --max-workers=1 runGameTestServer`. | Test sources compile; all registered tests pass, including exit and travel-hardening additions. | Passed | E-2026-10-23-EXIT32: all 32 passed after correcting new arrival-test fixture yaw. |
| TT-003 | With Minecraft closed, run `bash ./build-test.sh`. | Uses configured mod ID/version; verified production JAR reaches the specified CurseForge instance; no GameTests or game launch. | Pending | Shell syntax checked only. |
| TT-004 | Exercise build failure, missing destination directory, invalid properties, and missing artifact in an isolated script fixture. | Explicit nonzero failure; existing installed mods are preserved before successful replacement. | Pending | Not executed. |
| TT-005 | Exercise copy/verification failure and successful install with older mod versions and unrelated mods in a fixture. | No cleanup of old versions before successful install; only matching old mod JARs removed; temporary file cleaned. | Pending | Not executed. |
| TT-006 | Launch installed JAR in a fresh and reopened disposable world and on a dedicated server. Inspect packaged classes/resources. | Correct version loads without registry/resource errors; no GameTest classes in production JAR. | Pending | Current JAR not built or launched. |

## Exit service and safe return

| ID | Check / procedure | Expected result | Status | Latest evidence |
| --- | --- | --- | --- | --- |
| TT-007 | Run `keyless_exit_control` with a recorded session and no placed anchor. | Generated south-wall control exits without a key; exact exterior position/yaw/pitch restored; matching session removed. | Passed | E-2026-10-23-EXIT32; headless control interaction, not manual client behavior. |
| TT-008 | Run `rejected_exit_teleport`. | Rejected teleport reports failure, leaves player inside, and preserves session. | Passed | E-2026-10-23-EXIT32; controlled rejection override. |
| TT-009 | Use the generated control empty-handed, holding unrelated items, and with either hand; include key cooldown. | Keyless exit remains available; no accidental item use, duplicate teleport, or cooldown-dependent lockout. | Pending | Interaction overrides exist; complete coverage not written. |
| TT-010 | Use player-placed controls in another dimension, interior, unused reservation, and another room. | No unauthorized exit or entry; explicit failure without session mutation. | Pending | Automated coverage needed. |
| TT-011 | Attempt exit without a session, outside the recorded room, or with missing/incomplete allocation. | No teleport or session deletion; explicit diagnostic feedback. | Pending | Automated coverage needed. |
| TT-012 | Fully charge the current room's key while its anchor is absent. Repeat after early release and during cooldown. | Normal charged exit independent of anchor availability; early release has no effect; successful key exit applies shared 400-tick cooldown. | Pending | Key path wired; exit-specific charge coverage needed. |
| TT-013 | Reject unsafe exit and failed key teleport; retry through key and control. | Failed key exit does not apply success cooldown; original return/session retained; retry remains possible. | Pending | Rejected-teleport test covers only direct control service call. |
| TT-014 | Block saved return, remove its dimension, invalidate/obstruct/deplete personal respawn, and obstruct world spawn. | Saved/nearby return, personal respawn, then bounded Overworld spawn priority; exhaustion reports failure and retains session. | Pending | Resolver-only tests passed previously; integrated travel not validated. |
| TT-015 | Return to exact fractional coordinates and partial-height floors; test hazards, fluids, portals, border, build height, and radius endpoints. | Safe standing destination only; bounded, non-destructive search; no respawn-anchor charge consumption. | Pending | Resolver-only coverage exists; integrated rerun pending. |
| TT-016 | Run `canceled_reentrant_exit`; cancel NeoForge travel, nest key/control requests and retry. Inspect position, motion, fall distance and session. | Cancellation preserves source state/session; nested calls rejected; guard released; confirmed retry clears motion/fall distance. | Passed | E-2026-10-23-EXIT32; real NeoForge cancellation. Arbitrary mod/network compatibility not inferred. |
| TT-017 | Run `replaced_exit_sessions`; replace/remove sessions and change rooms away/back during hooks; include another occupant. | Captured-session identity required for removal; hook changes and other occupants untouched; no false success or leaked guard. | Passed | E-2026-10-23-EXIT32; controlled mutation scenarios, not live concurrent-player gameplay. |
| TT-018 | Exercise cleanup conflict after confirmed movement, then recover the exterior player's retained session. | Explicit administrative diagnostic; no normal success or destructive overwrite; reconciliation before entry is enabled. | Blocked | Conflict diagnostics and written tests exist; automatic recovery/reconciliation not implemented. |
| TT-042 | Run `exceptional_exit_teleports`: throw before/after movement, return true without moving, reject after moving, redirect or rotate player, obstruct destination, or change dimension. | No false success/session removal; explicit feedback; guard released; no automatic second teleport. | Passed | E-2026-10-23-EXIT32; eight controlled-override scenarios. |
| TT-043 | Run `player_travel_snapshots`; exercise per-player guard isolation/release/reload and value-equal recreated/ABA sessions. | No guard serialization/dirty-state change; only the same captured session object can complete exit. | Passed | E-2026-10-23-EXIT32; saved-data schema unchanged. |
| TT-044 | Run `arrival_validation_limits` and `travel_thread_liveness` for tolerances, wrapped yaw, non-finite positions/rotation, dead/removed players and off-thread calls. | Only a live safe arrival within 0.000001 blocks and 0.0001 degrees is confirmed; off-thread travel rejected before mutation. | Passed | E-2026-10-23-EXIT32; position inside/outside/diagonal checks, inclusive rotation endpoints and adjacent floats; invalid getter/coordinate fixtures; dead/removed and off-thread checks. |
| TT-045 | Exercise real key completion on cancellation, exceptions, redirects and cleanup conflicts; include failures in post-arrival checks/reset. | No success cooldown or confirmation sound on unconfirmed exit; visible diagnostic; guard always releases; successful key exit retains shared cooldown. | Pending | Exit failure result remains false; end-to-end key/hook failure coverage still needed. |
| TT-046 | Mutate the player's location/session or kill/remove the player during destination-resolution hooks; include resolver failure and retry. | Abort before teleport when the captured session, source location or liveness changes; no session overwrite or stale guard. | Pending | Pre-teleport snapshot checks implemented; hook-driven resolver coverage still needed. |

## Entry, transitions, and lifecycle safety gates

Rows blocked on future implementation must not be interpreted as playable features.

| ID | Check / procedure | Expected result | Status | Latest evidence |
| --- | --- | --- | --- | --- |
| TT-019 | Charge keys externally and for another room with the current implementation. | Room preparation only; no entry teleport or new session; feedback says entry is disabled. | Pending | Existing prototype assertions need rerun against current changes. |
| TT-020 | Implement and exercise authorized entry and room-to-room travel with safe destinations. | North-facing entry; `world -> A -> B -> exit` preserves the first exterior return; no block-entry route. | Blocked | Entry and room-to-room integration not implemented. |
| TT-021 | Exercise teleport rejection, exceptions, reentrancy, and session-write failure during entry/change-room. | No lost exterior return, unauthorized location, duplicate transaction, or silent partial success. | Blocked | Entry transaction not implemented. |
| TT-022 | Disconnect/reconnect and restart with an active session, including an absent anchor. | Occupants resume safely and can exit; no new entry through an absent anchor. | Pending | E-2026-10-23-RECONCILE36: login-event preservation passed; real network/restart checks still required. Entry remains disabled. |
| TT-023 | Die/respawn inside and outside rooms, including stale sessions and invalid respawns. | Approved session reconciliation; normal death consequences; no false boundary death or death loop. | Pending | E-2026-10-23-RECONCILE36: actual vanilla exterior respawn and pocket event handling passed; full death/drop/invalid-spawn integration remains pending. |
| TT-047 | Run `reconcile_room_occupants` with absent anchor and repeated pocket login/respawn events. | Same session, location and velocity; no expulsion, room generation or anchor-availability dependency. | Passed | E-2026-10-23-RECONCILE36. |
| TT-048 | Run `reconcile_inconsistent_locations` for exterior stale session, absent session, missing allocation, wrong room and invalid coordinates. | Explicit diagnostics; location/session preserved, no inferred entry or automatic death/teleport. | Passed | E-2026-10-23-RECONCILE36; five event-driven scenarios. |
| TT-049 | Run `reconcile_exterior_respawn` and `reconcile_vanilla_respawn`. | Guard isolation; inactive player preserved; live exterior vanilla replacement clears only matching session, without changing vanilla destination; repeated events are no-ops. | Passed | E-2026-10-23-RECONCILE36; event cases plus actual `PlayerList.respawn`. |
| TT-050 | Validate normal network reconnect/restart after extraction, recovery, upgrade, missing/corrupt data, external teleport and respawn modifications by another mod. | No pocket expulsion; no session loss or unauthorized new entry; ambiguous exterior login reports unresolved state. | Pending | Headless hook coverage does not simulate these complete network/mod interactions. |
| TT-024 | Exercise walls, flight, knockback, pearls, commands, external teleports, invalid coordinates, and manipulated movement. | Approved enforcement and exact boundary death message; legitimate travel is exempt; no recursive/false wall deaths. | Pending | Post-player-tick enforcement implemented; real packets, movement mechanics and mod interoperability still need manual/integration checks. |
| TT-051 | Run `movement_room_faces` across all tiers and standing/crouching/swimming poses, including collision rounding. | Full-body contact with all interior faces/corners is safe; absent anchor never triggers punishment. | Passed | E-2026-10-23-MOVEMENT44; headless bounding-box/position checks, not real client physics. |
| TT-052 | Run `movement_departures` for all six faces, neighboring reservation, dimension change and non-finite coordinates after valid occupancy. | One vanilla generic-kill damage attempt; preserve session until normal respawn reconciliation. | Passed | E-2026-10-23-MOVEMENT44; six-face threshold cases use the copper room. |
| TT-053 | Run `movement_vanilla_death` with keepInventory on/off and creative/survival. | Exact approved text in the actual outbound death packet; normal retained inventory or actual drops, without duplication; creative does not bypass enforcement. | Passed | E-2026-10-23-MOVEMENT44; creative + keepInventory on, survival + off; actual packet and item counts. |
| TT-054 | Run `movement_canceled_death` with recursive ticks from a canceling/resurrecting hook. | At most one damage attempt per observed session violation; explicit unresolved-state diagnostic, no repeated death loop. | Passed | E-2026-10-23-MOVEMENT44; real NeoForge death hook restores health and posts nested ticks. |
| TT-055 | Run `movement_session_lifecycle` and reconciliation regressions. | Active travel exempt; logout clears baseline; ambiguous initial locations suspend, not kill; valid login restores observation, without anchor dependency. | Passed | E-2026-10-23-MOVEMENT44; synthetic server events, not network reconnect. |
| TT-056 | Run key/control exit and exceptional-exit regressions with armed movement observations and follow-up ticks. | Confirmed exits clear observation/session; partial, redirected or throwing exits retain session and suspend punitive checks until reconciliation. | Passed | E-2026-10-23-MOVEMENT44; controlled overrides plus real control/key service exit. |
| TT-057 | Run `movement_invalid_exit_attempts`; submit unrelated keys and incorrect controls before unauthorized movement. | An ordinary rejected request cannot disable an armed boundary monitor. | Passed | E-2026-10-23-MOVEMENT44. |
| TT-058 | Interrupt room expansion or remove authoritative allocation/session during movement; restart/reconnect after cancellation or failed travel; test unloaded room chunks and multiple players. | No destructive guess, stale-player observation or automatic retry; explicit unresolved diagnostics and per-player isolation; no scan/chunk ticket per tick. | Pending | Dedicated integration/manual checks remain required; suspended state is transient, not a recovery policy. |
| TT-059 | Run `movement_loading_immunity` with a real cross-dimension teleport and client-load acknowledgment. | Defer damage during vanilla loading immunity without discarding the armed baseline; enforce on the next eligible tick if still outside. | Passed | E-2026-10-23-MOVEMENT44; initial loading-immunity failure and correction recorded above. |
| TT-060 | Run `movement_pocket_respawn` with actual vanilla player replacement, followed by obsolete-player logout/tick events and a new unauthorized departure. | Pocket respawn preserves the session without expulsion; stale events from the old object cannot erase the new object's armed baseline. | Passed | E-2026-10-23-MOVEMENT44; actual `PlayerList.respawn`, then obsolete-object events and new departure. |
| TT-025 | Remove/destroy/recover A after traveling from A to B; repeat for occupied B. | Current room determines behavior; preserved rooms/content/entities; no forced removal death or teleport; keyless exit works. | Blocked | Full lifecycle/travel integration pending. |
| TT-026 | Enter remotely with unloaded anchor chunks; lose all keys; restore/recover absent anchors. | Valid-key access only, no ownership gate; unavailable anchors deny new entry; lost keys never prevent exit or delete the room. | Blocked | Playable entry not implemented. |

## Anchor recovery command checkpoint

| ID | Check / procedure | Expected result | Status | Latest evidence |
| --- | --- | --- | --- | --- |
| TT-061 | Run `recover_anchor_tiers` with all five tiers, registered/unregistered records, existing rooms and occupant sessions. | Exactly one matching anchor item with the original UUID, tier and registration; no entry until placement, session mutation, room regeneration or reallocation. | Passed | E-2026-10-23-RECOVER51; sentinel room contents, reservations and recorded sessions preserved. |
| TT-062 | Run `recover_command_failures` with ordinary permissions, malformed/unknown UUID, console and full inventory. | Administrator player only; explicit failure, no item drop, no mutation on refusal. | Passed | E-2026-10-23-RECOVER51; real registered dispatcher and stale placed record on full-inventory refusal. |
| TT-063 | Run `recover_placed_conflicts` with a present block, stale missing block and multiple placed records. | Refuse present/conflicting anchors; verify recorded chunk before recovering an actually missing block; no arbitrary identity selection. | Passed | E-2026-10-23-RECOVER51. |
| TT-064 | Run `recover_relocated_duplicates`; recover twice, place/relocate across dimensions, try the spare item and recover again. | At most one placed identity across dimensions; relocate authority without obsolete records; existing key UUID remains available. | Passed | E-2026-10-23-RECOVER51; actual cross-dimension placement/extraction and availability check. |
| TT-065 | Run `recover_unplaced_upgrades` using actual crafting/smithing output pickup, before placing the result; try an obsolete lower-tier item. | Preview does not mutate records; completed upgrades persist tier for recovery; reject downgrades. | Passed | E-2026-10-23-RECOVER51; actual Iron crafting and Netherite smithing menu output pickups. |
| TT-066 | Run `recover_anchor_storage` for disk reload, partially readable and corrupt anchor stores. | Recovery tier survives reload; reject unreadable authoritative stores without partial reset/overwrite. | Passed | E-2026-10-23-RECOVER51; byte-for-byte retained invalid files. |
| TT-067 | Recover after real creative/explosion removal, with recorded chunk unloaded, full client inventory and repeated administrator actions across restart. | Explicit verified outcome, no duplicate placed identities, no dropped recovery items or occupant movement. | Pending | Dedicated client/restart/unloaded-chunk checks remain required. |
| TT-068 | Lose an upgraded anchor that predates tier tracking and was never placed; recover from unavailable-dimension or missing/conflicting metadata. | Never guess an unrecorded tier/identity or silently create a new room; document legacy limitations and refuse inconsistent known data. | Pending | Old unplaced item upgrades cannot be reconstructed from absent historical evidence. |
| TT-069 | Run `recover_unloaded_record` with an unloaded recorded chunk. | Load and inspect the exact recorded chunk before marking an absent block recoverable. | Passed | E-2026-10-23-RECOVER51; initially unloaded pocket chunk verified loaded. A present block saved/unloaded across restart remains part of TT-067. |
| TT-070 | Implement/reload the approved transition journal and reconcile proven arrival, unchanged source, intermediate failure and unrelated session changes. | Persisted operation identity/stage; bounded conservative completion/rollback, no overwrite of another operation, no blind session deletion. | Blocked | Policy approved; player-transition recovery implementation is the next checkpoint. |
| TT-071 | Implement both approved entry routes and A-to-B travel with source rollback and fixed exterior return. | Remote-use position versus safe block-front return captured at entry; original exterior return survives A-to-B; no entry without matching key or placed anchor. | Blocked | Entry/facing/room-change integration remains disabled. |
| TT-072 | Track actual safe exterior respawns; exercise current respawn, historical safe spawn, exterior return and Overworld exhaustion. Include pocket occupant without session and explicit retry. | Revalidate ordered fallbacks; rescue without arbitrary room assignment; failed recovery preserves state and has an explicit bounded retry route. | Blocked | Approved historical-spawn/rescue/retry behavior is not implemented by the anchor recovery command. |

## Existing feature regression and manual backlog

| ID | Check / procedure | Expected result | Status | Latest evidence |
| --- | --- | --- | --- | --- |
| TT-027 | Create independent anchors; register once; relocate/restart; attempt duplicate UUID placement and unrelated/blank keys. | Stable distinct identities, one-time registration, no unauthorized entry or duplicate placed identity. | Pending | Earlier automated coverage; current rerun pending. |
| TT-028 | Exercise all approved crafting/upgrade recipes and Netherite smithing. | Correct ingredients/counts/template consumption; identity, registration, key validity, room data and contents preserved. | Pending | Earlier automated/client evidence is historical, not current validation. |
| TT-029 | Copy keys in an anvil, including copies-of-copies, shift-click and repeated output collection. | Original retained, one blank and one level consumed on collection, no wear/duplication/cost escalation; unavailable-room copying grants no access. | Pending | Earlier owner confirmation; current regression rerun pending. |
| TT-030 | Check blank/registered/copied key sprites, glint, max stack size, animation, translated messages, and audible effects. | Distinct intended presentation; exactly one completion effect; explicit failure feedback. | Pending | Earlier animation/copy-sound confirmation; new exit feedback unvalidated. |
| TT-031 | Charge at 0/1/30/59/60 ticks in survival/creative and both hands; switch keys/copies and players during cooldown. | Exact completion threshold; early release no penalty; shared cooldown does not leak across players; prototype behavior unchanged outside exits. | Pending | Existing charging GameTest awaits rerun. |
| TT-032 | Generate every tier; check all shell coordinates, room spacing, door/entry orientation and fixed floor. | Correct interior sizes, two layers on six faces, single south-wall control, non-overlapping maximum reservations. | Pending | Earlier headless coverage; client checks pending. |
| TT-033 | Build near walls, store inventories, expand every tier, recharge, save/restart, and resume interrupted generation. | No content loss/moved floor; door follows expansion; unexpected obstruction/damaged shell fails explicitly; neighbor physics reviewed. | Pending | Earlier headless coverage; manual expansion/restart pending. |
| TT-034 | Break generated shell/control in survival/creative; compare player-placed utility blocks. | Generated-only protection and client resynchronization without drops; player-placed blocks remain breakable in creative. | Pending | Break-event tests exist; actual client prediction unvalidated. |
| TT-035 | Exercise adequate/inadequate tools, blasts, pistons, fluids/fire, commands, and modded bypasses for every tier. | Approved tool gating/resistance; generated protection behavior explicit; preserved rooms never deleted by anchor destruction. | Pending | Full mechanics/manual coverage pending; ordinary break cancellation is not proof. |
| TT-036 | Run session/allocation codec and disk tests, including corrupt/future/partial stores. | Exact per-player data retained across dimensions/reload; fail closed without resetting or overwriting authoritative data. | Pending | Earlier checkpoint passed; current regression rerun pending. |
| TT-037 | Inspect fresh/reopened pocket dimension in spectator with an operator return path. | Void terrain, intended biome/End-like rendering and lighting; no unplanned generation. | Pending | Headless loading passed previously; manual client checks pending. |
| TT-038 | Dedicated multiplayer with concurrent generation, upgrades, charges, exits, and independent rooms. | Isolation, no duplicate transitions/allocations, bounded work and no unnecessary persistent chunk tickets. | Pending | Current multiplayer behavior not validated. |
| TT-039 | Reproduce [DA-001](KNOWN_ISSUES.md#da-001-identified-anchor-remains-unavailable-after-creative-destruction) through actual creative breaking, then replace/restart. | Removal clears availability without changing identity or bypassing duplicate-placement protection. | Blocked | Known defect; fix explicitly deferred. |
| TT-040 | If [DA-002](KNOWN_ISSUES.md#da-002-extracted-registered-anchor-reported-with-an-unset-registration-flag) recurs, inspect server held-item data before/after Gold upgrade, restart and extraction. | Same UUID and registered flag; no workaround that resets identity or registration. | Pending | Follow-up did not reproduce original report; cause unexplained. |
| TT-041 | Run repository CI validation after changes are ready. Record workflow URL and exact result separately from local runs. | Build and GameTests pass in the configured environment; release/tag operations remain separately authorized. | Pending | No current CI evidence. |
