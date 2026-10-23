# Shared Dimension, Environment, and Compatibility

[Design index](../DESIGN_DECISIONS.md)

## Generation and allocation

- Use one shared empty pocket dimension, not a new dimension for each anchor.
- Each room receives an exclusive, non-overlapping region reserved for its maximum supported size.
- Generate physical contents on first access after registration. Reserve before generation to avoid overlapping concurrent allocations.
- Expand inside the reserved region without relocating existing contents.
- Tier dimensions are fixed at the five approved defaults for now; they are not configurable. Determine spacing from the largest fixed room; four-chunk spacing is not approved.
- Rendering/view distance is not protection. A forced view distance of three is not an approved requirement.
- Enforce room bounds and access server-side even when another player or mod loads nearby regions.
- Support persistent room/anchor mappings and migration when saved data changes.

## Environment

- Use vanilla The End as the reference for environmental settings: lighting, sky/time presentation, weather, and dimension-type behavior.
- Verify the actual target-version settings and conventions.
- This is not the real End: do not add End islands, structures, portals, or a dragon fight.
- Use a custom mod biome. Do not allow natural entity spawning in the pocket dimension.

## Resource use

- Do not permanently load inactive rooms or the remote anchor's chunk.
- Remote key access must continue to work when the anchor's chunk is unloaded, using persistent authoritative lifecycle data and bounded loading if needed.
- No continuous full-dimension ticking, repeated world scans, expensive unbounded per-tick work, or unlimited room allocation/configuration.
- Prefer event-driven or bounded validation and targeted incremental work.
- Do not perform destructive cache/build cleanup without demonstrated need or launch avoidably concurrent clients, servers, and builds.
- Request approval before unusually resource-intensive operations; diagnose excessive use rather than blindly retrying.

## Modded content and entities

- Allow arbitrary vanilla/modded blocks and items inside rooms; no content whitelist.
- Compatibility with every other mod's machinery or behavior is not guaranteed.
- Preserve block data and inventories during expansion/upgrades, while enforcing boundaries against bypass mechanics.
- Non-player entities can exist inside; the mod supplies no mechanism to transport them in.
- On anchor extraction/destruction, players and non-player entities remain in the preserved room; existing players can exit, but new entry is suspended while the anchor is absent.
- Anchor-block destruction does not delete room blocks, entities, or the reserved region.

Room generation, migration, and removal compatibility
still require [technical planning](OPEN_QUESTIONS.md).

## Implemented reservation foundation

- The approved cap is 4,096 rooms per world, laid out as a 64-by-64 grid.
  Slot `s` has reference X=`(s % 64) * 32 + 16` and
  Z=`floor(s / 64) * 32 + 16`.
- References are 32 blocks apart, not four chunks. Each maximum room requires a
  20-by-20 horizontal footprint including its two-block-thick boundary, leaving
  12 blocks between neighboring shells. No view-distance restriction is
  introduced.
- `RoomAllocationSavedData` stores UUID-to-slot mappings in the Overworld's
  saved-data storage, shared by requests from every dimension. Existing
  dimension-local anchor records are not migrated or otherwise changed here.
- Reservations are idempotent for an existing UUID, append-only, and never
  reclaimed. Exhaustion raises an explicit error without changing existing
  reservations. Player-facing translation is deferred until first-access
  gameplay calls the allocator; there is no new player entry path yet.
- Schema version 1 rejects unsupported versions, invalid UUIDs, out-of-range,
  duplicate, or non-contiguous slots, including partial codec results.
  Unreadable existing files block allocation rather than being replaced by an
  empty store. Layout/schema changes require an explicit future migration.
- This foundation does not reserve on crafting, placement, registration, or
  copying. First-access generation will call it before generating a room.
  No custom dimension, biome, chunks, physical room, or teleports are created
  by this implementation.

## Implemented empty dimension checkpoint

- `dimensionalanchors:pocket` is the single shared dimension, defined by a
  standalone data resource. It uses a custom dimension type of the same name
  and the custom `dimensionalanchors:pocket_void` biome.
- The dimension type mirrors Minecraft 26.1.2's actual End resource, except
  `has_ender_dragon_fight` is false. The 26.1 environmental attributes, End
  clock/timelines, skybox, ambient light, music, and vertical range (Y=0..255)
  are retained. No dimension-type Java registration or custom renderer is needed.
- The owner approved inheriting End gameplay rules as well: beds explode and
  cannot set spawn; respawn anchors do not work.
- A flat generator with zero layers, disabled features/lakes, and an explicit
  empty structure override list generates air only. The custom biome has no
  carvers, placed features, precipitation, spawn costs, or natural spawn entries.
  There is no End platform, portal, island, spike, or dragon fight.
- This does not prohibit entities intentionally introduced by players or other
  mods. No global entity removal or dimension ticking handler is added.
- No forced chunk tickets, player teleport, room generation, allocation on
  registration, or new key behavior is introduced by these resources.
- Existing dimension-local anchor records remain unchanged. This checkpoint
  does not resolve remote-anchor persistence or migration.

The GameTest server explicitly discards standalone datapack dimensions when
building its world. A test-only flat preset adds the pocket dimension while
preserving vanilla Overworld/Nether/End settings. The test decodes the production
dimension resource and verifies parity with that preset. The preset and test
classes must not be included in the production JAR.
