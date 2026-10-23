package com.iloelias.dimensionalanchors;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.minecraft.world.level.Level;

public final class AnchorNetheriteUpgradeRecipe extends SmithingTransformRecipe {
    public static final RecipeSerializer<SmithingTransformRecipe> SERIALIZER = new RecipeSerializer<>(
            SmithingTransformRecipe.MAP_CODEC.xmap(AnchorNetheriteUpgradeRecipe::new, recipe -> recipe),
            SmithingTransformRecipe.STREAM_CODEC.map(AnchorNetheriteUpgradeRecipe::new, recipe -> recipe));

    private AnchorNetheriteUpgradeRecipe(SmithingTransformRecipe recipe) {
        super(new CommonInfo(recipe.showNotification()),
                recipe.templateIngredient(), recipe.baseIngredient(), recipe.additionIngredient(),
                AnchorUpgradeRecipe.resultTemplate(recipe));
    }

    @Override
    public boolean matches(SmithingRecipeInput input, Level level) {
        return super.matches(input, level) && AnchorItemData.hasValidTier(input.base());
    }

    @Override
    public ItemStack assemble(SmithingRecipeInput input) {
        return AnchorItemData.transferTo(input.base(), super.assemble(input));
    }

    @Override
    public RecipeSerializer<SmithingTransformRecipe> getSerializer() {
        return DimensionalAnchors.ANCHOR_NETHERITE_UPGRADE_SERIALIZER.get();
    }
}
