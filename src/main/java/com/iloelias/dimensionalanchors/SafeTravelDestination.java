package com.iloelias.dimensionalanchors;

import java.util.Comparator;
import java.util.Optional;
import java.util.stream.StreamSupport;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

final class SafeTravelDestination {
    static final int RETURN_HORIZONTAL_RADIUS = 4;
    static final int RETURN_VERTICAL_RADIUS = 4;

    enum Source {
        ROOM, SAVED_RETURN, PERSONAL_RESPAWN, WORLD_SPAWN
    }

    record Destination(ServerLevel level, Vec3 position, float yaw, float pitch, Source source) {
    }

    private SafeTravelDestination() {
    }

    static Destination entry(ServerLevel pocket, Player player, RoomGeometry geometry) {
        if (!pocket.dimension().equals(RoomGeneration.POCKET)) {
            throw new IllegalArgumentException("Room entry requires the pocket dimension");
        }
        var interior = geometry.interior();
        Vec3 preferred = Vec3.atBottomCenterOf(geometry.entryFeet());
        return search(pocket, player, preferred, interior, Optional.of(interior))
                .map(position -> new Destination(pocket, position, 180.0F, 0.0F, Source.ROOM))
                .orElseThrow(() -> new IllegalStateException("No safe landing inside room slot " + geometry.slot()));
    }

    static Destination exit(ServerPlayer player, ExteriorReturn exterior) {
        var server = player.level().getServer();
        var savedLevel = server.getLevel(exterior.dimension());
        if (savedLevel != null) {
            var saved = nearby(savedLevel, player, exterior.position());
            if (saved.isPresent()) {
                return new Destination(savedLevel, saved.orElseThrow(), exterior.yaw(), exterior.pitch(), Source.SAVED_RETURN);
            }
        }
        var config = player.getRespawnConfig();
        if (config != null && !config.respawnData().dimension().equals(RoomGeneration.POCKET)) {
            var level = server.getLevel(config.respawnData().dimension());
            var position = config.respawnData().pos();
            if (level != null && level.isInValidBounds(position) && level.getWorldBorder().isWithinBounds(position)) {
                loadChunks(level, new BoundingBox(position.getX() - 2, position.getY(), position.getZ() - 2,
                        position.getX() + 2, position.getY(), position.getZ() + 2));
                var personal = ServerPlayer.findRespawnAndUseSpawnBlock(level, config, false);
                if (personal.isPresent()) {
                    var resolved = personal.orElseThrow();
                    if (safe(level, player, resolved.position(), Optional.empty())) {
                        return new Destination(level, resolved.position(), resolved.yaw(), resolved.pitch(), Source.PERSONAL_RESPAWN);
                    }
                }
            }
        }
        var overworld = server.overworld();
        var spawn = overworld.getRespawnData();
        return nearby(overworld, player, Vec3.atBottomCenterOf(spawn.pos()))
                .map(position -> new Destination(overworld, position, spawn.yaw(), spawn.pitch(), Source.WORLD_SPAWN))
                .orElseThrow(() -> new IllegalStateException("No safe saved return, personal respawn, or Overworld spawn"));
    }

    static Optional<Vec3> nearby(ServerLevel level, Player player, Vec3 preferred) {
        if (!Double.isFinite(preferred.x()) || !Double.isFinite(preferred.y()) || !Double.isFinite(preferred.z())
                || !net.minecraft.world.level.Level.isInSpawnableBounds(BlockPos.containing(preferred))) {
            throw new IllegalArgumentException("Invalid safe-position search origin");
        }
        var origin = BlockPos.containing(preferred);
        var bounds = new BoundingBox(origin.getX() - RETURN_HORIZONTAL_RADIUS,
                origin.getY() - RETURN_VERTICAL_RADIUS, origin.getZ() - RETURN_HORIZONTAL_RADIUS,
                origin.getX() + RETURN_HORIZONTAL_RADIUS, origin.getY() + RETURN_VERTICAL_RADIUS,
                origin.getZ() + RETURN_HORIZONTAL_RADIUS);
        return search(level, player, preferred, bounds, Optional.empty());
    }

    static boolean safeArrival(ServerPlayer player, Destination destination) {
        Vec3 actual = player.position();
        return player.level() == destination.level() && player.isAlive() && !player.isRemoved()
                && Double.isFinite(actual.x()) && Double.isFinite(actual.y()) && Double.isFinite(actual.z())
                && actual.distanceToSqr(destination.position()) <= 1.0E-12
                && Float.isFinite(player.getYRot()) && Float.isFinite(player.getXRot())
                && Math.abs(net.minecraft.util.Mth.wrapDegrees(player.getYRot() - destination.yaw())) <= 1.0E-4F
                && Math.abs(player.getXRot() - destination.pitch()) <= 1.0E-4F
                && safe(destination.level(), player, actual, Optional.empty());
    }

    private static Optional<Vec3> search(ServerLevel level, Player player, Vec3 preferred,
            BoundingBox search, Optional<BoundingBox> interior) {
        int minY = Math.max(search.minY(), level.getMinY() + 1);
        int maxY = Math.min(search.maxY(), level.getMaxY());
        if (minY > maxY || !level.getWorldBorder().isWithinBounds(BlockPos.containing(preferred))) {
            return Optional.empty();
        }
        loadChunks(level, search);
        if (safe(level, player, preferred, interior)) {
            return Optional.of(preferred);
        }
        return StreamSupport.stream(BlockPos.betweenClosed(search.minX(), minY, search.minZ(),
                        search.maxX(), maxY, search.maxZ()).spliterator(), false)
                .map(position -> DismountHelper.findSafeDismountLocation(EntityType.PLAYER, level, position, true))
                .filter(java.util.Objects::nonNull)
                .filter(position -> position.y() >= minY && position.y() <= maxY)
                .sorted(Comparator.comparingDouble((Vec3 position) -> position.distanceToSqr(preferred))
                        .thenComparingDouble(Vec3::y).thenComparingDouble(Vec3::z).thenComparingDouble(Vec3::x))
                .filter(position -> safe(level, player, position, interior))
                .findFirst();
    }

    private static void loadChunks(ServerLevel level, BoundingBox bounds) {
        // Include the standing player's horizontal footprint, without persistent tickets.
        for (int x = Math.floorDiv(bounds.minX() - 1, 16); x <= Math.floorDiv(bounds.maxX() + 1, 16); x++) {
            for (int z = Math.floorDiv(bounds.minZ() - 1, 16); z <= Math.floorDiv(bounds.maxZ() + 1, 16); z++) {
                level.getChunk(x, z);
            }
        }
    }

    static boolean recoveredArrival(ServerPlayer player, PlayerTransition transition) {
        Vec3 actual = player.position();
        return player.level().dimension().equals(transition.destinationDimension())
                && player.isAlive() && !player.isRemoved() && actual.isFinite()
                && actual.distanceToSqr(transition.destinationPosition()) <= 1.0
                && safe(player.level(), player, actual, Optional.empty());
    }

    private static boolean safe(ServerLevel level, Player player, Vec3 position, Optional<BoundingBox> interior) {
        AABB body = player.getLocalBoundsForPose(Pose.STANDING).move(position);
        if (body.minY < level.getMinY() + 1 || body.maxY > level.getMaxY() + 1
                || !level.getWorldBorder().isWithinBounds(body)
                || interior.filter(bounds -> body.minX < bounds.minX() || body.maxX > bounds.maxX() + 1
                        || body.minY < bounds.minY() || body.maxY > bounds.maxY() + 1
                        || body.minZ < bounds.minZ() || body.maxZ > bounds.maxZ() + 1).isPresent()
                || !level.noCollision(player, body)
                || level.findSupportingBlock(player, body.move(0, -0.0625, 0)).isEmpty()) {
            return false;
        }
        var checked = body.inflate(0.001, 0, 0.001);
        for (var pos : BlockPos.betweenClosed(BlockPos.containing(checked.minX, checked.minY - 0.0625, checked.minZ),
                BlockPos.containing(checked.maxX, checked.maxY - 0.001, checked.maxZ))) {
            var state = level.getBlockState(pos);
            if (!state.getFluidState().isEmpty() || EntityType.PLAYER.isBlockDangerous(state)
                    || state.is(BlockTags.INVALID_SPAWN_INSIDE) || state.is(Blocks.NETHER_PORTAL)
                    || state.is(Blocks.END_PORTAL) || state.is(Blocks.END_GATEWAY)) {
                return false;
            }
        }
        return true;
    }
}
