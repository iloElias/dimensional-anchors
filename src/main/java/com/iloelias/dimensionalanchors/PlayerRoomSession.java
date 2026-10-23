package com.iloelias.dimensionalanchors;

import java.util.Objects;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.UUIDUtil;

public record PlayerRoomSession(UUID anchorId, ExteriorReturn exteriorReturn) {
    public static final Codec<PlayerRoomSession> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.STRING_CODEC.fieldOf("anchor_id").forGetter(PlayerRoomSession::anchorId),
            ExteriorReturn.CODEC.fieldOf("exterior_return").forGetter(PlayerRoomSession::exteriorReturn)
    ).apply(instance, PlayerRoomSession::new));

    public PlayerRoomSession {
        Objects.requireNonNull(anchorId, "anchorId");
        Objects.requireNonNull(exteriorReturn, "exteriorReturn");
    }
}
