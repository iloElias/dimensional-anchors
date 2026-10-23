package com.iloelias.dimensionalanchors;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

final class RoomMovementEnforcement {
    private static final double COLLISION_TOLERANCE = 1.0E-5;
    private final Map<UUID, Observation> observations = new HashMap<>();

    private enum Status {
        WATCHING, SUSPENDED, VIOLATED
    }

    private record Observation(ServerPlayer player, PlayerRoomSession session, Status status) {
    }

    static void onTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            var sessions = PlayerRoomSavedData.get(player.level());
            sessions.movement().check(player, sessions);
        }
    }

    static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerRoomSavedData.get(player.level()).movement().forget(player);
        }
    }

    void forget(ServerPlayer player) {
        observations.computeIfPresent(player.getUUID(),
                (id, observation) -> observation.player() == player ? null : observation);
    }

    void forget(UUID playerId) {
        observations.remove(playerId);
    }

    void suspend(ServerPlayer player, PlayerRoomSession session) {
        observations.put(player.getUUID(), new Observation(player, session, Status.SUSPENDED));
    }

    void resume(ServerPlayer player, PlayerRoomSession session, RoomGeometry geometry) {
        if (player.level().dimension().equals(RoomGeneration.POCKET) && contains(player, geometry)) {
            observations.put(player.getUUID(), new Observation(player, session, Status.WATCHING));
        } else {
            suspend(player, session);
            report(player, "Room reconciliation cannot establish an interior movement baseline");
        }
    }

    private void check(ServerPlayer player, PlayerRoomSavedData sessions) {
        if (!player.level().getServer().isSameThread()) {
            throw new IllegalStateException("Room movement enforcement must run on the server thread");
        }
        if (sessions.isTravelActive(player.getUUID())) {
            return;
        }
        var session = sessions.find(player.getUUID()).orElse(null);
        if (session == null || !player.isAlive() || player.isRemoved()) {
            forget(player);
            return;
        }
        var previous = observations.get(player.getUUID());
        boolean watching = previous != null && previous.player() == player && previous.session() == session;
        if (watching && previous.status() != Status.WATCHING) {
            return;
        }

        var allocation = RoomAllocationSavedData.get(player.level());
        var slot = allocation.find(session.anchorId());
        var room = allocation.room(session.anchorId()).filter(RoomGenerationState::complete);
        if (slot.isEmpty() || room.isEmpty()) {
            suspend(player, session);
            report(player, "Room movement check has no completed allocated room");
            return;
        }
        var geometry = new RoomGeometry(slot.getAsInt(), room.orElseThrow().tier());
        if (player.level().dimension().equals(RoomGeneration.POCKET) && contains(player, geometry)) {
            if (!watching) {
                observations.put(player.getUUID(), new Observation(player, session, Status.WATCHING));
            }
            return;
        }
        if (!watching) {
            suspend(player, session);
            report(player, "Room movement check cannot establish a valid initial occupant");
            return;
        }
        // Vanilla rejects even generic-kill damage while dimension/client loading is pending.
        if (player.isChangingDimension() || !player.connection.hasClientLoaded()) {
            return;
        }

        // Mark before damage so death/tick hooks cannot recursively apply the violation.
        observations.put(player.getUUID(), new Observation(player, session, Status.VIOLATED));
        DimensionalAnchors.LOGGER.warn("Unauthorized room departure by player {} from room {} at {} {}",
                player.getUUID(), session.anchorId(), player.level().dimension(), player.position());
        DamageSource source = new DamageSource(player.damageSources().genericKill().typeHolder()) {
            @Override
            public Component getLocalizedDeathMessage(LivingEntity victim) {
                return Component.translatableWithFallback("death.attack.dimensionalanchors.boundary",
                        "%1$s tried to access places beyond imagination", victim.getName());
            }
        };
        player.hurtServer(player.level(), source, Float.MAX_VALUE);
        if (player.isAlive() && !player.isRemoved()) {
            report(player, "Boundary violation damage did not kill the player; no repeated damage will be attempted");
        }
    }

    private static boolean contains(ServerPlayer player, RoomGeometry geometry) {
        AABB body = player.getBoundingBox();
        var interior = geometry.interior();
        return player.position().isFinite()
                && Double.isFinite(body.minX) && Double.isFinite(body.minY) && Double.isFinite(body.minZ)
                && Double.isFinite(body.maxX) && Double.isFinite(body.maxY) && Double.isFinite(body.maxZ)
                && body.minX >= interior.minX() - COLLISION_TOLERANCE
                && body.minY >= interior.minY() - COLLISION_TOLERANCE
                && body.minZ >= interior.minZ() - COLLISION_TOLERANCE
                && body.maxX <= interior.maxX() + 1 + COLLISION_TOLERANCE
                && body.maxY <= interior.maxY() + 1 + COLLISION_TOLERANCE
                && body.maxZ <= interior.maxZ() + 1 + COLLISION_TOLERANCE;
    }

    private static void report(ServerPlayer player, String reason) {
        DimensionalAnchors.LOGGER.warn("{}: player {} at {} {}", reason,
                player.getUUID(), player.level().dimension(), player.position());
        player.sendOverlayMessage(Component.translatable("message.dimensionalanchors.movement_unresolved"));
    }
}
