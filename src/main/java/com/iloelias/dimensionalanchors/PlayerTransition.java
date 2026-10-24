package com.iloelias.dimensionalanchors;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Persisted before travel is committed so an interrupted operation can be reconciled after a restart. */
public record PlayerTransition(UUID operationId, Type type, Optional<UUID> sourceRoom, Optional<UUID> targetRoom,
        ResourceKey<Level> destinationDimension,
        Vec3 destinationPosition, float destinationYaw, float destinationPitch, ExteriorReturn exteriorReturn,
        Stage stage) {
    public static final Codec<PlayerTransition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.STRING_CODEC.fieldOf("operation_id").forGetter(PlayerTransition::operationId),
            StringRepresentable.fromEnum(Type::values).fieldOf("type").forGetter(PlayerTransition::type),
            UUIDUtil.STRING_CODEC.optionalFieldOf("source_room").forGetter(PlayerTransition::sourceRoom),
            UUIDUtil.STRING_CODEC.optionalFieldOf("target_room").forGetter(PlayerTransition::targetRoom),
            Level.RESOURCE_KEY_CODEC.fieldOf("destination_dimension").forGetter(PlayerTransition::destinationDimension),
            Vec3.CODEC.fieldOf("destination_position").forGetter(PlayerTransition::destinationPosition),
            Codec.FLOAT.fieldOf("destination_yaw").forGetter(PlayerTransition::destinationYaw),
            Codec.FLOAT.fieldOf("destination_pitch").forGetter(PlayerTransition::destinationPitch),
            ExteriorReturn.CODEC.fieldOf("exterior_return").forGetter(PlayerTransition::exteriorReturn),
            StringRepresentable.fromEnum(Stage::values).fieldOf("stage").forGetter(PlayerTransition::stage)
    ).apply(instance, PlayerTransition::new));

    public PlayerTransition {
        Objects.requireNonNull(operationId, "operationId");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(sourceRoom, "sourceRoom");
        Objects.requireNonNull(targetRoom, "targetRoom");
        if (sourceRoom.isPresent() != (type != Type.ENTRY) || targetRoom.isPresent() != (type != Type.EXIT)) {
            throw new IllegalArgumentException("Transition rooms do not match its type: " + type);
        }
        Objects.requireNonNull(destinationDimension, "destinationDimension");
        Objects.requireNonNull(destinationPosition, "destinationPosition");
        Objects.requireNonNull(exteriorReturn, "exteriorReturn");
        Objects.requireNonNull(stage, "stage");
        if (!destinationPosition.isFinite() || !Float.isFinite(destinationYaw) || !Float.isFinite(destinationPitch)) {
            throw new IllegalArgumentException("Invalid transition destination");
        }
    }

    PlayerTransition withStage(Stage next) {
        return new PlayerTransition(operationId, type, sourceRoom, targetRoom, destinationDimension, destinationPosition,
                destinationYaw, destinationPitch, exteriorReturn, next);
    }

    public enum Type implements StringRepresentable {
        ENTRY,
        ROOM_CHANGE,
        EXIT;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public enum Stage implements StringRepresentable {
        PREPARED,
        TELEPORTED;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }
}
