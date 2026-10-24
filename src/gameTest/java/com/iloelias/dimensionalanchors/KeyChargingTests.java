package com.iloelias.dimensionalanchors;

import java.util.ArrayList;
import java.util.UUID;
import java.util.function.Consumer;

import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;

final class KeyChargingTests {
    static void charging(GameTestHelper helper) {
        var sounds = new ArrayList<PlayLevelSoundEvent.AtPosition>();
        var availableSounds = new ArrayList<PlayLevelSoundEvent.AtPosition>();
        var unavailableSounds = new ArrayList<PlayLevelSoundEvent.AtPosition>();
        Consumer<PlayLevelSoundEvent.AtPosition> listener = event -> {
            if (event.getLevel() == helper.getLevel() && event.getSound() != null) {
                var sound = event.getSound().value();
                if (sound == SoundEvents.PORTAL_TRIGGER) {
                    sounds.add(event);
                } else if (sound == SoundEvents.CHORUS_FRUIT_TELEPORT) {
                    availableSounds.add(event);
                } else if (sound == SoundEvents.BEACON_DEACTIVATE) {
                    unavailableSounds.add(event);
                }
            }
        };
        NeoForge.EVENT_BUS.addListener(listener);
        try {
            anchorAvailability(helper);
            chargingCases(helper);
            unavailableAnchorFeedback(helper);
            generationFailureFeedback(helper);
            helper.assertTrue(sounds.size() == 25, "Portal contact sound must play exactly once per started charge");
            helper.assertTrue(sounds.stream().allMatch(sound -> sound.getOriginalVolume() == 0.25F
                    && sound.getOriginalPitch() == 1.0F), "Unexpected portal contact audio settings");
            helper.assertTrue(availableSounds.size() == 4,
                    "Successful anchor checks must play one teleport-confirmation sound each");
            helper.assertTrue(unavailableSounds.size() == 2,
                    "An unavailable anchor and a generation failure must each play one failure sound");
        } finally {
            NeoForge.EVENT_BUS.unregister(listener);
        }
    }

    private static void chargingCases(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos anchorPos = helper.absolutePos(new BlockPos(8, 2, 8));
        level.setBlockAndUpdate(anchorPos, DimensionalAnchors.COPPER_DIMENSIONAL_ANCHOR.get().defaultBlockState());
        UUID anchorId = UUID.randomUUID();
        AnchorSavedData.get(level).put(AnchorRecord.unplaced(anchorId, AnchorTier.COPPER, true)
                .placedAt(GlobalPos.of(level.dimension(), anchorPos)));
        for (GameType mode : new GameType[]{GameType.SURVIVAL, GameType.CREATIVE}) {
            for (InteractionHand hand : InteractionHand.values()) {
                var messages = new ArrayList<Component>();
                var player = new Player(level, new GameProfile(UUID.randomUUID(), "key-charge-test")) {
                    @Override
                    public GameType gameMode() {
                        return mode;
                    }

                    @Override
                    public boolean isClientAuthoritative() {
                        return false;
                    }

                    @Override
                    public void sendOverlayMessage(Component message) {
                        messages.add(message);
                    }
                };
                mode.updatePlayerAbilities(player.getAbilities());
                player.setNoGravity(true);
                player.setPos(helper.absoluteVec(new net.minecraft.world.phys.Vec3(1, 2, 1)));
                var key = DimensionalAnchors.DIMENSIONAL_KEY.get().getDefaultInstance();
                key.set(DimensionalAnchors.REGISTERED_KEY_DATA.get(), new RegisteredKeyData(anchorId));
                var original = key.copy();
                player.setItemInHand(hand, key);
                var item = key.getItem();
                helper.assertTrue(!player.isUsingItem(), "Idle key is already charging");
                helper.assertTrue(item.getUseDuration(key, player) == 60
                        && item.getUseAnimation(key) == ItemUseAnimation.BOW && !item.useOnRelease(key),
                        "Key must use a 60-tick bow animation with automatic completion");

                for (int releaseAt : new int[]{0, 1, 30, 59}) {
                    var reservations = RoomAllocationSavedData.get(level).reservations();
                    var room = RoomAllocationSavedData.get(level).room(anchorId);
                    helper.assertTrue(item.use(level, player, hand).consumesAction()
                            && player.isUsingItem() && player.getUsedItemHand() == hand,
                            "Linked key did not start charging in the selected hand");
                    for (int tick = 0; tick < releaseAt; tick++) {
                        player.tick();
                    }
                    helper.assertTrue(messages.isEmpty(), "Charge completed before 60 ticks");
                    player.releaseUsingItem();
                    player.tick();
                    helper.assertTrue(!player.isUsingItem() && messages.isEmpty()
                            && !player.getCooldowns().isOnCooldown(key), "Early release caused an effect or cooldown");
                    helper.assertTrue(ItemStack.matches(original, player.getItemInHand(hand)),
                            "Cancellation changed the key");
                    helper.assertTrue(RoomAllocationSavedData.get(level).reservations().equals(reservations)
                            && RoomAllocationSavedData.get(level).room(anchorId).equals(room),
                            "Early release allocated or generated a room");
                }
                var position = player.position();
                int xp = player.experienceLevel;
                item.use(level, player, hand);
                for (int tick = 0; tick < 59; tick++) {
                    player.tick();
                }
                helper.assertTrue(messages.isEmpty() && player.isUsingItem()
                        && player.getUseItemRemainingTicks() == 1, "Incorrect charge threshold");
                player.tick();
                helper.assertTrue(!player.isUsingItem() && messages.size() == 1
                        && messages.getFirst().getContents() instanceof TranslatableContents text
                        && text.getKey().equals("message.dimensionalanchors.key_charge_complete"),
                        "A placed linked anchor must pass the entry check on tick 60");
                helper.assertTrue(RoomAllocationSavedData.get(level).room(anchorId).orElseThrow().complete(),
                        "A successful charge did not prepare the room");
                helper.assertTrue(!player.getCooldowns().isOnCooldown(key),
                        "A charge that did not teleport applied the remote-travel cooldown");
                // Cooldown is applied by successful remote travel (see room_entry); verify its blocking rules here.
                player.getCooldowns().addCooldown(key, RegisteredKeyItem.COOLDOWN_TICKS);
                player.tick();
                var copy = key.copy();
                var otherKey = key.copy();
                otherKey.set(DimensionalAnchors.REGISTERED_KEY_DATA.get(), new RegisteredKeyData(UUID.randomUUID()));
                for (var blocked : new ItemStack[]{key, copy, otherKey}) {
                    player.setItemInHand(hand, blocked);
                    helper.assertTrue(player.getCooldowns().isOnCooldown(blocked)
                            && item.use(level, player, hand) == InteractionResult.FAIL && !player.isUsingItem(),
                            "Switching copies or anchor references bypassed shared cooldown");
                }
                player.setItemInHand(hand, key);
                var otherPlayer = helper.makeMockPlayer(mode);
                helper.assertTrue(!otherPlayer.getCooldowns().isOnCooldown(key),
                        "Cooldown leaked to another player");
                for (int tick = 0; tick < 5; tick++) {
                    player.tick();
                }
                player.releaseUsingItem();
                helper.assertTrue(messages.size() == 1 && player.position().equals(position)
                        && player.level() == level && player.experienceLevel == xp
                        && player.getCooldowns().isOnCooldown(key)
                        && ItemStack.matches(original, player.getItemInHand(hand)),
                        "Charge without travel moved the player, consumed resources, repeated completion, or lost cooldown");
                for (int tick = 6; tick < 399; tick++) {
                    player.getCooldowns().tick();
                }
                helper.assertTrue(player.getCooldowns().isOnCooldown(key)
                        && item.use(level, player, hand) == InteractionResult.FAIL,
                        "Cooldown ended before 400 ticks");
                player.getCooldowns().tick();
                helper.assertTrue(!player.getCooldowns().isOnCooldown(key)
                        && item.use(level, player, hand).consumesAction() && player.isUsingItem(),
                        "Key did not become usable after exactly 400 ticks");
                player.releaseUsingItem();
                helper.assertTrue(!player.getCooldowns().isOnCooldown(key),
                        "Cancelling after cooldown expiration applied another cooldown");
                messages.clear();
                var unlinked = item.getDefaultInstance();
                player.setItemInHand(hand, unlinked);
                helper.assertTrue(item.use(level, player, hand) == InteractionResult.FAIL
                        && !player.isUsingItem() && messages.size() == 1
                        && messages.getFirst().getContents() instanceof TranslatableContents text
                        && text.getKey().equals("message.dimensionalanchors.unlinked_key"),
                        "Unlinked creative preview must reject charging with explicit feedback");
                var blank = DimensionalAnchors.UNASSIGNED_DIMENSIONAL_KEY.get().getDefaultInstance();
                player.setItemInHand(hand, blank);
                helper.assertTrue(!blank.getItem().use(level, player, hand).consumesAction()
                        && !player.isUsingItem(), "Blank key unexpectedly started charging");
            }
        }
    }

    private static void anchorAvailability(GameTestHelper helper) {
        var level = helper.getLevel();
        var saved = AnchorSavedData.get(level);
        BlockPos pos = helper.absolutePos(new BlockPos(10, 2, 10));
        UUID id = UUID.randomUUID();
        var placed = AnchorRecord.unplaced(id, AnchorTier.COPPER, true)
                .placedAt(GlobalPos.of(level.dimension(), pos));

        saved.put(placed);
        level.setBlockAndUpdate(pos, DimensionalAnchors.COPPER_DIMENSIONAL_ANCHOR.get().defaultBlockState());
        helper.assertTrue(AnchorAvailability.isRegisteredAndPlaced(level, id),
                "Registered anchor block and saved location should pass the entry check");

        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        helper.assertTrue(!AnchorAvailability.isRegisteredAndPlaced(level, id),
                "A stale placed record without its anchor block must fail the entry check");

        level.setBlockAndUpdate(pos, DimensionalAnchors.IRON_DIMENSIONAL_ANCHOR.get().defaultBlockState());
        helper.assertTrue(!AnchorAvailability.isRegisteredAndPlaced(level, id),
                "An anchor block with a tier different from its record must fail the entry check");

        level.setBlockAndUpdate(pos, DimensionalAnchors.COPPER_DIMENSIONAL_ANCHOR.get().defaultBlockState());
        saved.put(AnchorRecord.unplaced(id, AnchorTier.COPPER, true));
        helper.assertTrue(!AnchorAvailability.isRegisteredAndPlaced(level, id),
                "An unplaced registered anchor must fail the entry check");

        saved.put(placed.withRegistered(false));
        helper.assertTrue(!AnchorAvailability.isRegisteredAndPlaced(level, id),
                "An unregistered anchor must fail the entry check");
        saved.remove(id);
        helper.assertTrue(!AnchorAvailability.isRegisteredAndPlaced(level, id),
                "A missing anchor record must fail the entry check");
    }

    private static void unavailableAnchorFeedback(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(12, 2, 12));
        UUID anchorId = UUID.randomUUID();
        var staleRecord = AnchorRecord.unplaced(anchorId, AnchorTier.COPPER, true)
                .placedAt(GlobalPos.of(level.dimension(), pos));
        AnchorSavedData.get(level).put(staleRecord);
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());

        var messages = new ArrayList<Component>();
        var player = new Player(level, new GameProfile(UUID.randomUUID(), "missing-anchor-test")) {
            @Override
            public GameType gameMode() {
                return GameType.SURVIVAL;
            }

            @Override
            public boolean isClientAuthoritative() {
                return false;
            }

            @Override
            public void sendOverlayMessage(Component message) {
                messages.add(message);
            }
        };
        var key = DimensionalAnchors.DIMENSIONAL_KEY.get().getDefaultInstance();
        key.set(DimensionalAnchors.REGISTERED_KEY_DATA.get(), new RegisteredKeyData(anchorId));
        player.setNoGravity(true);
        player.setPos(helper.absoluteVec(new net.minecraft.world.phys.Vec3(1, 2, 1)));
        player.setItemInHand(InteractionHand.MAIN_HAND, key);
        var position = player.position();
        helper.assertTrue(key.getItem().use(level, player, InteractionHand.MAIN_HAND).consumesAction(),
                "The stale linked key should begin its charge");
        for (int tick = 0; tick < RegisteredKeyItem.CHARGE_TICKS; tick++) {
            player.tick();
        }
        helper.assertTrue(messages.size() == 1
                && messages.getFirst().getContents() instanceof TranslatableContents text
                && text.getKey().equals("message.dimensionalanchors.key_anchor_unavailable"),
                "A stale anchor record must show unavailable feedback when the 60-tick charge completes");
        helper.assertTrue(!player.getCooldowns().isOnCooldown(key) && player.position().equals(position)
                && player.level() == level, "Unavailable completion teleported or applied the travel cooldown");
        helper.assertTrue(RoomAllocationSavedData.get(level).find(anchorId).isEmpty(),
                "Unavailable anchor allocated a room");
    }

    private static void generationFailureFeedback(GameTestHelper helper) {
        var level = helper.getLevel();
        UUID id = UUID.randomUUID();
        BlockPos anchorPos = helper.absolutePos(new BlockPos(14, 2, 14));
        level.setBlockAndUpdate(anchorPos, DimensionalAnchors.COPPER_DIMENSIONAL_ANCHOR.get().defaultBlockState());
        AnchorSavedData.get(level).put(AnchorRecord.unplaced(id, AnchorTier.COPPER, true)
                .placedAt(GlobalPos.of(level.dimension(), anchorPos)));
        var data = RoomAllocationSavedData.get(level);
        var geometry = new RoomGeometry(data.reserve(id), AnchorTier.COPPER);
        var pocket = level.getServer().getLevel(RoomGeneration.POCKET);
        pocket.setBlockAndUpdate(geometry.exitControl(), Blocks.STONE.defaultBlockState());
        var messages = new ArrayList<Component>();
        var player = new Player(level, new GameProfile(UUID.randomUUID(), "room-failure-test")) {
            @Override
            public GameType gameMode() {
                return GameType.SURVIVAL;
            }

            @Override
            public boolean isClientAuthoritative() {
                return false;
            }

            @Override
            public void sendOverlayMessage(Component message) {
                messages.add(message);
            }
        };
        var key = DimensionalAnchors.DIMENSIONAL_KEY.get().getDefaultInstance();
        key.set(DimensionalAnchors.REGISTERED_KEY_DATA.get(), new RegisteredKeyData(id));
        var position = player.position();
        key.getItem().finishUsingItem(key, level, player);
        helper.assertTrue(messages.size() == 1
                && messages.getFirst().getContents() instanceof TranslatableContents text
                && text.getKey().equals("message.dimensionalanchors.room_generation_failed"),
                "Generation failure did not show explicit feedback");
        helper.assertTrue(data.room(id).isEmpty() && player.position().equals(position)
                && player.level() == level && !player.getCooldowns().isOnCooldown(key)
                && pocket.getBlockState(geometry.exitControl()).is(Blocks.STONE),
                "Generation failure changed blocks, moved the player, or applied the travel cooldown");
        pocket.setBlockAndUpdate(geometry.exitControl(), Blocks.AIR.defaultBlockState());
    }
}
