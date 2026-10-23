package com.iloelias.dimensionalanchors;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import com.mojang.authlib.GameProfile;

import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

final class RoomMovementTests {
    static void interiorFaces(GameTestHelper helper) {
        var pocket = helper.getLevel().getServer().getLevel(RoomGeneration.POCKET);
        var sessions = PlayerRoomSavedData.get(pocket);
        for (var tier : AnchorTier.values()) {
            UUID room = UUID.randomUUID();
            var geometry = RoomGeneration.prepare(pocket, room, tier);
            var player = occupant(helper, geometry, room);
            try {
                helper.assertTrue(!AnchorAvailability.isRegisteredAndPlaced(pocket, room),
                        "Movement fixture unexpectedly has a placed anchor");
                var bounds = geometry.interior();
                for (var pose : new Pose[]{Pose.STANDING, Pose.CROUCHING, Pose.SWIMMING}) {
                    player.setPose(pose);
                    double halfWidth = player.getBoundingBox().getXsize() / 2;
                    double height = player.getBoundingBox().getYsize();
                    for (double x : new double[]{bounds.minX() + halfWidth, bounds.maxX() + 1 - halfWidth}) {
                        for (double y : new double[]{bounds.minY(), bounds.maxY() + 1 - height}) {
                            for (double z : new double[]{bounds.minZ() + halfWidth, bounds.maxZ() + 1 - halfWidth}) {
                                player.setPos(x, y, z);
                                tick(player);
                                helper.assertTrue(player.isAlive(), "False wall death at " + tier + " " + pose);
                            }
                        }
                    }
                }
                player.setPose(Pose.STANDING);
                player.setPos(bounds.minX() + player.getBoundingBox().getXsize() / 2 - 0.5E-5,
                        bounds.minY(), geometry.reference().getZ() + 0.5);
                tick(player);
                helper.assertTrue(player.isAlive(), "Collision rounding tolerance rejected");
            } finally {
                sessions.completeExit(player.getUUID(), room);
            }
        }
    }

    static void departures(GameTestHelper helper) {
        var pocket = helper.getLevel().getServer().getLevel(RoomGeneration.POCKET);
        var sessions = PlayerRoomSavedData.get(pocket);
        UUID room = UUID.randomUUID();
        var geometry = RoomGeneration.prepare(pocket, room, AnchorTier.COPPER);
        var bounds = geometry.interior();
        for (int scenario = 0; scenario < 9; scenario++) {
            var player = occupant(helper, geometry, room);
            var original = sessions.find(player.getUUID()).orElseThrow();
            try {
                double halfWidth = player.getBoundingBox().getXsize() / 2;
                double height = player.getBoundingBox().getYsize();
                var center = Vec3.atBottomCenterOf(geometry.reference());
                switch (scenario) {
                    case 0 -> player.setPos(bounds.minX() + halfWidth - 2.0E-5, center.y(), center.z());
                    case 1 -> player.setPos(bounds.maxX() + 1 - halfWidth + 2.0E-5, center.y(), center.z());
                    case 2 -> player.setPos(center.x(), bounds.minY() - 2.0E-5, center.z());
                    case 3 -> player.setPos(center.x(), bounds.maxY() + 1 - height + 2.0E-5, center.z());
                    case 4 -> player.setPos(center.x(), center.y(), bounds.minZ() + halfWidth - 2.0E-5);
                    case 5 -> player.setPos(center.x(), center.y(), bounds.maxZ() + 1 - halfWidth + 2.0E-5);
                    case 6 -> player.setPos(center.add(RoomGeometry.CELL_SIZE, 0, 0));
                    case 7 -> player.setServerLevel(helper.getLevel());
                    case 8 -> player.setPosRaw(Double.NaN, center.y(), center.z());
                }
                tick(player);
                helper.assertTrue(!player.isAlive() && sessions.find(player.getUUID()).orElseThrow() == original,
                        "Unauthorized departure survived or erased its session: " + scenario);
            } finally {
                sessions.completeExit(player.getUUID(), room);
            }
        }
    }

    static void vanillaDeath(GameTestHelper helper) {
        var pocket = helper.getLevel().getServer().getLevel(RoomGeneration.POCKET);
        var sessions = PlayerRoomSavedData.get(pocket);
        var deathLevel = helper.getLevel();
        var rules = deathLevel.getGameRules();
        boolean oldKeep = rules.get(GameRules.KEEP_INVENTORY);
        UUID room = UUID.randomUUID();
        var geometry = RoomGeneration.prepare(pocket, room, AnchorTier.COPPER);
        try {
            for (boolean keep : new boolean[]{true, false}) {
                rules.set(GameRules.KEEP_INVENTORY, keep, deathLevel.getServer());
                var player = occupant(helper, geometry, room);
                if (keep) {
                    player.setGameMode(GameType.CREATIVE);
                }
                var marker = Component.literal(player.getUUID().toString());
                var diamonds = new ItemStack(Items.DIAMOND, 7);
                diamonds.set(DataComponents.CUSTOM_NAME, marker);
                player.getInventory().setItem(0, diamonds);
                try {
                    // Query actual drops in the already entity-visible GameTest chunk.
                    var departure = helper.absoluteVec(new Vec3(2.5, 3, 2.5));
                    helper.assertTrue(player.teleportTo(deathLevel, departure.x(), departure.y(), departure.z(),
                            Set.of(), 0, 0, false), "Could not create the unauthorized departure fixture");
                    player.hasChangedDimension();
                    player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
                    tick(player);
                    helper.assertTrue(!player.isAlive(), "Generic-kill boundary damage did not bypass creative");
                    helper.assertTrue(player.connection.getConnection().channel() instanceof EmbeddedChannel channel
                            && channel.outboundMessages().stream().anyMatch(packet ->
                                    packet instanceof ClientboundPlayerCombatKillPacket death
                                    && death.playerId() == player.getId()
                                    && death.message().getString().equals(player.getName().getString()
                                            + " tried to access places beyond imagination")),
                            "Boundary death message does not match the approved text");
                    helper.assertTrue(keep ? player.getInventory().getItem(0).getCount() == 7
                            : player.getInventory().getItem(0).isEmpty(), "Vanilla keepInventory was not respected");
                    int drops = deathLevel.getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(2)).stream()
                            .filter(item -> item.getItem().is(Items.DIAMOND)
                                    && marker.equals(item.getItem().get(DataComponents.CUSTOM_NAME)))
                            .mapToInt(item -> item.getItem().getCount()).sum();
                    helper.assertTrue(drops == (keep ? 0 : 7),
                            "Vanilla death drops were lost or duplicated: keep=" + keep + ", drops=" + drops);
                } finally {
                    sessions.completeExit(player.getUUID(), room);
                }
            }
        } finally {
            rules.set(GameRules.KEEP_INVENTORY, oldKeep, deathLevel.getServer());
        }
    }

    static void canceledDeath(GameTestHelper helper) {
        var pocket = helper.getLevel().getServer().getLevel(RoomGeneration.POCKET);
        var sessions = PlayerRoomSavedData.get(pocket);
        UUID room = UUID.randomUUID();
        var geometry = RoomGeneration.prepare(pocket, room, AnchorTier.COPPER);
        var player = occupant(helper, geometry, room);
        var attempts = new AtomicInteger();
        Consumer<LivingDeathEvent> listener = event -> {
            if (event.getEntity() == player) {
                attempts.incrementAndGet();
                event.setCanceled(true);
                player.setHealth(20);
                tick(player);
            }
        };
        NeoForge.EVENT_BUS.addListener(listener);
        try {
            player.setPos(player.position().add(RoomGeometry.CELL_SIZE, 0, 0));
            tick(player);
            tick(player);
            tick(player);
            helper.assertTrue(player.isAlive() && attempts.get() == 1,
                    "Canceled boundary death recursed or retried on subsequent ticks");
        } finally {
            NeoForge.EVENT_BUS.unregister(listener);
            sessions.completeExit(player.getUUID(), room);
        }
    }

    static void lifecycle(GameTestHelper helper) {
        var pocket = helper.getLevel().getServer().getLevel(RoomGeneration.POCKET);
        var sessions = PlayerRoomSavedData.get(pocket);
        UUID room = UUID.randomUUID();
        var geometry = RoomGeneration.prepare(pocket, room, AnchorTier.COPPER);
        var player = occupant(helper, geometry, room);
        var position = player.position();
        try {
            helper.assertTrue(sessions.beginTravel(player.getUUID()), "Could not begin movement guard fixture");
            try {
                player.setPos(position.add(RoomGeometry.CELL_SIZE, 0, 0));
                tick(player);
                helper.assertTrue(player.isAlive(), "An active legitimate transition triggered punishment");
                player.setPos(position);
            } finally {
                sessions.endTravel(player.getUUID());
            }
            tick(player);
            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedOutEvent(player));
            player.setPos(position.add(RoomGeometry.CELL_SIZE, 0, 0));
            tick(player);
            helper.assertTrue(player.isAlive(), "A forgotten observation punished an ambiguous initial location");
            player.setPos(position);
            tick(player);
            player.setPos(position.add(RoomGeometry.CELL_SIZE, 0, 0));
            tick(player);
            helper.assertTrue(player.isAlive(), "A suspended observation silently rearmed without reconciliation");
            player.setPos(position);
            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedInEvent(player));
            player.setPos(position.add(RoomGeometry.CELL_SIZE, 0, 0));
            tick(player);
            helper.assertTrue(!player.isAlive(), "Valid login did not establish the movement baseline");
        } finally {
            sessions.completeExit(player.getUUID(), room);
        }
    }

    static void invalidExitAttempts(GameTestHelper helper) {
        var pocket = helper.getLevel().getServer().getLevel(RoomGeneration.POCKET);
        var sessions = PlayerRoomSavedData.get(pocket);
        UUID room = UUID.randomUUID();
        var geometry = RoomGeneration.prepare(pocket, room, AnchorTier.COPPER);
        var player = occupant(helper, geometry, room);
        try {
            helper.assertTrue(!PlayerTravelService.exitWithKey(player, UUID.randomUUID()),
                    "An unrelated key was accepted as exit");
            helper.assertTrue(!PlayerTravelService.exitFromControl(player, geometry.reference()),
                    "An invalid control was accepted as exit");
            player.setPos(player.position().add(RoomGeometry.CELL_SIZE, 0, 0));
            tick(player);
            helper.assertTrue(!player.isAlive(), "Rejected invalid exits disabled boundary enforcement");
        } finally {
            sessions.completeExit(player.getUUID(), room);
        }
    }

    static void loadingImmunity(GameTestHelper helper) {
        var pocket = helper.getLevel().getServer().getLevel(RoomGeneration.POCKET);
        var sessions = PlayerRoomSavedData.get(pocket);
        UUID room = UUID.randomUUID();
        var geometry = RoomGeneration.prepare(pocket, room, AnchorTier.COPPER);
        var player = occupant(helper, geometry, room);
        try {
            var position = helper.absoluteVec(new Vec3(2.5, 3, 2.5));
            helper.assertTrue(player.teleportTo(helper.getLevel(), position.x(), position.y(), position.z(),
                    Set.of(), 0, 0, false), "Could not perform unauthorized dimension change");
            helper.assertTrue(player.isChangingDimension(), "Dimension transfer immunity fixture was not active");
            tick(player);
            tick(player);
            helper.assertTrue(player.isAlive(), "Loading immunity was bypassed");
            player.hasChangedDimension();
            player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
            tick(player);
            helper.assertTrue(!player.isAlive(), "Loading immunity permanently disabled enforcement");
        } finally {
            sessions.completeExit(player.getUUID(), room);
        }
    }

    static void pocketRespawn(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var pocket = server.getLevel(RoomGeneration.POCKET);
        var sessions = PlayerRoomSavedData.get(pocket);
        UUID room = UUID.randomUUID();
        var geometry = RoomGeneration.prepare(pocket, room, AnchorTier.COPPER);
        var player = occupant(helper, geometry, room);
        var original = sessions.find(player.getUUID()).orElseThrow();
        player.setRespawnPosition(new ServerPlayer.RespawnConfig(
                LevelData.RespawnData.of(RoomGeneration.POCKET, geometry.reference(), 0, 0), true), false);
        try {
            player.setHealth(0);
            var replacement = server.getPlayerList().respawn(player, false,
                    net.minecraft.world.entity.Entity.RemovalReason.KILLED);
            helper.assertTrue(replacement != player && replacement.level() == pocket && replacement.isAlive()
                    && sessions.find(player.getUUID()).orElseThrow() == original,
                    "Actual pocket respawn lost its session or expelled the player");
            replacement.hasChangedDimension();
            replacement.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedOutEvent(player));
            tick(player);
            replacement.setPos(replacement.position().add(RoomGeometry.CELL_SIZE, 0, 0));
            tick(replacement);
            helper.assertTrue(!replacement.isAlive(), "An obsolete player erased the replacement's movement baseline");
        } finally {
            sessions.completeExit(player.getUUID(), room);
        }
    }

    private static ServerPlayer occupant(GameTestHelper helper, RoomGeometry geometry, UUID room) {
        var server = helper.getLevel().getServer();
        var pocket = server.getLevel(RoomGeneration.POCKET);
        var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "boundary-player"), false);
        var player = new ServerPlayer(server, helper.getLevel(), cookie.gameProfile(), cookie.clientInformation());
        var connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        server.getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setGameMode(GameType.SURVIVAL);
        var position = Vec3.atBottomCenterOf(geometry.reference());
        helper.assertTrue(player.teleportTo(pocket, position.x(), position.y(), position.z(),
                Set.of(), 0, 0, false), "Could not place boundary fixture");
        player.hasChangedDimension();
        player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
        PlayerRoomSavedData.get(pocket).enter(player.getUUID(), room,
                new ExteriorReturn(Level.OVERWORLD, new Vec3(0.5, 64, 0.5), 0, 0));
        NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedInEvent(player));
        return player;
    }

    private static void tick(ServerPlayer player) {
        NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(player));
    }
}
