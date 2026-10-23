# Dimensional Keys

[Design index](../DESIGN_DECISIONS.md)

## Registration and authorization

- A blank key becomes registered when used on an anchor that has never been registered.
- Another blank key cannot register to that anchor afterward, including after extraction and replacement.
- Registered keys reference the existing anchor/room; they do not contain a room backup or create another room.
- Possessing and using a valid key is sufficient for entry. There are no owner-only entry rules, invitation lists, or separate player authorization checks.
- Giving a registered key to another player grants access.
- The server validates the key reference and anchor lifecycle. Client-provided identity alone is insufficient.
- Blank and registered keys are non-stackable: maximum stack size one, even when copies link to the same room.
- Registered keys retain validity after legitimate upgrades and relocation.

## Use

- Hold the use button continuously for 3 seconds: 60 game ticks at the nominal 20 ticks per second.
- Teleport immediately when full charge is reached, not on release.
- Aborting early does nothing: no teleport, key/XP consumption, penalty, or cooldown.
- Successful key teleportation applies a 20-second cooldown: 400 game ticks at the nominal rate.
- Reuse the vanilla Ender Pearl cooldown system, with a shared registered-key cooldown group per player so switching copies cannot bypass it.
- Entry is remote; proximity to the anchor is unnecessary, but the anchor must be placed and activated.
- Remote access works with the anchor's chunk unloaded, without permanent chunk loading.
- A different room's key may be used inside a room. The current room's key instead triggers normal exit, with the same charge/cooldown behavior.
- Waystones crystals are a behavioral reference, not a dependency or a verified API choice.
- No additional anti-combat-logout system is planned. Verify ordinary damage/item-use behavior rather than introducing an unapproved custom interruption rule.

Destinations and keyless exit are specified in [travel](TRAVEL.md).

## Anvil copying

Combine a registered key in the first input with a blank key in the second input:

| Slot | Before collecting the result | After collecting the result |
| --- | --- | --- |
| First input | Registered key | Same original key, unchanged |
| Second input | One blank key | Empty |
| Output | One registered-key copy | Empty |

- The player obtains a second, separate registered key. The original remains retrievable from the first input.
- Without another blank key, the original alone produces no further result.
- Each completed copy costs exactly one experience level, not one experience point. Previewing costs nothing.
- The anvil suffers no wear or damage from copying. Repeated copies do not increase the fixed cost.
- Copies can generate further copies. No copy limit or additional copying permission applies.
- Copying does not create, duplicate, upgrade, or activate a room.
- Keys to rooms whose anchor is absent can still be copied, but originals and copies cannot bypass suspended entry.
- Investigate the target-version anvil API: ordinary behavior must not be assumed to preserve the first input. Shift-click and repeated collection must consume exactly one blank key and the required XP per copy.

## Loss and appearance

- Use a previously created copy if a key is lost.
- If every key is lost, entry becomes unavailable. No keyless recovery, replacement issuance, or registration reset is provided.
- Losing keys does not delete the room or invoke destruction death. Occupants can still leave through the south-wall control block without a key.
- Lost or stolen keys cannot be revoked.
- Blank and registered keys have different sprites. Registration changes the appearance; copies use the registered sprite.
- If technically feasible, show `Anchor unavailable` for keys whose anchor block is absent. An unloaded chunk must never be mislabeled as anchor loss.
- Plan server-to-client status synchronization before claiming the tooltip is supported.

The [blank-key recipe](RECIPES.md) is approved. Whether states use distinct item registrations and whether copies need individual identifiers remain technical choices.

## Planned handle colors (deferred)

Approved for future work; not implemented in the current checkpoint.

- Combine one dimensional key with one dye in the crafting table to produce one
  key with a handle matching that dye's color. Support all 16 vanilla dye colors.
- Both blank and registered keys can be colored.
- Preserve the visual distinction between blank and registered sprites for each
  color: two visual variants per color, not necessarily separate item types or
  individual texture files.
- Coloring must preserve registration state, the linked anchor UUID when present,
  and non-color item data. It does not register, copy, or grant additional access.
- Implementation and artwork are deferred. Recipe layout, recoloring an already
  colored key, color retention during registration, and which input supplies the
  color during anvil copying remain undecided.

## Current implementation checkpoint

Blank and registered states use separate item registrations. Right-click a placed
anchor with a blank key in either hand to register it once. The server replaces
the held item with one registered key, including in creative, and persists the
anchor's registration flag. Further blank-key attempts leave the key unchanged.
The key stores `dimensionalanchors:registered_key`, containing the anchor UUID;
the extracted anchor's `dimensionalanchors:anchor_data` retains `registered=true`.

This checkpoint implements identity registration, anvil copying, and a charging
prototype and physical room preparation. It does not teleport the player. At charge
completion it checks that the linked anchor has a registered, placed record;
when its chunk is already loaded, it also verifies the block and tier. It does
not load an unloaded anchor chunk for this check. An available anchor now triggers
room generation/expansion, temporarily approved before teleportation exists.
An unlinked registered-key item from the creative preview is not a valid key.
Successful registration plays the vanilla Vault insert-item sound
(`minecraft:block.vault.insert_item`), selected by the owner after auditioning it.
Registered keys with a UUID component have glint; blanks and unlinked preview
keys do not. The owner confirmed glint and success-only playback with the
previous sounds; the Vault sound was approved through an in-game command preview.
See [progress](../PROGRESS.md) for test evidence and pending client validation.

For copying, place a linked registered key in the left anvil input and one blank
key in the right. Collecting one copy leaves the original unchanged and consumes
the blank. Survival costs one level; creative retains vanilla's XP exemption but
still consumes the blank. No anvil wear is applied. The copy preserves all item
components, including its name and UUID; copying does not combine with renaming.
Normal anvil operations outside this input pair remain vanilla.

Copying only reproduces the key reference, without reading or modifying anchor
records, generating rooms, or forcing chunks to load. It works while the anchor
is absent. An unlinked preview key cannot be copied.

### Charging prototype

- A linked registered key uses the vanilla bow-use pose while the interaction
  button is held. Its existing sprite is unchanged; no new artwork is needed.
- Beginning use broadcasts `minecraft:block.portal.trigger`, the sound played
  when a player touches a Nether portal, once from the server at the player.
- At 60 ticks of uninterrupted use, the server checks whether the linked anchor
  is registered and placed in a loaded dimension. If its chunk is loaded, the
  expected anchor block and tier must also match; an unloaded chunk is not
  forced to load and relies on the persistent placed record. The
  server then prepares or expands the room. The completion message reports room
  preparation, but does not imply room access or teleportation is implemented.
  Successful preparation plays
  `minecraft:item.chorus_fruit.teleport`; an unavailable anchor plays
  `minecraft:block.beacon.deactivate`. Generation failures use the same failure
  sound, separate translated feedback, and a detailed server error log.
  When teleportation is implemented, only
  play the teleport sound after the teleport succeeds. Completion starts a
  shared 400-tick (20-second) registered-key cooldown per player using vanilla
  item cooldowns. Copies and keys linked to other anchors cannot bypass it.
- Releasing before completion cancels without a completion message, resource
  consumption, or cooldown and restores the idle item pose. The already-started
  portal sound is a one-shot, not a looping sound that is explicitly stopped.
- Completion consumes nothing. At the owner's request, placeholder completion
  now applies the cooldown so it can be tested before travel exists. Once
  actual travel is implemented, apply it only after successful teleportation.
- Unlinked creative-preview keys reject use with an explicit message. Blank
  keys do not charge. Both hands and survival/creative are supported.
- This is a preparation prototype, not travel authorization. It allocates a
  permanent region and generates/expands the room, loading destination chunks
  only as needed; it does not force-load exterior anchor chunks or teleport.
  Repeated access verifies completed boundaries without clearing contents.
  Travel must still revalidate lifecycle state, destination safety, and session
  returns immediately before entering.
- A blank key used on an already-registered anchor is rejected with
  `minecraft:block.vault.reject_rewarded_player`; successful registration keeps
  `minecraft:block.vault.insert_item`.
