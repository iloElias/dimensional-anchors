package com.iloelias.dimensionalanchors;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;

public final class AnchorBlockItem extends BlockItem {
    public AnchorBlockItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        ItemStack stack = context.getItemInHand();
        if (!AnchorItemData.hasValidTier(stack)) {
            DimensionalAnchors.LOGGER.warn("Rejected anchor placement with invalid tier at {}", context.getClickedPos());
            return InteractionResult.FAIL;
        }
        AnchorItemData data = stack.get(DimensionalAnchors.ANCHOR_ITEM_DATA.get());
        if (data != null && context.getLevel() instanceof ServerLevel serverLevel
                && AnchorSavedData.get(serverLevel).get(data.anchorId()).filter(AnchorRecord::placed).isPresent()) {
            DimensionalAnchors.LOGGER.warn("Rejected duplicate placement of anchor {}", data.anchorId());
            return InteractionResult.FAIL;
        }
        return super.place(context);
    }
}
