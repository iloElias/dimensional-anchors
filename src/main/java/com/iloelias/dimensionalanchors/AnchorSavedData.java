package com.iloelias.dimensionalanchors;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.SavedDataStorage;

public final class AnchorSavedData extends SavedData {
    private static final Identifier ID = Identifier.fromNamespaceAndPath(
            DimensionalAnchors.MODID, "anchors");
    private static final Codec<Map<UUID, AnchorRecord>> RECORDS_CODEC =
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, AnchorRecord.CODEC);
    private static final Codec<AnchorSavedData> RAW_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            RECORDS_CODEC.fieldOf("records").forGetter(data -> data.records)
    ).apply(instance, AnchorSavedData::new));
    private static final Codec<AnchorSavedData> CODEC = SavedDataSupport.strictCodec(RAW_CODEC);

    public static final SavedDataType<AnchorSavedData> TYPE = new SavedDataType<>(
            ID, AnchorSavedData::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private final Map<UUID, AnchorRecord> records;

    private AnchorSavedData() {
        this(new HashMap<>());
    }

    private AnchorSavedData(Map<UUID, AnchorRecord> records) {
        this.records = new HashMap<>(records);
    }

    public static AnchorSavedData get(ServerLevel level) {
        var directory = DimensionType.getStorageFolder(level.dimension(),
                level.getServer().getWorldPath(LevelResource.ROOT)).resolve("data");
        return load(level.getDataStorage(), directory);
    }

    static AnchorSavedData load(SavedDataStorage storage, Path directory) {
        return SavedDataSupport.load(storage, directory, TYPE);
    }

    public Optional<AnchorRecord> get(UUID anchorId) {
        return Optional.ofNullable(records.get(anchorId));
    }

    public Optional<AnchorRecord> findAt(GlobalPos position) {
        return records.values().stream()
                .filter(record -> record.exteriorPosition().filter(position::equals).isPresent())
                .findFirst();
    }

    public void put(AnchorRecord record) {
        records.put(record.anchorId(), record);
        setDirty();
    }

    public Optional<AnchorRecord> remove(UUID anchorId) {
        AnchorRecord removed = records.remove(anchorId);
        if (removed != null) {
            setDirty();
        }
        return Optional.ofNullable(removed);
    }

    public Map<UUID, AnchorRecord> records() {
        return Map.copyOf(records);
    }
}
