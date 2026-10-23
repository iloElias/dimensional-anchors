package com.iloelias.dimensionalanchors;

import java.util.Optional;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;

public record AnchorRecord(
        UUID anchorId,
        AnchorTier tier,
        boolean registered,
        boolean placed,
        Optional<GlobalPos> exteriorPosition,
        int dataVersion) {
    public static final int CURRENT_DATA_VERSION = 1;

    public static final Codec<AnchorRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.STRING_CODEC.fieldOf("anchor_id").forGetter(AnchorRecord::anchorId),
            AnchorTier.CODEC.fieldOf("tier").forGetter(AnchorRecord::tier),
            Codec.BOOL.fieldOf("registered").forGetter(AnchorRecord::registered),
            Codec.BOOL.fieldOf("placed").forGetter(AnchorRecord::placed),
            GlobalPos.CODEC.optionalFieldOf("exterior_position")
                    .forGetter(AnchorRecord::exteriorPosition),
            Codec.INT.fieldOf("data_version").forGetter(AnchorRecord::dataVersion)
    ).apply(instance, AnchorRecord::new));

    public static AnchorRecord unplaced(UUID anchorId, AnchorTier tier, boolean registered) {
        return new AnchorRecord(anchorId, tier, registered, false, Optional.empty(), CURRENT_DATA_VERSION);
    }

    public AnchorRecord placedAt(GlobalPos position) {
        return new AnchorRecord(anchorId, tier, registered, true, Optional.of(position), dataVersion);
    }

    public AnchorRecord removedFromWorld() {
        return new AnchorRecord(anchorId, tier, registered, false, Optional.empty(), dataVersion);
    }

    public AnchorRecord withTier(AnchorTier newTier) {
        return new AnchorRecord(anchorId, newTier, registered, placed, exteriorPosition, dataVersion);
    }

    public AnchorRecord withRegistered(boolean newRegistered) {
        return new AnchorRecord(anchorId, tier, newRegistered, placed, exteriorPosition, dataVersion);
    }
}
