package com.iloelias.dimensionalanchors;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class RegisteredKeyItem extends Item {
    public RegisteredKeyItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.has(DimensionalAnchors.REGISTERED_KEY_DATA.get());
    }
}