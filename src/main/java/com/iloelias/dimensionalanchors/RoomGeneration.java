package com.iloelias.dimensionalanchors;

import java.util.Optional;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

final class RoomGeneration {
    static final ResourceKey<Level> POCKET = ResourceKey.create(Registries.DIMENSION,
            Identifier.fromNamespaceAndPath(DimensionalAnchors.MODID, "pocket"));

    private RoomGeneration() {
    }

    static RoomGeometry prepare(ServerLevel source, UUID anchorId, AnchorTier tier) {
        ServerLevel pocket = source.getServer().getLevel(POCKET);
        if (pocket == null) {
            throw new IllegalStateException("Pocket dimension is unavailable");
        }
        var data = RoomAllocationSavedData.get(source);
        int slot = data.reserve(anchorId);
        var geometry = new RoomGeometry(slot, tier);
        var existing = data.room(anchorId).orElse(null);
        if (existing != null && (!existing.complete() && existing.tier() != tier
                || existing.tier().ordinal() > tier.ordinal())) {
            throw new IllegalStateException("Cannot downgrade or change an interrupted generation: " + anchorId);
        }
        if (existing != null && existing.complete() && existing.tier() == tier) {
            verifyBoundary(pocket, geometry);
            return geometry;
        }
        Optional<AnchorTier> previous = existing == null ? Optional.empty()
                : existing.complete() ? Optional.of(existing.tier()) : existing.previousTier();
        RoomGeometry old = previous.map(value -> new RoomGeometry(slot, value)).orElse(null);
        boolean resuming = existing != null && !existing.complete();
        if (old != null && !resuming) {
            verifyBoundary(pocket, old);
        }
        var shell = geometry.shell();
        // Preflight every changed coordinate before writing, preserving all existing interior contents.
        for (BlockPos position : BlockPos.betweenClosed(shell.minX(), shell.minY(), shell.minZ(),
                shell.maxX(), shell.maxY(), shell.maxZ())) {
            BlockState current = pocket.getBlockState(position);
            boolean oldBoundary = old != null && RoomGenerationState.boundary(old, position);
            boolean newBoundary = RoomGenerationState.boundary(geometry, position);
            if (old != null && old.interior().isInside(position)) {
                continue;
            }
            if (current.isAir() || oldBoundary && current.equals(boundaryState(old, position))
                    || resuming && newBoundary && current.equals(boundaryState(geometry, position))) {
                continue;
            }
            throw new IllegalStateException("Room generation would overwrite an unexpected block at " + position);
        }
        data.putRoom(anchorId, new RoomGenerationState(tier, previous, false));
        // Persist provenance before any chunk changes, so interrupted operations can be retried safely.
        try {
            source.getServer().overworld().getDataStorage().saveAndJoin();
        } catch (java.util.concurrent.CompletionException exception) {
            throw new IllegalStateException("Cannot persist room generation intent: " + anchorId, exception);
        }
        for (BlockPos position : BlockPos.betweenClosed(shell.minX(), shell.minY(), shell.minZ(),
                shell.maxX(), shell.maxY(), shell.maxZ())) {
            if (RoomGenerationState.boundary(geometry, position)) {
                setBlock(pocket, position, boundaryState(geometry, position));
            } else if (old != null && RoomGenerationState.boundary(old, position)) {
                setBlock(pocket, position, Blocks.AIR.defaultBlockState());
            }
        }
        verifyBoundary(pocket, geometry);
        data.putRoom(anchorId, new RoomGenerationState(tier, Optional.empty(), true));
        return geometry;
    }

    private static void verifyBoundary(ServerLevel level, RoomGeometry geometry) {
        var shell = geometry.shell();
        for (BlockPos position : BlockPos.betweenClosed(shell.minX(), shell.minY(), shell.minZ(),
                shell.maxX(), shell.maxY(), shell.maxZ())) {
            if (RoomGenerationState.boundary(geometry, position)
                    && !level.getBlockState(position).equals(boundaryState(geometry, position))) {
                throw new IllegalStateException("Generated room boundary is missing or modified at " + position);
            }
        }
    }

    private static BlockState boundaryState(RoomGeometry geometry, BlockPos position) {
        return (position.equals(geometry.exitControl())
                ? DimensionalAnchors.DIMENSIONAL_EXIT_BLOCK
                : DimensionalAnchors.DIMENSIONAL_LIMITER_BLOCK).get().defaultBlockState();
    }

    private static void setBlock(ServerLevel level, BlockPos position, BlockState state) {
        if (!level.getBlockState(position).equals(state)
                && !level.setBlock(position, state, Block.UPDATE_ALL)) {
            throw new IllegalStateException("Cannot write generated room block at " + position);
        }
    }
}
