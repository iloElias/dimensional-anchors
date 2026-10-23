package com.iloelias.dimensionalanchors;

import java.util.ArrayList;
import java.util.UUID;

import com.mojang.authlib.GameProfile;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

final class PlayerRoomReconciliationTests {
    static void vanillaRespawn(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var pocket = server.getLevel(RoomGeneration.POCKET);
        var player = helper.makeMockServerPlayerInLevel();
        var feet = helper.absolutePos(new BlockPos(2, 3, 2));
        helper.getLevel().setBlockAndUpdate(feet.below(), Blocks.STONE.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(feet, Blocks.AIR.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(feet.above(), Blocks.AIR.defaultBlockState());
        player.setRespawnPosition(new ServerPlayer.RespawnConfig(
                LevelData.RespawnData.of(helper.getLevel().dimension(), feet, 35, 0), true), false);
        UUID room = UUID.randomUUID();
        var geometry = RoomGeneration.prepare(pocket, room, AnchorTier.COPPER);
        var entry = Vec3.atBottomCenterOf(geometry.reference());
        helper.assertTrue(player.teleportTo(pocket, entry.x(), entry.y(), entry.z(),
                java.util.Set.of(), 0, 0, false), "Could not place respawn fixture inside room");
        var sessions = PlayerRoomSavedData.get(pocket);
        sessions.enter(player.getUUID(), room, exterior());
        var expected = player.findRespawnPositionAndUseSpawnBlock(false, TeleportTransition.DO_NOTHING);
        player.setHealth(0);
        var respawned = server.getPlayerList().respawn(player, false,
                net.minecraft.world.entity.Entity.RemovalReason.KILLED);
        helper.assertTrue(respawned != player && respawned.getUUID().equals(player.getUUID())
                && respawned.isAlive() && respawned.level() == expected.newLevel()
                && respawned.position().equals(expected.position())
                && respawned.getYRot() == expected.yRot() && respawned.getXRot() == expected.xRot()
                && sessions.find(player.getUUID()).isEmpty(),
                "Actual vanilla respawn failed session cleanup or altered respawn placement");
    }

    static void occupants(GameTestHelper helper) {
        var pocket = helper.getLevel().getServer().getLevel(RoomGeneration.POCKET);
        UUID room = UUID.randomUUID();
        var geometry = RoomGeneration.prepare(pocket, room, AnchorTier.COPPER);
        var player = new ReconciliationPlayer(pocket);
        player.setPos(Vec3.atBottomCenterOf(geometry.reference()));
        var sessions = PlayerRoomSavedData.get(pocket);
        sessions.enter(player.getUUID(), room, exterior());
        var original = sessions.find(player.getUUID()).orElseThrow();
        var velocity = new Vec3(0.2, 0.1, 0.3);
        player.setDeltaMovement(velocity);
        var position = player.position();
        try {
            helper.assertTrue(!AnchorAvailability.isRegisteredAndPlaced(pocket, room),
                    "Test room unexpectedly has a placed anchor");
            for (int i = 0; i < 2; i++) {
                NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedInEvent(player));
                NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(player));
                NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerRespawnEvent(player, false));
                NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerRespawnEvent(player, true));
            }
            helper.assertTrue(player.level() == pocket && player.position().equals(position)
                    && player.getDeltaMovement().equals(velocity)
                    && sessions.find(player.getUUID()).orElseThrow() == original && player.messages.isEmpty(),
                    "An absent anchor, login or pocket respawn expelled or changed an existing occupant");
            helper.assertTrue(sessions.beginTravel(player.getUUID()), "Reconciliation leaked its guard");
            sessions.endTravel(player.getUUID());
        } finally {
            sessions.completeExit(player.getUUID(), room);
        }
    }

    static void inconsistentLocations(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var pocket = server.getLevel(RoomGeneration.POCKET);
        UUID room = UUID.randomUUID();
        var geometry = RoomGeneration.prepare(pocket, room, AnchorTier.COPPER);
        var sessions = PlayerRoomSavedData.get(pocket);
        for (int scenario = 0; scenario < 5; scenario++) {
            var player = new ReconciliationPlayer(scenario == 0 ? server.overworld() : pocket);
            player.setPos(Vec3.atBottomCenterOf(geometry.reference()));
            if (scenario != 1) {
                sessions.enter(player.getUUID(), scenario == 2 ? UUID.randomUUID() : room, exterior());
            }
            if (scenario == 3) {
                player.setPos(Vec3.atBottomCenterOf(geometry.reference().offset(32, 0, 0)));
            } else if (scenario == 4) {
                player.setPosRaw(Double.NaN, 64, 0);
            }
            var original = sessions.find(player.getUUID()).orElse(null);
            var position = player.position();
            var level = player.level();
            try {
                NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedInEvent(player));
                NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(player));
                helper.assertTrue(player.level() == level && player.position() == position
                        && sessions.find(player.getUUID()).orElse(null) == original
                        && player.isAlive() && player.hasMessage("message.dimensionalanchors.session_reconciliation_failed"),
                        "Inconsistent login moved a player, erased a session or failed silently: " + scenario);
                if (scenario != 0) {
                    NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerRespawnEvent(player, false));
                    helper.assertTrue(player.level() == level && player.position() == position
                            && sessions.find(player.getUUID()).orElse(null) == original,
                            "Pocket respawn attempted automatic expulsion/recovery");
                }
            } finally {
                if (original != null) {
                    sessions.completeExit(player.getUUID(), original.anchorId());
                }
            }
        }
    }

    static void exteriorRespawn(GameTestHelper helper) {
        var level = helper.getLevel();
        var sessions = PlayerRoomSavedData.get(level);
        UUID room = UUID.randomUUID();
        var player = new ReconciliationPlayer(level);
        var other = UUID.randomUUID();
        player.setPos(helper.absoluteVec(new Vec3(2.5, 3, 2.5)));
        sessions.enter(player.getUUID(), room, exterior());
        sessions.enter(other, room, exterior());
        var position = player.position();
        var original = sessions.find(player.getUUID()).orElseThrow();
        try {
            sessions.beginTravel(player.getUUID());
            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerRespawnEvent(player, false));
            helper.assertTrue(sessions.find(player.getUUID()).orElseThrow() == original,
                    "Respawn reconciliation interfered with active travel");
            helper.assertTrue(!sessions.beginTravel(player.getUUID()), "Reconciliation released another operation's guard");
            sessions.endTravel(player.getUUID());
            player.setHealth(0);
            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerRespawnEvent(player, false));
            helper.assertTrue(sessions.find(player.getUUID()).orElseThrow() == original,
                    "Inactive respawn removed a session");
            player.setHealth(20);
            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerRespawnEvent(player, false));
            helper.assertTrue(sessions.find(player.getUUID()).isEmpty() && sessions.find(other).isPresent()
                    && player.position() == position && player.level() == level
                    && player.hasMessage("message.dimensionalanchors.session_respawn_complete"),
                    "Exterior respawn changed vanilla placement or failed to isolate session cleanup");
            int messages = player.messages.size();
            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerRespawnEvent(player, true));
            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedInEvent(player));
            helper.assertTrue(player.messages.size() == messages, "Repeated no-session reconciliation was not a no-op");
        } finally {
            sessions.endTravel(player.getUUID());
            sessions.find(player.getUUID()).ifPresent(session -> sessions.completeExit(player.getUUID(), session.anchorId()));
            sessions.completeExit(other, room);
        }
    }

    private static ExteriorReturn exterior() {
        return new ExteriorReturn(Level.OVERWORLD, new Vec3(2.5, 80, 2.5), 70, -10);
    }

    private static final class ReconciliationPlayer extends ServerPlayer {
        private final ArrayList<Component> messages = new ArrayList<>();

        private ReconciliationPlayer(ServerLevel level) {
            super(level.getServer(), level, new GameProfile(UUID.randomUUID(), "reconcile-test"),
                    ClientInformation.createDefault());
        }

        @Override
        public void sendOverlayMessage(Component message) {
            messages.add(message);
        }

        private boolean hasMessage(String key) {
            return messages.stream().anyMatch(message -> message.getContents() instanceof TranslatableContents text
                    && text.getKey().equals(key));
        }
    }
}
