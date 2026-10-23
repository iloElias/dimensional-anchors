package com.iloelias.dimensionalanchors;

import java.util.UUID;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

final class AnchorRecoveryCommand {
    private AnchorRecoveryCommand() {
    }

    static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("dimensionalanchors")
                .requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
                .then(Commands.literal("recover")
                        .executes(context -> recoverHeld(context.getSource()))
                        .then(Commands.argument("uuid", UuidArgument.uuid())
                                .executes(context -> recover(context.getSource(),
                                        UuidArgument.getUuid(context, "uuid"), false)))));
    }

    private static int recoverHeld(CommandSourceStack source) throws CommandSyntaxException {
        var player = source.getPlayerOrException();
        var held = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!held.is(DimensionalAnchors.DIMENSIONAL_KEY.get()) || held.getCount() != 1) {
            source.sendFailure(Component.translatable("commands.dimensionalanchors.recover.hold_key"));
            return 0;
        }
        RegisteredKeyData data = held.get(DimensionalAnchors.REGISTERED_KEY_DATA.get());
        if (data == null) {
            source.sendFailure(Component.translatable("commands.dimensionalanchors.recover.hold_registered_key"));
            return 0;
        }
        return recover(source, data.anchorId(), false);
    }

    private static int recover(CommandSourceStack source, UUID id, boolean replaceHeld) throws CommandSyntaxException {
        var player = source.getPlayerOrException();
        var level = source.getLevel();
        if (!level.getServer().isSameThread()) {
            throw new IllegalStateException("Anchor recovery must run on the server thread");
        }
        try {
            var record = AnchorIdentity.find(level, id).orElse(null);
            if (record == null) {
                return fail(source, "unknown", id);
            }
            if (record.placed()) {
                var position = record.exteriorPosition().orElseThrow();
                var anchorLevel = source.getServer().getLevel(position.dimension());
                if (anchorLevel == null) {
                    return fail(source, "unresolved", id);
                }
                // Load the recorded chunk before deciding that a placed record is stale.
                anchorLevel.getChunkAt(position.pos());
                if (anchorLevel.getBlockState(position.pos()).getBlock() instanceof AnchorBlock) {
                    return fail(source, "placed", id);
                }
                if (AnchorSavedData.get(anchorLevel).records().values().stream()
                        .anyMatch(other -> !other.anchorId().equals(id)
                                && other.exteriorPosition().equals(record.exteriorPosition()))) {
                    return fail(source, "unresolved", id);
                }
            }
            int slot = replaceHeld ? -1 : player.getInventory().getFreeSlot();
            if (!replaceHeld && slot < 0) {
                return fail(source, "inventory_full", id);
            }
            var recovered = new ItemStack(DimensionalAnchors.anchorItem(record.tier()).get());
            recovered.set(DimensionalAnchors.ANCHOR_ITEM_DATA.get(),
                    new AnchorItemData(id, record.tier(), record.registered()));
            AnchorIdentity.store(level, record.removedFromWorld());
            if (replaceHeld) {
                player.setItemInHand(InteractionHand.MAIN_HAND, recovered);
            } else {
                player.getInventory().setItem(slot, recovered);
            }
            player.containerMenu.broadcastChanges();
            DimensionalAnchors.LOGGER.info("Administrator {} recovered anchor {} at tier {}",
                    player.getUUID(), id, record.tier());
            source.sendSuccess(() -> Component.translatable("commands.dimensionalanchors.recover.success",
                    id.toString(), record.tier().getSerializedName()), true);
            return 1;
        } catch (IllegalStateException exception) {
            DimensionalAnchors.LOGGER.error("Cannot recover anchor {} for administrator {}",
                    id, player.getUUID(), exception);
            return fail(source, "unresolved", id);
        }
    }

    private static int fail(CommandSourceStack source, String reason, UUID id) {
        source.sendFailure(Component.translatable("commands.dimensionalanchors.recover." + reason, id.toString()));
        return 0;
    }
}
