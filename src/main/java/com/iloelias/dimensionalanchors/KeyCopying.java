package com.iloelias.dimensionalanchors;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LevelEvent;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.entity.player.AnvilCraftEvent;

public final class KeyCopying {
    private KeyCopying() {
    }

    public static void update(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();
        if (!isKeyPair(left, right)) {
            return;
        }
        if (!canCopy(left, right)) {
            event.setCanceled(true);
            return;
        }
        event.setOutput(left.copyWithCount(1));
        event.setXpCost(1);
        event.setMaterialCost(1);
    }

    public static void take(AnvilCraftEvent.Pre event) {
        if (!canCopy(event.getLeft(), event.getRight())) {
            return;
        }
        // Vanilla would consume the original and randomly damage the anvil.
        event.setCanceled(true);
        if (!event.getEntity().hasInfiniteMaterials()) {
            event.getEntity().giveExperienceLevels(-1);
        }
        event.getMenu().getSlot(1).set(ItemStack.EMPTY);
        event.getMenu().createResult();
        event.getMenu().broadcastChanges();
        event.getMenu().access.execute((level, pos) -> {
            if (!level.isClientSide()) {
                level.levelEvent(LevelEvent.SOUND_ANVIL_USED, pos, 0);
            }
        });
    }

    private static boolean isKeyPair(ItemStack left, ItemStack right) {
        return left.getItem() == DimensionalAnchors.DIMENSIONAL_KEY.get()
                && right.getItem() == DimensionalAnchors.UNASSIGNED_DIMENSIONAL_KEY.get();
    }

    private static boolean canCopy(ItemStack left, ItemStack right) {
        return isKeyPair(left, right)
                && left.getCount() == 1 && right.getCount() == 1
                && left.has(DimensionalAnchors.REGISTERED_KEY_DATA.get())
                && !right.has(DimensionalAnchors.REGISTERED_KEY_DATA.get());
    }
}
