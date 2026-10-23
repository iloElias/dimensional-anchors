package com.iloelias.dimensionalanchors;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;

public final class RegisteredKeyItem extends Item {
    public static final int CHARGE_TICKS = 60;
    public static final int COOLDOWN_TICKS = 400;

    public RegisteredKeyItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.has(DimensionalAnchors.REGISTERED_KEY_DATA.get());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.FAIL;
        }
        if (!stack.has(DimensionalAnchors.REGISTERED_KEY_DATA.get())) {
            if (!level.isClientSide()) {
                player.sendOverlayMessage(Component.translatable("message.dimensionalanchors.unlinked_key"));
            }
            return InteractionResult.FAIL;
        }
        player.startUsingItem(hand);
        if (player.isUsingItem() && !level.isClientSide()) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PORTAL_TRIGGER, SoundSource.PLAYERS, 0.25F, 1.0F);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return CHARGE_TICKS;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (!level.isClientSide() && user instanceof Player player) {
            player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
            player.sendOverlayMessage(Component.translatable("message.dimensionalanchors.key_charge_complete"));
        }
        return stack;
    }
}