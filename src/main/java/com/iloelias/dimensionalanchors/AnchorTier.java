package com.iloelias.dimensionalanchors;

import net.minecraft.util.StringRepresentable;

public enum AnchorTier implements StringRepresentable {
    COPPER("copper"),
    IRON("iron"),
    GOLD("gold"),
    DIAMOND("diamond"),
    NETHERITE("netherite");

    public static final EnumCodec<AnchorTier> CODEC = StringRepresentable.fromEnum(AnchorTier::values);

    private final String serializedName;

    AnchorTier(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
