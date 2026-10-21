# Shared Dimension, Environment, and Compatibility

[Design index](../DESIGN_DECISIONS.md)

## Generation and allocation

- Use one shared empty pocket dimension, not a new dimension for each anchor.
- Each room receives an exclusive, non-overlapping region reserved for its maximum supported size.
- Generate physical contents on first access after registration. Reserve before generation to avoid overlapping concurrent allocations.
- Expand inside the reserved region without relocating existing contents.
- Determine spacing from supported configurable maximums; four-chunk spacing is not approved.
- Rendering/view distance is not protection. A forced view distance of three is not an approved requirement.
- Enforce room bounds and access server-side even when another player or mod loads nearby regions.
- Support persistent room/anchor mappings and migration when saved data changes.

## Environment

- Use vanilla The End as the reference for environmental settings: lighting, sky/time presentation, weather, and dimension-type behavior.
- Verify the actual target-version settings and conventions.
- This is not the real End: do not add End islands, structures, portals, or a dragon fight.
- Biome and natural spawning need explicit planning; dimension type alone does not define them.

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
- On extraction, non-player entities stay in the preserved room while players are expelled.
- On destruction, living entities die and other entities and drops are removed with the room.

Allocation limits, cleanup/reuse, biome/spawning, persistence APIs, and removal compatibility require [technical planning](OPEN_QUESTIONS.md).
