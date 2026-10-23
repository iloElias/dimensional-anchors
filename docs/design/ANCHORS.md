# Anchor Blocks and Lifecycle

[Design index](../DESIGN_DECISIONS.md)

## Identity and activation

- An anchor is a block associated with one persistent room, not a portable key.
- Tiers are Copper, Iron, Gold, Diamond, and Netherite. Each has an inventory item form for extraction, placement, and upgrades.
- Independently created anchors must have distinct room identities, independent of player UUIDs.
- A newly crafted anchor is inactive. Any player may register its first blank key by interacting with it. Registration happens once per anchor identity.
- Registration does not generate the physical room; generation occurs on first access.
- Entry requires a valid registered key: charged remote [key use](KEYS.md), or
  interaction with its matching anchor while holding the key. Anchor-interaction
  entry was approved on 2026-10-23 and supersedes the earlier prohibition; both
  entry paths remain gated pending complete transition/recovery integration.
- Any player may register, mine, or destroy the anchor; those actions have no ownership or key-possession restriction.
- Room identity, registration, contents, spatial allocation, and existing keys survive legitimate upgrades and relocation. Downgrades are forbidden.

## Extraction and relocation

- An iron pickaxe or a higher-tier pickaxe extracts the anchor as an item, preserving its information and room.
- Inadequate tools, including bare hands, make no breaking progress.
- While the anchor is absent from the world, the room is preserved but new entry is suspended, including through existing keys.
- Collecting/extracting the anchor does not expel or teleport players already inside. They remain in the room and can leave normally.
- Non-player entities remain inside the preserved room.
- Placing the anchor again reopens the same room; existing registered keys remain valid. Registration is not reset.
- Relocation does not move or reallocate the room in the pocket dimension.
- If an occupant disconnects inside while the anchor is absent and the same room is recovered/reopened before they reconnect, they resume inside. If it is still unavailable, preserve their room session; they may reconnect there and leave, but may not newly enter.
- An explosion that leaves a surviving anchor-item drop makes the room unavailable to new entrants but does not expel current occupants. Destruction of the dropped item also preserves the room and region.

## Block destruction and room preservation

- Destroying the anchor block or later destroying/despawning its dropped item does not delete the room, its contents, or its reserved region.
- When the anchor block is absent because it was destroyed, suspend new entry through every registered key until a recovery process is defined and restores access.
- Players already inside remain there; they are not killed or teleported because the anchor block was destroyed. They can leave using normal room exit controls.
- Disconnected occupants are not killed or relocated by room preservation. They may resume their existing room session and leave, but cannot initiate a new entry while the room is unavailable.
- There is no destruction death message, room cleanup, or region reclamation for anchor-block destruction.
- Destruction of the room itself is out of scope under this simplified lifecycle. Do not implement cleanup that deletes its contents or makes the region available to another anchor.
- Administrative item recovery is defined below. Keys do not bypass the
  unavailable state; the recovered block must be placed before new entry.

## Administrative anchor recovery

- `/dimensionalanchors recover <uuid>` is the only mechanism for recovering a
  lost anchor block/item. It requires the target version's administrator command
  permission and a player executor; the console has no inventory recipient.
- Give exactly one anchor item of the highest authoritatively recorded tier,
  preserving the anchor/room UUID and registration in `dimensionalanchors:anchor_data`
  (persisted item components). Do not create a room, issue a key, reset
  registration, move an occupant, erase contents, or change a return destination.
- Resolve existing records across server dimensions. Refuse unknown UUIDs,
  conflicting/malformed records or unreadable authoritative data. Verify a
  recorded placed location by loading that chunk before treating it as absent;
  if an anchor block is still present there, refuse recovery.
- Require a free main-inventory slot. A full inventory fails without dropping an
  item or clearing a stale placed record. Successful recovery marks the identity
  unplaced; only subsequent placement restores normal key availability.
- Repeated recovery can leave spare items, since inventories/dropped items
  cannot be exhaustively searched. Placement rejects a UUID already placed in
  any dimension and rejects obsolete lower-tier items. Relocation consolidates
  the authoritative record in the new dimension without losing registration.
- Completed crafting/smithing upgrades now record their tier before placement;
  previews do not. Recovery cannot reconstruct an unrecorded historical upgrade
  lost before this tracking existed. It restores recorded metadata, not unknown
  item state or custom names.
- The command and cross-dimension placement safeguards are implemented; see
  [verification](VERIFICATION.md) and [TO_TEST](../TO_TEST.md) for actual evidence.
  This does not implement player-session recovery or enable gameplay entry.

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
| Anchor extracted or surviving explosion drop | Preserved | New entry suspended | Existing players and non-player entities remain; players can exit |
| Anchor replaced/recovered | Same identity and contents | Existing keys work again | Existing room sessions continue |
| Anchor block/item destroyed | Preserved; region not reclaimed | Unavailable until an administrator recovers the item and it is placed | Current and disconnected occupants are not killed or teleported; they can leave |

Technical questions about recovery, removal hooks, and persistence are tracked in [open questions](OPEN_QUESTIONS.md).
