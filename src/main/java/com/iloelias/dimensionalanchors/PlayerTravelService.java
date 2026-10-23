package com.iloelias.dimensionalanchors;

import java.util.Set;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.phys.Vec3;

final class PlayerTravelService {
    private PlayerTravelService() {
    }

    static boolean exitFromControl(ServerPlayer player, BlockPos controlPos) {
        return exit(player, null, controlPos);
    }

    static boolean exitWithKey(ServerPlayer player, UUID keyAnchorId) {
        return exit(player, keyAnchorId, null);
    }

    private static boolean exit(ServerPlayer player, UUID keyAnchorId, BlockPos controlPos) {
        var level = player.level();
        if (!level.getServer().isSameThread()) {
            throw new IllegalStateException("Player travel must run on the server thread");
        }
        var sessions = PlayerRoomSavedData.get(level);
        if (!sessions.beginTravel(player.getUUID())) {
            player.sendOverlayMessage(Component.translatable("message.dimensionalanchors.travel_in_progress"));
            return false;
        }
        try {
            return exit(player, keyAnchorId, controlPos, sessions);
        } finally {
            sessions.endTravel(player.getUUID());
        }
    }

    private static boolean exit(ServerPlayer player, UUID keyAnchorId, BlockPos controlPos,
            PlayerRoomSavedData sessions) {
        var level = player.level();
        if (!level.dimension().equals(RoomGeneration.POCKET)) {
            player.sendOverlayMessage(Component.translatable("message.dimensionalanchors.exit_not_in_room"));
            return false;
        }

        if (!player.isAlive() || player.isRemoved()) {
            player.sendOverlayMessage(Component.translatable("message.dimensionalanchors.exit_failed"));
            return false;
        }
        var session = sessions.find(player.getUUID()).orElse(null);
        if (session == null) {
            player.sendOverlayMessage(Component.translatable("message.dimensionalanchors.exit_no_session"));
            return false;
        }
        if (keyAnchorId != null && !session.anchorId().equals(keyAnchorId)) {
            player.sendOverlayMessage(Component.translatable("message.dimensionalanchors.exit_wrong_room"));
            return false;
        }

        RoomGeometry geometry;
        try {
            geometry = currentRoom(level, player.blockPosition(), session);
        } catch (IllegalStateException exception) {
            DimensionalAnchors.LOGGER.warn("Cannot validate exit room for player {}", player.getUUID(), exception);
            player.sendOverlayMessage(Component.translatable("message.dimensionalanchors.exit_not_in_room"));
            return false;
        }
        if (controlPos != null && (!controlPos.equals(geometry.exitControl())
                || !level.getBlockState(controlPos).is(DimensionalAnchors.DIMENSIONAL_EXIT_BLOCK.get()))) {
            player.sendOverlayMessage(Component.translatable("message.dimensionalanchors.exit_control_invalid"));
            return false;
        }

        SafeTravelDestination.Destination destination;
        Vec3 sourcePosition = player.position();
        try {
            destination = SafeTravelDestination.exit(player, session.exteriorReturn());
        } catch (RuntimeException exception) {
            DimensionalAnchors.LOGGER.warn("No safe exit destination for player {} from room {}",
                    player.getUUID(), session.anchorId(), exception);
            if (player.level() != level || !player.position().equals(sourcePosition)
                    || sessions.find(player.getUUID()).orElse(null) != session) {
                return unconfirmed(player, sessions);
            }
            player.sendOverlayMessage(Component.translatable("message.dimensionalanchors.exit_failed"));
            return false;
        }

        if (sessions.find(player.getUUID()).orElse(null) != session || player.level() != level
                || !player.position().equals(sourcePosition) || !player.isAlive() || player.isRemoved()) {
            DimensionalAnchors.LOGGER.warn("Player {} changed state while resolving exit from room {}",
                    player.getUUID(), session.anchorId());
            return unconfirmed(player, sessions);
        }

        Vec3 position = destination.position();
        boolean teleported;
        try {
            teleported = player.teleportTo(destination.level(), position.x(), position.y(), position.z(),
                    Set.<Relative>of(), destination.yaw(), destination.pitch(), false);
        } catch (RuntimeException exception) {
            // Mod hooks can throw after moving the player; do not erase the session or attempt a second teleport.
            DimensionalAnchors.LOGGER.error("Exit teleport threw for player {} from room {}; session retained if unchanged",
                    player.getUUID(), session.anchorId(), exception);
            return unconfirmed(player, sessions);
        }
        if (!teleported) {
            boolean unmoved = player.level() == level && player.position().equals(sourcePosition);
            DimensionalAnchors.LOGGER.warn("Server rejected exit teleport for player {} from room {}; unmoved={}",
                    player.getUUID(), session.anchorId(), unmoved);
            if (!unmoved) {
                return unconfirmed(player, sessions);
            }
            player.sendOverlayMessage(Component.translatable("message.dimensionalanchors.exit_failed"));
            return false;
        }

        try {
            if (!SafeTravelDestination.safeArrival(player, destination)) {
                DimensionalAnchors.LOGGER.error("Exit arrival could not be confirmed for player {} from room {} at {} {}",
                        player.getUUID(), session.anchorId(), player.level().dimension(), player.position());
                return unconfirmed(player, sessions);
            }
            player.setDeltaMovement(Vec3.ZERO);
            player.resetFallDistance();
        } catch (RuntimeException exception) {
            DimensionalAnchors.LOGGER.error("Cannot verify or settle exit arrival for player {} from room {}",
                    player.getUUID(), session.anchorId(), exception);
            return unconfirmed(player, sessions);
        }

        try {
            sessions.completeExitIfUnchanged(player.getUUID(), session);
        } catch (IllegalStateException exception) {
            DimensionalAnchors.LOGGER.error("Player {} exited room {} but their session could not be cleared",
                    player.getUUID(), session.anchorId(), exception);
            sessions.find(player.getUUID()).ifPresent(current -> sessions.movement().suspend(player, current));
            player.sendOverlayMessage(Component.translatable("message.dimensionalanchors.exit_session_error"));
            return false;
        }

        player.sendOverlayMessage(Component.translatable("message.dimensionalanchors.exit_complete"));
        destination.level().playSound(null, position.x(), position.y(), position.z(),
                net.minecraft.sounds.SoundEvents.CHORUS_FRUIT_TELEPORT,
                net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    private static boolean unconfirmed(ServerPlayer player, PlayerRoomSavedData sessions) {
        sessions.find(player.getUUID()).ifPresent(session -> sessions.movement().suspend(player, session));
        player.sendOverlayMessage(Component.translatable("message.dimensionalanchors.exit_unconfirmed"));
        return false;
    }

    static RoomGeometry currentRoom(net.minecraft.server.level.ServerLevel level, BlockPos playerPos,
            PlayerRoomSession session) {
        var allocation = RoomAllocationSavedData.get(level);
        var slot = allocation.find(session.anchorId());
        var state = allocation.room(session.anchorId()).filter(RoomGenerationState::complete)
                .orElseThrow(() -> new IllegalStateException("Player room is not fully generated"));
        if (slot.isEmpty()) {
            throw new IllegalStateException("Player room has no reservation");
        }
        var geometry = new RoomGeometry(slot.getAsInt(), state.tier());
        if (!geometry.interior().isInside(playerPos)) {
            throw new IllegalStateException("Player is outside their recorded room");
        }
        return geometry;
    }
}
