package com.iloelias.dimensionalanchors;

import java.util.Optional;
import java.util.UUID;

import com.mojang.serialization.JsonOps;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

final class RoomGenerationTests {
    static void generation(GameTestHelper helper) {
        ServerLevel pocket = helper.getLevel().getServer().getLevel(RoomGeneration.POCKET);
        helper.assertTrue(pocket != null, "Pocket dimension is missing");
        var data = RoomAllocationSavedData.get(pocket);
        for (AnchorTier tier : AnchorTier.values()) {
            UUID id = UUID.randomUUID();
            RoomGeometry geometry = RoomGeneration.prepare(pocket, id, tier);
            assertRoom(helper, pocket, geometry);
            var interior = geometry.interior();
            for (BlockPos position : BlockPos.betweenClosed(interior.minX(), interior.minY(), interior.minZ(),
                    interior.maxX(), interior.maxY(), interior.maxZ())) {
                helper.assertTrue(pocket.getBlockState(position).isAir(), "New room interior is not empty");
            }
            helper.assertTrue(data.room(id).orElseThrow().complete(), "Generation was not marked complete");
            int reservations = data.reservations().size();
            helper.assertTrue(RoomGeneration.prepare(pocket, id, tier).equals(geometry)
                    && data.reservations().size() == reservations, "Repeated generation changed allocation");
        }

        UUID id = UUID.randomUUID();
        RoomGeometry first = RoomGeneration.prepare(pocket, id, AnchorTier.COPPER);
        BlockPos chestPos = first.reference();
        pocket.setBlockAndUpdate(chestPos, Blocks.CHEST.defaultBlockState());
        var chest = (ChestBlockEntity) pocket.getBlockEntity(chestPos);
        helper.assertTrue(chest != null, "Test chest was not created");
        chest.setItem(0, Items.DIAMOND.getDefaultInstance());
        BlockPos placedLimiter = chestPos.above();
        pocket.setBlockAndUpdate(placedLimiter, DimensionalAnchors.DIMENSIONAL_LIMITER_BLOCK.get().defaultBlockState());
        for (AnchorTier tier : AnchorTier.values()) {
            var geometry = RoomGeneration.prepare(pocket, id, tier);
            helper.assertTrue(pocket.getBlockEntity(chestPos) == chest && chest.getItem(0).is(Items.DIAMOND),
                    "Repeated access or expansion lost block entity contents");
            helper.assertTrue(pocket.getBlockState(placedLimiter)
                    .is(DimensionalAnchors.DIMENSIONAL_LIMITER_BLOCK.get()),
                    "Expansion removed a player-placed interior limiter");
            helper.assertTrue(!data.protects(chestPos) && !data.protects(placedLimiter),
                    "Interior construction was incorrectly protected");
            assertRoom(helper, pocket, geometry);
            if (tier != AnchorTier.COPPER) {
                helper.assertTrue(pocket.getBlockState(first.exitControl()).isAir(),
                        "Old south-wall control was not removed on expansion");
            }
        }
        expectFailure(helper, () -> RoomGeneration.prepare(pocket, id, AnchorTier.COPPER),
                "Room downgrade was accepted");
        helper.assertTrue(pocket.getBlockEntity(chestPos) == chest, "Rejected downgrade changed contents");
    }

    static void recovery(GameTestHelper helper) {
        var pocket = helper.getLevel().getServer().getLevel(RoomGeneration.POCKET);
        var data = RoomAllocationSavedData.get(pocket);
        UUID id = UUID.randomUUID();
        var geometry = new RoomGeometry(data.reserve(id), AnchorTier.COPPER);
        BlockPos conflict = geometry.exitControl();
        pocket.setBlockAndUpdate(conflict, Blocks.STONE.defaultBlockState());
        expectFailure(helper, () -> RoomGeneration.prepare(pocket, id, AnchorTier.COPPER),
                "First generation overwrote an existing block");
        helper.assertTrue(pocket.getBlockState(conflict).is(Blocks.STONE) && data.room(id).isEmpty(),
                "Failed preflight changed blocks or generation state");
        pocket.setBlockAndUpdate(conflict, Blocks.AIR.defaultBlockState());
        data.putRoom(id, new RoomGenerationState(AnchorTier.COPPER, Optional.empty(), false));
        BlockPos partiallyWritten = new BlockPos(geometry.shell().minX(), geometry.shell().minY(),
                geometry.shell().minZ());
        pocket.setBlockAndUpdate(partiallyWritten,
                DimensionalAnchors.DIMENSIONAL_LIMITER_BLOCK.get().defaultBlockState());
        RoomGeneration.prepare(pocket, id, AnchorTier.COPPER);
        assertRoom(helper, pocket, geometry);

        var expanded = new RoomGeometry(geometry.slot(), AnchorTier.IRON);
        BlockPos obstructed = expanded.exitControl();
        pocket.setBlockAndUpdate(obstructed, Blocks.CHEST.defaultBlockState());
        expectFailure(helper, () -> RoomGeneration.prepare(pocket, id, AnchorTier.IRON),
                "Expansion overwrote an obstruction");
        helper.assertTrue(data.room(id).orElseThrow().tier() == AnchorTier.COPPER
                && pocket.getBlockState(conflict).is(DimensionalAnchors.DIMENSIONAL_EXIT_BLOCK.get()),
                "Rejected expansion changed the existing boundary");
        pocket.setBlockAndUpdate(obstructed, Blocks.AIR.defaultBlockState());
        data.putRoom(id, new RoomGenerationState(AnchorTier.IRON, Optional.of(AnchorTier.COPPER), false));
        pocket.setBlockAndUpdate(conflict, Blocks.AIR.defaultBlockState());
        expectFailure(helper, () -> RoomGeneration.prepare(pocket, id, AnchorTier.GOLD),
                "Interrupted generation accepted a different target tier");
        RoomGeneration.prepare(pocket, id, AnchorTier.IRON);
        assertRoom(helper, pocket, expanded);
        helper.assertTrue(pocket.getBlockState(conflict).isAir(), "Interrupted expansion left the old control");

        pocket.setBlockAndUpdate(expanded.exitControl(), Blocks.AIR.defaultBlockState());
        expectFailure(helper, () -> RoomGeneration.prepare(pocket, id, AnchorTier.IRON),
                "Damaged completed boundary silently regenerated");
        helper.assertTrue(pocket.getBlockState(expanded.exitControl()).isAir(),
                "Damaged boundary was rewritten without approval");
        pocket.setBlockAndUpdate(expanded.exitControl(),
                DimensionalAnchors.DIMENSIONAL_EXIT_BLOCK.get().defaultBlockState());
    }

    static void placementLimits(GameTestHelper helper) {
        var pocket = helper.getLevel().getServer().getLevel(RoomGeneration.POCKET);
        var id = UUID.randomUUID();
        var geometry = RoomGeneration.prepare(pocket, id, AnchorTier.COPPER);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var inside = geometry.reference();
        var outside = inside.above(7);
        var insideEvent = placementEvent(pocket, inside, player);
        NeoForge.EVENT_BUS.post(insideEvent);
        helper.assertTrue(!insideEvent.isCanceled(), "Placement inside a generated room was canceled");

        var outsideEvent = placementEvent(pocket, outside, player);
        NeoForge.EVENT_BUS.post(outsideEvent);
        helper.assertTrue(outsideEvent.isCanceled() && pocket.getBlockState(outside).isAir(),
                "Building outside the existing room was allowed");

        var multiEvent = new BlockEvent.EntityMultiPlaceEvent(java.util.List.of(
                BlockSnapshot.create(pocket.dimension(), pocket, inside),
                BlockSnapshot.create(pocket.dimension(), pocket, outside)),
                pocket.getBlockState(inside.below()), player);
        NeoForge.EVENT_BUS.post(multiEvent);
        helper.assertTrue(multiEvent.isCanceled(),
                "Multi-block placement crossing the room boundary was allowed");

        var goldBoundary = inside.above(7);
        helper.assertTrue(!RoomGeneration.prepare(pocket, id, AnchorTier.GOLD).equals(geometry)
                && pocket.getBlockState(goldBoundary).is(DimensionalAnchors.DIMENSIONAL_LIMITER_BLOCK.get()),
                "A blocked outside placement still prevented the later room upgrade");

        var allocations = RoomAllocationSavedData.get(pocket);
        allocations.putRoom(id, new RoomGenerationState(AnchorTier.DIAMOND,
                Optional.of(AnchorTier.GOLD), false));
        helper.assertTrue(allocations.allowsPlacement(inside) && !allocations.allowsPlacement(goldBoundary)
                && !allocations.allowsPlacement(new BlockPos(5_000, 64, 5_000)),
                "Placement was allowed outside the prior completed interior or without a room allocation");
        allocations.putRoom(id, new RoomGenerationState(AnchorTier.GOLD, Optional.empty(), true));
    }

    static void protection(GameTestHelper helper) {
        var pocket = helper.getLevel().getServer().getLevel(RoomGeneration.POCKET);
        var geometry = RoomGeneration.prepare(pocket, UUID.randomUUID(), AnchorTier.COPPER);
        var shell = geometry.shell();
        var reference = geometry.reference();
        var positions = java.util.List.of(
                new BlockPos(shell.minX(), reference.getY(), reference.getZ()),
                new BlockPos(shell.minX() + 1, reference.getY(), reference.getZ()),
                new BlockPos(shell.maxX(), reference.getY(), reference.getZ()),
                new BlockPos(shell.maxX() - 1, reference.getY(), reference.getZ()),
                new BlockPos(reference.getX(), shell.minY(), reference.getZ()),
                new BlockPos(reference.getX(), shell.minY() + 1, reference.getZ()),
                new BlockPos(reference.getX(), shell.maxY(), reference.getZ()),
                new BlockPos(reference.getX(), shell.maxY() - 1, reference.getZ()),
                new BlockPos(reference.getX(), reference.getY(), shell.minZ()),
                new BlockPos(reference.getX(), reference.getY(), shell.minZ() + 1),
                new BlockPos(reference.getX(), reference.getY(), shell.maxZ()),
                new BlockPos(reference.getX(), reference.getY(), shell.maxZ() - 1),
                geometry.exitControl());
        for (GameType mode : new GameType[]{GameType.SURVIVAL, GameType.CREATIVE}) {
            var player = helper.makeMockPlayer(mode);
            for (BlockPos position : positions) {
                var state = pocket.getBlockState(position);
                var event = new BreakBlockEvent(pocket, position, state, player);
                NeoForge.EVENT_BUS.post(event);
                helper.assertTrue(event.isCanceled() && event.shouldNotifyClient()
                        && pocket.getBlockState(position).equals(state),
                        "Generated boundary break was not canceled and resynchronized");
                helper.assertTrue(net.minecraft.world.level.block.Block.getDrops(state, pocket, position, null).isEmpty(),
                        "Generated boundary has item drops");
            }
            for (var block : java.util.List.of(DimensionalAnchors.DIMENSIONAL_LIMITER_BLOCK.get(),
                    DimensionalAnchors.DIMENSIONAL_EXIT_BLOCK.get())) {
                for (BlockPos position : java.util.List.of(reference, reference.offset(8, 0, 0))) {
                    pocket.setBlockAndUpdate(position, block.defaultBlockState());
                    var event = new BreakBlockEvent(pocket, position, pocket.getBlockState(position), player);
                    NeoForge.EVENT_BUS.post(event);
                    helper.assertTrue(!event.isCanceled(), "Player-placed block received generated protection");
                    pocket.setBlockAndUpdate(position, Blocks.AIR.defaultBlockState());
                }
                var outside = helper.absolutePos(new BlockPos(1, 2, 1));
                var event = new BreakBlockEvent(helper.getLevel(), outside, block.defaultBlockState(), player);
                NeoForge.EVENT_BUS.post(event);
                helper.assertTrue(!event.isCanceled(), "Protection leaked outside the pocket dimension");
            }
        }
        var data = RoomAllocationSavedData.get(pocket);
        var restored = RoomAllocationSavedData.CODEC.parse(JsonOps.INSTANCE,
                RoomAllocationSavedData.CODEC.encodeStart(JsonOps.INSTANCE, data).getOrThrow()).getOrThrow();
        helper.assertTrue(positions.stream().allMatch(restored::protects) && !restored.protects(reference),
                "Reload lost generated provenance or protected the interior");
    }

    private static void assertRoom(GameTestHelper helper, ServerLevel pocket, RoomGeometry geometry) {
        int controls = 0;
        var shell = geometry.shell();
        var data = RoomAllocationSavedData.get(pocket);
        for (BlockPos position : BlockPos.betweenClosed(shell.minX(), shell.minY(), shell.minZ(),
                shell.maxX(), shell.maxY(), shell.maxZ())) {
            boolean boundary = RoomGenerationState.boundary(geometry, position);
            helper.assertTrue(data.protects(position) == boundary, "Incorrect generated boundary provenance");
            if (boundary) {
                var block = position.equals(geometry.exitControl())
                        ? DimensionalAnchors.DIMENSIONAL_EXIT_BLOCK.get()
                        : DimensionalAnchors.DIMENSIONAL_LIMITER_BLOCK.get();
                helper.assertTrue(pocket.getBlockState(position).is(block), "Missing shell block or wrong material");
            }
            if (pocket.getBlockState(position).is(DimensionalAnchors.DIMENSIONAL_EXIT_BLOCK.get())) {
                controls++;
            }
        }
        helper.assertTrue(controls == 1, "Room must contain exactly one generated south-wall control");
    }

    private static BlockEvent.EntityPlaceEvent placementEvent(ServerLevel level, BlockPos position, Player player) {
        return new BlockEvent.EntityPlaceEvent(
                BlockSnapshot.create(level.dimension(), level, position),
                level.getBlockState(position.below()), player);
    }

    private static void expectFailure(GameTestHelper helper, Runnable action, String message) {
        boolean rejected = false;
        try {
            action.run();
        } catch (IllegalStateException expected) {
            rejected = true;
        }
        helper.assertTrue(rejected, message);
    }
}
