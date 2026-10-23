package com.iloelias.dimensionalanchors;

import java.util.List;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;

final class ItemDescriptionTests {
    static void tooltips(GameTestHelper helper) {
        var shift = new TooltipFlag() {
            @Override
            public boolean isAdvanced() {
                return false;
            }

            @Override
            public boolean isCreative() {
                return false;
            }

            @Override
            public boolean hasShiftDown() {
                return true;
            }
        };
        UUID id = UUID.randomUUID();
        var context = Item.TooltipContext.of(helper.getLevel());
        for (var tier : AnchorTier.values()) {
            var stack = new ItemStack(DimensionalAnchors.anchorItem(tier).get());
            int size = new RoomGeometry(0, tier).interiorSize();
            var normal = stack.getTooltipLines(context, null, TooltipFlag.NORMAL);
            helper.assertTrue(normal.get(1).getString().equals(
                    "Provides a " + size + "x" + size + "x" + size + " dimensional space.")
                    && normal.get(1).getStyle().getColor().equals(
                            net.minecraft.network.chat.TextColor.fromLegacyFormat(ChatFormatting.GRAY)),
                    "Wrong anchor description or color: " + tier);
            helper.assertTrue(stack.getTooltipLines(context, null, shift).stream()
                    .noneMatch(line -> line.getString().contains("UUID")), "Pristine anchor invented a UUID");
            stack.set(DimensionalAnchors.ANCHOR_ITEM_DATA.get(), new AnchorItemData(id, tier, true));
            checkUuid(helper, stack, shift, id);
        }
        var blank = new ItemStack(DimensionalAnchors.UNASSIGNED_DIMENSIONAL_KEY.get());
        helper.assertTrue(blank.getTooltipLines(context, null, TooltipFlag.NORMAL).get(1).getString()
                .equals("Use on an unregistered anchor to link this key."), "Missing blank-key description");
        var key = new ItemStack(DimensionalAnchors.DIMENSIONAL_KEY.get());
        helper.assertTrue(key.getTooltipLines(context, null, shift).get(1).getString()
                .equals("This key is not linked to an anchor."), "Unlinked preview claims to grant travel");
        key.set(DimensionalAnchors.REGISTERED_KEY_DATA.get(), new RegisteredKeyData(id));
        var normal = key.getTooltipLines(context, null, TooltipFlag.NORMAL);
        helper.assertTrue(normal.subList(1, 4).stream().map(line -> line.getString()).toList().equals(List.of(
                "Hold use for 3 seconds to travel.", "Use inside its linked room to return.",
                "Room entry is not enabled yet.")), "Missing or incorrectly ordered registered-key descriptions");
        checkUuid(helper, key, shift, id);
        var vanilla = new ItemStack(Items.STONE);
        helper.assertTrue(vanilla.getTooltipLines(context, null, TooltipFlag.NORMAL).size() == 1,
                "Mod descriptions changed a vanilla item");
    }

    private static void checkUuid(GameTestHelper helper, ItemStack stack, TooltipFlag shift, UUID id) {
        var context = Item.TooltipContext.of(helper.getLevel());
        for (var flags : new TooltipFlag[]{TooltipFlag.NORMAL, TooltipFlag.ADVANCED}) {
            helper.assertTrue(stack.getTooltipLines(context, null, flags).stream()
                    .noneMatch(line -> line.getString().contains(id.toString())),
                    "UUID displayed without Shift");
        }
        helper.assertTrue(stack.getTooltipLines(context, null, shift).stream()
                .filter(line -> line.getString().equals("Anchor UUID: " + id)).count() == 1,
                "Shift did not show exactly one matching UUID");
    }
}
