package com.iloelias.dimensionalanchors;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.SavedDataStorage;

final class RoomAllocationTests {
    static void geometry(GameTestHelper helper) {
        var regions = new ArrayList<BoundingBox>();
        int[] sizes = {3, 5, 7, 10, 16};
        for (int slot = 0; slot < RoomGeometry.CAPACITY; slot++) {
            BoundingBox previous = null;
            for (AnchorTier tier : AnchorTier.values()) {
                var geometry = new RoomGeometry(slot, tier);
                var interior = geometry.interior();
                int size = sizes[tier.ordinal()];
                helper.assertTrue(interior.getXSpan() == size && interior.getYSpan() == size
                        && interior.getZSpan() == size, "Incorrect interior dimensions");
                helper.assertTrue(interior.minY() == RoomGeometry.FLOOR_Y + 1, "Floor moved on upgrade");
                var shell = geometry.shell();
                helper.assertTrue(geometry.reservation().isInside(
                        shell.minX(), shell.minY(), shell.minZ())
                        && geometry.reservation().isInside(shell.maxX(), shell.maxY(), shell.maxZ()),
                        "Tier escaped maximum reservation");
                if (previous != null) {
                    helper.assertTrue(interior.isInside(previous.minX(), previous.minY(), previous.minZ())
                            && interior.isInside(previous.maxX(), previous.maxY(), previous.maxZ()),
                            "Upgrade moved existing usable coordinates outside the interior");
                }
                helper.assertTrue(interior.minX() == geometry.reference().getX() - size / 2
                        && interior.minZ() == geometry.reference().getZ() - size / 2,
                        "Horizontal rounding changed");
                var feet = geometry.entryFeet();
                var control = geometry.exitControl();
                helper.assertTrue(interior.isInside(feet) && interior.isInside(feet.above()),
                        "Default entry lacks standing space");
                helper.assertTrue(control.getX() == feet.getX() && control.getZ() == feet.getZ() + 1
                        && control.getY() == feet.getY() + 1 && shell.isInside(control)
                        && !interior.isInside(control), "Control is not in the south wall behind entry");
                helper.assertTrue(shell.getXSpan() == size + RoomGeometry.BOUNDARY_THICKNESS * 2
                        && shell.getYSpan() == size + RoomGeometry.BOUNDARY_THICKNESS * 2
                        && shell.getZSpan() == size + RoomGeometry.BOUNDARY_THICKNESS * 2,
                        "Boundary thickness changed");
                previous = interior;
            }
            regions.add(new RoomGeometry(slot, AnchorTier.NETHERITE).reservation());
        }
        for (int left = 0; left < regions.size(); left++) {
            for (int right = left + 1; right < regions.size(); right++) {
                helper.assertTrue(!regions.get(left).intersects(regions.get(right)), "Room reservations overlap");
            }
        }
        for (int slot : new int[]{-1, RoomGeometry.CAPACITY}) {
            boolean rejected = false;
            try {
                new RoomGeometry(slot, AnchorTier.COPPER);
            } catch (IllegalArgumentException expected) {
                rejected = true;
            }
            helper.assertTrue(rejected, "Out-of-capacity geometry accepted");
        }
    }

    static void allocation(GameTestHelper helper) {
        var data = emptyData();
        UUID first = UUID.randomUUID();
        helper.assertTrue(data.find(first).isEmpty(), "Unknown anchor already reserved");
        helper.assertTrue(data.reserve(first) == 0 && data.isDirty(), "First reservation was not persisted");
        data.setDirty(false);
        helper.assertTrue(data.reserve(first) == 0 && !data.isDirty(), "Repeated access allocated again");
        for (int slot = 1; slot < RoomGeometry.CAPACITY; slot++) {
            helper.assertTrue(data.reserve(new UUID(0, slot)) == slot, "Allocation skipped or reused a slot");
        }
        var json = RoomAllocationSavedData.CODEC.encodeStart(JsonOps.INSTANCE, data).getOrThrow();
        helper.assertTrue(json.getAsJsonObject().get("data_version").getAsInt() == 2
                && json.getAsJsonObject().has("rooms"), "Legacy allocations did not migrate to schema 2");
        var reloaded = RoomAllocationSavedData.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        helper.assertTrue(reloaded.reservations().equals(data.reservations()), "Reload changed reservations");
        helper.assertTrue(reloaded.reserve(first) == 0, "Existing room rejected at capacity after reload");
        boolean rejected = false;
        try {
            reloaded.reserve(UUID.randomUUID());
        } catch (IllegalStateException expected) {
            rejected = true;
        }
        helper.assertTrue(rejected && reloaded.reservations().size() == RoomGeometry.CAPACITY,
                "Exhaustion failed to reject the new room without modifying existing reservations");
        var partial = emptyData();
        partial.reserve(first);
        var restored = RoomAllocationSavedData.CODEC.parse(JsonOps.INSTANCE,
                RoomAllocationSavedData.CODEC.encodeStart(JsonOps.INSTANCE, partial).getOrThrow()).getOrThrow();
        helper.assertTrue(restored.reserve(UUID.randomUUID()) == 1, "Reload lost the next free slot");

        var global = RoomAllocationSavedData.get(helper.getLevel());
        helper.assertTrue(global == RoomAllocationSavedData.get(helper.getLevel().getServer().overworld()),
                "Allocations are not server-global");
        for (var level : helper.getLevel().getServer().getAllLevels()) {
            helper.assertTrue(global == RoomAllocationSavedData.get(level), "Dimension has an independent allocator");
        }
    }

    static void invalidData(GameTestHelper helper) {
        String first = UUID.randomUUID().toString();
        String second = UUID.randomUUID().toString();
        for (var slots : java.util.List.of(Map.of(first, -1), Map.of(first, RoomGeometry.CAPACITY),
                Map.of(first, 1), Map.of(first, 0, second, 0), Map.of("invalid-uuid", 0))) {
            var json = new JsonObject();
            json.addProperty("data_version", 1);
            var entries = new JsonObject();
            slots.forEach(entries::addProperty);
            json.add("slots", entries);
            var decoded = RoomAllocationSavedData.CODEC.parse(JsonOps.INSTANCE, json);
            helper.assertTrue(decoded.error().isPresent() && decoded.resultOrPartial(error -> {}).isEmpty(),
                    "Malformed allocation data accepted: " + json);
        }
        for (int version : new int[]{0, 3}) {
            var json = new JsonObject();
            json.addProperty("data_version", version);
            json.add("slots", new JsonObject());
            helper.assertTrue(RoomAllocationSavedData.CODEC.parse(JsonOps.INSTANCE, json).error().isPresent(),
                    "Unsupported allocation version accepted");
        }
        var valid = emptyData();
        UUID id = UUID.randomUUID();
        valid.reserve(id);
        valid.putRoom(id, new RoomGenerationState(AnchorTier.COPPER, java.util.Optional.empty(), true));
        var encoded = RoomAllocationSavedData.CODEC.encodeStart(JsonOps.INSTANCE, valid)
                .getOrThrow().getAsJsonObject();
        for (int scenario = 0; scenario < 5; scenario++) {
            var invalid = encoded.deepCopy();
            if (scenario == 0) {
                invalid.remove("rooms");
            } else if (scenario == 1) {
                invalid.getAsJsonObject("rooms").add(UUID.randomUUID().toString(),
                        invalid.getAsJsonObject("rooms").get(id.toString()).deepCopy());
            } else {
                var room = invalid.getAsJsonObject("rooms").getAsJsonObject(id.toString());
                if (scenario == 2) {
                    room.addProperty("tier", "invalid");
                } else if (scenario == 3) {
                    room.addProperty("previous_tier", "copper");
                } else {
                    room.addProperty("complete", false);
                    room.addProperty("previous_tier", "netherite");
                }
            }
            var decoded = RoomAllocationSavedData.CODEC.parse(JsonOps.INSTANCE, invalid);
            helper.assertTrue(decoded.error().isPresent() && decoded.resultOrPartial(error -> {}).isEmpty(),
                    "Invalid generation data accepted or partially recovered");
        }
    }

    static void disk(GameTestHelper helper) {
        try {
            var directory = Files.createTempDirectory("dimensionalanchors-allocations-");
            var file = RoomAllocationSavedData.TYPE.id().withSuffix(".dat").resolveAgainst(directory);
            var level = helper.getLevel();
            UUID first = UUID.randomUUID();
            UUID second = UUID.randomUUID();
            try {
                try (var storage = new SavedDataStorage(level, directory,
                        DataFixers.getDataFixer(), level.registryAccess())) {
                    var data = RoomAllocationSavedData.load(storage, directory);
                    helper.assertTrue(data.reserve(first) == 0 && data.reserve(second) == 1,
                            "Fresh disk allocations failed");
                    data.putRoom(first, new RoomGenerationState(AnchorTier.IRON,
                            java.util.Optional.of(AnchorTier.COPPER), false));
                    data.putRoom(second, new RoomGenerationState(AnchorTier.NETHERITE,
                            java.util.Optional.empty(), true));
                    storage.saveAndJoin();
                }
                helper.assertTrue(Files.isRegularFile(file), "Allocation file was not written");
                try (var storage = new SavedDataStorage(level, directory,
                        DataFixers.getDataFixer(), level.registryAccess())) {
                    var data = RoomAllocationSavedData.load(storage, directory);
                    helper.assertTrue(data.find(first).orElseThrow() == 0
                            && data.find(second).orElseThrow() == 1, "Disk reload changed room mappings");
                    helper.assertTrue(!data.room(first).orElseThrow().complete()
                            && data.room(first).orElseThrow().previousTier().orElseThrow() == AnchorTier.COPPER
                            && data.room(second).orElseThrow().complete()
                            && data.protects(new RoomGeometry(0, AnchorTier.COPPER).exitControl())
                            && data.protects(new RoomGeometry(1, AnchorTier.NETHERITE).exitControl()),
                            "Disk reload lost pending/completed generation or boundary provenance");
                    helper.assertTrue(data.reserve(UUID.randomUUID()) == 2, "Disk reload lost next slot");
                    storage.saveAndJoin();
                }
                var tag = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
                tag.getCompound("data").orElseThrow().putInt("data_version", 3);
                NbtIo.writeCompressed(tag, file);
                for (int scenario = 0; scenario < 3; scenario++) {
                    if (scenario == 1) {
                        var dataTag = tag.getCompound("data").orElseThrow();
                        dataTag.putInt("data_version", 1);
                        dataTag.getCompound("slots").orElseThrow().putInt("invalid-uuid", 3);
                        NbtIo.writeCompressed(tag, file);
                    } else if (scenario == 2) {
                        Files.writeString(file, "deliberately corrupt allocation data");
                    }
                    byte[] before = Files.readAllBytes(file);
                    boolean rejected = false;
                    try (var storage = new SavedDataStorage(level, directory,
                            DataFixers.getDataFixer(), level.registryAccess())) {
                        try {
                            RoomAllocationSavedData.load(storage, directory);
                        } catch (IllegalStateException expected) {
                            rejected = true;
                        }
                    }
                    helper.assertTrue(rejected && java.util.Arrays.equals(before, Files.readAllBytes(file)),
                            "Unreadable/future allocation data was reset or overwritten");
                }
            } finally {
                Files.deleteIfExists(file);
                Files.deleteIfExists(file.getParent());
                Files.delete(directory);
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("Allocation disk regression failed", exception);
        }
    }

    private static RoomAllocationSavedData emptyData() {
        var json = new JsonObject();
        json.addProperty("data_version", 1);
        json.add("slots", new JsonObject());
        return RoomAllocationSavedData.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
    }
}
