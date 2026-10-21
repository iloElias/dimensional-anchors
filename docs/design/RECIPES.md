# Approved Recipes and Progression

[Design index](../DESIGN_DECISIONS.md)

All outputs below are exactly one item/block. Crafting-table recipes are shaped unless explicitly marked shapeless.

## Dimensional Catalyst

```text
[Copper Ingot]   [Amethyst Shard] [Copper Ingot]
[Amethyst Shard] [Ender Pearl]    [Amethyst Shard]
[Iron Ingot]     [Amethyst Shard] [Iron Ingot]
```

Consumes two Copper Ingots, two Iron Ingots, four Amethyst Shards, and one Ender Pearl.

Echo Shards are not part of the approved recipe, replacing the earlier candidate ingredient.

## Copper Dimensional Anchor

```text
[Copper Ingot] [Copper Ingot]         [Copper Ingot]
[Copper Ingot] [Dimensional Catalyst] [Copper Ingot]
[Copper Ingot] [Copper Ingot]         [Copper Ingot]
```

Consumes eight Copper Ingots and one Dimensional Catalyst. Produces an inactive, unregistered anchor; crafting does not activate or physically generate its room.

## Iron, Gold, and Diamond upgrades

```text
[Material] [Material]        [Material]
[Material] [Previous Anchor] [Material]
[Material] [Material]        [Material]
```

| Result | Center | Eight surrounding ingredients |
| --- | --- | --- |
| Iron Dimensional Anchor | Copper Dimensional Anchor | Iron Ingots |
| Gold Dimensional Anchor | Iron Dimensional Anchor | Gold Ingots |
| Diamond Dimensional Anchor | Gold Dimensional Anchor | Diamonds |

Consume the previous anchor and eight materials. No additional catalyst is required: this replaces the original seven-material-plus-catalyst upgrade proposal.

## Dimensional Upgrade Template

Shapeless crafting-table recipe:

```text
1 Netherite Upgrade Smithing Template + 1 Dimensional Catalyst
    -> 1 Dimensional Upgrade Template
```

Consumes both ingredients, including the vanilla template. No dedicated template-copying recipe is approved.

## Netherite Dimensional Anchor

Smithing-table recipe:

| Template slot | Base slot | Addition slot | Result |
| --- | --- | --- | --- |
| Dimensional Upgrade Template | Diamond Dimensional Anchor | Netherite Ingot | Netherite Dimensional Anchor |

Consumes one template and one ingot, transforming one Diamond anchor into one Netherite anchor. The template is not retained or returned.

## Blank dimensional key

```text
[Iron Ingot] [Dimensional Catalyst] [Iron Ingot]
[Empty]      [Iron Ingot]           [Empty]
[Empty]      [Iron Ingot]           [Iron Ingot]
```

Consumes five Iron Ingots and one Dimensional Catalyst; empty slots must remain empty. Produces one non-stackable blank key without a room association.

Registered keys come from initial registration or [anvil copying](KEYS.md), not a separate crafting-table recipe.

## Upgrade invariants

- Upgrade the anchor's item form in the crafting/smithing table, not the placed block.
- Preserve room identity, registration, spatial allocation, contents, and existing key validity.
- An unregistered anchor stays unregistered; a registered one retains the same room.
- Upgrades are permanent and do not duplicate rooms.
- Verify target-version custom recipe/data-transfer APIs; ordinary recipes must not be assumed to preserve room components automatically.
