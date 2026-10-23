package com.iloelias.dimensionalanchors;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.SavedDataStorage;

public final class RoomAllocationSavedData extends SavedData {
    private static final int DATA_VERSION = 2;
    private static final Codec<Map<UUID, Integer>> SLOTS_CODEC =
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.intRange(0, RoomGeometry.CAPACITY - 1))
                    .flatXmap(RoomAllocationSavedData::validateSlots, RoomAllocationSavedData::validateSlots);
    private static final Codec<RoomAllocationSavedData> CURRENT_CODEC = RecordCodecBuilder.<RoomAllocationSavedData>create(instance -> instance.group(
            Codec.intRange(DATA_VERSION, DATA_VERSION).fieldOf("data_version").forGetter(data -> DATA_VERSION),
            SLOTS_CODEC.fieldOf("slots").forGetter(data -> data.slots),
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, RoomGenerationState.CODEC)
                    .fieldOf("rooms").forGetter(data -> data.rooms)
    ).apply(instance, (version, slots, rooms) -> new RoomAllocationSavedData(slots, rooms)))
            .validate(data -> data.slots.keySet().containsAll(data.rooms.keySet())
                    ? DataResult.success(data)
                    : DataResult.error(() -> "Generated rooms must have permanent reservations"));
    private static final Codec<RoomAllocationSavedData> LEGACY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(1, 1).fieldOf("data_version").forGetter(data -> 1),
            SLOTS_CODEC.fieldOf("slots").forGetter(data -> data.slots)
    ).apply(instance, (version, slots) -> new RoomAllocationSavedData(slots, Map.of())));
    private static final Codec<RoomAllocationSavedData> RAW_CODEC = Codec.either(CURRENT_CODEC, LEGACY_CODEC)
            .xmap(value -> value.map(data -> data, data -> data), Either::left);
    public static final Codec<RoomAllocationSavedData> CODEC = RAW_CODEC.mapResult(new Codec.ResultFunction<>() {
        @Override
        public <T> DataResult<Pair<RoomAllocationSavedData, T>> apply(DynamicOps<T> ops, T input,
                DataResult<Pair<RoomAllocationSavedData, T>> result) {
            // SavedDataStorage accepts partial decodes; dropped reservations could cause overlap.
            return result.error().isPresent()
                    ? DataResult.error(() -> result.error().orElseThrow().message()) : result;
        }

        @Override
        public <T> DataResult<T> coApply(DynamicOps<T> ops, RoomAllocationSavedData input, DataResult<T> result) {
            return result;
        }
    });
    public static final SavedDataType<RoomAllocationSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(DimensionalAnchors.MODID, "room_allocations"),
            RoomAllocationSavedData::new, CODEC);

    private final Map<UUID, Integer> slots;
    private final Map<UUID, RoomGenerationState> rooms;

    private RoomAllocationSavedData() {
        this(Map.of(), Map.of());
    }

    private RoomAllocationSavedData(Map<UUID, Integer> slots, Map<UUID, RoomGenerationState> rooms) {
        this.slots = new HashMap<>(slots);
        this.rooms = new HashMap<>(rooms);
    }

    public static RoomAllocationSavedData get(ServerLevel level) {
        var server = level.getServer();
        return load(server.overworld().getDataStorage(), server.getWorldPath(LevelResource.DATA));
    }

    static RoomAllocationSavedData load(SavedDataStorage storage, Path dataDirectory) {
        RoomAllocationSavedData loaded = storage.get(TYPE);
        if (loaded != null) {
            return loaded;
        }
        var file = TYPE.id().withSuffix(".dat").resolveAgainst(dataDirectory);
        // Vanilla treats unreadable saved data as absent; never reset permanent reservations.
        if (!Files.notExists(file)) {
            throw new IllegalStateException("Cannot load room allocations; refusing to replace existing data: " + file);
        }
        return storage.computeIfAbsent(TYPE);
    }

    public OptionalInt find(UUID anchorId) {
        Integer slot = slots.get(Objects.requireNonNull(anchorId, "anchorId"));
        return slot == null ? OptionalInt.empty() : OptionalInt.of(slot);
    }

    public int reserve(UUID anchorId) {
        Objects.requireNonNull(anchorId, "anchorId");
        Integer existing = slots.get(anchorId);
        if (existing != null) {
            return existing;
        }
        if (slots.size() >= RoomGeometry.CAPACITY) {
            throw new IllegalStateException("Room allocation capacity exhausted: " + RoomGeometry.CAPACITY);
        }
        int slot = slots.size();
        slots.put(anchorId, slot);
        setDirty();
        return slot;
    }

    public Map<UUID, Integer> reservations() {
        return Map.copyOf(slots);
    }

    public Optional<RoomGenerationState> room(UUID anchorId) {
        return Optional.ofNullable(rooms.get(anchorId));
    }

    void putRoom(UUID anchorId, RoomGenerationState state) {
        if (!slots.containsKey(anchorId)) {
            throw new IllegalStateException("Cannot generate an unreserved room: " + anchorId);
        }
        rooms.put(anchorId, Objects.requireNonNull(state, "state"));
        setDirty();
    }

    boolean protects(BlockPos position) {
        int x = Math.floorDiv(position.getX(), RoomGeometry.CELL_SIZE);
        int z = Math.floorDiv(position.getZ(), RoomGeometry.CELL_SIZE);
        if (x < 0 || x >= RoomGeometry.GRID_WIDTH || z < 0 || z >= RoomGeometry.GRID_WIDTH) {
            return false;
        }
        int slot = z * RoomGeometry.GRID_WIDTH + x;
        for (var entry : rooms.entrySet()) {
            if (slots.get(entry.getKey()) == slot && entry.getValue().protects(slot, position)) {
                return true;
            }
        }
        return false;
    }

    private static DataResult<Map<UUID, Integer>> validateSlots(Map<UUID, Integer> slots) {
        var occupied = new HashSet<>(slots.values());
        if (slots.size() > RoomGeometry.CAPACITY || occupied.size() != slots.size()) {
            return DataResult.error(() -> "Room allocations exceed capacity or share a slot");
        }
        for (int slot = 0; slot < slots.size(); slot++) {
            if (!occupied.contains(slot)) {
                return DataResult.error(() -> "Room allocation slots must be contiguous and never reclaimed");
            }
        }
        return DataResult.success(slots);
    }
}
