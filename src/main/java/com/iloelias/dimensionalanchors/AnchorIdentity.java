package com.iloelias.dimensionalanchors;

import java.util.Optional;
import java.util.UUID;

import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;

final class AnchorIdentity {
    private AnchorIdentity() {
    }

    static Optional<AnchorRecord> find(ServerLevel level, UUID id) {
        AnchorTier tier = null;
        boolean registered = false;
        GlobalPos placedAt = null;
        for (var candidate : level.getServer().getAllLevels()) {
            var record = AnchorSavedData.get(candidate).get(id).orElse(null);
            if (record == null) {
                continue;
            }
            if (!record.anchorId().equals(id) || record.dataVersion() != AnchorRecord.CURRENT_DATA_VERSION
                    || record.placed() != record.exteriorPosition().isPresent()
                    || record.exteriorPosition().filter(pos -> !pos.dimension().equals(candidate.dimension())).isPresent()) {
                throw new IllegalStateException("Inconsistent anchor record: " + id);
            }
            if (record.placed()) {
                if (placedAt != null) {
                    throw new IllegalStateException("Multiple placed records for anchor: " + id);
                }
                placedAt = record.exteriorPosition().orElseThrow();
            }
            if (tier == null || record.tier().ordinal() > tier.ordinal()) {
                tier = record.tier();
            }
            registered |= record.registered();
        }
        if (tier == null) {
            return Optional.empty();
        }
        var room = RoomAllocationSavedData.get(level).room(id).orElse(null);
        if (room != null) {
            if (!registered) {
                throw new IllegalStateException("Generated room has no registered anchor: " + id);
            }
            if (room.tier().ordinal() > tier.ordinal()) {
                tier = room.tier();
            }
        }
        return Optional.of(new AnchorRecord(id, tier, registered, placedAt != null,
                Optional.ofNullable(placedAt), AnchorRecord.CURRENT_DATA_VERSION));
    }

    static void store(ServerLevel level, AnchorRecord record) {
        for (var candidate : level.getServer().getAllLevels()) {
            if (candidate != level) {
                AnchorSavedData.get(candidate).remove(record.anchorId());
            }
        }
        AnchorSavedData.get(level).put(record);
    }
}
