package com.iloelias.dimensionalanchors;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;

public record RoomGenerationState(AnchorTier tier, Optional<AnchorTier> previousTier, boolean complete) {
    public static final Codec<RoomGenerationState> CODEC = RecordCodecBuilder.<RoomGenerationState>create(instance -> instance.group(
            AnchorTier.CODEC.fieldOf("tier").forGetter(RoomGenerationState::tier),
            AnchorTier.CODEC.optionalFieldOf("previous_tier").forGetter(RoomGenerationState::previousTier),
            Codec.BOOL.fieldOf("complete").forGetter(RoomGenerationState::complete)
    ).apply(instance, RoomGenerationState::new)).validate(state ->
            (state.complete && state.previousTier.isPresent())
                    || state.previousTier.filter(previous -> previous.ordinal() >= state.tier.ordinal()).isPresent()
                    ? DataResult.error(() -> "Invalid room generation transition")
                    : DataResult.success(state));

    static boolean boundary(RoomGeometry geometry, BlockPos position) {
        return geometry.shell().isInside(position) && !geometry.interior().isInside(position);
    }

    boolean protects(int slot, BlockPos position) {
        return boundary(new RoomGeometry(slot, tier), position)
                || previousTier.filter(previous -> boundary(new RoomGeometry(slot, previous), position)).isPresent();
    }
}
