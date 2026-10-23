package com.iloelias.dimensionalanchors;

import java.util.UUID;

import com.mojang.serialization.Codec;

import net.minecraft.core.UUIDUtil;

public record RegisteredKeyData(UUID anchorId) {
    public static final Codec<RegisteredKeyData> CODEC = UUIDUtil.STRING_CODEC.xmap(
            RegisteredKeyData::new,
            RegisteredKeyData::anchorId);
}
