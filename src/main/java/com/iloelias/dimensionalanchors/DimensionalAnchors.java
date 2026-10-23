package com.iloelias.dimensionalanchors;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(DimensionalAnchors.MODID)
public class DimensionalAnchors {
    public static final String MODID = "dimensionalanchors";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<net.minecraft.world.item.crafting.RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, MODID);
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredBlock<Block> COPPER_DIMENSIONAL_ANCHOR =
            registerAnchor("copper_dimensional_anchor", AnchorTier.COPPER, Blocks.COPPER_BLOCK, Blocks.COPPER_ORE);
    public static final DeferredBlock<Block> IRON_DIMENSIONAL_ANCHOR =
            registerAnchor("iron_dimensional_anchor", AnchorTier.IRON, Blocks.IRON_BLOCK, Blocks.IRON_ORE);
    public static final DeferredBlock<Block> GOLD_DIMENSIONAL_ANCHOR =
            registerAnchor("gold_dimensional_anchor", AnchorTier.GOLD, Blocks.GOLD_BLOCK, Blocks.GOLD_ORE);
    public static final DeferredBlock<Block> DIAMOND_DIMENSIONAL_ANCHOR =
            registerAnchor("diamond_dimensional_anchor", AnchorTier.DIAMOND, Blocks.DIAMOND_BLOCK, Blocks.DIAMOND_ORE);
    public static final DeferredBlock<Block> NETHERITE_DIMENSIONAL_ANCHOR =
            registerAnchor("netherite_dimensional_anchor", AnchorTier.NETHERITE,
                    Blocks.NETHERITE_BLOCK, Blocks.ANCIENT_DEBRIS);

    public static final DeferredBlock<Block> DIMENSIONAL_LIMITER_BLOCK =
            registerUtilityBlock("dimensional_limiter_block");
    public static final DeferredBlock<Block> DIMENSIONAL_EXIT_BLOCK =
            BLOCKS.register("dimensional_exit_block",
                    () -> new DimensionalExitBlock(utilityBlockProperties("dimensional_exit_block")));

    public static final DeferredItem<BlockItem> COPPER_DIMENSIONAL_ANCHOR_ITEM =
            registerAnchorItem("copper_dimensional_anchor", COPPER_DIMENSIONAL_ANCHOR);
    public static final DeferredItem<BlockItem> IRON_DIMENSIONAL_ANCHOR_ITEM =
            registerAnchorItem("iron_dimensional_anchor", IRON_DIMENSIONAL_ANCHOR);
    public static final DeferredItem<BlockItem> GOLD_DIMENSIONAL_ANCHOR_ITEM =
            registerAnchorItem("gold_dimensional_anchor", GOLD_DIMENSIONAL_ANCHOR);
    public static final DeferredItem<BlockItem> DIAMOND_DIMENSIONAL_ANCHOR_ITEM =
            registerAnchorItem("diamond_dimensional_anchor", DIAMOND_DIMENSIONAL_ANCHOR);
    public static final DeferredItem<BlockItem> NETHERITE_DIMENSIONAL_ANCHOR_ITEM =
            registerAnchorItem("netherite_dimensional_anchor", NETHERITE_DIMENSIONAL_ANCHOR);
    public static final DeferredItem<BlockItem> DIMENSIONAL_LIMITER_BLOCK_ITEM =
            ITEMS.registerSimpleBlockItem("dimensional_limiter_block", DIMENSIONAL_LIMITER_BLOCK);
    public static final DeferredItem<BlockItem> DIMENSIONAL_EXIT_BLOCK_ITEM =
            ITEMS.registerSimpleBlockItem("dimensional_exit_block", DIMENSIONAL_EXIT_BLOCK);

    public static final DeferredItem<Item> DIMENSIONAL_CATALYST =
            ITEMS.register("dimensional_catalyst", () -> new Item(new Item.Properties()
                    .setId(itemKey("dimensional_catalyst"))
                    .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)));
    public static final DeferredItem<Item> DIMENSIONAL_UPGRADE_TEMPLATE =
            ITEMS.registerSimpleItem("dimensional_upgrade_template");
    public static final DeferredItem<Item> DIMENSIONAL_KEY =
            ITEMS.register("dimensional_key", () -> new RegisteredKeyItem(new Item.Properties()
                    .setId(itemKey("dimensional_key"))
                    .stacksTo(1)));
    public static final DeferredItem<Item> UNASSIGNED_DIMENSIONAL_KEY =
            registerKeyItem("unassigned_dimensional_key");

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<AnchorItemData>> ANCHOR_ITEM_DATA =
            DATA_COMPONENTS.registerComponentType("anchor_data", builder -> builder
                    .persistent(AnchorItemData.CODEC)
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.fromCodec(AnchorItemData.CODEC)));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<RegisteredKeyData>> REGISTERED_KEY_DATA =
            DATA_COMPONENTS.registerComponentType("registered_key", builder -> builder
                    .persistent(RegisteredKeyData.CODEC)
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.fromCodec(RegisteredKeyData.CODEC)));
    public static final DeferredHolder<net.minecraft.world.item.crafting.RecipeSerializer<?>, net.minecraft.world.item.crafting.RecipeSerializer<ShapedRecipe>>
            ANCHOR_UPGRADE_SERIALIZER = RECIPE_SERIALIZERS.register("anchor_upgrade",
                    () -> AnchorUpgradeRecipe.SERIALIZER);
    public static final DeferredHolder<net.minecraft.world.item.crafting.RecipeSerializer<?>, net.minecraft.world.item.crafting.RecipeSerializer<SmithingTransformRecipe>>
            ANCHOR_NETHERITE_UPGRADE_SERIALIZER = RECIPE_SERIALIZERS.register("anchor_netherite_upgrade",
                    () -> AnchorNetheriteUpgradeRecipe.SERIALIZER);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CONTENTS_TAB =
            CREATIVE_MODE_TABS.register("contents", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.dimensionalanchors"))
                    .icon(() -> COPPER_DIMENSIONAL_ANCHOR_ITEM.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(COPPER_DIMENSIONAL_ANCHOR_ITEM.get());
                        output.accept(IRON_DIMENSIONAL_ANCHOR_ITEM.get());
                        output.accept(GOLD_DIMENSIONAL_ANCHOR_ITEM.get());
                        output.accept(DIAMOND_DIMENSIONAL_ANCHOR_ITEM.get());
                        output.accept(NETHERITE_DIMENSIONAL_ANCHOR_ITEM.get());
                        output.accept(UNASSIGNED_DIMENSIONAL_KEY.get());
                        output.accept(DIMENSIONAL_KEY.get());
                        output.accept(DIMENSIONAL_CATALYST.get());
                        output.accept(DIMENSIONAL_UPGRADE_TEMPLATE.get());
                        output.accept(DIMENSIONAL_LIMITER_BLOCK_ITEM.get());
                        output.accept(DIMENSIONAL_EXIT_BLOCK_ITEM.get());
                    })
                    .build());

    public DimensionalAnchors(IEventBus modEventBus, ModContainer modContainer) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        DATA_COMPONENTS.register(modEventBus);
        RECIPE_SERIALIZERS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        NeoForge.EVENT_BUS.addListener(KeyCopying::update);
        NeoForge.EVENT_BUS.addListener(KeyCopying::take);
        NeoForge.EVENT_BUS.addListener(RoomBoundaryProtection::onBreak);
        NeoForge.EVENT_BUS.addListener(PlayerRoomReconciliation::onLogin);
        NeoForge.EVENT_BUS.addListener(PlayerRoomReconciliation::onRespawn);
        NeoForge.EVENT_BUS.addListener(RoomMovementEnforcement::onTick);
        NeoForge.EVENT_BUS.addListener(RoomMovementEnforcement::onLogout);
        NeoForge.EVENT_BUS.addListener(AnchorRecoveryCommand::register);
        NeoForge.EVENT_BUS.addListener(ItemDescriptions::append);
    }

    private static DeferredBlock<Block> registerAnchor(
            String name, AnchorTier tier, Block soundReference, Block explosionResistanceReference) {
        return BLOCKS.register(name, () -> new AnchorBlock(tier, BlockBehaviour.Properties.of()
                .setId(blockKey(name))
                .mapColor(MapColor.METAL)
                .strength(soundReference.defaultDestroyTime(), explosionResistanceReference.getExplosionResistance())
                .requiresCorrectToolForDrops()
                .sound(soundReference.defaultBlockState().getSoundType())));
    }

    private static DeferredItem<BlockItem> registerAnchorItem(String name, DeferredBlock<Block> block) {
        return ITEMS.register(name, () -> new AnchorBlockItem(block.get(),
                new Item.Properties().setId(itemKey(name))));
    }

    public static DeferredItem<BlockItem> anchorItem(AnchorTier tier) {
        return switch (tier) {
            case COPPER -> COPPER_DIMENSIONAL_ANCHOR_ITEM;
            case IRON -> IRON_DIMENSIONAL_ANCHOR_ITEM;
            case GOLD -> GOLD_DIMENSIONAL_ANCHOR_ITEM;
            case DIAMOND -> DIAMOND_DIMENSIONAL_ANCHOR_ITEM;
            case NETHERITE -> NETHERITE_DIMENSIONAL_ANCHOR_ITEM;
        };
    }

    private static DeferredBlock<Block> registerUtilityBlock(String name) {
        return BLOCKS.register(name, () -> new Block(utilityBlockProperties(name)));
    }

    private static BlockBehaviour.Properties utilityBlockProperties(String name) {
        return BlockBehaviour.Properties.of()
                .setId(blockKey(name))
                .mapColor(MapColor.STONE)
                .strength(Blocks.BEDROCK.defaultDestroyTime(), Blocks.BEDROCK.getExplosionResistance())
                .noLootTable()
                .pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK)
                .sound(Blocks.STONE.defaultBlockState().getSoundType());
    }

    private static ResourceKey<Block> blockKey(String name) {
        return ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MODID, name));
    }

    private static DeferredItem<Item> registerKeyItem(String name) {
        return ITEMS.register(name, () -> new Item(new Item.Properties()
                .setId(itemKey(name))
                .stacksTo(1)));
    }

    private static ResourceKey<Item> itemKey(String name) {
        return ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, name));
    }
}
