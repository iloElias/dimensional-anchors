package com.iloelias.dimensionalanchors;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.TransmuteRecipe;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;

public final class AnchorUpgradeRecipe extends ShapedRecipe {
    public static final RecipeSerializer<ShapedRecipe> SERIALIZER = new RecipeSerializer<>(
            ShapedRecipe.MAP_CODEC.xmap(AnchorUpgradeRecipe::new, recipe -> recipe),
            ShapedRecipe.STREAM_CODEC.map(AnchorUpgradeRecipe::new, recipe -> recipe));

    private final ItemStackTemplate result;

    private AnchorUpgradeRecipe(ShapedRecipe recipe) {
        super(new CommonInfo(recipe.showNotification()),
                new CraftingBookInfo(recipe.category(), recipe.group()),
                recipe.pattern, resultTemplate(recipe));
        result = resultTemplate(recipe);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return super.matches(input, level) && AnchorItemData.hasValidTier(input.getItem(1, 1));
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        return AnchorItemData.transferTo(
                input.getItem(1, 1), TransmuteRecipe.createWithOriginalComponents(result, input.getItem(1, 1)));
    }

    @Override
    public RecipeSerializer<ShapedRecipe> getSerializer() {
        return DimensionalAnchors.ANCHOR_UPGRADE_SERIALIZER.get();
    }

    static ItemStackTemplate resultTemplate(Recipe<?> recipe) {
        // Recipe loading precedes item component binding, so do not create an ItemStack here.
        if (recipe.display().getFirst().result() instanceof SlotDisplay.ItemStackSlotDisplay display) {
            return display.stack();
        }
        throw new IllegalArgumentException("Anchor upgrade requires an item-stack result display");
    }
}
