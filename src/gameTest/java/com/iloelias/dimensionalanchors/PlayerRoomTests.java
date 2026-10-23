package com.iloelias.dimensionalanchors;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.UUID;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.SavedDataStorage;
import net.minecraft.world.phys.Vec3;

final class PlayerRoomTests {
    static void travelSnapshots(GameTestHelper helper) {
        var data = emptyData();
        UUID player = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        UUID room = UUID.randomUUID();
        var exterior = new ExteriorReturn(Level.OVERWORLD, new Vec3(2.5, 70, 4.5), 0, 0);
        data.enter(player, room, exterior);
        var original = data.find(player).orElseThrow();
        data.setDirty(false);
        helper.assertTrue(data.beginTravel(player) && !data.beginTravel(player) && data.beginTravel(other),
                "Travel guard did not isolate players or reject reentry");
        helper.assertTrue(!data.isDirty(), "Transient travel guard dirtied saved data");
        var restored = reload(data);
        helper.assertTrue(restored.beginTravel(player), "Travel guard was serialized across reload");
        restored.endTravel(player);
        data.endTravel(player);
        data.endTravel(other);
        helper.assertTrue(data.beginTravel(player), "Travel guard did not release");
        data.endTravel(player);

        expectStateFailure(helper, () -> data.completeExitIfUnchanged(player,
                new PlayerRoomSession(room, exterior)));
        helper.assertTrue(data.find(player).orElseThrow() == original && !data.isDirty(),
                "A value-equal snapshot was mistaken for the captured session");
        data.changeRoom(player, UUID.randomUUID());
        data.changeRoom(player, room);
        data.setDirty(false);
        expectStateFailure(helper, () -> data.completeExitIfUnchanged(player, original));
        helper.assertTrue(!data.isDirty() && data.find(player).orElseThrow().equals(original),
                "ABA session check changed or removed the replacement");
        data.completeExitIfUnchanged(player, data.find(player).orElseThrow());
        helper.assertTrue(data.find(player).isEmpty() && data.isDirty(),
                "Matching captured-session completion failed");
    }

    static void lifecycle(GameTestHelper helper) {
        var data = emptyData();
        UUID player = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        UUID firstRoom = UUID.randomUUID();
        UUID secondRoom = UUID.randomUUID();
        var exterior = new ExteriorReturn(Level.NETHER, new Vec3(-12.75, 70.125, 3.5), 123.5F, -17.25F);
        var otherExterior = new ExteriorReturn(Level.OVERWORLD, new Vec3(5.5, 65, -8.25), -45, 0);
        helper.assertTrue(data.find(player).isEmpty(), "Fresh player has an unexpected session");
        data.enter(player, firstRoom, exterior);
        data.enter(other, secondRoom, otherExterior);
        helper.assertTrue(data.isDirty(), "Entry did not mark sessions dirty");
        data.setDirty(false);
        var initial = data;
        expectStateFailure(helper, () -> initial.enter(player, secondRoom, otherExterior));
        helper.assertTrue(!data.isDirty() && data.find(player).orElseThrow().exteriorReturn().equals(exterior),
                "Duplicate entry overwrote the exterior return");
        data.changeRoom(player, firstRoom);
        helper.assertTrue(!data.isDirty(), "Repeated current-room selection changed the session");
        data.changeRoom(player, secondRoom);
        helper.assertTrue(data.isDirty() && data.find(player).orElseThrow()
                .equals(new PlayerRoomSession(secondRoom, exterior)), "Room-to-room travel lost exterior return");
        helper.assertTrue(data.find(other).orElseThrow().exteriorReturn().equals(otherExterior),
                "Transition changed another player's return");
        data = reload(data);
        helper.assertTrue(data.find(player).orElseThrow().equals(new PlayerRoomSession(secondRoom, exterior)),
                "Codec reload lost current room or exact exterior coordinates/orientation");
        var restored = data;
        data.setDirty(false);
        expectStateFailure(helper, () -> restored.completeExit(player, firstRoom));
        helper.assertTrue(!data.isDirty() && data.find(player).isPresent(),
                "Stale exit removed the current session");
        data.completeExit(player, secondRoom);
        helper.assertTrue(data.isDirty() && data.find(player).isEmpty()
                && data.find(other).isPresent(), "Successful exit did not isolate session removal");
        expectStateFailure(helper, () -> restored.changeRoom(player, firstRoom));
        expectStateFailure(helper, () -> restored.completeExit(player, secondRoom));
        data.enter(player, firstRoom, otherExterior);
        helper.assertTrue(data.find(player).orElseThrow().exteriorReturn().equals(otherExterior),
                "New exterior entry after exit kept a stale return");
        var global = PlayerRoomSavedData.get(helper.getLevel());
        for (var level : helper.getLevel().getServer().getAllLevels()) {
            helper.assertTrue(global == PlayerRoomSavedData.get(level), "Session store is not server-global");
        }
    }

    static void invalidData(GameTestHelper helper) {
        var data = emptyData();
        UUID player = UUID.randomUUID();
        data.enter(player, UUID.randomUUID(), new ExteriorReturn(Level.END, new Vec3(1.5, 64, -2.25), 90, 45));
        var valid = encode(data);
        for (int scenario = 0; scenario < 11; scenario++) {
            var invalid = valid.deepCopy();
            var sessions = invalid.getAsJsonObject("sessions");
            var session = sessions.getAsJsonObject(player.toString());
            var destination = session.getAsJsonObject("exterior_return");
            switch (scenario) {
                case 0 -> invalid.addProperty("data_version", 2);
                case 1 -> invalid.remove("sessions");
                case 2 -> sessions.add("invalid-player-uuid", session.deepCopy());
                case 3 -> session.addProperty("anchor_id", "invalid-room-uuid");
                case 4 -> destination.addProperty("dimension", RoomGeneration.POCKET.identifier().toString());
                case 5 -> destination.remove("position");
                case 6 -> destination.getAsJsonArray("position").set(0, new com.google.gson.JsonPrimitive(30000000));
                case 7 -> destination.getAsJsonArray("position").set(1, new com.google.gson.JsonPrimitive(Double.NaN));
                case 8 -> destination.addProperty("yaw", Float.POSITIVE_INFINITY);
                case 9 -> destination.addProperty("pitch", Float.NaN);
                case 10 -> destination.addProperty("pitch", 91);
                default -> throw new IllegalStateException("Unhandled malformed session case");
            }
            var decoded = PlayerRoomSavedData.CODEC.parse(JsonOps.INSTANCE, invalid);
            helper.assertTrue(decoded.error().isPresent() && decoded.resultOrPartial(error -> {}).isEmpty(),
                    "Invalid session data accepted or partially recovered: " + scenario);
        }
        boolean rejected = false;
        try {
            new ExteriorReturn(RoomGeneration.POCKET, Vec3.ZERO, 0, 0);
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        helper.assertTrue(rejected, "Programmatic room return destination was accepted");
        helper.assertTrue(encode(data).equals(valid), "Malformed-data checks changed valid sessions");
    }

    static void disk(GameTestHelper helper) {
        try {
            var directory = Files.createTempDirectory("dimensionalanchors-player-rooms-");
            var file = PlayerRoomSavedData.TYPE.id().withSuffix(".dat").resolveAgainst(directory);
            var level = helper.getLevel();
            UUID player = UUID.randomUUID();
            UUID other = UUID.randomUUID();
            UUID roomA = UUID.randomUUID();
            UUID roomB = UUID.randomUUID();
            var exterior = new ExteriorReturn(Level.NETHER, new Vec3(-5.25, 70.5, 19.75), 160, -30);
            try {
                try (var storage = new SavedDataStorage(level, directory, DataFixers.getDataFixer(), level.registryAccess())) {
                    var data = PlayerRoomSavedData.load(storage, directory);
                    data.enter(player, roomA, exterior);
                    data.enter(other, roomA, exterior);
                    data.changeRoom(player, roomB);
                    storage.saveAndJoin();
                }
                try (var storage = new SavedDataStorage(level, directory, DataFixers.getDataFixer(), level.registryAccess())) {
                    var data = PlayerRoomSavedData.load(storage, directory);
                    helper.assertTrue(data.find(player).orElseThrow().equals(new PlayerRoomSession(roomB, exterior))
                            && data.find(other).orElseThrow().anchorId().equals(roomA),
                            "Disk reload lost per-player room or original return");
                    data.completeExit(player, roomB);
                    storage.saveAndJoin();
                }
                try (var storage = new SavedDataStorage(level, directory, DataFixers.getDataFixer(), level.registryAccess())) {
                    var data = PlayerRoomSavedData.load(storage, directory);
                    helper.assertTrue(data.find(player).isEmpty() && data.find(other).isPresent(),
                            "Exited session returned on disk reload or another occupant was removed");
                }
                var valid = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
                for (int scenario = 0; scenario < 3; scenario++) {
                    var invalid = valid.copy();
                    var content = invalid.getCompound("data").orElseThrow();
                    if (scenario == 0) {
                        content.putInt("data_version", 2);
                    } else if (scenario == 1) {
                        var sessions = content.getCompound("sessions").orElseThrow();
                        sessions.put("invalid-player-uuid", sessions.getCompound(other.toString()).orElseThrow().copy());
                    }
                    if (scenario == 2) {
                        Files.writeString(file, "deliberately corrupt session data");
                    } else {
                        NbtIo.writeCompressed(invalid, file);
                    }
                    byte[] before = Files.readAllBytes(file);
                    try (var storage = new SavedDataStorage(level, directory, DataFixers.getDataFixer(), level.registryAccess())) {
                        expectStateFailure(helper, () -> PlayerRoomSavedData.load(storage, directory));
                    }
                    helper.assertTrue(Arrays.equals(before, Files.readAllBytes(file)),
                            "Unreadable/future/partial player sessions were overwritten");
                }
            } finally {
                Files.deleteIfExists(file);
                Files.deleteIfExists(file.getParent());
                Files.delete(directory);
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("Player room disk regression failed", exception);
        }
    }

    private static PlayerRoomSavedData emptyData() {
        var json = new JsonObject();
        json.addProperty("data_version", 1);
        json.add("sessions", new JsonObject());
        return PlayerRoomSavedData.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
    }

    private static JsonObject encode(PlayerRoomSavedData data) {
        return PlayerRoomSavedData.CODEC.encodeStart(JsonOps.INSTANCE, data).getOrThrow().getAsJsonObject();
    }

    private static PlayerRoomSavedData reload(PlayerRoomSavedData data) {
        JsonElement encoded = encode(data);
        return PlayerRoomSavedData.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
    }

    private static void expectStateFailure(GameTestHelper helper, Runnable action) {
        boolean rejected = false;
        try {
            action.run();
        } catch (IllegalStateException expected) {
            rejected = true;
        }
        helper.assertTrue(rejected, "Invalid session operation was accepted");
    }
}
