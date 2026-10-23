package com.iloelias.dimensionalanchors;

import java.util.Objects;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public record ExteriorReturn(ResourceKey<Level> dimension, Vec3 position, float yaw, float pitch) {
    private static final Codec<ResourceKey<Level>> DIMENSION_CODEC = Level.RESOURCE_KEY_CODEC.validate(
            value -> value.equals(RoomGeneration.POCKET)
                    ? DataResult.error(() -> "Exterior return cannot be inside the pocket dimension")
                    : DataResult.success(value));
    private static final Codec<Vec3> POSITION_CODEC = Vec3.CODEC.validate(value -> validPosition(value)
            ? DataResult.success(value) : DataResult.error(() -> "Invalid exterior return coordinates"));
    private static final Codec<Float> YAW_CODEC = Codec.FLOAT.validate(value -> Float.isFinite(value)
            ? DataResult.success(value) : DataResult.error(() -> "Non-finite return yaw"));
    private static final Codec<Float> PITCH_CODEC = Codec.FLOAT.validate(
            value -> Float.isFinite(value) && value >= -90.0F && value <= 90.0F
                    ? DataResult.success(value) : DataResult.error(() -> "Invalid return pitch"));
    public static final Codec<ExteriorReturn> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            DIMENSION_CODEC.fieldOf("dimension").forGetter(ExteriorReturn::dimension),
            POSITION_CODEC.fieldOf("position").forGetter(ExteriorReturn::position),
            YAW_CODEC.fieldOf("yaw").forGetter(ExteriorReturn::yaw),
            PITCH_CODEC.fieldOf("pitch").forGetter(ExteriorReturn::pitch)
    ).apply(instance, ExteriorReturn::new));

    public ExteriorReturn {
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(position, "position");
        if (dimension.equals(RoomGeneration.POCKET) || !validPosition(position)
                || !Float.isFinite(yaw) || !Float.isFinite(pitch) || pitch < -90.0F || pitch > 90.0F) {
            throw new IllegalArgumentException("Invalid exterior return destination");
        }
    }

    private static boolean validPosition(Vec3 position) {
        return Double.isFinite(position.x()) && Double.isFinite(position.y()) && Double.isFinite(position.z())
                && Level.isInSpawnableBounds(BlockPos.containing(position));
    }
}
