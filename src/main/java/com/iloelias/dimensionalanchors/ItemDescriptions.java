package com.iloelias.dimensionalanchors;

import java.util.ArrayList;
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
        var lines = new ArrayList<Component>();
        UUID id = null;
        if (stack.getItem() instanceof BlockItem item && item.getBlock() instanceof AnchorBlock block) {
            int size = new RoomGeometry(0, block.tier()).interiorSize();
            lines.add(description("anchor_space", size, size, size));
            var data = stack.get(DimensionalAnchors.ANCHOR_ITEM_DATA.get());
            if (data != null) {
                id = data.anchorId();
            }
        } else if (stack.is(DimensionalAnchors.UNASSIGNED_DIMENSIONAL_KEY.get())) {
            lines.add(description("blank_key"));
        } else if (stack.is(DimensionalAnchors.DIMENSIONAL_KEY.get())) {
            var data = stack.get(DimensionalAnchors.REGISTERED_KEY_DATA.get());
            if (data == null) {
                lines.add(description("unlinked_key"));
            } else {
                lines.add(description("key_travel"));
                lines.add(description("key_return"));
                lines.add(description("entry_disabled"));
                id = data.anchorId();
            }
        }
        if (id != null && event.getFlags().hasShiftDown()) {
            lines.add(description("anchor_uuid", id.toString()));
        }
        event.getToolTip().addAll(Math.min(1, event.getToolTip().size()), lines);
    }

    private static Component description(String key, Object... arguments) {
        return Component.translatable("tooltip.dimensionalanchors." + key, arguments)
                .withStyle(ChatFormatting.GRAY);
    }
}
