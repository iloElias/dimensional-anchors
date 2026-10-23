package com.iloelias.dimensionalanchors;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

final class TravelServiceTests {
    static void arrivalLimits(GameTestHelper helper) {
        var level = helper.getLevel();
        var exterior = safeExterior(helper, new BlockPos(2, 3, 2));
        var destination = new SafeTravelDestination.Destination(level, exterior.position(), 0, 0,
                SafeTravelDestination.Source.SAVED_RETURN);
        var player = new ExitTestPlayer(level);
        player.setPos(destination.position());
        player.setYRot(destination.yaw());
        player.setXRot(destination.pitch());
        helper.assertTrue(SafeTravelDestination.safeArrival(player, destination), "Exact safe arrival rejected");
        player.setYRot(720);
        helper.assertTrue(SafeTravelDestination.safeArrival(player, destination), "Equivalent wrapped yaw rejected");
        for (float difference : new float[]{Math.nextDown(1.0E-4F), 1.0E-4F, Math.nextUp(1.0E-4F)}) {
            player.setYRot(difference);
            helper.assertTrue(SafeTravelDestination.safeArrival(player, destination) == (difference <= 1.0E-4F),
                    "Yaw tolerance endpoint incorrect: " + difference);
            player.setYRot(0);
            player.setXRot(difference);
            helper.assertTrue(SafeTravelDestination.safeArrival(player, destination) == (difference <= 1.0E-4F),
                    "Pitch tolerance endpoint incorrect: " + difference);
            player.setXRot(0);
        }
        for (double offset : new double[]{0.5E-6, 1.5E-6}) {
            player.setPos(destination.position().add(offset, 0, 0));
            helper.assertTrue(SafeTravelDestination.safeArrival(player, destination) == (offset < 1.0E-6),
                    "Position tolerance incorrect: " + offset);
        }
        player.setPos(destination.position().add(0.75E-6, 0, 0.75E-6));
        helper.assertTrue(!SafeTravelDestination.safeArrival(player, destination),
                "Position tolerance was applied per axis instead of total distance");
        player.setPos(destination.position());
        for (double value : new double[]{Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            for (int axis = 0; axis < 3; axis++) {
                var pos = destination.position();
                player.setPosRaw(axis == 0 ? value : pos.x(), axis == 1 ? value : pos.y(), axis == 2 ? value : pos.z());
                helper.assertTrue(!SafeTravelDestination.safeArrival(player, destination),
                        "Non-finite arrival coordinate accepted");
                player.setPos(destination.position());
            }
        }
        for (float value : new float[]{Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY}) {
            // Vanilla setters discard these values; emulate a modded getter instead.
            player.reportedYaw = value;
            helper.assertTrue(!SafeTravelDestination.safeArrival(player, destination), "Non-finite yaw accepted");
            player.reportedYaw = null;
            player.reportedPitch = value;
            helper.assertTrue(!SafeTravelDestination.safeArrival(player, destination), "Non-finite pitch accepted");
            player.reportedPitch = null;
        }
        player.setHealth(0);
        helper.assertTrue(!SafeTravelDestination.safeArrival(player, destination), "Dead arrival accepted");
        player.setHealth(20);
        player.setRemoved(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
        helper.assertTrue(!SafeTravelDestination.safeArrival(player, destination), "Removed arrival accepted");
    }

    static void travelThreadAndLiveness(GameTestHelper helper) {
        var pocket = helper.getLevel().getServer().getLevel(RoomGeneration.POCKET);
        UUID anchorId = UUID.randomUUID();
        var geometry = RoomGeneration.prepare(pocket, anchorId, AnchorTier.COPPER);
        var exterior = safeExterior(helper, new BlockPos(2, 3, 2));
        var player = new ExitTestPlayer(pocket);
        player.setPos(Vec3.atBottomCenterOf(geometry.reference()));
        var sessions = PlayerRoomSavedData.get(pocket);
        sessions.enter(player.getUUID(), anchorId, exterior);
        var original = sessions.find(player.getUUID()).orElseThrow();
        try {
            boolean rejected = java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                try {
                    PlayerTravelService.exitFromControl(player, geometry.exitControl());
                    return false;
                } catch (IllegalStateException expected) {
                    return expected.getMessage().equals("Player travel must run on the server thread");
                }
            }).join();
            helper.assertTrue(rejected && player.messages.isEmpty() && player.level() == pocket
                    && sessions.find(player.getUUID()).orElseThrow() == original,
                    "Off-thread travel was not rejected before side effects");
            player.setHealth(0);
            helper.assertTrue(!PlayerTravelService.exitWithKey(player, anchorId), "Dead player traveled");
            player.setHealth(20);
            player.setRemoved(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
            helper.assertTrue(!PlayerTravelService.exitWithKey(player, anchorId), "Removed player traveled");
            helper.assertTrue(player.level() == pocket && sessions.find(player.getUUID()).orElseThrow() == original,
                    "Dead/removed-player rejection changed room/session");
            helper.assertTrue(sessions.beginTravel(player.getUUID()), "Rejected travel leaked its guard");
            sessions.endTravel(player.getUUID());
        } finally {
            sessions.completeExit(player.getUUID(), anchorId);
        }
    }

    static void controlExit(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var pocket = server.getLevel(RoomGeneration.POCKET);
        var player = helper.makeMockServerPlayerInLevel();
        UUID anchorId = UUID.randomUUID();
        var geometry = RoomGeneration.prepare(pocket, anchorId, AnchorTier.COPPER);
        var exterior = safeExterior(helper, new BlockPos(2, 3, 2));
        var roomPosition = Vec3.atBottomCenterOf(geometry.reference());
        helper.assertTrue(player.teleportTo(pocket, roomPosition.x(), roomPosition.y(), roomPosition.z(),
                Set.<Relative>of(), 0, 0, false), "Could not place test player in the generated room");
        var sessions = PlayerRoomSavedData.get(server.overworld());
        sessions.enter(player.getUUID(), anchorId, exterior);

        var control = geometry.exitControl();
        var hit = new BlockHitResult(Vec3.atCenterOf(control), Direction.NORTH, control, false);
        NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(player));
        var result = pocket.getBlockState(control).useWithoutItem(pocket, player, hit);
        NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(player));
        helper.assertTrue(player.isAlive(), "A legitimate control exit triggered boundary punishment");
        helper.assertTrue(result.consumesAction(), "Keyless exit control did not consume its interaction");
        helper.assertTrue(player.level() == server.overworld()
                && player.position().equals(exterior.position())
                && player.getYRot() == exterior.yaw() && player.getXRot() == exterior.pitch(),
                "Exit did not restore the exact exterior destination and orientation");
        helper.assertTrue(sessions.find(player.getUUID()).isEmpty(),
                "Successful exit did not clear only the player's room session");
    }

    static void cancellationAndReentry(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var pocket = server.getLevel(RoomGeneration.POCKET);
        var player = helper.makeMockServerPlayerInLevel();
        UUID anchorId = UUID.randomUUID();
        var geometry = RoomGeneration.prepare(pocket, anchorId, AnchorTier.COPPER);
        var exterior = safeExterior(helper, new BlockPos(2, 3, 2));
        var roomPosition = Vec3.atBottomCenterOf(geometry.reference());
        helper.assertTrue(player.teleportTo(pocket, roomPosition.x(), roomPosition.y(), roomPosition.z(),
                Set.<Relative>of(), 0, 0, false), "Could not place test player in the room");
        var sessions = PlayerRoomSavedData.get(pocket);
        sessions.enter(player.getUUID(), anchorId, exterior);
        var original = sessions.find(player.getUUID()).orElseThrow();
        var velocity = new Vec3(0.1, -0.5, 0.2);
        player.setDeltaMovement(velocity);
        player.fallDistance = 20;
        int[] attempts = {0};
        Consumer<EntityTravelToDimensionEvent> listener = event -> {
            if (event.getEntity() == player) {
                attempts[0]++;
                helper.assertTrue(!PlayerTravelService.exitWithKey(player, anchorId)
                        && !PlayerTravelService.exitFromControl(player, geometry.exitControl()),
                        "A nested exit bypassed the per-player travel guard");
                event.setCanceled(true);
            }
        };
        NeoForge.EVENT_BUS.addListener(listener);
        try {
            helper.assertTrue(!PlayerTravelService.exitFromControl(player, geometry.exitControl()),
                    "Canceled NeoForge travel was reported as successful");
            helper.assertTrue(attempts[0] == 1 && player.level() == pocket
                    && player.position().equals(roomPosition)
                    && sessions.find(player.getUUID()).orElseThrow() == original
                    && player.getDeltaMovement().equals(velocity) && player.fallDistance == 20,
                    "Cancellation changed location, session, motion, or fall state");
        } finally {
            NeoForge.EVENT_BUS.unregister(listener);
        }
        NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(player));
        helper.assertTrue(PlayerTravelService.exitWithKey(player, anchorId),
                "Canceled/reentrant exit left a stale travel guard or prevented retry");
        NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(player));
        helper.assertTrue(player.isAlive(), "A legitimate key exit triggered boundary punishment");
        helper.assertTrue(player.level() == server.overworld() && player.position().equals(exterior.position())
                && player.getDeltaMovement().equals(Vec3.ZERO) && player.fallDistance == 0
                && sessions.find(player.getUUID()).isEmpty(), "Retry did not complete a safe exit");
    }

    static void exceptionalExits(GameTestHelper helper) {
        var pocket = helper.getLevel().getServer().getLevel(RoomGeneration.POCKET);
        UUID anchorId = UUID.randomUUID();
        var geometry = RoomGeneration.prepare(pocket, anchorId, AnchorTier.COPPER);
        var exterior = safeExterior(helper, new BlockPos(2, 3, 2));
        var sessions = PlayerRoomSavedData.get(pocket);
        for (int scenario = 0; scenario < 8; scenario++) {
            var player = new ExitTestPlayer(pocket);
            player.setPos(Vec3.atBottomCenterOf(geometry.reference()));
            sessions.enter(player.getUUID(), anchorId, exterior);
            var original = sessions.find(player.getUUID()).orElseThrow();
            NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(player));
            switch (scenario) {
                case 0 -> player.beforeTeleport = () -> {
                    throw new UnsupportedOperationException("Expected pre-teleport hook failure");
                };
                case 1 -> player.move = false;
                case 2 -> player.afterTeleport = () -> {
                    throw new UnsupportedOperationException("Expected post-teleport hook failure");
                };
                case 3 -> player.accepted = false;
                case 4 -> player.afterTeleport = () -> player.setPos(exterior.position().add(1, 0, 0));
                case 5 -> player.afterTeleport = () -> player.setXRot(exterior.pitch() + 1);
                case 6 -> player.afterTeleport = () -> helper.getLevel().setBlockAndUpdate(
                        BlockPos.containing(exterior.position()).above(), Blocks.STONE.defaultBlockState());
                case 7 -> player.afterTeleport = () -> player.setServerLevel(pocket);
                default -> throw new IllegalStateException("Unexpected exit failure scenario");
            }
            try {
                helper.assertTrue(!PlayerTravelService.exitFromControl(player, geometry.exitControl()),
                        "Unconfirmed/exceptional teleport reported success: " + scenario);
                NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(player));
                NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(player));
                helper.assertTrue(player.isAlive(), "Unconfirmed legitimate exit triggered punishment: " + scenario);
                helper.assertTrue(sessions.find(player.getUUID()).orElseThrow() == original
                        && !hasMessage(player, "message.dimensionalanchors.exit_complete")
                        && hasMessage(player, "message.dimensionalanchors.exit_unconfirmed"),
                        "Unconfirmed exit cleared its session or omitted failure feedback: " + scenario);
                helper.assertTrue(sessions.beginTravel(player.getUUID()), "Failed exit leaked its travel guard");
                sessions.endTravel(player.getUUID());
            } finally {
                helper.getLevel().setBlockAndUpdate(BlockPos.containing(exterior.position()).above(),
                        Blocks.AIR.defaultBlockState());
                sessions.completeExit(player.getUUID(), anchorId);
            }
        }
    }

    static void replacedSessions(GameTestHelper helper) {
        var pocket = helper.getLevel().getServer().getLevel(RoomGeneration.POCKET);
        UUID anchorId = UUID.randomUUID();
        var geometry = RoomGeneration.prepare(pocket, anchorId, AnchorTier.COPPER);
        var exterior = safeExterior(helper, new BlockPos(2, 3, 2));
        var sessions = PlayerRoomSavedData.get(pocket);
        UUID otherPlayer = UUID.randomUUID();
        sessions.enter(otherPlayer, anchorId, exterior);
        var otherSession = sessions.find(otherPlayer).orElseThrow();
        try {
            for (int scenario = 0; scenario < 4; scenario++) {
                var player = new ExitTestPlayer(pocket);
                player.setPos(Vec3.atBottomCenterOf(geometry.reference()));
                sessions.enter(player.getUUID(), anchorId, exterior);
                int selected = scenario;
                var replacement = new PlayerRoomSession[1];
                player.afterTeleport = () -> {
                    switch (selected) {
                        case 0 -> sessions.changeRoom(player.getUUID(), UUID.randomUUID());
                        case 1 -> {
                            sessions.completeExit(player.getUUID(), anchorId);
                            sessions.enter(player.getUUID(), anchorId, exterior);
                        }
                        case 2 -> {
                            sessions.changeRoom(player.getUUID(), UUID.randomUUID());
                            sessions.changeRoom(player.getUUID(), anchorId);
                        }
                        case 3 -> sessions.completeExit(player.getUUID(), anchorId);
                        default -> throw new IllegalStateException("Unexpected session replacement scenario");
                    }
                    replacement[0] = sessions.find(player.getUUID()).orElse(null);
                };
                helper.assertTrue(!PlayerTravelService.exitWithKey(player, anchorId),
                        "Exit with changed session was reported as normal success");
                helper.assertTrue(sessions.find(player.getUUID()).orElse(null) == replacement[0]
                        && sessions.find(otherPlayer).orElseThrow() == otherSession
                        && hasMessage(player, "message.dimensionalanchors.exit_session_error")
                        && !hasMessage(player, "message.dimensionalanchors.exit_complete"),
                        "Exit removed/recreated a replacement session or another occupant's session");
                helper.assertTrue(sessions.beginTravel(player.getUUID()), "Cleanup failure leaked its travel guard");
                sessions.endTravel(player.getUUID());
                sessions.find(player.getUUID()).ifPresent(session ->
                        sessions.completeExit(player.getUUID(), session.anchorId()));
            }
        } finally {
            sessions.completeExit(otherPlayer, anchorId);
        }
    }

    static void rejectedTeleportKeepsSession(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var pocket = server.getLevel(RoomGeneration.POCKET);
        UUID anchorId = UUID.randomUUID();
        var geometry = RoomGeneration.prepare(pocket, anchorId, AnchorTier.COPPER);
        var exterior = safeExterior(helper, new BlockPos(10, 3, 2));
        var attempts = new int[1];
        var messages = new java.util.ArrayList<Component>();
        var player = new ServerPlayer(server, pocket,
                new GameProfile(UUID.randomUUID(), "rejected-travel-test"), ClientInformation.createDefault()) {
            @Override
            public boolean teleportTo(ServerLevel target, double x, double y, double z,
                    Set<Relative> relatives, float yaw, float pitch, boolean resetCamera) {
                attempts[0]++;
                return false;
            }

            @Override
            public void sendOverlayMessage(Component message) {
                messages.add(message);
            }
        };
        player.setPos(Vec3.atBottomCenterOf(geometry.reference()));
        var sessions = PlayerRoomSavedData.get(server.overworld());
        sessions.enter(player.getUUID(), anchorId, exterior);

        helper.assertTrue(!PlayerTravelService.exitFromControl(player, geometry.exitControl()),
                "Rejected teleport was reported as a successful exit");
        helper.assertTrue(attempts[0] == 1 && player.level() == pocket
                && sessions.find(player.getUUID()).orElseThrow().anchorId().equals(anchorId),
                "Rejected teleport moved the player or removed their room session");
        helper.assertTrue(messages.stream().anyMatch(message ->
                message.getContents() instanceof TranslatableContents contents
                        && contents.getKey().equals("message.dimensionalanchors.exit_failed")),
                "Rejected teleport did not report an explicit failure");
    }

    private static ExteriorReturn safeExterior(GameTestHelper helper, BlockPos localFeet) {
        var level = helper.getLevel();
        BlockPos feet = helper.absolutePos(localFeet);
        for (var pos : BlockPos.betweenClosed(feet.offset(-5, -5, -5), feet.offset(5, 5, 5))) {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
        level.setBlockAndUpdate(feet.below(), Blocks.STONE.defaultBlockState());
        return new ExteriorReturn(Level.OVERWORLD, Vec3.atBottomCenterOf(feet), 73, -8);
    }

    private static boolean hasMessage(ExitTestPlayer player, String key) {
        return player.messages.stream().anyMatch(message ->
                message.getContents() instanceof TranslatableContents contents && contents.getKey().equals(key));
    }

    private static final class ExitTestPlayer extends ServerPlayer {
        private final ArrayList<Component> messages = new ArrayList<>();
        private Runnable beforeTeleport = () -> {};
        private Runnable afterTeleport = () -> {};
        private boolean move = true;
        private boolean accepted = true;
        private Float reportedYaw;
        private Float reportedPitch;

        private ExitTestPlayer(ServerLevel level) {
            super(level.getServer(), level, new GameProfile(UUID.randomUUID(), "exit-hook-test"),
                    ClientInformation.createDefault());
        }

        @Override
        public boolean teleportTo(ServerLevel target, double x, double y, double z,
                Set<Relative> relatives, float yaw, float pitch, boolean resetCamera) {
            beforeTeleport.run();
            if (move) {
                setServerLevel(target);
                setPos(x, y, z);
                setYRot(yaw);
                setXRot(pitch);
            }
            afterTeleport.run();
            return accepted;
        }

        @Override
        public float getYRot() {
            return reportedYaw == null ? super.getYRot() : reportedYaw;
        }

        @Override
        public float getXRot() {
            return reportedPitch == null ? super.getXRot() : reportedPitch;
        }

        @Override
        public void sendOverlayMessage(Component message) {
            messages.add(message);
        }
    }
}
