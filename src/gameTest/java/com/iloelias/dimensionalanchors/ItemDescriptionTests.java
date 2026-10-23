package com.iloelias.dimensionalanchors;

import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;

final class ItemDescriptionTests {
    static void tooltips(GameTestHelper helper) {
        UUID id = UUID.randomUUID();
        var context = Item.TooltipContext.of(helper.getLevel());
        for (var tier : AnchorTier.values()) {
            var stack = new ItemStack(DimensionalAnchors.anchorItem(tier).get());
            helper.assertTrue(stack.getTooltipLines(context, null, TooltipFlag.NORMAL).size() == 1,
                    "Pristine anchor gained an extra tooltip line");
            stack.set(DimensionalAnchors.ANCHOR_ITEM_DATA.get(), new AnchorItemData(id, tier, true));
            checkUuid(helper, stack, id);
        }
        var blank = new ItemStack(DimensionalAnchors.UNASSIGNED_DIMENSIONAL_KEY.get());
        helper.assertTrue(blank.getTooltipLines(context, null, TooltipFlag.NORMAL).size() == 1,
                "Blank key gained an extra tooltip line");
        var key = new ItemStack(DimensionalAnchors.DIMENSIONAL_KEY.get());
        helper.assertTrue(key.getTooltipLines(context, null, TooltipFlag.NORMAL).size() == 1,
                "Unlinked key gained an extra tooltip line");
        key.set(DimensionalAnchors.REGISTERED_KEY_DATA.get(), new RegisteredKeyData(id));
        checkUuid(helper, key, id);
        var vanilla = new ItemStack(Items.STONE);
        helper.assertTrue(vanilla.getTooltipLines(context, null, TooltipFlag.NORMAL).size() == 1,
                "Mod descriptions changed a vanilla item");
    }

    private static void checkUuid(GameTestHelper helper, ItemStack stack, UUID id) {
        var context = Item.TooltipContext.of(helper.getLevel());
        for (var flags : new TooltipFlag[]{TooltipFlag.NORMAL, TooltipFlag.ADVANCED}) {
            var lines = stack.getTooltipLines(context, null, flags);
            helper.assertTrue(lines.size() == 2 && lines.get(1).getString().equals("UUID: " + id)
                    && TextColor.fromLegacyFormat(ChatFormatting.DARK_GRAY).equals(lines.get(1).getStyle().getColor()),
                    "Tooltip did not show only the UUID without requiring Shift");
        }
    }
}
