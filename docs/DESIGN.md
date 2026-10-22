# Dimensional Anchors — Consolidated Design Specification

## Subsequent Design Decisions

Start with the [design index](DESIGN_DECISIONS.md) and its topic documents. Their confirmed decisions supersede conflicting requirements here, including the original portable-key anchor concept, invitation-based entry, symmetric vertical expansion, original recipe proposals, and anchor-removal lifecycle. Collecting or destroying the anchor no longer ejects occupants or deletes the room; new entry is suspended until the anchor is available/recovered. Requirements not superseded remain applicable. The original text below is retained as a historical baseline; open questions must be interpreted in light of the subsequent decisions.

## 1. Project Overview

**Mod name:** Dimensional Anchors  
**Mod ID:** `dimensionalanchors`  
**Package:** `com.iloelias.dimensionalanchors`  
**Minecraft version:** `26.1.2`  
**Mod loader:** NeoForge  
**Build system:** Gradle with ModDevGradle  
**Author:** Murilo Elias

### Purpose

Dimensional Anchors introduces portable, upgradeable keys that provide access to individual pocket rooms. Each anchor is associated with its own persistent room, which can be expanded through a progression system ranging from Copper to Netherite.

Rooms are enclosed by indestructible boundary blocks and include a dedicated exit block. Players may authorize other players to enter their rooms.

The implementation must prioritize server authority, persistent data, multiplayer safety, low resource consumption, and compatibility with the exact target versions of Minecraft and NeoForge.

## 2. Confirmed Design Decisions

| Feature                      | Decision                                                                                               |
| ---------------------------- | ------------------------------------------------------------------------------------------------------ |
| Room association             | One room per anchor identity                                                                           |
| Room access                  | Room owner and explicitly invited players                                                              |
| Expansion                    | Symmetrical around a fixed center                                                                      |
| Tier downgrade               | Forbidden                                                                                              |
| Boundary violation           | Normal Minecraft death rules                                                                           |
| Netherite upgrade template   | Consumed during the upgrade                                                                            |
| Emergency return destination | Unresolved; investigate and recommend options                                                          |
| Entry orientation            | Player always enters facing north                                                                      |
| Exit block                   | Located on the south wall, behind the entering player                                                  |
| Room persistence             | Must survive server restarts                                                                           |
| Room ownership               | Must be represented by persistent server-side data                                                     |
| Item sprites                 | Must be provided as PNG assets by the project owner; the coding agent must not generate or edit images |

## 3. Anchor Identity and Room Ownership

Every anchor must have a unique, persistent room identifier independent of the current holder's player UUID.

The server must maintain a persistent association between:

- Anchor/room identifier.
- Owner UUID.
- Current upgrade tier.
- Room center and allocated spatial region.
- Authorized player UUIDs.
- Relevant room-generation and migration metadata.

The room identifier must survive legitimate upgrades. Upgrading an anchor must not silently create a new room or disconnect the player from existing constructions.

### Required behavior

- Each independently created anchor starts with its own room.
- Two anchors created independently must never share a room identifier.
- Upgrading an anchor preserves its room identifier, owner, invitations, room center, and existing room contents.
- Authorized visitors can enter the associated room without becoming its owner.
- Access checks must use server-side player UUIDs rather than usernames.
- Access must be checked on every entry attempt, not only when an anchor is crafted or first used.

### Technical risks requiring investigation

**Item duplication:** A duplicated anchor may carry the same room identifier as the original. The implementation must assess duplication exploits and determine whether multiple physical keys are permitted to access the same room. Room identity must never be derived solely from item count, inventory slot, or mutable client-provided data.

**Upgrade data preservation:** Standard crafting and smithing recipes may not automatically preserve custom item components or room identifiers. Investigate the appropriate custom recipe or data-transfer mechanism for Minecraft 26.1.2 and NeoForge before choosing an implementation.

**Ownership changes:** The policy for transferring ownership when an anchor is traded, dropped, stolen, or recovered is not yet defined. Do not implement an implicit transfer of room ownership.

**Lost anchors:** The recovery process for a lost or destroyed anchor remains an open design question. Do not assume that losing the item should delete the room or its contents.

## 4. Invitations and Access Control

Each room must support a persistent list of authorized players.

### Required behavior

- The owner can authorize another player to enter the room.
- Authorized players can enter and exit without acquiring ownership.
- Unauthorized players cannot enter, modify room data, or use the room's exit to gain access to another player's room.
- Revoking an invitation prevents future unauthorized entry.
- Access-control changes must be validated and committed by the server.
- The system must remain correct after restarts, player disconnections, and legitimate anchor upgrades.

The exact invitation interface is not yet specified. The planning stage should compare practical approaches, such as commands, a dedicated interaction interface, or another version-compatible mechanism, and recommend an approach before implementation.

Also investigate how access revocation should behave when an invited player is already inside the room. The implementation must not strand a player or allow the player to bypass access restrictions indefinitely.

## 5. Room Dimensions and Upgrade Tiers

The following dimensions represent the usable interior space, excluding walls, floor, and ceiling.

| Tier      | Interior dimensions | Usable interior volume |
| --------- | ------------------: | ---------------------: |
| Copper    |           3 × 3 × 3 |              27 blocks |
| Iron      |           5 × 5 × 5 |             125 blocks |
| Gold      |           7 × 7 × 7 |             343 blocks |
| Diamond   |        10 × 10 × 10 |           1,000 blocks |
| Netherite |        16 × 16 × 16 |           4,096 blocks |

### Configuration requirements

- Dimensions must be configurable by the server administrator.
- The table above provides the default dimensions.
- Configuration values must be validated and bounded by sensible maximums.
- Invalid configurations must not generate overlapping rooms, corrupt existing rooms, or cause uncontrolled chunk loading.
- The implementation must document whether dimensions are measured from interior boundaries or room-center coordinates.

### Centered expansion

Every room has a stable center. Upgrading its tier expands the usable interior symmetrically around that center.

The implementation must preserve existing constructions and avoid relocating previously placed blocks unnecessarily.

The planning stage must define the coordinate transformation for the room's interior, boundary walls, floor, ceiling, and exit. It must also account for even-sized dimensions, such as 10 and 16 blocks, which require an explicit convention for positioning the center relative to the block grid.

### Upgrade invariants

- Upgrades are permanent.
- Tier downgrades are not supported.
- Existing blocks and room data must survive upgrades.
- New boundary blocks must be generated without replacing existing interior constructions.
- The exit block must remain in the correct position after every upgrade.
- Expansion must not overwrite neighboring rooms or expose adjacent regions.

## 6. Pocket-Dimension Architecture

The preferred architecture to investigate is one dedicated pocket dimension containing spatially isolated rooms, rather than creating a separate dimension for every anchor.

This is a design preference, not a final technical commitment. The planning stage must verify whether it is suitable for Minecraft 26.1.2 and NeoForge.

The architecture must provide:

- Deterministic allocation of non-overlapping room regions.
- Persistent room-to-anchor mappings.
- Sufficient separation between neighboring rooms, including at maximum tier.
- Server-side ownership and access validation.
- Safe chunk generation and loading.
- Protection against entering neighboring rooms through movement, teleportation, or boundary exploits.
- Migration support for saved room data when the mod is updated.

Do not keep the entire pocket dimension loaded continuously. Investigate safe chunk-loading strategies and ensure the implementation does not retain unnecessary chunks or create unbounded background work.

The planning stage must also explain what happens to a room when its anchor is lost, destroyed, or no longer reachable.

## 7. Boundary Blocks and Room Integrity

Every room must be enclosed on all six sides by an indestructible custom boundary block:

- North wall.
- South wall.
- East wall.
- West wall.
- Floor.
- Ceiling.

Boundary blocks must be persistent and must not drop items when interacted with or destroyed through ordinary gameplay mechanisms.

### Protection requirements

Investigate and protect against:

- Normal mining and block replacement.
- Explosions.
- Pistons and related movement mechanics.
- Fluids and fire, where applicable.
- Ender pearls and other teleportation mechanics.
- Flight, knockback, and unusual movement.
- Commands or modded mechanics available to ordinary players.
- Interactions with other mods that could bypass standard block protection.

Boundary blocks must not be obtainable or placeable by ordinary players outside the intended room-generation system.

The boundary must be enforced server-side. Client-side rendering or collision alone is not a security mechanism.

The implementation must evaluate relevant NeoForge hooks and Minecraft APIs for the target version rather than assuming that protection implemented for an older version remains compatible.

## 8. Dedicated Exit Block and Orientation

Each room must contain a dedicated, interactive exit block on its south wall.

### Entry orientation

Whenever a player enters a room, the teleport destination must orient the player facing north. Consequently, the south wall is always behind the player.

This orientation must not depend on the player's original facing direction in the exterior world.

### Exit behavior

- The exit block must be distinct from the ordinary boundary block.
- It must be generated at a deterministic position on the south wall.
- It must remain correctly positioned after room upgrades.
- It must not be breakable, replaceable, or movable through ordinary gameplay.
- Its interaction must request a server-authoritative exit operation.
- Only a valid player currently inside the associated room can use it to exit.
- It must not provide a route into another player's room or bypass access control.

The exit must return the player to a safe exterior position associated with the current entry session or another explicitly validated return destination.

### Safe return and recovery

The fallback destination remains unresolved.

During planning, investigate at least:

1. The original exterior entry position, with a safe-position search nearby.
2. The last recorded safe exterior position.
3. The world spawn, after validating a safe destination.

Compare reliability, persistence, multiplayer behavior, and failure modes. Recommend one strategy and explain whether a hierarchy of fallbacks is necessary. Do not silently finalize this decision.

The exit operation must handle blocked destinations, unloaded chunks, disconnections, invalid saved positions, and server restarts without placing the player inside blocks or leaving the player stranded.

## 9. Boundary Violation and Death

If a player leaves the permitted safe interior through an unauthorized movement or teleportation mechanism, the server must kill that player immediately.

Required death message:

`<player> tried to access places beyond imagination`

The message must display the actual player's name using the appropriate supported message and death-reporting APIs for Minecraft 26.1.2.

### Enforcement requirements

- The server is authoritative.
- Normal movement near walls must not trigger false deaths.
- Legitimate exits must be processed before boundary-violation enforcement.
- Internal mod-controlled teleports must not be mistaken for unauthorized boundary crossings.
- Flight, teleportation, Ender pearls, knockback, login, respawn, and invalid coordinates must be evaluated.
- Client-side position manipulation must not bypass the rule.
- Boundary enforcement must not create recursive death handling or repeated death events.

### Death consequences

Use normal Minecraft death rules and respect applicable server configuration, including `keepInventory`, rather than introducing an independent item-loss system.

The planning stage must verify how to detect and report this particular death reason using supported APIs. Do not assume an older event or method signature remains valid.

## 10. Progression and Crafting

The planned progression is:

**Copper → Iron → Gold → Diamond → Netherite**

The first four tiers are intended to use crafting-table upgrades. Netherite uses the smithing table.

### Standard upgrade pattern

The proposed pattern uses the previous-tier anchor in the center, the target-tier material around it, and a Dimensional Catalyst in the bottom-center slot.

```text
[MATERIAL] [MATERIAL] [MATERIAL]
[MATERIAL] [ANCHOR]  [MATERIAL]
[MATERIAL] [CATALYST][MATERIAL]
```

This pattern contains seven target-material slots, one anchor slot, and one catalyst slot.

The precise material choices, quantities, and recipes remain subject to approval. The coding agent must not invent or finalize recipes during the research stage.

### Dimensional Catalyst

A Dimensional Catalyst is intended to be a mandatory ingredient in standard tier upgrades.

Echo Shards are a candidate ingredient because they connect progression to exploration of Ancient Cities and the Deep Dark. Their exact role and the complete catalyst recipe remain undecided.

The planning stage must identify compatible recipe and item-component mechanisms without committing to an unapproved recipe.

### Netherite upgrade

The proposed progression is:

1. Craft a Dimensional Upgrade Template by combining a Dimensional Catalyst with the vanilla Netherite Upgrade Smithing Template.
2. Use the Dimensional Upgrade Template, a Diamond-tier Dimensional Anchor, and a Netherite Ingot in a smithing-table upgrade.
3. Consume the Dimensional Upgrade Template during the upgrade.

The template-consumption rule is confirmed. The precise recipe for crafting the template remains open.

The smithing implementation must preserve the anchor's room identifier, owner, invitation list, and room association. Investigate the correct recipe and data-transfer APIs for the target Minecraft and NeoForge versions.

## 11. Assets and Resource Conventions

The main item is named **Dimensional Anchor**. Its icon should resemble a key rather than a placeable block.

The coding agent must not generate, edit, or synthesize sprite PNG files. It must identify the required asset paths and implement their registration and references.

Proposed assets include:

- Copper Dimensional Anchor.
- Iron Dimensional Anchor.
- Gold Dimensional Anchor.
- Diamond Dimensional Anchor.
- Netherite Dimensional Anchor.
- Dimensional Catalyst.
- Dimensional Upgrade Template.
- Boundary block and exit block textures, if their appearance requires custom assets.

All resource paths, namespaces, model formats, texture dimensions, item definitions, and language-file conventions must be verified against the exact target version and generated project template.

Use `dimensionalanchors` as the expected resource namespace unless version-specific project conventions establish otherwise.

All project-facing names, code comments, documentation, configuration descriptions, and initial UI strings must be in English.

## 12. Performance and Resource Constraints

The implementation must minimize unnecessary server work and avoid unbounded resource usage.

Requirements:

- No continuous full-dimension ticking for inactive rooms.
- No indefinite chunk loading for inactive rooms.
- No repeated full-world scans to identify room owners or boundaries.
- No expensive per-tick checks when event-driven or bounded validation is sufficient.
- No unbounded room allocation or unlimited configuration values.
- No destructive cache or build-directory cleanup without a demonstrated need.
- No concurrent client/server launches or builds that create avoidable resource pressure.

Prefer targeted inspection, incremental implementation, and focused tests. Request approval before unusually resource-intensive operations. If a command or build consumes excessive resources, stop and diagnose the cause rather than repeating it blindly.

## 13. Compatibility and Testing

The mod must be designed for the exact Minecraft 26.1.2 and compatible NeoForge environment.

Before implementation, verify the actual APIs, registry conventions, recipe formats, dimension registration patterns, player movement events, death handling, persistent data mechanisms, and resource formats.

The testing plan must include:

1. Creating multiple independent anchors and verifying room isolation.
2. Upgrading anchors while preserving room identity and access lists.
3. Inviting and revoking access for other players.
4. Testing access after server restart and player reconnection.
5. Testing all five tiers and their room dimensions.
6. Expanding rooms containing existing constructions near the new boundaries.
7. Testing entry orientation and the south-wall exit.
8. Testing blocked return positions and the selected recovery strategy.
9. Testing boundary protections against normal and unusual movement.
10. Verifying the exact death message and normal death consequences.
11. Testing item duplication, anchor loss, ownership edge cases, and room recovery.
12. Verifying that inactive rooms do not cause unnecessary chunk loading.
13. Testing dedicated-server behavior and relevant multiplayer edge cases.

Do not claim a test passed unless it was actually executed and its result was inspected.

## 14. Open Questions — Do Not Resolve Without Approval

The following decisions remain open:

1. **Initial Copper Anchor recipe:** required ingredients and quantities.
2. **Dimensional Catalyst recipe:** ingredients, quantities, and progression requirements.
3. **Dimensional Upgrade Template recipe:** exact crafting pattern and quantities.
4. **Ownership transfer:** whether an anchor can be transferred to another player and how ownership changes.
5. **Lost anchor recovery:** how the owner can regain access without unintentionally losing the room.
6. **Duplicate anchor behavior:** whether multiple copies of the same anchor identity may legitimately access one room and how duplication exploits are handled.
7. **Invitation interface:** commands, UI, or another interaction mechanism.
8. **Access revocation during occupancy:** what happens when a visitor is already inside the room.
9. **Emergency return policy:** which safe exterior fallback or fallback hierarchy should be adopted.
10. **Even-sized room centering:** the precise coordinate convention for 10 × 10 × 10 and 16 × 16 × 16 interiors.
11. **Orphaned-room policy:** how rooms are handled if their anchor is permanently lost or their persistent data becomes invalid.
12. **Dimension allocation and cleanup:** how room regions are allocated, migrated, and safely reclaimed, if reclamation is ever supported.

The planning report must explain the trade-offs for these questions and make recommendations where appropriate, but must not treat a recommendation as an approved design decision.

## 15. Implementation Workflow and Approval Gates

### Stage 1 — Research and Planning Only

Inspect the generated project and verify version-specific documentation and APIs.

Deliver:

- Environment and project-structure assessment.
- Compatibility findings with supporting sources.
- Proposed dimension and room-allocation architecture.
- Persistent data model and room-identity strategy.
- Registry, recipe, model, texture, and language-resource plan.
- Security and boundary-enforcement analysis.
- Multiplayer, failure-recovery, and performance analysis.
- Test strategy.
- Open-question list with recommendations.
- A staged implementation roadmap.

**Do not modify files, write implementation code, create resources, run builds, or launch Minecraft during Stage 1.** Read-only inspection is allowed. Finish the report and stop.

### Stage 2 — Approved Foundation

Proceed only after explicit user approval. Implement the approved foundation and report the changed files, decisions, and test results.

### Stage 3 — Incremental Features

Implement features in small, reviewable steps. Preserve checkpoints, validate each stage, and stop when an unresolved design decision affects implementation.

### Stage 4 — Verification

Run the approved focused tests and builds, inspect their results, document remaining risks, and provide clear instructions for manual verification.

## Final Instruction to the Coding Agent

Treat this document as the current source of truth for the mod's design. Confirmed decisions are requirements. Open questions are not permission to invent behavior.

Research the exact target environment, identify conflicts between requirements and Minecraft/NeoForge mechanics, and present a technically grounded plan. If two requirements conflict, explain the conflict and propose alternatives rather than silently weakening either requirement.

**For now, perform Stage 1 only. Do not modify the project. Stop and wait for explicit approval before implementation.**
