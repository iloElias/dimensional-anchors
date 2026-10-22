# Remaining Decisions and Technical Planning

[Design index](../DESIGN_DECISIONS.md)

These are unresolved details, not permission to invent gameplay. Recipes, key timings, anvil costs, and the core lifecycle are already approved.

## Decisions requiring approval

- **Door/control layout:** whether the entire south-wall 3 x 3 area is interactive or only a dedicated control block.
- **Configuration:** maximum room dimensions/allocation limits and whether configuration changes affect existing rooms.
- **Biome/natural spawning:** explicit settings compatible with the approved End-like environment.
- **Region reuse:** whether deleted regions may be reclaimed and under what safeguards.
- **Presentation:** final names/appearance for keys and internal blocks where not yet fixed.

## Technical work to propose and verify

- Exact horizontal rounding, door geometry, landing coordinates, allocation spacing, and migration formulas.
- Persistent identity, room lifecycle, region reservations, pending returns, and saved-data versioning.
- Key-copy validation versus illicit duplication, and whether individual copy identifiers are useful without revocation or copy limits.
- Safe landing/return search bounds, unavailable dimensions, obstructed personal/world spawn, and bounded chunk loading.
- Custom crafting/smithing preservation and anvil input/XP/output handling in the exact target version.
- Vanilla item-use behavior when receiving damage; no new anti-combat-logout system is approved.
- Classification of commands, modded removal, qualifying modded tools, and invalid/orphaned saved data.
- Detection of extracted-item destruction without confusing chunk unload with deletion, or merely lost-but-existing items with destruction.
- Correct death/respawn, experience-drop, entity-removal, and cleanup sequencing while preserving `keepInventory` and irreversible item loss.
- Protection against stale/duplicated anchors reviving destroyed rooms.
- Reliable destroyed-room status synchronization and tooltip rendering, if feasible.
- Handling initial key registration versus any other non-entry block interactions; no management UI is implicitly approved.

## Not open anymore

- No ownership/invitation authorization for entry: a valid key is sufficient.
- Keys cannot be revoked, copies are unlimited, and destroyed-room keys remain copyable.
- Recipes are approved in [recipes](RECIPES.md).
- Charge is 3 seconds, successful key cooldown is 20 seconds, and early cancellation has no penalty.
- Inadequate tools make no breaking progress; iron-or-better pickaxes extract.
- Relocation preserves keys. An offline occupant resumes inside if the anchor is placed again before reconnect; otherwise they return to the latest extraction location. Destruction while offline takes precedence and returns them alive to personal respawn, then Overworld world spawn.
- One shared dimension, first-access generation, fixed floor, upward growth, and exterior-only return chaining are approved.
- The End is the environmental reference, not a requirement to generate End terrain.

Implementation still requires explicit approval under the [workflow](VERIFICATION.md).
