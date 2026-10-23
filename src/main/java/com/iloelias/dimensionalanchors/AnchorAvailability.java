package com.iloelias.dimensionalanchors;

import java.util.UUID;

import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;

final class AnchorAvailability {
    private AnchorAvailability() {
    }

    static boolean isRegisteredAndPlaced(ServerLevel level, UUID anchorId) {
        var server = level.getServer();
        for (ServerLevel dataLevel : server.getAllLevels()) {
            AnchorRecord record = AnchorSavedData.get(dataLevel).get(anchorId).orElse(null);
            if (record == null || !record.registered() || !record.placed()
                    || record.exteriorPosition().isEmpty()) {
                continue;
            }

            GlobalPos position = record.exteriorPosition().orElseThrow();
            ServerLevel anchorLevel = server.getLevel(position.dimension());
            if (anchorLevel == null || !anchorLevel.dimension().equals(dataLevel.dimension())) {
                continue;
            }
            if (!anchorLevel.hasChunkAt(position.pos())) {
                return true;
            }

            var state = anchorLevel.getBlockState(position.pos());
            if (state.getBlock() instanceof AnchorBlock anchorBlock && anchorBlock.tier() == record.tier()) {
                return true;
            }
        }
        return false;
    }
}
