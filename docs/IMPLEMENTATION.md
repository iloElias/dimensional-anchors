# Implementation Kickoff

## Purpose and authorization

Use this guide to start Dimensional Anchors in small, verifiable stages. It is a work order for a future implementation session, not evidence that any feature exists.

Creating this guide does not authorize code changes, builds, or Minecraft launches. First complete the read-only technical assessment required by the [approval workflow](design/VERIFICATION.md), present the proposed foundation, and obtain explicit approval before implementing it. Approval of a stage does not resolve unrelated open gameplay questions.

## Required reading

1. [Contribution rules](../CONTRIBUTING.md).
2. [Current design index](DESIGN_DECISIONS.md) and all linked topic documents.
3. [Remaining decisions](design/OPEN_QUESTIONS.md).
4. [Verification matrix](design/VERIFICATION.md).

Current topic documents override conflicting statements in the [historical baseline](DESIGN.md). This guide describes implementation order, not new gameplay rules.

## Target and working rules

- Minecraft 26.1.2; NeoForge 26.1.2.122; Java 25; Gradle with ModDevGradle.
- Inspect the actual project and available dependency sources before choosing APIs. Do not copy older-version signatures or resource formats without verification.
- Preserve existing work. Inspect Git status before editing; do not revert unrelated changes or commit unless requested.
- Use English for project-facing code comments, strings, and documentation.
- Do not generate or edit PNG sprites. The project owner provides artwork; report required paths and missing assets.
- Prefer existing patterns and tools. Keep changes focused and avoid speculative frameworks.
- Never use client state as authority for room identity, lifecycle, access, or teleportation.
- Surface invalid state and failures explicitly; do not silently recreate rooms, discard saved data, or report unsuccessful operations as successful.
- No unnecessary world scans, permanently loaded inactive rooms, or permanently loaded remote anchor chunks.

## First session: read-only technical assessment

Before implementation:

1. Inspect current source, Gradle configuration, resources, available tests, and working-tree changes.
2. Identify template examples versus implemented features. Do not assume design approval means implementation is present.
3. Verify target-version mechanisms for:
   - Block/item registration, custom item data, saved data, and serialization/migration.
   - Shared void dimension, End-like environment, biome/spawning, and chunk loading.
   - Item-use charging, shared cooldown, player teleportation, respawn resolution, and death messages.
   - Crafting/smithing data transfer, anvil input preservation, XP cost, and suppressed anvil wear.
   - Mining qualification, explosions, item destruction/despawning, and distinction from chunk unload.
   - Server-side room bounds, movement/teleport hooks, and legitimate transition ordering.
4. Propose the smallest persistent data model and lifecycle transition table, supported by inspected APIs/sources.
5. Identify decisions that block the next stage; separate them from implementation details and recommendations.
6. Present a bounded foundation change, its file scope, tests, and known risks. Stop for explicit approval.

Do not run builds or launch Minecraft during this initial assessment.

## Persistent model to design

The exact types and serialization API depend on the assessment. The model must represent:

| Concern | Required information |
| --- | --- |
| Anchor/room | Stable identity, tier, registration, placed/extracted/destroyed state, exterior location/dimension, allocated region, generation status, data version |
| Key | Blank/registered state and anchor/room reference; separate-copy identifiers only if justified |
| Player session | Current room, one last exterior return position/dimension, and any pending return or destruction-survivor outcome |
| Allocation | Reserved regions and bounds, supported maximum size, generation/cleanup progress where needed |

Preserve identities through upgrades and relocation. Prevent stale keys or duplicate anchor items from reviving destroyed identities. Define restart recovery and concurrent-operation ordering before destructive features.

Do not add owner/invitation authorization: valid key possession is the approved entry model.

## Staged implementation order

Each stage requires its applicable approvals and a verified checkpoint. Do not expose incomplete destructive or access paths as working gameplay.

### 1. Foundation and persistence

- Replace relevant template examples with approved content registration and metadata without unrelated cleanup.
- Establish tiers, validated state representation, serialization, and server-side identity lookup.
- Register anchor variants, keys, catalyst, template, boundary, and control content as appropriate to approved names/layout.
- Identify required owner-supplied assets and use verified resource formats; do not invent PNGs.
- Verify distinct identities, serialization round trips, invalid-state handling, and separation of client-only code from dedicated-server logic.

### 2. Allocation, geometry, and room generation

Prerequisites: approved dimension/configuration limits, biome/spawning, and door layout where required.

- Add the shared empty dimension with End-like environmental settings.
- Reserve non-overlapping maximum-size regions; generate the physical room only on first access after registration.
- Implement floor-fixed upward expansion, consistent horizontal rounding, protected boundaries, south-wall control area, and north-facing entry.
- Keep geometry/allocation logic testable without launching the game where practical.
- Verify all default sizes, no overlap, preserved existing blocks/data, and bounded resource use.

### 3. Registration, entry, and normal exit

- Register the first blank key once; reject further blank-key registration.
- Implement key-only authorization, remote lifecycle lookup, and access with the anchor chunk unloaded.
- Charge for 60 ticks, teleport immediately at full charge, and apply a 400-tick shared registered-key cooldown only after successful key teleport.
- Cancel early without penalty; do not introduce an anti-combat-logout system.
- Support current-room key exit, different-room key entry, and keyless south-wall exit.
- Persist one exterior return destination across room-to-room travel, disconnect, and restart.
- Verify return fallbacks: saved destination, valid personal respawn, then Overworld world spawn.
- Wire boundary protection and legitimate-transition handling before exposing entry to playable rooms.

### 4. Recipes, copies, and upgrades

- Implement exactly the [approved recipes](design/RECIPES.md).
- Preserve room identity, registration, allocation, contents, and existing keys through every upgrade.
- Implement anvil copying: retain original key, consume one blank, cost one level, no wear, unlimited copies.
- Verify repeated collection and shift-click cannot produce free outputs or lose the original.
- Allow copies of destroyed-room keys without restoring access.
- Verify distinct key sprites once supplied; implement the destroyed-room tooltip only with reliable synchronized status.

### 5. Extraction and relocation

- Iron-or-better pickaxes extract; inadequate tools make no breaking progress.
- Preserve room contents and non-player entities while denying entry to an extracted anchor.
- Expel online players to the recorded removal location and persist offline returns.
- Keep pending offline extraction destinations unchanged after relocation.
- Replacing the anchor preserves the same room and existing keys.
- Verify removal, replacement, and restart behavior before enabling permanent deletion.

### 6. Destruction and failure recovery

Prerequisites: approved removal classification and offline-event precedence; validated persistence and cleanup plan.

- Distinguish surviving explosion drops from destruction and detect later anchor-item loss through lava, void, or despawning.
- Apply the approved blast-resistance reference per tier.
- Kill online occupants with `The universe forgot <player>`; return offline destruction survivors alive to personal respawn or Overworld spawn.
- Respect `keepInventory`; remove unrecoverable drops and non-player entities with the destroyed room.
- Persist permanent invalidation before allowing stale references to access or recreate the room.
- Clean only the affected region; do not reclaim regions without approved reuse policy.
- Verify restart/interruption recovery using disposable test worlds, never valuable saves.

### 7. Multiplayer and release verification

- Complete the [verification matrix](design/VERIFICATION.md), including boundary death message, movement bypasses, concurrent requests, restart, and dedicated-server behavior.
- Check compatibility claims against actual evidence, not arbitrary mod lists.
- Verify inactive rooms and remote anchors do not retain unnecessary chunk loads.
- Document manual checks, remaining risks, missing assets, and unsupported/conditional features.

## Blocking decisions

Consult [open questions](design/OPEN_QUESTIONS.md) before each stage. In particular:

- Door/control layout and final presentation where needed.
- Configuration maximums, allocation limits, and changes to existing rooms.
- Biome/natural spawning and safe-destination policy details.
- Offline extraction followed by destruction before reconnect.
- Commands/modded removal, cleanup safety, and whether region reuse is permitted.

Do not let an unrelated late-stage question block read-only assessment or independent approved foundation work. Conversely, do not bake an unapproved answer into persistent data or gameplay.

## Validation and completion

After code execution is approved:

- Use the smallest existing check covering each change, adding focused tests for new behavior.
- The documented build command is `./gradlew build`, requiring Java 25. Run it at appropriate integration checkpoints, not after every documentation edit.
- The project configures a GameTest server run, but first verify the actual generated task and register relevant tests; an empty GameTest run is not passing coverage.
- Launch clients/servers only when authorized and necessary; avoid concurrent resource-heavy runs.
- Never imply a successful build proves multiplayer, persistence, or gameplay tests passed.

At every checkpoint report:

1. Scope completed and changed files.
2. Approved behavior implemented and any deviations needing approval.
3. Exact validation commands and observed results.
4. Manual scenarios actually executed, not merely proposed.
5. Remaining blockers/risks and the next bounded stage.

Do not declare a stage complete until its expected behavior is verified and saved persistently. If validation is blocked, report the blocker rather than claiming success.
