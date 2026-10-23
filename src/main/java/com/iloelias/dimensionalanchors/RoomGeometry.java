package com.iloelias.dimensionalanchors;

import java.util.Objects;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public record RoomGeometry(int slot, AnchorTier tier) {
    public static final int GRID_WIDTH = 64;
    public static final int CAPACITY = GRID_WIDTH * GRID_WIDTH;
    public static final int CELL_SIZE = 32;
    public static final int FLOOR_Y = 63;

    public RoomGeometry {
        if (slot < 0 || slot >= CAPACITY) {
            throw new IllegalArgumentException("Room slot outside allocation capacity: " + slot);
        }
        Objects.requireNonNull(tier, "tier");
    }

    public int interiorSize() {
        return switch (tier) {
            case COPPER -> 3;
            case IRON -> 5;
            case GOLD -> 7;
            case DIAMOND -> 10;
            case NETHERITE -> 16;
        };
    }

    public BlockPos reference() {
        return new BlockPos((slot % GRID_WIDTH) * CELL_SIZE + CELL_SIZE / 2,
                FLOOR_Y + 1, (slot / GRID_WIDTH) * CELL_SIZE + CELL_SIZE / 2);
    }

    public BoundingBox interior() {
        BlockPos reference = reference();
        int size = interiorSize();
        // Even widths extend one extra block north/west; upgrades never move the reference.
        int minX = reference.getX() - size / 2;
        int minZ = reference.getZ() - size / 2;
        return new BoundingBox(minX, FLOOR_Y + 1, minZ,
                minX + size - 1, FLOOR_Y + size, minZ + size - 1);
    }

    public BoundingBox shell() {
        return interior().inflatedBy(1);
    }

    public BoundingBox reservation() {
        return new RoomGeometry(slot, AnchorTier.NETHERITE).shell();
    }

    public BlockPos entryFeet() {
        return new BlockPos(reference().getX(), FLOOR_Y + 1, interior().maxZ());
    }

    public BlockPos exitControl() {
        return new BlockPos(reference().getX(), FLOOR_Y + 2, shell().maxZ());
    }
}
