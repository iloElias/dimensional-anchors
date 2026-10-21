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
- Permit unlimited copies, including destroyed-room keys, without restoration/access. Check the tooltip if implemented.
- Losing every key denies entry without resetting registration or deleting the room; keyless south-wall exit remains available.

### Travel and lifecycle

- Verify teleport exactly at 60 charge ticks, no teleport/penalty on early release, and 400-tick shared cooldown after success.
- Use the current room's key to exit; use other keys to travel between rooms while retaining one exterior return point.
- Remote access works with the anchor chunk unloaded; neither anchor nor inactive room stays permanently loaded.
- Check north-facing entry and door behind the player for every tier.
- Check normal return position/dimension, personal-respawn then Overworld-spawn fallbacks, blocked destinations, and unavailable dimensions.
- Persist per-player return/session state across disconnect and server restart.
- Extract occupied anchors with qualifying pickaxes; expel players but keep non-player entities/content inside.
- Prevent all breaking progress with inadequate tools; permit any player to register, extract, or destroy.
- Preserve room position, contents, identity, and keys when replacing the anchor.
- Resolve offline extraction at the recorded removal location; subsequent relocation does not retarget it.
- Distinguish surviving explosion drops from permanent destruction, including later lava/void/despawn destruction.
- Compare each tier's blast resistance with its approved reference block, using Ancient Debris for Netherite.
- Destroy the occupied room irreversibly; verify online deaths and exact `The universe forgot <player>` message.
- Verify disconnected destruction survivors return alive to personal respawn or Overworld spawn.
- Respect `keepInventory`; when disabled, inventory drops disappear with the room.
- Apply death/removal and cleanup to non-player entities only on destruction, not extraction.
- Prevent stale keys or duplicated anchor items from reviving deleted rooms; protect neighboring rooms.

### Geometry, protection, and performance

- Verify all five interior sizes, fixed-floor upward growth, consistent horizontal rounding, and unchanged existing builds near boundaries.
- Move the south-wall door and north-facing entry together; handle blocked landing positions.
- Reserve non-overlapping maximum-size regions and generate only on first access.
- Exercise boundary mining/replacement, explosions, piston/fluid/fire mechanics, pearls, other teleports, flight, knockback, commands, and relevant modded behavior.
- Verify exact boundary death message `<player> tried to access places beyond imagination`, normal death consequences, no false wall deaths, and no repeated/recursive deaths.
- Distinguish legitimate exits/internal teleports from violations; test login, respawn, invalid coordinates, and client position manipulation.
- Check dedicated-server multiplayer, configuration validation, no unbounded world scans/ticking/allocation, and no unnecessary inactive-room chunk loading.

Do not claim compatibility or a passed check without executing and inspecting the relevant evidence. Documentation organization alone requires no game build.
