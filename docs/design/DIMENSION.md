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

Allocation limits, biome implementation, persistence APIs, and removal compatibility require [technical planning](OPEN_QUESTIONS.md).
