package com.iloelias.dimensionalanchors;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
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
        if (context.getLevel().dimension().equals(RoomGeneration.POCKET)) {
            if (!context.getLevel().isClientSide() && context.getPlayer() != null) {
                context.getPlayer().sendOverlayMessage(Component.translatable(
                        "message.dimensionalanchors.anchor_in_pocket"));
            }
            return InteractionResult.FAIL;
        }
        AnchorItemData data = stack.get(DimensionalAnchors.ANCHOR_ITEM_DATA.get());
        if (data != null && context.getLevel() instanceof ServerLevel serverLevel) {
            try {
                var existing = AnchorIdentity.find(serverLevel, data.anchorId()).orElse(null);
                if (existing != null && (existing.placed() || existing.tier().ordinal() > data.tier().ordinal())) {
                    DimensionalAnchors.LOGGER.warn("Rejected duplicate or downgraded placement of anchor {}", data.anchorId());
                    notifyPlacementFailure(context);
                    return InteractionResult.FAIL;
                }
            } catch (IllegalStateException exception) {
                DimensionalAnchors.LOGGER.error("Cannot validate anchor placement {}", data.anchorId(), exception);
                notifyPlacementFailure(context);
                return InteractionResult.FAIL;
            }
        }
        return super.place(context);
    }

    @Override
    public void onCraftedPostProcess(ItemStack stack, Level level) {
        super.onCraftedPostProcess(stack, level);
        var data = stack.get(DimensionalAnchors.ANCHOR_ITEM_DATA.get());
        if (!(level instanceof ServerLevel serverLevel) || data == null) {
            return;
        }
        try {
            if (!AnchorItemData.hasValidTier(stack)) {
                throw new IllegalStateException("Crafted anchor has inconsistent tier: " + data.anchorId());
            }
            var existing = AnchorIdentity.find(serverLevel, data.anchorId())
                    .orElseThrow(() -> new IllegalStateException("Crafted anchor identity is missing: " + data.anchorId()));
            if (existing.placed()) {
                throw new IllegalStateException("Crafted anchor identity is still placed: " + data.anchorId());
            }
            if (data.tier().ordinal() > existing.tier().ordinal()) {
                AnchorIdentity.store(serverLevel, existing.withTier(data.tier()));
            }
            if (existing.registered()) {
                RoomGeneration.tryExpandExisting(serverLevel, data.anchorId(), data.tier(), "crafting");
            }
        } catch (IllegalStateException exception) {
            DimensionalAnchors.LOGGER.error("Cannot record crafted anchor tier {}", data.anchorId(), exception);
        }
    }

    private static void notifyPlacementFailure(BlockPlaceContext context) {
        if (context.getPlayer() != null) {
            context.getPlayer().sendOverlayMessage(Component.translatable(
                    "message.dimensionalanchors.anchor_placement_error"));
        }
    }
}
