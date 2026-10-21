# Anchor Blocks and Lifecycle

[Design index](../DESIGN_DECISIONS.md)

## Identity and activation

- An anchor is a block associated with one persistent room, not a portable key.
- Tiers are Copper, Iron, Gold, Diamond, and Netherite. Each has an inventory item form for extraction, placement, and upgrades.
- Independently created anchors must have distinct room identities, independent of player UUIDs.
- A newly crafted anchor is inactive. Any player may register its first blank key by interacting with it. Registration happens once per anchor identity.
- Registration does not generate the physical room; generation occurs on first access.
- Interaction with the anchor block never provides entry. Entry requires [key use](KEYS.md).
- Any player may register, mine, or destroy the anchor; those actions have no ownership or key-possession restriction.
- Room identity, registration, contents, spatial allocation, and existing keys survive legitimate upgrades and relocation. Downgrades are forbidden.

## Extraction and relocation

- An iron pickaxe or a higher-tier pickaxe extracts the anchor as an item, preserving its information and room.
- Inadequate tools, including bare hands, make no breaking progress.
- While extracted, the room is inaccessible, including through existing keys.
- All players inside are expelled under the [return policy](TRAVEL.md). Non-player entities remain inside.
- Placing the anchor again reopens the same room; existing registered keys remain valid. Registration is not reset.
- Relocation does not move or reallocate the room in the pocket dimension.
- Offline players' pending extraction returns are not canceled or retargeted by relocation.
- An explosion that leaves a surviving anchor-item drop follows the same preservation and expulsion rules. If the item is subsequently destroyed, permanent destruction applies.

## Permanent destruction

- Removing the anchor without a surviving item through an explosion permanently destroys its room, contents, and reserved space.
- Destruction of the extracted item, including lava, void loss, or dropped-item despawning, also destroys the room. Misplacing an item that still exists is not destruction.
- Connected occupants die with the exact message `The universe forgot <player>`, using the actual player's name.
- Disconnected occupants return alive to their personal respawn point on reconnect, falling back to Overworld world spawn if invalid.
- Respect `keepInventory`. When disabled, inventory drops disappear with the room; they are not relocated or recoverable.
- Living non-player entities die; other entities and resulting drops are removed with the room.
- Keys and their copies cannot restore destroyed rooms. Prevent stale or duplicated anchor items from reviving a destroyed identity.
- Delete only the affected room, never the shared dimension or neighboring rooms.

## Explosion resistance

Match the actual vanilla blast-resistance value in the target version:

| Anchor tier | Reference block |
| --- | --- |
| Copper | Copper Ore |
| Iron | Iron Ore |
| Gold | Gold Ore |
| Diamond | Diamond Ore |
| Netherite | Ancient Debris |

This specifies blast resistance only, not mining hardness or dropped-item immunity. Different reference ores may share a resistance value. Exact explosion-drop APIs require investigation.

## Lifecycle summary

| State/event | Room | Entry | Occupants |
| --- | --- | --- | --- |
| Unregistered | Not physically generated | Unavailable | None |
| Registered and placed | Generated on first access | Valid registered key | Normal behavior |
| All keys lost | Preserved | Unavailable; no replacement/reset | Keyless south-wall exit remains available |
| Extracted or surviving explosion drop | Preserved | Unavailable | Players expelled; other entities remain |
| Replaced | Same identity and contents | Existing keys remain valid | Pending offline expulsions still apply |
| Permanently destroyed | Irrecoverably deleted | Permanently unavailable | Online players die; offline players receive the nonlethal respawn exception |

Technical questions about removal hooks, persistence, and cleanup are tracked in [open questions](OPEN_QUESTIONS.md).
