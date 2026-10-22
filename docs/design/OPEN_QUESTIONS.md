# Remaining Decisions and Technical Planning

[Design index](../DESIGN_DECISIONS.md)

These are unresolved details, not permission to invent gameplay. Recipes, key timings, anvil costs, and the core lifecycle are already approved.

## Decisions requiring approval

- **Recovery after anchor loss:** how a room becomes available again after its anchor block/item is destroyed. Until recovery is defined, preserve the room and region but suspend new entry.
- **Allocation capacity:** practical maximum number of fixed-size rooms and behavior when the allocator reaches capacity.
- **Biome details:** exact custom-biome features/effects, while natural entity spawning remains disabled and skybox/lighting follow The End.
- **Presentation:** final appearance for internal blocks and whether existing anchor texture files are templates or finished art.

## Technical work to propose and verify

- Exact horizontal rounding, door geometry, landing coordinates, allocation spacing, and migration formulas.
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
- Destroyed-room region cleanup/reuse is disabled; anchor destruction preserves the room and reserved region.

Implementation still requires explicit approval under the [workflow](VERIFICATION.md).
