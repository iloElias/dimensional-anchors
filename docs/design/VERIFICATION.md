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
