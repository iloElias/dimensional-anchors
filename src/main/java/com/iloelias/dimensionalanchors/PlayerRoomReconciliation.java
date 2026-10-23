package com.iloelias.dimensionalanchors;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

final class PlayerRoomReconciliation {
    private PlayerRoomReconciliation() {
    }

    static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            reconcile(player, false);
        }
    }

    static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            reconcile(player, true);
        }
    }

    private static void reconcile(ServerPlayer player, boolean respawn) {
        var level = player.level();
        if (!level.getServer().isSameThread()) {
            throw new IllegalStateException("Room reconciliation must run on the server thread");
        }
        var sessions = PlayerRoomSavedData.get(level);
        if (!sessions.beginTravel(player.getUUID())) {
            report(player, "Room reconciliation interrupted by active travel");
            return;
        }
        try {
            sessions.movement().forget(player.getUUID());
            var session = sessions.find(player.getUUID()).orElse(null);
            boolean pocket = level.dimension().equals(RoomGeneration.POCKET);
            if (session == null && !pocket) {
                return;
            }
            if (!player.isAlive() || player.isRemoved() || !player.position().isFinite()) {
                report(player, "Cannot reconcile an inactive player or invalid position");
                return;
            }
            if (session == null) {
                report(player, "Player is in the pocket dimension without a recorded session");
                return;
            }
            if (!pocket) {
                if (respawn) {
                    sessions.completeExitIfUnchanged(player.getUUID(), session);
                    player.sendOverlayMessage(Component.translatable("message.dimensionalanchors.session_respawn_complete"));
                } else {
                    // Login alone cannot distinguish interrupted travel from an external teleport.
                    report(player, "Exterior login with an unresolved room session");
                }
                return;
            }
            try {
                var geometry = PlayerTravelService.currentRoom(level, player.blockPosition(), session);
                sessions.movement().resume(player, session, geometry);
            } catch (IllegalStateException exception) {
                DimensionalAnchors.LOGGER.warn("Cannot reconcile room {} for player {}",
                        session.anchorId(), player.getUUID(), exception);
                player.sendOverlayMessage(Component.translatable("message.dimensionalanchors.session_reconciliation_failed"));
            }
        } finally {
            sessions.endTravel(player.getUUID());
        }
    }

    private static void report(ServerPlayer player, String reason) {
        DimensionalAnchors.LOGGER.warn("{}: player {} at {} {}", reason,
                player.getUUID(), player.level().dimension(), player.position());
        player.sendOverlayMessage(Component.translatable("message.dimensionalanchors.session_reconciliation_failed"));
    }
}
