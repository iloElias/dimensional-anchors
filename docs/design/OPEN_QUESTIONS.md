# Remaining Decisions and Technical Planning

[Design index](../DESIGN_DECISIONS.md)

These are unresolved details, not permission to invent gameplay. Recipes, key timings, anvil costs, and the core lifecycle are already approved.

## Decisions requiring approval

- **Recovery after anchor loss:** how a room becomes available again after its anchor block/item is destroyed. Until recovery is defined, preserve the room and region but suspend new entry.
- **Presentation:** final appearance for internal blocks and whether existing anchor texture files are templates or finished art.
- **Deferred key colors:** coloring blank and registered keys with any of the
  16 dyes in the crafting table is approved for future work. Before implementing,
  settle the recipe layout, recoloring rules, color retention during registration,
  and color inheritance when copying a registered key using a colored blank.

## Technical work to propose and verify

- Physical door behavior, safe landing validation, and migration formulas.
  The initial coordinate convention and allocation layout are recorded in
  [rooms](ROOMS.md#implemented-geometry-foundation) and
  [dimension](DIMENSION.md#implemented-reservation-foundation).
- Persistent identity, room lifecycle, region reservations, pending returns, and saved-data versioning.
- Key-copy validation against illicit item duplication; copies themselves are unlimited, but item duplication must not corrupt room state.
- Safe landing/return search bounds, unavailable dimensions, obstructed personal/world spawn, and bounded chunk loading.
- Custom crafting/smithing preservation and anvil input/XP/output handling in the exact target version.
- Vanilla item-use behavior when receiving damage; no new anti-combat-logout system is approved.
- Classification of commands, modded removal, qualifying modded tools, and invalid/orphaned saved data.
- Distinguishing an absent anchor from chunk unload or a dropped item that still exists.
- Reliable unavailable-room status synchronization and key tooltip rendering, if feasible.
- Handling initial key registration versus any other non-entry block interactions; no management UI is implicitly approved.
- An existing occupant's room session is preserved through anchor removal/recovery. If offline, reconnect within the same room; current anchor state controls only whether new entries are allowed.

## Not open anymore

- No ownership/invitation authorization for entry: a valid key is sufficient.
- Keys cannot be revoked, copies are unlimited, and keys for unavailable rooms remain copyable.
- Recipes are approved in [recipes](RECIPES.md).
- Charge is 3 seconds, successful key cooldown is 20 seconds, and early cancellation has no penalty.
- Inadequate tools make no breaking progress; iron-or-better pickaxes extract.
- Relocation preserves keys. Anchor collection/destruction does not eject or kill occupants, and does not delete the room.
- While the anchor is absent, existing occupants can remain/exit, but new entries are suspended until recovery/replacement.
- One shared dimension, first-access generation, fixed tier sizes, one south-wall control block, fixed floor, and upward growth are approved.
- The custom biome has no natural entity spawning; skybox and lighting follow The End.
- Initial environment uses the target version's End settings, including music,
  fixed clock, lighting, no precipitation, exploding beds, and nonfunctional
  respawn anchors. The custom biome has no terrain features, carvers, structures,
  or spawn entries; dragon-fight initialization is disabled.
- Destroyed-room region cleanup/reuse is disabled; anchor destruction preserves the room and reserved region.
- Allocation is capped at 4,096 permanent reservations per world. At capacity,
  reject new allocations with explicit feedback; preserve existing reservations
  and do not reuse regions. Existing rooms remain accessible subject to their
  normal lifecycle rules.

Implementation still requires explicit approval under the [workflow](VERIFICATION.md).
