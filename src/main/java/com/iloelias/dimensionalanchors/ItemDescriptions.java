package com.iloelias.dimensionalanchors;

import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

final class ItemDescriptions {
    private ItemDescriptions() {
    }

    static void append(ItemTooltipEvent event) {
        var stack = event.getItemStack();
        UUID id = null;
        if (stack.getItem() instanceof BlockItem item && item.getBlock() instanceof AnchorBlock) {
            var data = stack.get(DimensionalAnchors.ANCHOR_ITEM_DATA.get());
            if (data != null) {
                id = data.anchorId();
            }
        } else if (stack.is(DimensionalAnchors.DIMENSIONAL_KEY.get())) {
            var data = stack.get(DimensionalAnchors.REGISTERED_KEY_DATA.get());
            if (data != null) {
                id = data.anchorId();
            }
        }
        if (id != null) {
            event.getToolTip().add(Math.min(1, event.getToolTip().size()),
                    description("anchor_uuid", id.toString()));
        }
    }

    private static Component description(String key, Object... arguments) {
        return Component.translatable("tooltip.dimensionalanchors." + key, arguments)
                .withStyle(ChatFormatting.DARK_GRAY);
    }
}
