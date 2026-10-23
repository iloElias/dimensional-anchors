package com.iloelias.dimensionalanchors;

import java.nio.file.Files;
import java.nio.file.Path;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.SavedDataStorage;

final class SavedDataSupport {
    private SavedDataSupport() {
    }

    static <A> Codec<A> strictCodec(Codec<A> codec) {
        return codec.mapResult(new Codec.ResultFunction<>() {
            @Override
            public <T> DataResult<Pair<A, T>> apply(DynamicOps<T> ops, T input, DataResult<Pair<A, T>> result) {
                // SavedDataStorage accepts partial results; losing reservations or return points is unsafe.
                return result.error().isPresent()
                        ? DataResult.error(() -> result.error().orElseThrow().message()) : result;
            }

            @Override
            public <T> DataResult<T> coApply(DynamicOps<T> ops, A input, DataResult<T> result) {
                return result;
            }
        });
    }

    static <T extends SavedData> T load(SavedDataStorage storage, Path directory, SavedDataType<T> type) {
        T loaded = storage.get(type);
        if (loaded != null) {
            return loaded;
        }
        var file = type.id().withSuffix(".dat").resolveAgainst(directory);
        // Vanilla treats unreadable data as absent; never replace an existing authoritative store.
        if (!Files.notExists(file)) {
            throw new IllegalStateException("Cannot load " + type.id()
                    + "; refusing to replace existing data: " + file);
        }
        return storage.computeIfAbsent(type);
    }
}
