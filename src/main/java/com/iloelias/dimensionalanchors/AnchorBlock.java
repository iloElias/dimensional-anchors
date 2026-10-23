package com.iloelias.dimensionalanchors;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public final class AnchorBlock extends Block {
    private final AnchorTier tier;

    public AnchorBlock(AnchorTier tier, BlockBehaviour.Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public AnchorTier tier() {
        return tier;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.LivingEntity placer,
            ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        AnchorItemData itemData = stack.get(DimensionalAnchors.ANCHOR_ITEM_DATA.get());
        if (itemData == null) {
            itemData = new AnchorItemData(UUID.randomUUID(), tier, false);
        }
        if (itemData.tier() != tier) {
            DimensionalAnchors.LOGGER.error("Missing or invalid anchor data for {} at {}", tier.getSerializedName(), pos);
            return;
        }

        AnchorSavedData savedData = AnchorSavedData.get(serverLevel);
        AnchorRecord existing = savedData.get(itemData.anchorId()).orElse(null);
        if (existing != null && existing.placed()) {
            DimensionalAnchors.LOGGER.error("Anchor is already placed: {}", itemData.anchorId());
            return;
        }
        AnchorRecord record = existing == null
                ? AnchorRecord.unplaced(itemData.anchorId(), tier, itemData.registered())
                : existing.withTier(tier);
        savedData.put(record.placedAt(GlobalPos.of(serverLevel.dimension(), pos)));
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state,
            net.minecraft.world.level.block.entity.BlockEntity blockEntity, ItemStack tool) {
        if (level instanceof ServerLevel serverLevel) {
            AnchorSavedData savedData = AnchorSavedData.get(serverLevel);
            AnchorRecord record = savedData.findAt(GlobalPos.of(serverLevel.dimension(), pos)).orElse(null);
            if (record != null) {
                ItemStack drop = new ItemStack(DimensionalAnchors.anchorItem(record.tier()).get());
                drop.set(DimensionalAnchors.ANCHOR_ITEM_DATA.get(),
                        new AnchorItemData(record.anchorId(), record.tier(), record.registered()));
                popResource(level, pos, drop);
                savedData.put(record.removedFromWorld());
            } else {
                DimensionalAnchors.LOGGER.error("No saved anchor record found at {}", pos);
            }
        }
    }
}
