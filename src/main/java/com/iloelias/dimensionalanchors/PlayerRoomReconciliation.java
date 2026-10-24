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
            var transition = sessions.transition(player.getUUID()).orElse(null);
            boolean pocket = level.dimension().equals(RoomGeneration.POCKET);
            if (session == null && transition == null && !pocket) {
                return;
            }
            if (!player.isAlive() || player.isRemoved() || !player.position().isFinite()) {
                report(player, "Cannot reconcile an inactive player or invalid position");
                return;
            }
            if (transition != null && transition.targetRoom().isPresent()) {
                if (pocket && SafeTravelDestination.recoveredArrival(player, transition)) {
                    var target = new PlayerRoomSession(transition.targetRoom().orElseThrow(), transition.exteriorReturn());
                    try {
                        var geometry = PlayerTravelService.currentRoom(level, player.blockPosition(), target);
                        var entered = sessions.completeTransition(player.getUUID(), transition);
                        DimensionalAnchors.LOGGER.info("Completed interrupted {} {} for player {} at its expected arrival",
                                transition.type(), transition.operationId(), player.getUUID());
                        sessions.movement().resume(player, entered, geometry);
                        player.sendOverlayMessage(Component.translatable("message.dimensionalanchors.entry_complete"));
                        return;
                    } catch (IllegalStateException exception) {
                        DimensionalAnchors.LOGGER.warn("Cannot complete interrupted {} {} for player {}",
                                transition.type(), transition.operationId(), player.getUUID(), exception);
                    }
                } else if (transition.type() == PlayerTransition.Type.ENTRY && session == null && !pocket) {
                    DimensionalAnchors.LOGGER.info("Cancelling interrupted {} {} for player {}: never entered the pocket",
                            transition.type(), transition.operationId(), player.getUUID());
                    sessions.cancelTransition(player.getUUID(), transition.operationId());
                    return;
                }
            }
            if (session == null) {
                report(player, "Player is in the pocket dimension without a recorded session");
                return;
            }
            if (!pocket) {
                if (transition != null && transition.type() == PlayerTransition.Type.EXIT
                        && transition.sourceRoom().equals(java.util.Optional.of(session.anchorId()))
                        && SafeTravelDestination.recoveredArrival(player, transition)) {
                    DimensionalAnchors.LOGGER.info("Completed interrupted {} {} for player {} at its expected arrival",
                            transition.type(), transition.operationId(), player.getUUID());
                    sessions.completeExitIfUnchanged(player.getUUID(), session);
                    player.sendOverlayMessage(Component.translatable("message.dimensionalanchors.exit_complete"));
                    return;
                }
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
                if (transition != null) {
                    DimensionalAnchors.LOGGER.info("Cancelling interrupted {} {} for player {}: source room unchanged",
                            transition.type(), transition.operationId(), player.getUUID());
                    sessions.cancelTransition(player.getUUID(), transition.operationId());
                }
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
