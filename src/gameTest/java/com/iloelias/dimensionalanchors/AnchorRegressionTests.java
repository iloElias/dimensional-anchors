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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerLevelAccess;
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
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

@EventBusSubscriber(modid = DimensionalAnchors.MODID)
public final class AnchorRegressionTests {
    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(id("anchor_regressions"));
        register(event, "item_descriptions", new TestData<>(environment, Identifier.withDefaultNamespace("empty"),
                100, 0, true), ItemDescriptionTests::tooltips);
        register(event, "recover_anchor_tiers", new TestData<>(environment, Identifier.withDefaultNamespace("empty"),
                100, 0, true), AnchorRecoveryTests::recoverTiers);
        register(event, "recover_command_failures", new TestData<>(environment, Identifier.withDefaultNamespace("empty"),
                100, 0, true), AnchorRecoveryTests::permissionsAndFailures);
        register(event, "recover_from_registered_key", new TestData<>(environment, Identifier.withDefaultNamespace("empty"),
                100, 0, true), AnchorRecoveryTests::recoverFromHeldKey);
        register(event, "recover_placed_conflicts", new TestData<>(environment, Identifier.withDefaultNamespace("empty"),
                100, 0, true), AnchorRecoveryTests::placedAndConflicting);
        register(event, "recover_unloaded_record", new TestData<>(environment, Identifier.withDefaultNamespace("empty"),
                100, 0, true), AnchorRecoveryTests::unloadedRecord);
        register(event, "recover_relocated_duplicates", new TestData<>(environment, Identifier.withDefaultNamespace("empty"),
                100, 0, true), AnchorRecoveryTests::repeatedAndRelocated);
        register(event, "recover_unplaced_upgrades", new TestData<>(environment, Identifier.withDefaultNamespace("empty"),
                100, 0, true), AnchorRecoveryTests::upgradeRecovery);
        register(event, "recover_anchor_storage", new TestData<>(environment, Identifier.withDefaultNamespace("empty"),
                100, 0, true), AnchorRecoveryTests::corruptStorage);
        register(event, "movement_room_faces", new TestData<>(environment, Identifier.withDefaultNamespace("empty"),
                100, 0, true), RoomMovementTests::interiorFaces);
        register(event, "movement_departures", new TestData<>(environment, Identifier.withDefaultNamespace("empty"),
                100, 0, true), RoomMovementTests::departures);
        register(event, "movement_vanilla_death", new TestData<>(environment, Identifier.withDefaultNamespace("empty"),
                100, 0, true), RoomMovementTests::vanillaDeath);
        register(event, "movement_canceled_death", new TestData<>(environment, Identifier.withDefaultNamespace("empty"),
                100, 0, true), RoomMovementTests::canceledDeath);
        register(event, "movement_session_lifecycle", new TestData<>(environment, Identifier.withDefaultNamespace("empty"),
                100, 0, true), RoomMovementTests::lifecycle);
        register(event, "movement_invalid_exit_attempts", new TestData<>(environment, Identifier.withDefaultNamespace("empty"),
                100, 0, true), RoomMovementTests::invalidExitAttempts);
        register(event, "movement_loading_immunity", new TestData<>(environment, Identifier.withDefaultNamespace("empty"),
                100, 0, true), RoomMovementTests::loadingImmunity);
        register(event, "movement_pocket_respawn", new TestData<>(environment, Identifier.withDefaultNamespace("empty"),
                100, 0, true), RoomMovementTests::pocketRespawn);
        register(event, "reconcile_room_occupants", new TestData<>(environment, Identifier.withDefaultNamespace("empty"),
                100, 0, true), PlayerRoomReconciliationTests::occupants);
        register(event, "reconcile_inconsistent_locations", new TestData<>(environment, Identifier.withDefaultNamespace("empty"),
                100, 0, true), PlayerRoomReconciliationTests::inconsistentLocations);
        register(event, "reconcile_exterior_respawn", new TestData<>(environment, Identifier.withDefaultNamespace("empty"),
                100, 0, true), PlayerRoomReconciliationTests::exteriorRespawn);
        register(event, "reconcile_vanilla_respawn", new TestData<>(environment, Identifier.withDefaultNamespace("empty"),
                100, 0, true), PlayerRoomReconciliationTests::vanillaRespawn);
        register(event, "safe_room_entry", new TestData<>(environment, Identifier.withDefaultNamespace("empty"),
                100, 0, true), SafeTravelTests::entry);
        register(event, "safe_nearby_destination", new TestData<>(environment, Identifier.withDefaultNamespace("empty"),
                100, 0, true), SafeTravelTests::nearby);
        register(event, "safe_return_fallbacks", new TestData<>(environment, Identifier.withDefaultNamespace("empty"),
                100, 0, true), SafeTravelTests::returns);
        register(event, "keyless_exit_control", new TestData<>(environment, Identifier.withDefaultNamespace("empty"),
                100, 0, true), TravelServiceTests::controlExit);
        register(event, "rejected_exit_teleport", new TestData<>(environment, Identifier.withDefaultNamespace("empty"),
                100, 0, true), TravelServiceTests::rejectedTeleportKeepsSession);
        register(event, "canceled_reentrant_exit", new TestData<>(environment, Identifier.withDefaultNamespace("empty"),
                100, 0, true), TravelServiceTests::cancellationAndReentry);
        register(event, "exceptional_exit_teleports", new TestData<>(environment, Identifier.withDefaultNamespace("empty"),
                100, 0, true), TravelServiceTests::exceptionalExits);
        register(event, "replaced_exit_sessions", new TestData<>(environment, Identifier.withDefaultNamespace("empty"),
                100, 0, true), TravelServiceTests::replacedSessions);
        register(event, "player_travel_snapshots", new TestData<>(environment, Identifier.withDefaultNamespace("empty"),
                100, 0, true), PlayerRoomTests::travelSnapshots);
        register(event, "arrival_validation_limits", new TestData<>(environment, Identifier.withDefaultNamespace("empty"),
                100, 0, true), TravelServiceTests::arrivalLimits);
        register(event, "travel_thread_liveness", new TestData<>(environment, Identifier.withDefaultNamespace("empty"),
                100, 0, true), TravelServiceTests::travelThreadAndLiveness);
        register(event, "player_room_sessions", new TestData<>(environment, Identifier.withDefaultNamespace("empty"),
                100, 0, true), PlayerRoomTests::lifecycle);
        register(event, "player_room_invalid_data", new TestData<>(environment, Identifier.withDefaultNamespace("empty"),
                100, 0, true), PlayerRoomTests::invalidData);
        register(event, "player_room_disk", new TestData<>(environment, Identifier.withDefaultNamespace("empty"),
                100, 0, true), PlayerRoomTests::disk);
        register(event, "upgrade_recipes", new TestData<>(environment, Identifier.withDefaultNamespace("empty"),
                100, 0, true), AnchorRegressionTests::upgradeRecipes);
        register(event, "stack_placement", new TestData<>(environment, Identifier.withDefaultNamespace("empty"),
                100, 0, true), helper -> stackPlacement(helper, GameType.SURVIVAL));
        register(event, "creative_placement", new TestData<>(environment, Identifier.withDefaultNamespace("empty"),
                100, 0, true), helper -> stackPlacement(helper, GameType.CREATIVE));
        register(event, "upgraded_extraction", new TestData<>(environment, Identifier.withDefaultNamespace("empty"),
                100, 0, true), AnchorRegressionTests::upgradedExtraction);
        register(event, "key_registration", new TestData<>(environment, Identifier.withDefaultNamespace("empty"),
                100, 0, true), AnchorRegressionTests::keyRegistration);
        register(event, "key_registration_lifecycle", new TestData<>(environment, Identifier.withDefaultNamespace("empty"),
                100, 0, true), AnchorRegressionTests::keyRegistrationLifecycle);
        register(event, "key_copying", new TestData<>(environment, Identifier.withDefaultNamespace("empty"),
                100, 0, true), AnchorRegressionTests::keyCopying);
        register(event, "key_copying_rejections", new TestData<>(environment, Identifier.withDefaultNamespace("empty"),
                100, 0, true), AnchorRegressionTests::keyCopyingRejections);
        register(event, "room_geometry", new TestData<>(environment, Identifier.withDefaultNamespace("empty"),
                100, 0, true), RoomAllocationTests::geometry);
        register(event, "room_allocation", new TestData<>(environment, Identifier.withDefaultNamespace("empty"),
                100, 0, true), RoomAllocationTests::allocation);
        register(event, "room_allocation_invalid_data", new TestData<>(environment, Identifier.withDefaultNamespace("empty"),
                100, 0, true), RoomAllocationTests::invalidData);
        register(event, "room_allocation_disk", new TestData<>(environment, Identifier.withDefaultNamespace("empty"),
                100, 0, true), RoomAllocationTests::disk);
        register(event, "key_charging", new TestData<>(environment, Identifier.withDefaultNamespace("empty"),
                100, 0, true), KeyChargingTests::charging);
        register(event, "pocket_dimension", new TestData<>(environment, Identifier.withDefaultNamespace("empty"),
                100, 0, true), PocketDimensionTests::dimension);
        register(event, "room_generation", new TestData<>(environment, Identifier.withDefaultNamespace("empty"),
                100, 0, true), RoomGenerationTests::generation);
        register(event, "room_placement_limits", new TestData<>(environment, Identifier.withDefaultNamespace("empty"),
                100, 0, true), RoomGenerationTests::placementLimits);
        register(event, "room_generation_recovery", new TestData<>(environment, Identifier.withDefaultNamespace("empty"),
                100, 0, true), RoomGenerationTests::recovery);
        register(event, "room_boundary_protection", new TestData<>(environment, Identifier.withDefaultNamespace("empty"),
                100, 0, true), RoomGenerationTests::protection);
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

    private static void keyCopying(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(0, 2, 0));
        for (var anvil : List.of(Blocks.ANVIL, Blocks.CHIPPED_ANVIL, Blocks.DAMAGED_ANVIL)) {
            for (ContainerInput click : List.of(ContainerInput.PICKUP, ContainerInput.QUICK_MOVE)) {
                level.setBlockAndUpdate(pos, anvil.defaultBlockState());
                var state = level.getBlockState(pos);
                var player = helper.makeMockPlayer(GameType.SURVIVAL);
                player.experienceLevel = 10;
                var completionEffects = new java.util.concurrent.atomic.AtomicInteger();
                ContainerLevelAccess access = new ContainerLevelAccess() {
                    @Override
                    public <T> java.util.Optional<T> evaluate(
                            java.util.function.BiFunction<net.minecraft.world.level.Level, BlockPos, T> action) {
                        completionEffects.incrementAndGet();
                        return java.util.Optional.of(action.apply(level, pos));
                    }
                };
                var menu = new AnvilMenu(1, player.getInventory(), access);
                player.containerMenu = menu;
                UUID anchorId = UUID.randomUUID();
                var saved = AnchorSavedData.get(level);
                saved.put(AnchorRecord.unplaced(anchorId, AnchorTier.COPPER, true));
                var recordsBefore = saved.records();
                ItemStack original = new ItemStack(DimensionalAnchors.DIMENSIONAL_KEY.get());
                original.set(DimensionalAnchors.REGISTERED_KEY_DATA.get(), new RegisteredKeyData(anchorId));
                original.set(DataComponents.CUSTOM_NAME, Component.literal("Original key"));
                original.set(DataComponents.REPAIR_COST, 100);
                ItemStack snapshot = original.copy();
                menu.getSlot(0).set(original);
                menu.setItemName("Original key");
                for (int iteration = 0; iteration < 5; iteration++) {
                    menu.getSlot(1).set(new ItemStack(DimensionalAnchors.UNASSIGNED_DIMENSIONAL_KEY.get()));
                    helper.assertTrue(menu.getCost() == 1, "Copy cost must not escalate with repair cost or repetition");
                    helper.assertTrue(player.experienceLevel == 10 - iteration, "Preview charged experience");
                    helper.assertTrue(ItemStack.isSameItemSameComponents(menu.getSlot(2).getItem(), snapshot),
                            "Preview must preserve UUID, name, and other components");
                    int effectsBefore = completionEffects.get();
                    menu.clicked(2, 0, click, player);
                    helper.assertTrue(completionEffects.get() == effectsBefore + 1,
                            "Completed copy must emit its anvil effect exactly once");
                    helper.assertTrue(menu.getSlot(0).getItem() == original
                            && ItemStack.isSameItemSameComponents(original, snapshot), "Original key was consumed or changed");
                    helper.assertTrue(menu.getSlot(1).getItem().isEmpty(), "Copy must consume exactly one blank");
                    helper.assertTrue(menu.getSlot(2).getItem().isEmpty(), "Output must clear after copying");
                    helper.assertTrue(player.experienceLevel == 9 - iteration, "Copy must charge exactly one level");
                    helper.assertTrue(level.getBlockState(pos).equals(state), "Copy damaged the anvil");
                    ItemStack copy;
                    if (click == ContainerInput.PICKUP) {
                        copy = menu.getCarried().copy();
                        menu.setCarried(ItemStack.EMPTY);
                    } else {
                        copy = ItemStack.EMPTY;
                        for (int slot = 0; slot < 36; slot++) {
                            ItemStack item = player.getInventory().getItem(slot);
                            if (!item.isEmpty()) {
                                helper.assertTrue(copy.isEmpty(), "Shift-click produced extra items");
                                copy = item.copy();
                                player.getInventory().setItem(slot, ItemStack.EMPTY);
                            }
                        }
                    }
                    helper.assertTrue(copy.getCount() == 1 && ItemStack.isSameItemSameComponents(snapshot, copy),
                            "Collected copy lost data or duplicated");
                    helper.assertTrue(copy.getItem().isFoil(copy), "Copied key must have glint");
                    effectsBefore = completionEffects.get();
                    menu.clicked(2, 0, click, player);
                    helper.assertTrue(completionEffects.get() == effectsBefore,
                            "Repeated collection without a blank must not emit an anvil effect");
                    helper.assertTrue(player.experienceLevel == 9 - iteration && menu.getCarried().isEmpty(),
                            "Repeated collection without another blank must do nothing");
                    helper.assertTrue(saved.records().equals(recordsBefore), "Copying changed anchor lifecycle data");
                    if (iteration == 4) {
                        menu.getSlot(0).set(copy);
                        menu.setItemName("Original key");
                        menu.getSlot(1).set(new ItemStack(DimensionalAnchors.UNASSIGNED_DIMENSIONAL_KEY.get()));
                        menu.clicked(2, 0, ContainerInput.PICKUP, player);
                        helper.assertTrue(ItemStack.isSameItemSameComponents(menu.getCarried(), snapshot),
                                "A copied key must be usable as another original");
                        helper.assertTrue(player.experienceLevel == 4, "Copy from copy must still cost one level");
                    }
                }
            }
        }
    }

    private static void keyCopyingRejections(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var menu = new AnvilMenu(1, player.getInventory());
        player.containerMenu = menu;
        ItemStack linked = new ItemStack(DimensionalAnchors.DIMENSIONAL_KEY.get());
        linked.set(DimensionalAnchors.REGISTERED_KEY_DATA.get(), new RegisteredKeyData(UUID.randomUUID()));
        ItemStack blank = new ItemStack(DimensionalAnchors.UNASSIGNED_DIMENSIONAL_KEY.get());
        for (List<ItemStack> pair : List.of(
                List.of(new ItemStack(DimensionalAnchors.DIMENSIONAL_KEY.get()), blank),
                List.of(blank, linked))) {
            menu.getSlot(0).set(pair.get(0).copy());
            menu.getSlot(1).set(pair.get(1).copy());
            helper.assertTrue(menu.getSlot(2).getItem().isEmpty(),
                    "Invalid key pair produced a copy: " + pair);
        }
        // Menu slots clamp non-stackable items to one; exercise malformed counts at the event boundary.
        for (List<ItemStack> pair : List.of(List.of(linked.copyWithCount(2), blank),
                List.of(linked, blank.copyWithCount(2)))) {
            var event = new net.neoforged.neoforge.event.AnvilUpdateEvent(
                    pair.get(0), pair.get(1), null, ItemStack.EMPTY, 0, 0, player);
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event);
            helper.assertTrue(event.isCanceled(), "Malformed stacked keys must be rejected");
        }
        menu.getSlot(0).set(linked.copy());
        menu.getSlot(1).set(blank.copy());
        helper.assertTrue(!menu.getSlot(2).mayPickup(player), "Player without XP must not collect copy");
        menu.clicked(2, 0, ContainerInput.PICKUP, player);
        helper.assertTrue(menu.getCarried().isEmpty() && !menu.getSlot(1).getItem().isEmpty(),
                "Failed no-XP collection consumed a blank or awarded a key");
        menu.clicked(2, 0, ContainerInput.QUICK_MOVE, player);
        helper.assertTrue(player.getInventory().isEmpty() && !menu.getSlot(1).getItem().isEmpty(),
                "Shift-click without XP awarded a copy or consumed a blank");
        player.experienceLevel = 3;
        for (int slot = 0; slot < 36; slot++) {
            player.getInventory().setItem(slot, new ItemStack(Items.STONE, 64));
        }
        menu.clicked(2, 0, ContainerInput.QUICK_MOVE, player);
        helper.assertTrue(player.experienceLevel == 3 && !menu.getSlot(1).getItem().isEmpty()
                && !menu.getSlot(2).getItem().isEmpty(), "Full inventory shift-click charged or consumed inputs");
        menu.getSlot(1).set(ItemStack.EMPTY);
        helper.assertTrue(menu.getSlot(2).getItem().isEmpty(), "Removing blank must remove output");
        helper.assertTrue(player.experienceLevel == 3, "Cancelling preview charged XP");
        ItemStack malformedBlank = blank.copy();
        malformedBlank.set(DimensionalAnchors.REGISTERED_KEY_DATA.get(), new RegisteredKeyData(UUID.randomUUID()));
        menu.getSlot(1).set(malformedBlank);
        helper.assertTrue(menu.getSlot(2).getItem().isEmpty(), "Blank with a forged reference produced a copy");
        menu.getSlot(1).set(blank.copy());
        menu.setItemName("Copy name must not replace original name");
        helper.assertTrue(!menu.getSlot(2).getItem().has(DataComponents.CUSTOM_NAME),
                "Copying must reproduce the original, not combine with renaming");
        var ordinary = new AnvilMenu(3, player.getInventory());
        ordinary.getSlot(0).set(new ItemStack(Items.IRON_SWORD));
        ordinary.setItemName("Vanilla rename");
        helper.assertTrue(ordinary.getSlot(2).getItem().getItem() == Items.IRON_SWORD
                && ordinary.getCost() == 1, "Key hooks must not change ordinary anvil operations");
        var creative = helper.makeMockPlayer(GameType.CREATIVE);
        GameType.CREATIVE.updatePlayerAbilities(creative.getAbilities());
        var creativeMenu = new AnvilMenu(2, creative.getInventory());
        creative.containerMenu = creativeMenu;
        creativeMenu.getSlot(0).set(linked.copy());
        creativeMenu.getSlot(1).set(blank.copy());
        creativeMenu.clicked(2, 0, ContainerInput.PICKUP, creative);
        helper.assertTrue(creativeMenu.getCarried().getCount() == 1
                && creativeMenu.getSlot(0).getItem().getCount() == 1
                && creativeMenu.getSlot(1).getItem().isEmpty(), "Creative copying must retain original and consume blank");
        helper.assertTrue(creative.experienceLevel == 0, "Creative should retain vanilla XP exemption");
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

            validateInvalidUpgrade(helper, recipe, raw, tier);
        }
    }

    private static void keyRegistration(GameTestHelper helper) {
        var level = helper.getLevel();
        var saved = AnchorSavedData.get(level);
        alreadyRegisteredSound(helper, saved);
        BlockPos pos = helper.absolutePos(new BlockPos(0, 2, 0));
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        for (AnchorTier tier : AnchorTier.values()) {
            for (GameType mode : List.of(GameType.SURVIVAL, GameType.CREATIVE)) {
                for (InteractionHand hand : InteractionHand.values()) {
                    var player = helper.makeMockPlayer(mode);
                    mode.updatePlayerAbilities(player.getAbilities());
                    var otherHand = hand == InteractionHand.MAIN_HAND
                            ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
                    ItemStack untouched = new ItemStack(Items.STONE);
                    player.setItemInHand(otherHand, untouched);
                    var block = ((BlockItem) DimensionalAnchors.anchorItem(tier).get()).getBlock();
                    level.setBlockAndUpdate(pos, block.defaultBlockState());
                    UUID anchorId = UUID.randomUUID();
                    var record = AnchorRecord.unplaced(anchorId, tier, false)
                            .placedAt(GlobalPos.of(level.dimension(), pos));
                    saved.put(record);
                    ItemStack blank = new ItemStack(DimensionalAnchors.UNASSIGNED_DIMENSIONAL_KEY.get());
                    helper.assertTrue(!blank.getItem().isFoil(blank), "Blank key must not glow");
                    player.setItemInHand(hand, blank);
                    helper.assertTrue(level.getBlockState(pos).useItemOn(blank, level, player, hand, hit).consumesAction(),
                            "First blank key must register for " + tier + ", " + mode + ", " + hand);
                    ItemStack key = player.getItemInHand(hand);
                    helper.assertTrue(key.getItem().isFoil(key), "Linked key must glow");
                    ItemStack unlinked = key.copy();
                    unlinked.remove(DimensionalAnchors.REGISTERED_KEY_DATA.get());
                    helper.assertTrue(!unlinked.getItem().isFoil(unlinked), "Removing the reference must remove the key glint");
                    helper.assertTrue(key.getItem() == DimensionalAnchors.DIMENSIONAL_KEY.get()
                            && key.getCount() == 1 && key.getMaxStackSize() == 1, "Registration must replace one key");
                    helper.assertTrue(new RegisteredKeyData(anchorId).equals(
                            key.get(DimensionalAnchors.REGISTERED_KEY_DATA.get())), "Key references the wrong UUID");
                    helper.assertTrue(saved.get(anchorId).orElseThrow().equals(record.withRegistered(true)),
                            "Registration must only change the saved registration flag");
                    helper.assertTrue(player.getItemInHand(otherHand) == untouched, "Registration changed the other hand");

                    var competitor = helper.makeMockPlayer(GameType.SURVIVAL);
                    ItemStack secondBlank = new ItemStack(DimensionalAnchors.UNASSIGNED_DIMENSIONAL_KEY.get());
                    competitor.setItemInHand(hand, secondBlank);
                    helper.assertTrue(!level.getBlockState(pos).useItemOn(secondBlank, level, competitor, hand, hit)
                            .consumesAction(), "A second player must not register another blank key");
                    helper.assertTrue(competitor.getItemInHand(hand) == secondBlank
                            && !secondBlank.has(DimensionalAnchors.REGISTERED_KEY_DATA.get()),
                            "Rejected registration consumed or changed the blank key");
                    helper.assertTrue(saved.get(anchorId).orElseThrow().registered(), "Rejection reset registration");

                    var ops = RegistryOps.create(JsonOps.INSTANCE, level.registryAccess());
                    var encoded = ItemStack.CODEC.encodeStart(ops, key).getOrThrow();
                    var restored = ItemStack.CODEC.parse(ops, encoded).getOrThrow();
                    helper.assertTrue(ItemStack.isSameItemSameComponents(key, restored), "Key persistence lost data");
                    helper.assertTrue(restored.getItem().isFoil(restored), "Restored linked key must glow");
                    var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
                    try {
                        ItemStack.STREAM_CODEC.encode(buffer, key);
                        helper.assertTrue(ItemStack.isSameItemSameComponents(key, ItemStack.STREAM_CODEC.decode(buffer)),
                                "Key synchronization lost data");
                    } finally {
                        buffer.release();
                    }

                    saved.remove(anchorId);
                    helper.assertTrue(!level.getBlockState(pos).useItemOn(secondBlank, level, competitor, hand, hit)
                            .consumesAction(), "Missing saved data must not silently recreate an anchor");
                    helper.assertTrue(saved.get(anchorId).isEmpty()
                            && competitor.getItemInHand(hand) == secondBlank, "Missing-data rejection changed state");
                }
            }
        }
        var block = DimensionalAnchors.COPPER_DIMENSIONAL_ANCHOR.get();
        level.setBlockAndUpdate(pos, block.defaultBlockState());
        UUID id = UUID.randomUUID();
        var record = AnchorRecord.unplaced(id, AnchorTier.COPPER, false)
                .placedAt(GlobalPos.of(level.dimension(), pos));
        saved.put(record);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack malformed = new ItemStack(DimensionalAnchors.UNASSIGNED_DIMENSIONAL_KEY.get(), 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, malformed);
        helper.assertTrue(!level.getBlockState(pos).useItemOn(malformed, level, player, InteractionHand.MAIN_HAND, hit)
                .consumesAction(), "Malformed blank stack must not register");
        malformed.setCount(1);
        malformed.set(DimensionalAnchors.REGISTERED_KEY_DATA.get(), new RegisteredKeyData(UUID.randomUUID()));
        helper.assertTrue(!level.getBlockState(pos).useItemOn(malformed, level, player, InteractionHand.MAIN_HAND, hit)
                .consumesAction(), "Blank key with a preexisting reference must not register");
        ItemStack preview = new ItemStack(DimensionalAnchors.DIMENSIONAL_KEY.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, preview);
        level.getBlockState(pos).useItemOn(preview, level, player, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(saved.get(id).orElseThrow().equals(record), "Registered-key preview changed registration");
        helper.assertTrue(!preview.has(DimensionalAnchors.REGISTERED_KEY_DATA.get()), "Preview key acquired identity");
        helper.assertTrue(!preview.getItem().isFoil(preview), "Unlinked creative preview key must not glow");
    }

    private static void alreadyRegisteredSound(GameTestHelper helper, AnchorSavedData saved) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(20, 2, 20));
        level.setBlockAndUpdate(pos, DimensionalAnchors.COPPER_DIMENSIONAL_ANCHOR.get().defaultBlockState());
        var record = AnchorRecord.unplaced(UUID.randomUUID(), AnchorTier.COPPER, true)
                .placedAt(GlobalPos.of(level.dimension(), pos));
        saved.put(record);

        var sounds = new ArrayList<PlayLevelSoundEvent.AtPosition>();
        Consumer<PlayLevelSoundEvent.AtPosition> listener = event -> {
            if (event.getLevel() == level && event.getSound() != null
                    && event.getSound().value() == SoundEvents.VAULT_REJECT_REWARDED_PLAYER) {
                sounds.add(event);
            }
        };
        NeoForge.EVENT_BUS.addListener(listener);
        try {
            var player = helper.makeMockPlayer(GameType.SURVIVAL);
            var blank = new ItemStack(DimensionalAnchors.UNASSIGNED_DIMENSIONAL_KEY.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, blank);
            var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
            helper.assertTrue(!level.getBlockState(pos)
                    .useItemOn(blank, level, player, InteractionHand.MAIN_HAND, hit).consumesAction(),
                    "A second registration attempt must be rejected");
            helper.assertTrue(sounds.size() == 1,
                    "Rejected repeated registration must play exactly one rejection sound");
        } finally {
            NeoForge.EVENT_BUS.unregister(listener);
        }
    }

    private static void keyRegistrationLifecycle(GameTestHelper helper) {
        var level = helper.getLevel();
        var saved = AnchorSavedData.get(level);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos pos = helper.absolutePos(new BlockPos(0, 2, 0));
        level.setBlockAndUpdate(pos, DimensionalAnchors.COPPER_DIMENSIONAL_ANCHOR.get().defaultBlockState());
        var block = level.getBlockState(pos).getBlock();
        block.setPlacedBy(level, pos, level.getBlockState(pos), player,
                new ItemStack(DimensionalAnchors.COPPER_DIMENSIONAL_ANCHOR_ITEM.get()));
        UUID id = saved.findAt(GlobalPos.of(level.dimension(), pos)).orElseThrow().anchorId();
        ItemStack blank = new ItemStack(DimensionalAnchors.UNASSIGNED_DIMENSIONAL_KEY.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, blank);
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        helper.assertTrue(level.getBlockState(pos).useItemOn(blank, level, player, InteractionHand.MAIN_HAND, hit)
                .consumesAction(), "Lifecycle registration failed");
        ItemStack key = player.getItemInHand(InteractionHand.MAIN_HAND).copy();
        block.playerDestroy(level, player, pos, level.getBlockState(pos), null, new ItemStack(Items.IRON_PICKAXE));
        var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1));
        helper.assertTrue(drops.size() == 1, "Registered anchor extraction produced the wrong number of drops");
        ItemStack anchor = drops.getFirst().getItem().copy();
        drops.forEach(ItemEntity::discard);
        helper.assertTrue(new AnchorItemData(id, AnchorTier.COPPER, true).equals(
                anchor.get(DimensionalAnchors.ANCHOR_ITEM_DATA.get())), "Registered anchor drop lost its data");
        anchor = upgrade(helper, recipe(helper, AnchorTier.IRON), anchor, AnchorTier.IRON);

        var ops = RegistryOps.create(JsonOps.INSTANCE, level.registryAccess());
        var restored = AnchorSavedData.TYPE.codec().parse(ops,
                AnchorSavedData.TYPE.codec().encodeStart(ops, saved).getOrThrow()).getOrThrow();
        saved.put(restored.get(id).orElseThrow());
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        BlockPos moved = pos.above(3);
        level.setBlockAndUpdate(moved.below(), Blocks.STONE.defaultBlockState());
        var context = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, anchor,
                new BlockHitResult(Vec3.atBottomCenterOf(moved), Direction.UP, moved.below(), false));
        helper.assertTrue(((BlockItem) anchor.getItem()).place(context).consumesAction(), "Registered relocation failed");
        ItemStack second = new ItemStack(DimensionalAnchors.UNASSIGNED_DIMENSIONAL_KEY.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, second);
        helper.assertTrue(!level.getBlockState(moved).useItemOn(second, level, player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(moved), Direction.UP, moved, false)).consumesAction(),
                "Reload, upgrade or relocation allowed a second registration");
        helper.assertTrue(saved.get(id).orElseThrow().tier() == AnchorTier.IRON
                && saved.get(id).orElseThrow().registered(), "Relocated registration state is incorrect");
        helper.assertTrue(new RegisteredKeyData(id).equals(key.get(DimensionalAnchors.REGISTERED_KEY_DATA.get())),
                "Existing key no longer references the upgraded anchor");
    }

    private static void validateInvalidUpgrade(GameTestHelper helper, Recipe<?> recipe, ItemStack raw, AnchorTier tier) {
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
