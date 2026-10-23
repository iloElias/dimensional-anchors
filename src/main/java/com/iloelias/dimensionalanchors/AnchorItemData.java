package com.iloelias.dimensionalanchors;

import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.UUIDUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;

public record AnchorItemData(UUID anchorId, AnchorTier tier, boolean registered) {
    public static final Codec<AnchorItemData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.STRING_CODEC.fieldOf("anchor_id").forGetter(AnchorItemData::anchorId),
            AnchorTier.CODEC.fieldOf("tier").forGetter(AnchorItemData::tier),
            Codec.BOOL.fieldOf("registered").forGetter(AnchorItemData::registered)
    ).apply(instance, AnchorItemData::new));

    public static boolean hasValidTier(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem item) || !(item.getBlock() instanceof AnchorBlock block)) {
            return false;
        }
        AnchorItemData data = stack.get(DimensionalAnchors.ANCHOR_ITEM_DATA.get());
        return data == null || data.tier() == block.tier();
    }

    public static ItemStack transferTo(ItemStack source, ItemStack result) {
        if (!hasValidTier(source)
                || !(result.getItem() instanceof BlockItem item)
                || !(item.getBlock() instanceof AnchorBlock block)) {
            throw new IllegalArgumentException("Invalid anchor upgrade input or result");
        }
        AnchorItemData data = source.get(DimensionalAnchors.ANCHOR_ITEM_DATA.get());
        if (data != null) {
            result.set(DimensionalAnchors.ANCHOR_ITEM_DATA.get(),
                    new AnchorItemData(data.anchorId(), block.tier(), data.registered()));
        }
        return result;
    }
}
