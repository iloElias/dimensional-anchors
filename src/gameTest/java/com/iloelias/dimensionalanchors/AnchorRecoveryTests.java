package com.iloelias.dimensionalanchors;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.UUID;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.SavedDataStorage;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

final class AnchorRecoveryTests {
    static void recoverTiers(GameTestHelper helper) {
        var level = helper.getLevel();
        var pocket = level.getServer().getLevel(RoomGeneration.POCKET);
        var player = helper.makeMockServerPlayerInLevel();
        var source = admin(player);
        var sessions = PlayerRoomSavedData.get(level);
        UUID occupant = UUID.randomUUID();
        for (var tier : AnchorTier.values()) {
            for (boolean registered : new boolean[]{false, true}) {
                UUID id = UUID.randomUUID();
                var geometry = registered ? RoomGeneration.prepare(pocket, id, tier) : null;
                if (geometry != null) {
                    pocket.setBlockAndUpdate(geometry.reference(), Blocks.GOLD_BLOCK.defaultBlockState());
                }
                var original = AnchorRecord.unplaced(id, tier, registered);
                AnchorSavedData.get(pocket).put(original);
                sessions.enter(occupant, id, new ExteriorReturn(Level.OVERWORLD, new Vec3(0.5, 64, 0.5), 0, 0));
                var session = sessions.find(occupant).orElseThrow();
                player.getInventory().clearContent();
                var reservations = RoomAllocationSavedData.get(level).reservations();
                try {
                    helper.assertTrue(execute(source, id.toString()) == 1, "Recovery command failed for " + tier);
                    var item = player.getInventory().getItem(0);
                    helper.assertTrue(item.is(DimensionalAnchors.anchorItem(tier).get()) && item.getCount() == 1
                            && new AnchorItemData(id, tier, registered).equals(
                                    item.get(DimensionalAnchors.ANCHOR_ITEM_DATA.get())),
                            "Recovery changed the tier, identity, registration or item count");
                    helper.assertTrue(!AnchorAvailability.isRegisteredAndPlaced(level, id)
                            && sessions.find(occupant).orElseThrow() == session
                            && RoomAllocationSavedData.get(level).reservations().equals(reservations),
                            "Recovery enabled entry, changed an occupant or allocated a new room");
                    if (registered) {
                        helper.assertTrue(RoomAllocationSavedData.get(level).find(id).orElseThrow() == geometry.slot(),
                                "Recovery moved the room reservation");
                        helper.assertTrue(pocket.getBlockState(geometry.reference()).is(Blocks.GOLD_BLOCK),
                                "Recovery changed existing room contents");
                    }
                } finally {
                    sessions.completeExit(occupant, id);
                }
            }
        }
    }

    static void permissionsAndFailures(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        var level = helper.getLevel();
        UUID id = UUID.randomUUID();
        var pos = helper.absolutePos(new BlockPos(2, 4, 2));
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        var original = AnchorRecord.unplaced(id, AnchorTier.GOLD, true)
                .placedAt(GlobalPos.of(level.dimension(), pos));
        AnchorSavedData.get(level).put(original);
        var source = admin(player);
        expectSyntaxFailure(helper, source.withPermission(PermissionSet.NO_PERMISSIONS), id.toString());
        expectSyntaxFailure(helper, source, "not-a-uuid");
        expectSyntaxFailure(helper, level.getServer().createCommandSourceStack(), id.toString());
        helper.assertTrue(execute(source, UUID.randomUUID().toString()) == 0,
                "Unknown UUID created an anchor");
        player.getInventory().clearContent();
        for (int slot = 0; slot < 36; slot++) {
            player.getInventory().setItem(slot, new ItemStack(Items.STONE, 64));
        }
        int drops = level.getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(4)).size();
        helper.assertTrue(execute(source, id.toString()) == 0
                && AnchorSavedData.get(level).get(id).orElseThrow().equals(original)
                && drops == level.getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(4)).size(),
                "Full inventory modified anchor data or dropped a recovery item");
    }

    static void placedAndConflicting(GameTestHelper helper) {
        var level = helper.getLevel();
        var pocket = level.getServer().getLevel(RoomGeneration.POCKET);
        var player = helper.makeMockServerPlayerInLevel();
        var source = admin(player);
        var pos = helper.absolutePos(new BlockPos(2, 3, 2));
        UUID id = UUID.randomUUID();
        var record = AnchorRecord.unplaced(id, AnchorTier.IRON, true)
                .placedAt(GlobalPos.of(level.dimension(), pos));
        AnchorSavedData.get(level).put(record);
        level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        AnchorSavedData.get(pocket).put(AnchorRecord.unplaced(id, AnchorTier.IRON, true)
                .placedAt(GlobalPos.of(pocket.dimension(), new BlockPos(800, 64, 800))));
        helper.assertTrue(execute(source, id.toString()) == 0, "Conflicting placed identities were guessed");
        AnchorSavedData.get(pocket).remove(id);
        level.setBlockAndUpdate(pos, DimensionalAnchors.IRON_DIMENSIONAL_ANCHOR.get().defaultBlockState());
        helper.assertTrue(execute(source, id.toString()) == 0
                && AnchorSavedData.get(level).get(id).orElseThrow().equals(record),
                "Recovery duplicated an existing placed anchor");
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        helper.assertTrue(execute(source, id.toString()) == 1
                && !AnchorSavedData.get(level).get(id).orElseThrow().placed(),
                "An actually absent block with a stale placed record could not be recovered");
    }

    static void unloadedRecord(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(RoomGeneration.POCKET);
        var player = helper.makeMockServerPlayerInLevel();
        var position = new BlockPos(2_000_000, 64, 2_000_000);
        UUID id = UUID.randomUUID();
        helper.assertTrue(!level.hasChunkAt(position), "Unloaded-record fixture chunk is already loaded");
        AnchorSavedData.get(level).put(AnchorRecord.unplaced(id, AnchorTier.IRON, true)
                .placedAt(GlobalPos.of(level.dimension(), position)));
        player.getInventory().clearContent();
        helper.assertTrue(execute(admin(player), id.toString()) == 1
                && level.hasChunkAt(position)
                && player.getInventory().getItem(0).is(DimensionalAnchors.IRON_DIMENSIONAL_ANCHOR_ITEM.get()),
                "Recovery treated an unloaded chunk as proof of absence without loading it");
    }

    static void repeatedAndRelocated(GameTestHelper helper) {
        var level = helper.getLevel();
        var pocket = level.getServer().getLevel(RoomGeneration.POCKET);
        var player = helper.makeMockServerPlayerInLevel();
        UUID id = UUID.randomUUID();
        AnchorSavedData.get(level).put(AnchorRecord.unplaced(id, AnchorTier.DIAMOND, true));
        player.getInventory().clearContent();
        var source = admin(player);
        helper.assertTrue(execute(source, id.toString()) == 1 && execute(source, id.toString()) == 1,
                "Repeated administrator recovery failed");
        var stack = player.getInventory().getItem(0).copyWithCount(1);
        var oldPos = helper.absolutePos(new BlockPos(2, 3, 2));
        place(helper, player, stack, oldPos, true);
        var remote = new BlockPos(1200, 64, 1200);
        helper.assertTrue(player.teleportTo(pocket, remote.getX() + 0.5, 66, remote.getZ() + 0.5,
                java.util.Set.of(), 0, 0, false), "Could not change dimension for relocation fixture");
        place(helper, player, stack.copy(), remote, false);
        helper.assertTrue(execute(admin(player), id.toString()) == 0,
                "Recovery ignored a placed anchor in another dimension");
        var block = level.getBlockState(oldPos);
        block.getBlock().playerDestroy(level, player, oldPos, block, null, new ItemStack(Items.IRON_PICKAXE));
        level.setBlockAndUpdate(oldPos, Blocks.AIR.defaultBlockState());
        place(helper, player, stack.copy(), remote, true);
        helper.assertTrue(AnchorSavedData.get(level).get(id).isEmpty()
                && AnchorSavedData.get(pocket).get(id).orElseThrow().placed()
                && AnchorAvailability.isRegisteredAndPlaced(level, id),
                "Cross-dimension relocation retained an obsolete authority or lost key availability");
        helper.assertTrue(execute(source, id.toString()) == 0, "Old dimension could duplicate relocated anchor");
    }

    static void upgradeRecovery(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = helper.makeMockServerPlayerInLevel();
        var source = admin(player);
        UUID id = UUID.randomUUID();
        AnchorSavedData.get(level).put(AnchorRecord.unplaced(id, AnchorTier.COPPER, true));
        var copper = new ItemStack(DimensionalAnchors.COPPER_DIMENSIONAL_ANCHOR_ITEM.get());
        copper.set(DimensionalAnchors.ANCHOR_ITEM_DATA.get(), new AnchorItemData(id, AnchorTier.COPPER, true));
        var obsoleteCopper = copper.copy();
        var menu = new CraftingMenu(10, player.getInventory(),
                ContainerLevelAccess.create(level, helper.absolutePos(new BlockPos(2, 2, 2))));
        player.containerMenu = menu;
        for (int slot = 1; slot <= 9; slot++) {
            menu.getSlot(slot).set(slot == 5 ? copper : new ItemStack(Items.IRON_INGOT));
        }
        helper.assertTrue(menu.getSlot(0).getItem().is(DimensionalAnchors.IRON_DIMENSIONAL_ANCHOR_ITEM.get())
                && AnchorIdentity.find(level, id).orElseThrow().tier() == AnchorTier.COPPER,
                "Crafting preview mutated the authoritative tier or produced the wrong item");
        menu.clicked(0, 0, ContainerInput.PICKUP, player);
        helper.assertTrue(AnchorIdentity.find(level, id).orElseThrow().tier() == AnchorTier.IRON,
                "Taking an upgraded crafting output did not persist its tier");
        player.getInventory().clearContent();
        helper.assertTrue(execute(source, id.toString()) == 1
                && player.getInventory().getItem(0).is(DimensionalAnchors.IRON_DIMENSIONAL_ANCHOR_ITEM.get()),
                "Recovery reverted an upgrade that had not been placed");
        place(helper, player, obsoleteCopper, helper.absolutePos(new BlockPos(4, 3, 2)), false);

        UUID smithId = UUID.randomUUID();
        AnchorSavedData.get(level).put(AnchorRecord.unplaced(smithId, AnchorTier.DIAMOND, true));
        var diamond = new ItemStack(DimensionalAnchors.DIAMOND_DIMENSIONAL_ANCHOR_ITEM.get());
        diamond.set(DimensionalAnchors.ANCHOR_ITEM_DATA.get(), new AnchorItemData(smithId, AnchorTier.DIAMOND, true));
        var smith = new SmithingMenu(11, player.getInventory());
        player.containerMenu = smith;
        smith.getSlot(0).set(new ItemStack(DimensionalAnchors.DIMENSIONAL_UPGRADE_TEMPLATE.get()));
        smith.getSlot(1).set(diamond);
        smith.getSlot(2).set(new ItemStack(Items.NETHERITE_INGOT));
        helper.assertTrue(smith.getSlot(3).getItem().is(DimensionalAnchors.NETHERITE_DIMENSIONAL_ANCHOR_ITEM.get())
                && AnchorIdentity.find(level, smithId).orElseThrow().tier() == AnchorTier.DIAMOND,
                "Smithing preview mutated the authoritative tier or produced the wrong item");
        smith.clicked(3, 0, ContainerInput.PICKUP, player);
        helper.assertTrue(AnchorIdentity.find(level, smithId).orElseThrow().tier() == AnchorTier.NETHERITE,
                "Taking the smithing output did not persist its tier");
        player.getInventory().clearContent();
        helper.assertTrue(execute(source, smithId.toString()) == 1
                && player.getInventory().getItem(0).is(DimensionalAnchors.NETHERITE_DIMENSIONAL_ANCHOR_ITEM.get()),
                "Recovery reverted an unplaced Netherite upgrade");
        player.containerMenu = player.inventoryMenu;
    }

    static void corruptStorage(GameTestHelper helper) {
        var level = helper.getLevel();
        try {
            var directory = Files.createTempDirectory("dimensionalanchors-recovery-");
            var file = AnchorSavedData.TYPE.id().withSuffix(".dat").resolveAgainst(directory);
            UUID id = UUID.randomUUID();
            try {
                try (var storage = new SavedDataStorage(level, directory, DataFixers.getDataFixer(), level.registryAccess())) {
                    AnchorSavedData.load(storage, directory).put(AnchorRecord.unplaced(id, AnchorTier.GOLD, true));
                    storage.saveAndJoin();
                }
                var valid = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
                try (var storage = new SavedDataStorage(level, directory, DataFixers.getDataFixer(), level.registryAccess())) {
                    helper.assertTrue(AnchorSavedData.load(storage, directory).get(id).orElseThrow().tier() == AnchorTier.GOLD,
                            "Disk reload lost recovery tier");
                }
                for (boolean partial : new boolean[]{true, false}) {
                    if (partial) {
                        var invalid = valid.copy();
                        var records = invalid.getCompound("data").orElseThrow().getCompound("records").orElseThrow();
                        records.put("invalid-uuid", records.getCompound(id.toString()).orElseThrow().copy());
                        NbtIo.writeCompressed(invalid, file);
                    } else {
                        Files.writeString(file, "deliberately corrupt anchor data");
                    }
                    var before = Files.readAllBytes(file);
                    boolean rejected = false;
                    try (var storage = new SavedDataStorage(level, directory, DataFixers.getDataFixer(), level.registryAccess())) {
                        AnchorSavedData.load(storage, directory);
                    } catch (IllegalStateException expected) {
                        rejected = true;
                    }
                    helper.assertTrue(rejected && Arrays.equals(before, Files.readAllBytes(file)),
                            "Unreadable or partial anchor data was silently reset/overwritten");
                }
            } finally {
                Files.deleteIfExists(file);
                Files.deleteIfExists(file.getParent());
                Files.delete(directory);
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("Anchor recovery disk regression failed", exception);
        }
    }

    private static void place(GameTestHelper helper, ServerPlayer player, ItemStack stack, BlockPos pos, boolean expected) {
        stack = stack.copy();
        var level = player.level();
        level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        var context = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack,
                new BlockHitResult(Vec3.atBottomCenterOf(pos), Direction.UP, pos.below(), false));
        var item = (BlockItem) stack.getItem();
        helper.assertTrue(item.place(context).consumesAction() == expected, "Unexpected recovered-anchor placement result");
        if (!expected) {
            helper.assertTrue(level.getBlockState(pos).isAir(), "Rejected placement left a duplicate block");
        }
    }

    private static CommandSourceStack admin(ServerPlayer player) {
        return player.createCommandSourceStack().withPermission(PermissionSet.ALL_PERMISSIONS).withSuppressedOutput();
    }

    private static int execute(CommandSourceStack source, String id) {
        try {
            return source.getServer().getCommands().getDispatcher().execute("dimensionalanchors recover " + id, source);
        } catch (CommandSyntaxException exception) {
            throw new IllegalStateException("Recovery command unexpectedly failed to execute", exception);
        }
    }

    private static void expectSyntaxFailure(GameTestHelper helper, CommandSourceStack source, String id) {
        boolean rejected = false;
        try {
            source.getServer().getCommands().getDispatcher().execute("dimensionalanchors recover " + id, source);
        } catch (CommandSyntaxException expected) {
            rejected = true;
        }
        helper.assertTrue(rejected, "Unauthorized, non-player or malformed recovery command was accepted");
    }
}
