package com.iloelias.dimensionalanchors;

import java.util.ArrayList;
import java.util.UUID;
import java.util.function.Consumer;

import com.mojang.authlib.GameProfile;

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
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;

final class KeyChargingTests {
    static void charging(GameTestHelper helper) {
        var sounds = new ArrayList<PlayLevelSoundEvent.AtPosition>();
        Consumer<PlayLevelSoundEvent.AtPosition> listener = event -> {
            if (event.getLevel() == helper.getLevel() && event.getSound() != null
                    && event.getSound().value() == SoundEvents.PORTAL_TRIGGER) {
                sounds.add(event);
            }
        };
        NeoForge.EVENT_BUS.addListener(listener);
        try {
            chargingCases(helper);
            helper.assertTrue(sounds.size() == 24, "Portal contact sound must play exactly once per started charge");
            helper.assertTrue(sounds.stream().allMatch(sound -> sound.getOriginalVolume() == 0.25F
                    && sound.getOriginalPitch() == 1.0F), "Unexpected portal contact audio settings");
        } finally {
            NeoForge.EVENT_BUS.unregister(listener);
        }
    }

    private static void chargingCases(GameTestHelper helper) {
        var level = helper.getLevel();
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
                key.set(DimensionalAnchors.REGISTERED_KEY_DATA.get(), new RegisteredKeyData(UUID.randomUUID()));
                var original = key.copy();
                player.setItemInHand(hand, key);
                var item = key.getItem();
                helper.assertTrue(!player.isUsingItem(), "Idle key is already charging");
                helper.assertTrue(item.getUseDuration(key, player) == 60
                        && item.getUseAnimation(key) == ItemUseAnimation.BOW && !item.useOnRelease(key),
                        "Key must use a 60-tick bow animation with automatic completion");

                for (int releaseAt : new int[]{0, 1, 30, 59}) {
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
                        "Exactly one completion message must appear on tick 60, without waiting for release");
                helper.assertTrue(player.getCooldowns().isOnCooldown(key),
                        "Completed charge must apply cooldown");
                helper.assertTrue(player.getCooldowns().getCooldownPercent(key, 0) == 399.0F / 400.0F,
                        "Cooldown duration must be 400 ticks, including the completing player tick");
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
                        "Prototype teleported, consumed resources, repeated completion, or lost cooldown");
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
}
