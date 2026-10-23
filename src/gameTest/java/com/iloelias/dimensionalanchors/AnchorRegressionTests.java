package com.iloelias.dimensionalanchors;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

@EventBusSubscriber(modid = DimensionalAnchors.MODID)
public final class AnchorRegressionTests {
    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(id("anchor_regressions"));
        register(event, "upgrade_recipes", new TestData<>(environment, Identifier.withDefaultNamespace("empty"),
                100, 0, true), AnchorRegressionTests::upgradeRecipes);
        register(event, "stack_placement", new TestData<>(environment, Identifier.withDefaultNamespace("empty"),
                100, 0, true), helper -> stackPlacement(helper, GameType.SURVIVAL));
        register(event, "creative_placement", new TestData<>(environment, Identifier.withDefaultNamespace("empty"),
                100, 0, true), helper -> stackPlacement(helper, GameType.CREATIVE));
        register(event, "upgraded_extraction", new TestData<>(environment, Identifier.withDefaultNamespace("empty"),
                100, 0, true), AnchorRegressionTests::upgradedExtraction);
    }

    private static void register(RegisterGameTestsEvent event, String name,
            TestData<net.minecraft.core.Holder<net.minecraft.gametest.framework.TestEnvironmentDefinition<?>>> data,
            Consumer<GameTestHelper> test) {
        event.registerTest(id(name), new GameTestInstance(data) {
            @Override
            public void run(GameTestHelper helper) {
                test.accept(helper);
                helper.succeed();
            }

            @Override
            public MapCodec<? extends GameTestInstance> codec() {
                return MapCodec.unit(this);
            }

            @Override
            protected MutableComponent typeDescription() {
                return Component.literal(name);
            }
        });
    }

    private static void upgradeRecipes(GameTestHelper helper) {
        for (AnchorTier tier : List.of(AnchorTier.IRON, AnchorTier.GOLD, AnchorTier.DIAMOND, AnchorTier.NETHERITE)) {
            Recipe<?> recipe = recipe(helper, tier);
            helper.assertTrue(!recipe.isSpecial(), "Upgrades must not be hidden special recipes");
            helper.assertTrue(recipe.display().size() == 1, "Upgrade must expose a recipe display");
            helper.assertTrue(!recipe.placementInfo().isImpossibleToPlace(), "Upgrade must expose its ingredients");
            helper.assertTrue(tier == AnchorTier.NETHERITE
                    ? recipe instanceof SmithingTransformRecipe : recipe instanceof ShapedRecipe,
                    "JEI must recognize the vanilla recipe class");
            roundTrip(helper, recipe, tier);

            AnchorTier previous = AnchorTier.values()[tier.ordinal() - 1];
            ItemStack raw = new ItemStack(DimensionalAnchors.anchorItem(previous).get());
            ItemStack rawResult = upgrade(helper, recipe, raw, tier);
            helper.assertTrue(rawResult.getItem() == DimensionalAnchors.anchorItem(tier).get(), "Wrong result item");
            helper.assertTrue(!rawResult.has(DimensionalAnchors.ANCHOR_ITEM_DATA.get()),
                    "Preview of a pristine anchor must not allocate a UUID");

            for (boolean registered : List.of(false, true)) {
                ItemStack source = raw.copy();
                AnchorItemData original = new AnchorItemData(UUID.randomUUID(), previous, registered);
                source.set(DimensionalAnchors.ANCHOR_ITEM_DATA.get(), original);
                source.set(DataComponents.CUSTOM_NAME, Component.literal("Persistent anchor"));
                ItemStack result = upgrade(helper, recipe, source, tier);
                AnchorItemData data = result.get(DimensionalAnchors.ANCHOR_ITEM_DATA.get());
                helper.assertTrue(new AnchorItemData(original.anchorId(), tier, registered).equals(data),
                        "Upgrade must preserve UUID and registration, and change tier");
                helper.assertTrue(original.equals(source.get(DimensionalAnchors.ANCHOR_ITEM_DATA.get())),
                        "Preview must not mutate its source");
                helper.assertTrue(result.getCount() == 1, "Upgrade must produce exactly one item");
                helper.assertTrue(source.get(DataComponents.CUSTOM_NAME).equals(result.get(DataComponents.CUSTOM_NAME)),
                        "Upgrade must preserve other item components");
                helper.assertTrue(ItemStack.isSameItemSameComponents(result, upgrade(helper, recipe, source, tier)),
                        "Repeated previews must be deterministic");
            }

            ItemStack invalid = raw.copy();
            invalid.set(DimensionalAnchors.ANCHOR_ITEM_DATA.get(),
                    new AnchorItemData(UUID.randomUUID(), tier, false));
            if (recipe instanceof ShapedRecipe shaped) {
                CraftingInput good = crafting(raw, material(tier));
                List<ItemStack> missing = new ArrayList<>(good.items());
                missing.set(0, ItemStack.EMPTY);
                helper.assertTrue(!shaped.matches(CraftingInput.of(3, 3, missing), helper.getLevel()),
                        "Missing surrounding material must fail");
                helper.assertTrue(!shaped.matches(crafting(raw, Items.DIRT), helper.getLevel()),
                        "Wrong material must fail");
                helper.assertTrue(!shaped.matches(crafting(ItemStack.EMPTY, material(tier)), helper.getLevel()),
                        "Missing anchor must fail");
                helper.assertTrue(!shaped.matches(crafting(new ItemStack(Items.STONE), material(tier)),
                        helper.getLevel()), "Wrong center item must fail");
                helper.assertTrue(!shaped.matches(crafting(invalid, material(tier)), helper.getLevel()),
                        "Mismatched component tier must fail");
                helper.assertTrue(!shaped.matches(CraftingInput.of(2, 2,
                        List.of(raw, raw, raw, raw)), helper.getLevel()), "2x2 crafting must fail");
            } else if (recipe instanceof SmithingTransformRecipe smithing) {
                helper.assertTrue(!smithing.matches(new SmithingRecipeInput(ItemStack.EMPTY, raw,
                        new ItemStack(Items.NETHERITE_INGOT)), helper.getLevel()), "Missing template must fail");
                helper.assertTrue(!smithing.matches(smithing(invalid), helper.getLevel()), "Invalid tier must fail");
                helper.assertTrue(!smithing.matches(new SmithingRecipeInput(
                        new ItemStack(DimensionalAnchors.DIMENSIONAL_UPGRADE_TEMPLATE.get()), raw,
                        new ItemStack(Items.DIRT)), helper.getLevel()), "Wrong addition must fail");
            }
        }
    }

    private static void roundTrip(GameTestHelper helper, Recipe<?> recipe, AnchorTier tier) {
        var ops = RegistryOps.create(JsonOps.INSTANCE, helper.getLevel().registryAccess());
        var json = Recipe.CODEC.encodeStart(ops, recipe).getOrThrow();
        Recipe<?> decoded = Recipe.CODEC.parse(ops, json).getOrThrow();
        helper.assertTrue(decoded.getClass() == recipe.getClass(), "JSON must retain custom recipe behavior");
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            Recipe.STREAM_CODEC.encode(buffer, decoded);
            Recipe<?> networkRecipe = Recipe.STREAM_CODEC.decode(buffer);
            helper.assertTrue(networkRecipe.getClass() == recipe.getClass(), "Network round trip lost recipe type");
            AnchorTier previous = AnchorTier.values()[tier.ordinal() - 1];
            upgrade(helper, networkRecipe, new ItemStack(DimensionalAnchors.anchorItem(previous).get()), tier);
            helper.assertTrue(networkRecipe.display().size() == 1, "Network round trip lost recipe display");
        } finally {
            buffer.release();
        }
    }

    private static void stackPlacement(GameTestHelper helper, GameType gameType) {
        var level = helper.getLevel();
        var player = helper.makeMockPlayer(gameType);
        gameType.updatePlayerAbilities(player.getAbilities());
        ItemStack stack = new ItemStack(DimensionalAnchors.COPPER_DIMENSIONAL_ANCHOR_ITEM.get(), 5);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var ids = new HashSet<UUID>();
        BlockPos origin = helper.absolutePos(new BlockPos(0, 2, 0));
        var saved = AnchorSavedData.get(level);
        for (int index = 0; index < 5; index++) {
            BlockPos pos = origin.above(index * 2);
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
            var context = new UseOnContext(player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atBottomCenterOf(pos), Direction.UP, pos.below(), false));
            helper.assertTrue(stack.useOn(context).consumesAction(), "Placement failed");
            AnchorRecord record = saved.findAt(GlobalPos.of(level.dimension(), pos)).orElseThrow();
            helper.assertTrue(ids.add(record.anchorId()), "Each placed anchor must have a distinct UUID");
            helper.assertTrue(!stack.has(DimensionalAnchors.ANCHOR_ITEM_DATA.get()), "Remaining stack was mutated");
            helper.assertTrue(stack.getCount() == (gameType == GameType.CREATIVE ? 5 : 4 - index),
                    "Placement consumed the wrong number of items");
        }
        helper.assertTrue(ids.stream().allMatch(id -> saved.get(id).orElseThrow().placed()),
                "All five independent anchors must remain placed");
        ItemStack blocked = new ItemStack(DimensionalAnchors.COPPER_DIMENSIONAL_ANCHOR_ITEM.get(), 5);
        player.setItemInHand(InteractionHand.MAIN_HAND, blocked);
        BlockPos pos = origin.above(10);
        level.setBlockAndUpdate(pos, Blocks.BEDROCK.defaultBlockState());
        level.setBlockAndUpdate(pos.below(), Blocks.BEDROCK.defaultBlockState());
        int recordsBefore = saved.records().size();
        var context = new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atBottomCenterOf(pos), Direction.UP, pos.below(), false));
        helper.assertTrue(!blocked.useOn(context).consumesAction(), "Blocked placement succeeded");
        helper.assertTrue(blocked.getCount() == 5 && !blocked.has(DimensionalAnchors.ANCHOR_ITEM_DATA.get()),
                "Failed placement changed the original stack");
        helper.assertTrue(saved.records().size() == recordsBefore, "Failed placement allocated an identity");
    }

    private static void upgradedExtraction(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos pos = helper.absolutePos(new BlockPos(0, 2, 0));
        var saved = AnchorSavedData.get(level);
        UUID id = UUID.randomUUID();
        saved.put(AnchorRecord.unplaced(id, AnchorTier.COPPER, true));
        ItemStack stack = new ItemStack(DimensionalAnchors.COPPER_DIMENSIONAL_ANCHOR_ITEM.get());
        stack.set(DimensionalAnchors.ANCHOR_ITEM_DATA.get(), new AnchorItemData(id, AnchorTier.COPPER, true));
        for (AnchorTier tier : List.of(AnchorTier.IRON, AnchorTier.GOLD, AnchorTier.DIAMOND, AnchorTier.NETHERITE)) {
            stack = upgrade(helper, recipe(helper, tier), stack, tier);
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
            var item = (BlockItem) stack.getItem();
            var context = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack,
                    new BlockHitResult(Vec3.atBottomCenterOf(pos), Direction.UP, pos.below(), false));
            helper.assertTrue(item.place(context).consumesAction(), "Upgraded placement failed");
            AnchorRecord record = saved.get(id).orElseThrow();
            helper.assertTrue(record.tier() == tier && record.registered() && record.placed(),
                    "SavedData retained a stale tier after upgrade");
            var state = level.getBlockState(pos);
            state.getBlock().playerDestroy(level, player, pos, state, null, new ItemStack(Items.IRON_PICKAXE));
            var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1));
            helper.assertTrue(drops.size() == 1, "Extraction must produce exactly one drop");
            stack = drops.getFirst().getItem().copy();
            drops.forEach(ItemEntity::discard);
            helper.assertTrue(stack.getItem() == DimensionalAnchors.anchorItem(tier).get(), "Drop reverted tier");
            helper.assertTrue(new AnchorItemData(id, tier, true).equals(
                    stack.get(DimensionalAnchors.ANCHOR_ITEM_DATA.get())), "Drop lost UUID or registration");
            helper.assertTrue(!saved.get(id).orElseThrow().placed(), "Extracted anchor is still marked placed");
            var ops = RegistryOps.create(JsonOps.INSTANCE, level.registryAccess());
            var encoded = AnchorSavedData.TYPE.codec().encodeStart(ops, saved).getOrThrow();
            var restored = AnchorSavedData.TYPE.codec().parse(ops, encoded).getOrThrow();
            helper.assertTrue(restored.get(id).equals(saved.get(id)), "SavedData round trip lost anchor data");
        }
    }

    private static Recipe<?> recipe(GameTestHelper helper, AnchorTier tier) {
        String name = tier.getSerializedName() + "_dimensional_anchor"
                + (tier == AnchorTier.NETHERITE ? "_smithing" : "");
        return helper.getLevel().getServer().getRecipeManager().getRecipes().stream()
                .filter(holder -> holder.id().identifier().equals(id(name)))
                .findFirst().orElseThrow().value();
    }

    private static ItemStack upgrade(GameTestHelper helper, Recipe<?> recipe, ItemStack source, AnchorTier tier) {
        if (recipe instanceof ShapedRecipe shaped) {
            CraftingInput input = crafting(source, material(tier));
            helper.assertTrue(shaped.matches(input, helper.getLevel()), "Valid 3x3 upgrade must match: " + tier);
            return shaped.assemble(input);
        }
        if (recipe instanceof SmithingTransformRecipe smithing) {
            SmithingRecipeInput input = smithing(source);
            helper.assertTrue(smithing.matches(input, helper.getLevel()), "Valid smithing upgrade must match");
            return smithing.assemble(input);
        }
        throw new IllegalArgumentException("Unsupported anchor recipe: " + recipe);
    }

    private static CraftingInput crafting(ItemStack source, Item material) {
        List<ItemStack> items = new ArrayList<>();
        for (int index = 0; index < 9; index++) {
            items.add(index == 4 ? source : new ItemStack(material));
        }
        return CraftingInput.of(3, 3, items);
    }

    private static SmithingRecipeInput smithing(ItemStack source) {
        return new SmithingRecipeInput(new ItemStack(DimensionalAnchors.DIMENSIONAL_UPGRADE_TEMPLATE.get()),
                source, new ItemStack(Items.NETHERITE_INGOT));
    }

    private static Item material(AnchorTier tier) {
        return switch (tier) {
            case IRON -> Items.IRON_INGOT;
            case GOLD -> Items.GOLD_INGOT;
            case DIAMOND -> Items.DIAMOND;
            default -> throw new IllegalArgumentException("Not a crafting upgrade: " + tier);
        };
    }

    private static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath(DimensionalAnchors.MODID, name);
    }
}
