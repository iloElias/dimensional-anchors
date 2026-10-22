package com.iloelias.dimensionalanchors;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
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
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredBlock<Block> COPPER_DIMENSIONAL_ANCHOR =
            registerAnchor("copper_dimensional_anchor", Blocks.COPPER_BLOCK, Blocks.COPPER_ORE);
    public static final DeferredBlock<Block> IRON_DIMENSIONAL_ANCHOR =
            registerAnchor("iron_dimensional_anchor", Blocks.IRON_BLOCK, Blocks.IRON_ORE);
    public static final DeferredBlock<Block> GOLD_DIMENSIONAL_ANCHOR =
            registerAnchor("gold_dimensional_anchor", Blocks.GOLD_BLOCK, Blocks.GOLD_ORE);
    public static final DeferredBlock<Block> DIAMOND_DIMENSIONAL_ANCHOR =
            registerAnchor("diamond_dimensional_anchor", Blocks.DIAMOND_BLOCK, Blocks.DIAMOND_ORE);
    public static final DeferredBlock<Block> NETHERITE_DIMENSIONAL_ANCHOR =
            registerAnchor("netherite_dimensional_anchor", Blocks.NETHERITE_BLOCK, Blocks.ANCIENT_DEBRIS);

    public static final DeferredBlock<Block> DIMENSIONAL_LIMITER_BLOCK =
            registerUtilityBlock("dimensional_limiter_block");
    public static final DeferredBlock<Block> DIMENSIONAL_EXIT_BLOCK =
            registerUtilityBlock("dimensional_exit_block");

    public static final DeferredItem<BlockItem> COPPER_DIMENSIONAL_ANCHOR_ITEM =
            ITEMS.registerSimpleBlockItem("copper_dimensional_anchor", COPPER_DIMENSIONAL_ANCHOR);
    public static final DeferredItem<BlockItem> IRON_DIMENSIONAL_ANCHOR_ITEM =
            ITEMS.registerSimpleBlockItem("iron_dimensional_anchor", IRON_DIMENSIONAL_ANCHOR);
    public static final DeferredItem<BlockItem> GOLD_DIMENSIONAL_ANCHOR_ITEM =
            ITEMS.registerSimpleBlockItem("gold_dimensional_anchor", GOLD_DIMENSIONAL_ANCHOR);
    public static final DeferredItem<BlockItem> DIAMOND_DIMENSIONAL_ANCHOR_ITEM =
            ITEMS.registerSimpleBlockItem("diamond_dimensional_anchor", DIAMOND_DIMENSIONAL_ANCHOR);
    public static final DeferredItem<BlockItem> NETHERITE_DIMENSIONAL_ANCHOR_ITEM =
            ITEMS.registerSimpleBlockItem("netherite_dimensional_anchor", NETHERITE_DIMENSIONAL_ANCHOR);
    public static final DeferredItem<BlockItem> DIMENSIONAL_LIMITER_BLOCK_ITEM =
            ITEMS.registerSimpleBlockItem("dimensional_limiter_block", DIMENSIONAL_LIMITER_BLOCK);
    public static final DeferredItem<BlockItem> DIMENSIONAL_EXIT_BLOCK_ITEM =
            ITEMS.registerSimpleBlockItem("dimensional_exit_block", DIMENSIONAL_EXIT_BLOCK);

    public static final DeferredItem<Item> DIMENSIONAL_CATALYST =
            ITEMS.registerSimpleItem("dimensional_catalyst");
    public static final DeferredItem<Item> DIMENSIONAL_UPGRADE_TEMPLATE =
            ITEMS.registerSimpleItem("dimensional_upgrade_template");
    public static final DeferredItem<Item> DIMENSIONAL_KEY =
            ITEMS.registerSimpleItem("dimensional_key");
    public static final DeferredItem<Item> UNASSIGNED_DIMENSIONAL_KEY =
            ITEMS.registerSimpleItem("unassigned_dimensional_key");

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
        CREATIVE_MODE_TABS.register(modEventBus);
    }

    private static DeferredBlock<Block> registerAnchor(
            String name, Block soundReference, Block explosionResistanceReference) {
        return BLOCKS.register(name, () -> new Block(BlockBehaviour.Properties.of()
                .setId(blockKey(name))
                .mapColor(MapColor.METAL)
                .strength(soundReference.defaultDestroyTime(), explosionResistanceReference.getExplosionResistance())
                .requiresCorrectToolForDrops()
                .sound(soundReference.defaultBlockState().getSoundType())));
    }

    private static DeferredBlock<Block> registerUtilityBlock(String name) {
        return BLOCKS.register(name, () -> new Block(BlockBehaviour.Properties.of()
                .setId(blockKey(name))
                .mapColor(MapColor.STONE)
                .strength(Blocks.BEDROCK.defaultDestroyTime(), Blocks.BEDROCK.getExplosionResistance())
                .sound(Blocks.STONE.defaultBlockState().getSoundType())));
    }

    private static ResourceKey<Block> blockKey(String name) {
        return ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MODID, name));
    }
}
