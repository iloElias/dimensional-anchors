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
- Keys to destroyed rooms can still be copied, but originals and copies remain unusable.
- Investigate the target-version anvil API: ordinary behavior must not be assumed to preserve the first input. Shift-click and repeated collection must consume exactly one blank key and the required XP per copy.

## Loss and appearance

- Use a previously created copy if a key is lost.
- If every key is lost, entry becomes unavailable. No keyless recovery, replacement issuance, or registration reset is provided.
- Losing keys does not delete the room or invoke destruction death. Occupants can still leave through the south-wall control block without a key.
- Lost or stolen keys cannot be revoked.
- Blank and registered keys have different sprites. Registration changes the appearance; copies use the registered sprite.
- If technically feasible, show `Linked room destroyed` for keys to confirmed destroyed rooms. An unloaded chunk or extracted anchor must never be mislabeled as destruction.
- Plan server-to-client status synchronization before claiming the tooltip is supported.

The [blank-key recipe](RECIPES.md) is approved. Whether states use distinct item registrations and whether copies need individual identifiers remain technical choices.
