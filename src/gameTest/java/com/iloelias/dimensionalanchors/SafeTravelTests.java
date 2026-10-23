package com.iloelias.dimensionalanchors;

import java.util.UUID;

import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.Vec3;

final class SafeTravelTests {
    static void entry(GameTestHelper helper) {
        var pocket = helper.getLevel().getServer().getLevel(RoomGeneration.POCKET);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        for (var tier : AnchorTier.values()) {
            var geometry = RoomGeneration.prepare(pocket, UUID.randomUUID(), tier);
            var destination = SafeTravelDestination.entry(pocket, player, geometry);
            helper.assertTrue(destination.position().equals(Vec3.atBottomCenterOf(geometry.entryFeet()))
                    && destination.yaw() == 180 && destination.pitch() == 0
                    && destination.source() == SafeTravelDestination.Source.ROOM,
                    "Entry did not prefer the default north-facing position");
            var blocked = geometry.entryFeet();
            pocket.setBlockAndUpdate(blocked.above(), Blocks.STONE.defaultBlockState());
            var alternate = SafeTravelDestination.entry(pocket, player, geometry);
            helper.assertTrue(!alternate.position().equals(destination.position())
                    && geometry.interior().isInside(BlockPos.containing(alternate.position())),
                    "Blocked entry did not find another interior position");
            var bounds = geometry.interior();
            for (var pos : BlockPos.betweenClosed(bounds.minX(), bounds.minY(), bounds.minZ(),
                    bounds.maxX(), bounds.maxY(), bounds.maxZ())) {
                pocket.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
            }
            expectFailure(helper, () -> SafeTravelDestination.entry(pocket, player, geometry));
            helper.assertTrue(pocket.getBlockState(blocked).is(Blocks.STONE),
                    "Failed entry destroyed a block");
        }
    }

    static void nearby(GameTestHelper helper) {
        var pocket = helper.getLevel().getServer().getLevel(RoomGeneration.POCKET);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var origin = new BlockPos(-2048, 100, -2048);
        clear(pocket, origin, 6);
        var preferred = new Vec3(origin.getX() + 0.35, origin.getY(), origin.getZ() + 0.65);
        pocket.setBlockAndUpdate(origin.below(), Blocks.STONE.defaultBlockState());
        helper.assertTrue(SafeTravelDestination.nearby(pocket, player, preferred).orElseThrow().equals(preferred),
                "Safe exact return coordinates were rounded");
        pocket.setBlockAndUpdate(origin.below(), Blocks.STONE_SLAB.defaultBlockState()
                .setValue(SlabBlock.TYPE, SlabType.BOTTOM));
        helper.assertTrue(SafeTravelDestination.nearby(pocket, player, preferred).orElseThrow()
                .equals(new Vec3(origin.getX() + 0.5, origin.getY() - 0.5, origin.getZ() + 0.5)),
                "Partial-height support was not resolved with vanilla floor geometry");
        for (var hazard : new net.minecraft.world.level.block.Block[]{Blocks.MAGMA_BLOCK, Blocks.CACTUS,
                Blocks.CAMPFIRE, Blocks.POWDER_SNOW, Blocks.LAVA, Blocks.WATER}) {
            pocket.setBlockAndUpdate(origin.below(), hazard.defaultBlockState());
            helper.assertTrue(SafeTravelDestination.nearby(pocket, player, preferred).isEmpty(),
                    "Unsafe floor accepted: " + hazard);
        }
        pocket.setBlockAndUpdate(origin.below(), Blocks.STONE.defaultBlockState());
        pocket.setBlockAndUpdate(origin, Blocks.STONE.defaultBlockState());
        helper.assertTrue(SafeTravelDestination.nearby(pocket, player, preferred).orElseThrow()
                .equals(Vec3.atBottomCenterOf(origin.above())),
                "Solid obstruction did not resolve to the safe supported position above it");
        for (var obstruction : new net.minecraft.world.level.block.Block[]{Blocks.FIRE,
                Blocks.WATER, Blocks.LAVA, Blocks.NETHER_PORTAL, Blocks.SWEET_BERRY_BUSH}) {
            pocket.setBlockAndUpdate(origin, obstruction.defaultBlockState());
            helper.assertTrue(SafeTravelDestination.nearby(pocket, player, preferred).isEmpty(),
                    "Obstructed or harmful standing space accepted: " + obstruction);
        }
        pocket.setBlockAndUpdate(origin, Blocks.AIR.defaultBlockState());
        pocket.setBlockAndUpdate(origin.below(), Blocks.AIR.defaultBlockState());
        var atLimit = origin.offset(4, 4, 4);
        pocket.setBlockAndUpdate(atLimit.below(), Blocks.STONE.defaultBlockState());
        helper.assertTrue(SafeTravelDestination.nearby(pocket, player, preferred).orElseThrow()
                .equals(Vec3.atBottomCenterOf(atLimit)), "Search did not include approved radius endpoints");
        pocket.setBlockAndUpdate(atLimit.below(), Blocks.AIR.defaultBlockState());
        pocket.setBlockAndUpdate(origin.offset(5, -1, 0), Blocks.STONE.defaultBlockState());
        helper.assertTrue(SafeTravelDestination.nearby(pocket, player, preferred).isEmpty(),
                "Search exceeded approved horizontal radius");
        pocket.setBlockAndUpdate(origin.offset(5, -1, 0), Blocks.AIR.defaultBlockState());
        pocket.setBlockAndUpdate(origin.offset(0, 4, 0), Blocks.STONE.defaultBlockState());
        helper.assertTrue(SafeTravelDestination.nearby(pocket, player, preferred).isEmpty(),
                "Search exceeded approved vertical radius or accepted unsupported feet");
        pocket.setBlockAndUpdate(origin.offset(0, 4, 0), Blocks.AIR.defaultBlockState());
        pocket.setBlockAndUpdate(origin.below(), Blocks.STONE.defaultBlockState());
        var border = pocket.getWorldBorder();
        double oldSize = border.getSize();
        double oldX = border.getCenterX();
        double oldZ = border.getCenterZ();
        try {
            border.setCenter(origin.getX() + 100, origin.getZ() + 100);
            border.setSize(16);
            helper.assertTrue(SafeTravelDestination.nearby(pocket, player, preferred).isEmpty(),
                    "Outside-world-border destination was accepted");
        } finally {
            border.setCenter(oldX, oldZ);
            border.setSize(oldSize);
        }
    }

    static void returns(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var level = server.overworld();
        var player = new ServerPlayer(server, level, new GameProfile(UUID.randomUUID(), "safe-return-test"),
                ClientInformation.createDefault());
        var savedPos = new BlockPos(-4096, 100, -4096);
        var bedPos = savedPos.offset(32, 0, 0);
        var spawnPos = savedPos.offset(64, 0, 0);
        clear(level, savedPos, 6);
        clear(level, bedPos, 6);
        clear(level, spawnPos, 6);
        var previousSpawn = level.getRespawnData();
        var sessions = PlayerRoomSavedData.get(level);
        UUID room = UUID.randomUUID();
        var saved = new ExteriorReturn(Level.OVERWORLD, Vec3.atBottomCenterOf(savedPos), 77, -12);
        sessions.enter(player.getUUID(), room, saved);
        try {
            level.setRespawnData(LevelData.RespawnData.of(Level.OVERWORLD, spawnPos, 24, 0));
            level.setBlockAndUpdate(spawnPos.below(), Blocks.STONE.defaultBlockState());
            var bed = Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.NORTH);
            level.setBlockAndUpdate(bedPos.below(), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(bedPos.south().below(), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(bedPos.south(), bed.setValue(BedBlock.PART, BedPart.FOOT));
            level.setBlockAndUpdate(bedPos, bed.setValue(BedBlock.PART, BedPart.HEAD));
            for (var pos : BlockPos.betweenClosed(bedPos.offset(-2, -1, -2), bedPos.offset(2, -1, 2))) {
                level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
            }
            player.setRespawnPosition(new ServerPlayer.RespawnConfig(
                    LevelData.RespawnData.of(Level.OVERWORLD, bedPos, 0, 0), false), false);
            level.setBlockAndUpdate(savedPos.below(), Blocks.STONE.defaultBlockState());
            var exact = SafeTravelDestination.exit(player, saved);
            helper.assertTrue(exact.source() == SafeTravelDestination.Source.SAVED_RETURN
                    && exact.position().equals(saved.position()) && exact.yaw() == 77 && exact.pitch() == -12,
                    "Saved destination did not take priority over personal respawn");
            level.setBlockAndUpdate(savedPos.below(), Blocks.AIR.defaultBlockState());
            var personal = SafeTravelDestination.exit(player, saved);
            helper.assertTrue(personal.source() == SafeTravelDestination.Source.PERSONAL_RESPAWN
                    && level.getBlockState(bedPos).is(Blocks.RED_BED), "Valid vanilla bed fallback failed");
            for (var pos : BlockPos.betweenClosed(bedPos.offset(-3, 1, -3), bedPos.offset(3, 3, 3))) {
                level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
            }
            helper.assertTrue(SafeTravelDestination.exit(player, saved).source() == SafeTravelDestination.Source.WORLD_SPAWN,
                    "Obstructed personal respawn did not fall back safely");
            for (var pos : BlockPos.betweenClosed(bedPos.offset(-3, 1, -3), bedPos.offset(3, 3, 3))) {
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            }
            level.setBlockAndUpdate(bedPos, Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(bedPos.south(), Blocks.AIR.defaultBlockState());
            var world = SafeTravelDestination.exit(player, saved);
            helper.assertTrue(world.source() == SafeTravelDestination.Source.WORLD_SPAWN
                    && world.level() == level && world.position().equals(Vec3.atBottomCenterOf(spawnPos)),
                    "Invalid personal respawn did not fall back to Overworld spawn");
            var missing = new ExteriorReturn(ResourceKey.create(Registries.DIMENSION,
                    Identifier.fromNamespaceAndPath(DimensionalAnchors.MODID, "missing")), saved.position(), 0, 0);
            helper.assertTrue(SafeTravelDestination.exit(player, missing).source() == SafeTravelDestination.Source.WORLD_SPAWN,
                    "Unavailable return dimension did not fall back");
            var nether = server.getLevel(Level.NETHER);
            clear(nether, bedPos, 6);
            for (var pos : BlockPos.betweenClosed(bedPos.offset(-2, -1, -2), bedPos.offset(2, -1, 2))) {
                nether.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
            }
            var anchor = Blocks.RESPAWN_ANCHOR.defaultBlockState().setValue(RespawnAnchorBlock.CHARGE, 1);
            nether.setBlockAndUpdate(bedPos, anchor);
            player.setRespawnPosition(new ServerPlayer.RespawnConfig(
                    LevelData.RespawnData.of(Level.NETHER, bedPos, 0, 0), false), false);
            helper.assertTrue(SafeTravelDestination.exit(player, saved).source() == SafeTravelDestination.Source.PERSONAL_RESPAWN
                    && nether.getBlockState(bedPos).getValue(RespawnAnchorBlock.CHARGE) == 1,
                    "Personal respawn consumed an anchor charge");
            nether.setBlockAndUpdate(bedPos, anchor.setValue(RespawnAnchorBlock.CHARGE, 0));
            helper.assertTrue(SafeTravelDestination.exit(player, saved).source() == SafeTravelDestination.Source.WORLD_SPAWN,
                    "Depleted personal respawn anchor was accepted");
            level.setBlockAndUpdate(spawnPos.below(), Blocks.AIR.defaultBlockState());
            expectFailure(helper, () -> SafeTravelDestination.exit(player, saved));
            helper.assertTrue(sessions.find(player.getUUID()).orElseThrow().equals(new PlayerRoomSession(room, saved)),
                    "Destination resolution mutated or deleted a session");
            helper.assertTrue(player.level() == level, "Destination resolution teleported the player");
        } finally {
            level.setRespawnData(previousSpawn);
            sessions.completeExit(player.getUUID(), room);
        }
    }

    private static void clear(ServerLevel level, BlockPos origin, int radius) {
        for (var pos : BlockPos.betweenClosed(origin.offset(-radius, -radius, -radius),
                origin.offset(radius, radius, radius))) {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
    }

    private static void expectFailure(GameTestHelper helper, Runnable action) {
        boolean failed = false;
        try {
            action.run();
        } catch (IllegalStateException expected) {
            failed = true;
        }
        helper.assertTrue(failed, "Unsafe destination search did not explicitly fail");
    }
}
